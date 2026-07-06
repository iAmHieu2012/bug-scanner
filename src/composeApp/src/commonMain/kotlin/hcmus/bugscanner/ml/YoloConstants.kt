package hcmus.bugscanner.ml

/**
 * Lớp đối tượng Singleton chứa các hằng số cấu hình tĩnh cho mô hình nhận diện vật thể YOLO.
 */
object YoloConstants {
    /**
     * Đường dẫn tham chiếu đến file mô hình đã được huấn luyện (TensorFlow Lite).
     * Mô hình hiện tại: YOLO11m Vietnam practical v3, xuất sang TFLite float16 ở kích thước 896.
     */
    const val MODEL_PATH = "model.tflite"

    /** Kích thước chiều dài và chiều rộng bắt buộc của hình ảnh đầu vào (pixels) trước khi đưa vào mô hình. */
    const val INPUT_SIZE = 896

    /** Ngưỡng điểm tin cậy tối thiểu (Confidence Score). Các kết quả dự đoán có điểm thấp hơn ngưỡng này sẽ bị loại bỏ. */
    const val CONFIDENCE_THRESHOLD = 0.35f

    /** Ngưỡng Intersection over Union (IoU) dùng cho thuật toán Non-Maximum Suppression (NMS) để lọc các hộp giới hạn trùng lặp. */
    const val IOU_THRESHOLD = 0.4f

    /**
     * Danh sách 33 nhãn theo đúng thứ tự của mô hình Vietnam practical v3.
     */
    val LABELS = arrayOf(
        "rice leaf folder",
        "rice leaf caterpillar",
        "rice stem borer",
        "rice stem maggot",
        "rice gall midge",
        "brown planthopper",
        "white backed planthopper",
        "small brown planthopper",
        "green leafhopper",
        "rice thrips",
        "rice bug",
        "soil grub",
        "mole cricket",
        "wireworm",
        "cutworm",
        "spider mite",
        "corn borer",
        "armyworm",
        "aphid",
        "flea beetle",
        "cabbage caterpillar",
        "locust or grasshopper",
        "thrips",
        "hairy caterpillar",
        "scale insect or mealybug",
        "longhorn borer",
        "mirid bug",
        "whitefly",
        "fruit fly",
        "citrus leafminer",
        "mango hopper",
        "mango borer or weevil",
        "leafhopper"
    )

    /**
     * Từ điển dịch thuật sang Tiếng Việt.
     * Dùng để hiển thị lên UI cho thân thiện với người dùng.
     */
    val BUG_DICTIONARY = mapOf(
        "rice leaf folder" to "Sâu cuốn lá lúa",
        "rice leaf caterpillar" to "Sâu ăn lá lúa",
        "rice stem borer" to "Sâu đục thân lúa",
        "rice stem maggot" to "Dòi đục nõn lúa",
        "rice gall midge" to "Muỗi hành hại lúa",
        "brown planthopper" to "Rầy nâu",
        "white backed planthopper" to "Rầy lưng trắng",
        "small brown planthopper" to "Rầy nâu nhỏ",
        "green leafhopper" to "Rầy xanh",
        "rice thrips" to "Bọ trĩ hại lúa",
        "rice bug" to "Bọ xít hại lúa",
        "soil grub" to "Sùng đất",
        "mole cricket" to "Dế nhũi",
        "wireworm" to "Sâu thép",
        "cutworm" to "Sâu xám",
        "spider mite" to "Nhện đỏ",
        "corn borer" to "Sâu đục thân ngô",
        "armyworm" to "Sâu keo / sâu khoang",
        "aphid" to "Rệp muội",
        "flea beetle" to "Bọ nhảy",
        "cabbage caterpillar" to "Sâu hại rau họ cải",
        "locust or grasshopper" to "Châu chấu",
        "thrips" to "Bọ trĩ",
        "hairy caterpillar" to "Sâu lông / sâu nái",
        "scale insect or mealybug" to "Rệp sáp / rệp vảy",
        "longhorn borer" to "Xén tóc đục thân",
        "mirid bug" to "Bọ xít mù",
        "whitefly" to "Bọ phấn trắng",
        "fruit fly" to "Ruồi đục quả",
        "citrus leafminer" to "Sâu vẽ bùa",
        "mango hopper" to "Rầy xoài",
        "mango borer or weevil" to "Sâu đục chồi / mọt xoài",
        "leafhopper" to "Rầy lá"
    )
}
