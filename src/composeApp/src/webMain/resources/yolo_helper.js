// Biến toàn cục lưu trữ mô hình AI
window.yoloModel = null;

// Hàm khởi tạo mô hình TensorFlow.js
window.initYolo = async function() {
    try {
        console.log("Đang tải AI Model TensorFlow.js...");
        window.yoloModel = await tf.loadGraphModel('best_web_model/model.json');
        console.log("🔥 AI Model đã sẵn sàng để hoạt động!");
    } catch (error) {
        console.error("Lỗi khi tải mô hình AI:", error);
    }
};

// Hàm xử lý ảnh và dự đoán kết quả
window.detectBugsJS = async function(sourceElement) { // Nhận trực tiếp HTMLVideoElement hoặc HTMLCanvasElement
    if (!window.yoloModel) {
        console.warn("Mô hình chưa tải xong, vui lòng thử lại sau.");
        return JSON.stringify([]);
    }

    try {
        const INPUT_SIZE = 896;

        // 1. TỐI ƯU FPS VÀ BỘ NHỚ: Dùng tf.tidy() để tự động dọn dẹp các tensor trung gian
        // (từ resizeBilinear, div, expandDims) tránh tràn RAM/VRAM
        const tensor = tf.tidy(() => {
            return tf.browser.fromPixels(sourceElement)
                .resizeBilinear([INPUT_SIZE, INPUT_SIZE])
                .div(255.0)
                .expandDims(0);
        });

        // 2. Chạy dự đoán
        const predictions = await window.yoloModel.executeAsync(tensor);

        // Chuyển đổi ma trận YOLO - Dùng tf.tidy() để dọn dẹp tensor trung gian từ hàm squeeze()
        const transposed = tf.tidy(() => {
            let output = Array.isArray(predictions) ? predictions[0] : predictions;
            const shape = output.shape;
            return (shape[1] < shape[2]) ? output.squeeze().transpose() : output.squeeze();
        });

        const data = await transposed.array();

        // Giải phóng triệt để bộ nhớ RAM cho các tensor còn lại
        tf.dispose([tensor, predictions, transposed]);

        const boxes = [];
        const scores = [];
        const classIndices = [];

        const CONFIDENCE_THRESHOLD = 0.25;
        const numClasses = data[0].length - 4;

        // 3. Trích xuất tọa độ Bounding Box
        for (let i = 0; i < data.length; i++) {
            const row = data[i];

            let maxScore = 0;
            let classIndex = -1;
            for (let j = 0; j < numClasses; j++) {
                if (row[4 + j] > maxScore) {
                    maxScore = row[4 + j];
                    classIndex = j;
                }
            }

            if (maxScore >= CONFIDENCE_THRESHOLD) {
                let [xc, yc, w, h] = row.slice(0, 4);

                if (xc > 1.5 || yc > 1.5) {
                    xc /= INPUT_SIZE; yc /= INPUT_SIZE;
                    w /= INPUT_SIZE; h /= INPUT_SIZE;
                }

                boxes.push([yc - h / 2, xc - w / 2, yc + h / 2, xc + w / 2]);
                scores.push(maxScore);
                classIndices.push(classIndex);
            }
        }

        if (boxes.length === 0) return JSON.stringify([]);

        // 4. Áp dụng Non-Max Suppression (NMS)
        const boxesTensor = tf.tensor2d(boxes);
        const scoresTensor = tf.tensor1d(scores);

        const nmsIndices = await tf.image.nonMaxSuppressionAsync(
            boxesTensor, scoresTensor, 50, 0.45, CONFIDENCE_THRESHOLD
        );

        const selectedIndices = await nmsIndices.array();
        tf.dispose([boxesTensor, scoresTensor, nmsIndices]);

        // 5. Trả kết quả định dạng JSON
        const finalResults = selectedIndices.map(i => {
            const [yMin, xMin, yMax, xMax] = boxes[i];
            return {
                x: xMin, y: yMin, width: xMax - xMin, height: yMax - yMin,
                label: classIndices[i].toString(), confidence: scores[i]
            };
        });

        return JSON.stringify(finalResults);

    } catch (error) {
        console.error("Lỗi trong quá trình dự đoán của mô hình:", error);
        return JSON.stringify([]);
    }
};
window.initYolo();