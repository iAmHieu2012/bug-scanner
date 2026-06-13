const TFJS_VERSION = "4.22.0";
const TFJS_URL = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@${TFJS_VERSION}/dist/tf.min.js`;
const TFJS_WASM_URL = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs-backend-wasm@${TFJS_VERSION}/dist/tf-backend-wasm.min.js`;
const TFJS_WASM_PATH = `https://cdn.jsdelivr.net/npm/@tensorflow/tfjs-backend-wasm@${TFJS_VERSION}/dist/`;

window.yoloModel = null;
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

        let backend = "none";
        let liveDetectionSupported = false;
        try {
            const webGlReady = await tf.setBackend("webgl");
            await tf.ready();
            if (webGlReady && tf.getBackend() === "webgl") {
                backend = "webgl";
                liveDetectionSupported = true;
            }
        } catch (error) {
            console.warn("WebGL backend không khả dụng, chuyển sang WASM.", error);
        }

        if (backend !== "webgl") {
            await loadScriptOnce("bugscanner-tfjs-wasm", TFJS_WASM_URL);
            if (tf.wasm?.setWasmPaths) tf.wasm.setWasmPaths(TFJS_WASM_PATH);
            const wasmReady = await tf.setBackend("wasm");
            await tf.ready();
            if (!wasmReady || tf.getBackend() !== "wasm") {
                throw new Error("Không có backend TensorFlow.js phù hợp.");
            }
            backend = "wasm";
        }

        window.yoloModel = await tf.loadGraphModel("best_web_model/model.json");
        window.yoloRuntime = { ready: true, backend, liveDetectionSupported, error: null };
        console.log(`BugScanner AI sẵn sàng với backend ${backend}.`);
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
            .expandDims(0));
        const predictions = window.yoloModel.execute(tensor);
        const transposed = tf.tidy(() => {
            const output = Array.isArray(predictions) ? predictions[0] : predictions;
            return output.shape[1] < output.shape[2] ? output.squeeze().transpose() : output.squeeze();
        });
        const data = await transposed.array();
        tf.dispose([tensor, predictions, transposed]);

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
