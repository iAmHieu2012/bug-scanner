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
        "Cnaphalocrocis medinalis",     // 0: rice leaf folder
        "Naranga aenescens",            // 1: rice leaf caterpillar
        "Chilo suppressalis",           // 2: rice stem borer
        "Hydrellia philippina",         // 3: rice stem maggot
        "Orseolia oryzae",              // 4: rice gall midge
        "Nilaparvata lugens",           // 5: brown planthopper
        "Sogatella furcifera",          // 6: white backed planthopper
        "Laodelphax striatellus",       // 7: small brown planthopper
        "Nephotettix cincticeps",       // 8: green leafhopper
        "Stenchaetothrips biformis",    // 9: rice thrips
        "Scotinophara lurida",          // 10: rice bug
        "Holotrichia sp.",              // 11: soil grub
        "Gryllotalpidae",               // 12: mole cricket
        "Elateridae",                   // 13: wireworm
        "Agrotis ipsilon",              // 14: cutworm
        "Tetranychus urticae",          // 15: spider mite
        "Ostrinia furnacalis",          // 16: corn borer
        "Mythimna separata",            // 17: armyworm
        "Aphididae",                    // 18: aphid
        "Phyllotreta striolata",        // 19: flea beetle
        "Pieris canidia",               // 20: cabbage caterpillar
        "Locustoidea",                  // 21: locust or grasshopper
        "Thripidae",                    // 22: thrips
        "Limacodidae",                  // 23: hairy caterpillar
        "Pseudococcus comstocki",       // 24: scale insect or mealybug
        "Xylotrechus quadripes",        // 25: longhorn borer
        "Miridae",                      // 26: mirid bug
        "Trialeurodes vaporariorum",    // 27: whitefly
        "Bactrocera dorsalis",          // 28: fruit fly
        "Phyllocnistis citrella",       // 29: citrus leafminer
        "Idioscopus clypealis",         // 30: mango hopper
        "Sternochetus frigidus",        // 31: mango borer or weevil
        "Cicadellidae"                  // 32: leafhopper
    )

    /**
     * Từ điển dịch thuật sang Tiếng Việt.
     * Dùng để hiển thị lên UI cho thân thiện với người dùng.
     */
    val BUG_DICTIONARY = mapOf(
        "Cnaphalocrocis medinalis" to "Sâu cuốn lá lúa nhỏ",
        "Naranga aenescens" to "Sâu xanh ăn lá lúa",
        "Hydrellia philippina" to "Dòi đục nõn lúa",
        "Chilo suppressalis" to "Sâu đục thân lúa châu Á",
        "Scirpophaga incertulas" to "Sâu đục thân lúa bướm vàng",
        "Orseolia oryzae" to "Muỗi hành hại lúa",
        "Atherigona exigua" to "Ruồi đục thân lúa",
        "Nilaparvata lugens" to "Rầy nâu",
        "Sogatella furcifera" to "Rầy lưng trắng",
        "Laodelphax striatellus" to "Rầy nâu nhỏ",
        "Lissorhoptrus oryzophilus" to "Mọt nước hại lúa",
        "Nephotettix cincticeps" to "Rầy xanh đuôi đen",
        "Stenchaetothrips biformis" to "Bọ trĩ hại lúa",
        "Scotinophara lurida" to "Bọ xít đen hại lúa",
        "Holotrichia sp." to "Sùng đất / Bọ hung",
        "Gryllotalpidae" to "Dế nhũi",
        "Elateridae" to "Sâu thép",
        "Spilosoma lubricipeda" to "Bướm trắng mép xám",
        "Agrotis ipsilon" to "Sâu xám",
        "Agrotis tokionis" to "Sâu xám lớn",
        "Agrotis segetum" to "Sâu xám nhỏ",
        "Tetranychus urticae" to "Nhện đỏ",
        "Ostrinia furnacalis" to "Sâu đục thân ngô",
        "Mythimna separata" to "Sâu cắn chẽn / Sâu keo",
        "Aphididae" to "Rệp cây",
        "Potosia brevitarsis" to "Bọ hung xanh",
        "Carposina sasakii" to "Sâu đục quả đào",
        "Sitobion avenae" to "Rệp lúa mì Anh",
        "Schizaphis graminum" to "Rệp xanh",
        "Rhopalosiphum padi" to "Rệp yến mạch",
        "Sitodiplosis mosellana" to "Muỗi hoa lúa mì",
        "Penthaleus major" to "Nhện chân dài",
        "Linopodes sp." to "Nhện đỏ chân dài",
        "Haplothrips tritici" to "Bọ trĩ lúa mì",
        "Cephus cinctus" to "Ong cưa lúa mì",
        "Cerodontha denticornis" to "Ruồi đục lá lúa mì",
        "Pegomya hyoscyami" to "Ruồi hại củ cải",
        "Phyllotreta striolata" to "Bọ nhảy",
        "Mamestra brassicae" to "Sâu keo bắp cải",
        "Spodoptera exigua" to "Sâu xanh da láng",
        "Scrobipalpa ocellatella" to "Ruồi đốm củ cải",
        "Loxostege sticticalis" to "Ngài đồng cỏ",
        "Bothynoderes punctiventris" to "Mọt củ cải",
        "Serica orientalis" to "Bọ hung cánh nâu",
        "Hypera postica" to "Mọt cỏ linh lăng",
        "Heliothis viriplaca" to "Sâu đục nụ lanh",
        "Adelphocoris lineolatus" to "Bọ xít cỏ linh lăng",
        "Lygus lineolaris" to "Bọ xít mù",
        "Locustoidea" to "Châu chấu",
        "Lytta polita" to "Bọ ban miêu",
        "Epicauta gorhami" to "Bọ ban miêu đậu",
        "Meloidae" to "Họ Bọ ban miêu",
        "Therioaphis maculata" to "Rệp đốm cỏ linh lăng",
        "Odontothrips loti" to "Bọ trĩ hoa",
        "Thripidae" to "Họ Bọ trĩ",
        "Bruchophagus roddi" to "Ong chalcid hạt",
        "Pieris canidia" to "Bướm trắng",
        "Apolygus lucorum" to "Bọ xít mù xanh",
        "Limacodidae" to "Sâu nái",
        "Daktulosphaira vitifoliae" to "Rệp sáp rễ nho",
        "Colomerus vitis" to "Nhện lông nhung nho",
        "Brevipalpus lewisi" to "Nhện dẹt",
        "Oides decempunctata" to "Bọ cánh cứng ăn lá",
        "Polyphagotarsonemus latus" to "Nhện trắng",
        "Pseudococcus comstocki" to "Rệp sáp Comstock",
        "Paranthrene regalis" to "Sâu đục thân nho",
        "Ampelophaga rubiginosa" to "Sâu sừng nho",
        "Lycorma delicatula" to "Ruồi đèn lồng",
        "Xylotrechus quadripes" to "Xén tóc đục thân",
        "Cicadella viridis" to "Rầy xanh",
        "Miridae" to "Họ Bọ xít mù",
        "Trialeurodes vaporariorum" to "Bọ phấn trắng",
        "Erythroneura apicalis" to "Rầy lá nho",
        "Papilio xuthus" to "Bướm phượng",
        "Panonychus citri" to "Nhện đỏ cam chanh",
        "Phyllocoptruta oleivora" to "Nhện gỉ sắt",
        "Icerya purchasi" to "Rệp sáp vảy ốc",
        "Unaspis yanonensis" to "Rệp vảy tên",
        "Ceroplastes rubens" to "Rệp sáp sừng",
        "Chrysomphalus aonidum" to "Rệp vảy đen",
        "Parlatoria zizyphi" to "Rệp vảy đen đuôi nhọn",
        "Nipaecoccus viridis" to "Rệp sáp bột",
        "Aleurocanthus spiniferus" to "Bọ phấn gai đen",
        "Bactrocera minax" to "Ruồi đục quả cam lớn",
        "Bactrocera dorsalis" to "Ruồi đục quả phương Đông",
        "Bactrocera tsuneonis" to "Ruồi đục quả cam Nhật Bản",
        "Spodoptera litura" to "Sâu khoang",
        "Adris tyrannus" to "Bướm chích hút quả",
        "Phyllocnistis citrella" to "Sâu vẽ bùa",
        "Toxoptera citricida" to "Rệp muội nâu",
        "Toxoptera aurantii" to "Rệp muội đen",
        "Aphis spiraecola" to "Rệp sáp xanh",
        "Scirtothrips dorsalis" to "Bọ trĩ vàng",
        "Dasineura sp." to "Muỗi nhuế",
        "Lawana imitata" to "Rầy sừng bông",
        "Salurnis marginellus" to "Rầy dẹt",
        "Deporaus marginatus" to "Mọt cắt lá",
        "Chlumetia transversa" to "Sâu đục chồi xoài",
        "Idioscopus clypealis" to "Rầy xoài",
        "Rhytidodera bowringii" to "Xén tóc xoài",
        "Sternochetus frigidus" to "Mọt hạt xoài",
        "Cicadellidae" to "Rầy nhày"
    )
}