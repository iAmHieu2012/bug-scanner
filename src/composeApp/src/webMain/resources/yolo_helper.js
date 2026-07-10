const TFJS_VERSION = "4.22.0";
const TFJS_URL = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@${TFJS_VERSION}/dist/tf.min.js`;
const TFJS_WASM_URL = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs-backend-wasm@${TFJS_VERSION}/dist/tf-backend-wasm.min.js`;
const TFJS_WASM_PATH = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs-backend-wasm@${TFJS_VERSION}/dist/`;
const TFJS_WEBGPU_URL = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs-backend-webgpu@${TFJS_VERSION}/dist/tf-backend-webgpu.min.js`;

window.yoloModel = null;
window.litertCore = null;
window.yoloRuntime = null;
window.yoloInitPromise = null;

function loadScriptOnce(id, src) {
    if (document.getElementById(id)) return Promise.resolve();
    return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.id = id;
        script.src = src;
        script.async = true;
        script.onload = resolve;
        script.onerror = () => reject(new Error(`Không thể tải ${src}`));
        document.head.appendChild(script);
    });
}

async function initializeYoloRuntime() {
    try {
        await loadScriptOnce("bugscanner-tfjs", TFJS_URL);
        await loadScriptOnce("bugscanner-tfjs-webgpu", TFJS_WEBGPU_URL);

        let backend = "none";
        let liveDetectionSupported = false;
        try {
            await tf.setBackend("webgpu");
            await tf.ready();
            if (tf.getBackend() === "webgpu") {
                backend = "webgpu";
                liveDetectionSupported = true;
            }
        } catch (error) {
            console.warn("WebGPU backend không khả dụng, chuyển sang WASM.", error);
        }

        if (backend !== "webgpu") {
            await loadScriptOnce("bugscanner-tfjs-wasm", TFJS_WASM_URL);
            if (tf.wasm?.setWasmPaths) tf.wasm.setWasmPaths(TFJS_WASM_PATH);
            await tf.setBackend("wasm");
            await tf.ready();
            if (tf.getBackend() !== "wasm") {
                throw new Error("Không có backend TensorFlow.js phù hợp.");
            }
            backend = "wasm";
        }

        // Import LiteRT JS (chỉ dùng core, bỏ tfjs-interop để tránh lỗi khởi tạo kép)
        const litertCore = await import('https://esm.sh/@litertjs/core');
        
        await litertCore.loadLiteRt('https://cdn.jsdelivr.net/npm/@litertjs/core/wasm/');
        
        window.yoloModel = await litertCore.loadAndCompile("model.tflite", {
            accelerator: backend === "webgpu" ? "webgpu" : "wasm"
        });
        window.litertCore = litertCore;

        window.yoloRuntime = { ready: true, backend, liveDetectionSupported, error: null };
        console.log(`BugScanner AI sẵn sàng với LiteRT.js (backend: ${backend}).`);
        return window.yoloRuntime;
    } catch (error) {
        console.error("Lỗi khi tải mô hình AI:", error);
        window.yoloRuntime = {
            ready: false,
            backend: "none",
            liveDetectionSupported: false,
            error: error?.message || "Không thể tải mô hình AI."
        };
        return window.yoloRuntime;
    }
}

window.initYolo = async function() {
    if (!window.yoloInitPromise) window.yoloInitPromise = initializeYoloRuntime();
    return JSON.stringify(await window.yoloInitPromise);
};

window.detectBugsJS = async function(sourceElement) {
    if (!window.yoloModel || !window.yoloRuntime?.ready) return JSON.stringify([]);

    try {
        const INPUT_SIZE = 896;
        const tensor = tf.tidy(() => tf.browser.fromPixels(sourceElement)
            .resizeBilinear([INPUT_SIZE, INPUT_SIZE])
            .div(255.0)
            .expandDims(0)
            .transpose([0, 3, 1, 2])); // Chuyển sang NCHW (1x3x896x896) cho LiteRT mới

        // Trích xuất mảng Float32Array từ TFJS Tensor
        const inputFloatArray = await tensor.data();
        
        // Tạo Tensor của LiteRT.js và chạy mô hình (Không thông qua interop)
        const litertTensor = new window.litertCore.Tensor(inputFloatArray, [1, 3, INPUT_SIZE, INPUT_SIZE]);
        const outputs = await window.yoloModel.run([litertTensor]);
        
        // Giải phóng tensor đầu vào của LiteRT
        litertTensor.delete();
        
        // Lấy kết quả đầu ra
        const output = Array.isArray(outputs) ? outputs[0] : Object.values(outputs)[0];
        const outputData = await output.data();
        
        // Cố gắng lấy shape, nếu API chưa hoàn thiện thì tự tính toán
        const outputShape = output.shape || [1, outputData.length / 16464, 16464];
        output.delete();
        
        // Đưa mảng Float32Array trở lại môi trường TFJS để dùng các hàm tính toán của TFJS
        const tfjsOutput = tf.tensor(outputData, outputShape);

        const transposed = tf.tidy(() => {
            return tfjsOutput.shape[1] < tfjsOutput.shape[2] ? tfjsOutput.squeeze().transpose() : tfjsOutput.squeeze();
        });
        const data = await transposed.array();
        
        // Giải phóng bộ nhớ TFJS
        tf.dispose([tensor, tfjsOutput, transposed]);

        const boxes = [];
        const scores = [];
        const classIndices = [];
        const threshold = 0.25;
        const numClasses = data[0].length - 4;

        for (const row of data) {
            let maxScore = 0;
            let classIndex = -1;
            for (let j = 0; j < numClasses; j++) {
                if (row[4 + j] > maxScore) {
                    maxScore = row[4 + j];
                    classIndex = j;
                }
            }
            if (maxScore < threshold) continue;
            let [xc, yc, w, h] = row.slice(0, 4);
            if (xc > 1.5 || yc > 1.5) {
                xc /= INPUT_SIZE; yc /= INPUT_SIZE; w /= INPUT_SIZE; h /= INPUT_SIZE;
            }
            boxes.push([yc - h / 2, xc - w / 2, yc + h / 2, xc + w / 2]);
            scores.push(maxScore);
            classIndices.push(classIndex);
        }

        if (boxes.length === 0) return JSON.stringify([]);
        const boxesTensor = tf.tensor2d(boxes);
        const scoresTensor = tf.tensor1d(scores);
        const nmsIndices = await tf.image.nonMaxSuppressionAsync(boxesTensor, scoresTensor, 50, 0.45, threshold);
        const selectedIndices = await nmsIndices.array();
        tf.dispose([boxesTensor, scoresTensor, nmsIndices]);

        return JSON.stringify(selectedIndices.map(i => {
            const [yMin, xMin, yMax, xMax] = boxes[i];
            return { x: xMin, y: yMin, width: xMax - xMin, height: yMax - yMin, label: classIndices[i].toString(), confidence: scores[i] };
        }));
    } catch (error) {
        console.error("Lỗi trong quá trình dự đoán:", error);
        return JSON.stringify([]);
    }
};
