// Biến toàn cục lưu trữ mô hình AI trong bộ nhớ của trình duyệt
window.yoloModel = null;

/**
 * Hàm khởi tạo: Tải mô hình YOLO định dạng TensorFlow.js (GraphModel) từ máy chủ.
 * Cần được gọi 1 lần duy nhất khi ứng dụng Web bắt đầu khởi chạy.
 */
window.initYolo = async function() {
    try {
        console.log("Đang tải AI Model TensorFlow.js...");
        window.yoloModel = await tf.loadGraphModel('best_web_model/model.json');
        console.log("🔥 AI Model đã sẵn sàng để hoạt động!");
        return true;
    } catch (error) {
        console.error("Lỗi khi tải mô hình AI:", error);
        return false;
    }
};

/**
 * Hàm xử lý ảnh và dự đoán kết quả bằng WebGL GPU.
 * Thiết kế theo luồng xử lý Tensor để tối ưu hiệu suất bộ nhớ (chống rò rỉ RAM).
 *
 * @param {HTMLVideoElement|HTMLImageElement} sourceElement Thẻ HTML chứa hình ảnh đầu vào.
 * @returns {Promise<string>} Chuỗi JSON chứa mảng các Bounding Box kết quả.
 */
window.detectBugsJS = async function(sourceElement) {
    if (!window.yoloModel) {
        console.warn("Mô hình chưa tải xong, vui lòng thử lại sau.");
        return JSON.stringify([]);
    }

    try {
        const INPUT_SIZE = 896;

        // 1. TIỀN XỬ LÝ (PRE-PROCESSING):
        // Dùng tf.tidy() để tự động dọn dẹp các tensor trung gian tạo ra trong quá trình
        // (từ resizeBilinear, div, expandDims) nhằm tránh tràn RAM/VRAM.
        const tensor = tf.tidy(() => {
            return tf.browser.fromPixels(sourceElement)
                .resizeBilinear([INPUT_SIZE, INPUT_SIZE]) // Cố định kích thước đầu vào theo mô hình
                .div(255.0) // Chuẩn hóa (Normalize) giá trị pixel từ [0..255] về [0..1]
                .expandDims(0); // Thêm chiều batch (1, 896, 896, 3)
        });

        // 2. CHẠY DỰ ĐOÁN (INFERENCE)
        const predictions = await window.yoloModel.executeAsync(tensor);

        // 3. ĐIỀU CHỈNH MA TRẬN ĐẦU RA (POST-PROCESSING)
        // Chuyển đổi ma trận YOLO - Dùng tf.tidy() để dọn dẹp tensor trung gian từ hàm squeeze/transpose.
        const transposed = tf.tidy(() => {
            let output = Array.isArray(predictions) ? predictions[0] : predictions;
            const shape = output.shape;
            // Xoay ma trận nếu số lượng class (cột) bé hơn số lượng bounding box (hàng)
            return (shape[1] < shape[2]) ? output.squeeze().transpose() : output.squeeze();
        });

        // Kéo dữ liệu từ GPU (Tensor) về mảng JavaScript thông thường (CPU)
        const data = await transposed.array();

        // Giải phóng triệt để bộ nhớ RAM cho các tensor chính không còn dùng
        tf.dispose([tensor, predictions, transposed]);

        const boxes = [];
        const scores = [];
        const classIndices = [];

        const CONFIDENCE_THRESHOLD = 0.25;
        const numClasses = data[0].length - 4;

        // 4. TRÍCH XUẤT TỌA ĐỘ VÀ ĐIỂM SỐ
        for (let i = 0; i < data.length; i++) {
            const row = data[i];

            let maxScore = 0;
            let classIndex = -1;
            // Tìm Class có điểm cao nhất trong 102 nhãn
            for (let j = 0; j < numClasses; j++) {
                if (row[4 + j] > maxScore) {
                    maxScore = row[4 + j];
                    classIndex = j;
                }
            }

            if (maxScore >= CONFIDENCE_THRESHOLD) {
                let [xc, yc, w, h] = row.slice(0, 4);

                // Chuyển đổi tọa độ hệ số tuyệt đối về tỷ lệ tương đối [0..1]
                if (xc > 1.5 || yc > 1.5) {
                    xc /= INPUT_SIZE; yc /= INPUT_SIZE;
                    w /= INPUT_SIZE; h /= INPUT_SIZE;
                }

                // Chuyển từ định dạng (Tâm X, Tâm Y, Rộng, Cao) sang (Y1, X1, Y2, X2) cho hàm NMS của TensorFlow
                boxes.push([yc - h / 2, xc - w / 2, yc + h / 2, xc + w / 2]);
                scores.push(maxScore);
                classIndices.push(classIndex);
            }
        }

        if (boxes.length === 0) return JSON.stringify([]);

        // 5. LỌC HỘP TRÙNG LẶP (NON-MAXIMUM SUPPRESSION - NMS)
        const boxesTensor = tf.tensor2d(boxes);
        const scoresTensor = tf.tensor1d(scores);

        const nmsIndices = await tf.image.nonMaxSuppressionAsync(
            boxesTensor, scoresTensor, 50, 0.45, CONFIDENCE_THRESHOLD
        );

        const selectedIndices = await nmsIndices.array();

        // Giải phóng các Tensor phục vụ quá trình NMS
        tf.dispose([boxesTensor, scoresTensor, nmsIndices]);

        // 6. KẾT XUẤT ĐỊNH DẠNG TRẢ VỀ
        const finalResults = selectedIndices.map(i => {
            const [yMin, xMin, yMax, xMax] = boxes[i];
            return {
                x: xMin,
                y: yMin,
                width: xMax - xMin,
                height: yMax - yMin,
                label: classIndices[i].toString(),
                confidence: scores[i]
            };
        });

        // Trả chuỗi JSON tương thích để Kotlin/JS parse thành Object
        return JSON.stringify(finalResults);

    } catch (error) {
        console.error("Lỗi trong quá trình dự đoán của mô hình:", error);
        return JSON.stringify([]);
    }
};