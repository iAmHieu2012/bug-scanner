package hcmus.bugscanner.ml

/**
 * Lớp đối tượng Singleton chứa các hằng số cấu hình tĩnh cho mô hình nhận diện vật thể YOLO.
 */
object YoloConstants {
    /**
     * Đường dẫn tham chiếu đến file mô hình đã được huấn luyện (TensorFlow Lite).
     * Nguồn tham chiếu mô hình gốc: HuggingFace (Yashwanth1508/AgroAI-pest-detection).
     */
    const val MODEL_PATH = "model.tflite"

    /** Kích thước chiều dài và chiều rộng bắt buộc của hình ảnh đầu vào (pixels) trước khi đưa vào mô hình. */
    const val INPUT_SIZE = 896

    /** Ngưỡng điểm tin cậy tối thiểu (Confidence Score). Các kết quả dự đoán có điểm thấp hơn ngưỡng này sẽ bị loại bỏ. */
    const val CONFIDENCE_THRESHOLD = 0.35f

    /** Ngưỡng Intersection over Union (IoU) dùng cho thuật toán Non-Maximum Suppression (NMS) để lọc các hộp giới hạn trùng lặp. */
    const val IOU_THRESHOLD = 0.4f

    /**
     * Danh sách 102 nhãn (labels) tương ứng với các loài sinh vật được mô hình nhận diện.
     * Mảng này đã được chuẩn hóa sử dụng Tên Khoa Học và khớp tuyệt đối với chỉ số Index đầu ra của tập dữ liệu IP102.
     */
    val LABELS = arrayOf(
        "Cnaphalocrocis medinalis",     // 0: rice leaf roller
        "Naranga aenescens",            // 1: rice leaf caterpillar
        "Hydrellia philippina",         // 2: paddy stem maggot
        "Chilo suppressalis",           // 3: asiatic rice borer
        "Scirpophaga incertulas",       // 4: yellow rice borer
        "Orseolia oryzae",              // 5: rice gall midge
        "Atherigona exigua",            // 6: Rice Stemfly
        "Nilaparvata lugens",           // 7: brown plant hopper
        "Sogatella furcifera",          // 8: white backed plant hopper
        "Laodelphax striatellus",       // 9: small brown plant hopper
        "Lissorhoptrus oryzophilus",    // 10: rice water weevil
        "Nephotettix cincticeps",       // 11: rice leafhopper
        "Stenchaetothrips biformis",    // 12: grain spreader thrips
        "Scotinophara lurida",          // 13: rice shell pest
        "Holotrichia sp.",              // 14: grub
        "Gryllotalpidae",               // 15: mole cricket
        "Elateridae",                   // 16: wireworm
        "Spilosoma lubricipeda",        // 17: white margined moth
        "Agrotis ipsilon",              // 18: black cutworm
        "Agrotis tokionis",             // 19: large cutworm
        "Agrotis segetum",              // 20: yellow cutworm
        "Tetranychus urticae",          // 21: red spider
        "Ostrinia furnacalis",          // 22: corn borer
        "Mythimna separata",            // 23: army worm
        "Aphididae",                    // 24: aphids
        "Potosia brevitarsis",          // 25: Potosiabre vitarsis
        "Carposina sasakii",            // 26: peach borer
        "Sitobion avenae",              // 27: english grain aphid
        "Schizaphis graminum",          // 28: green bug
        "Rhopalosiphum padi",           // 29: bird cherry-oataphid
        "Sitodiplosis mosellana",       // 30: wheat blossom midge
        "Penthaleus major",             // 31: penthaleus major
        "Linopodes sp.",                // 32: longlegged spider mite
        "Haplothrips tritici",          // 33: wheat phloeothrips
        "Cephus cinctus",               // 34: wheat sawfly
        "Cerodontha denticornis",       // 35: cerodonta denticornis
        "Pegomya hyoscyami",            // 36: beet fly
        "Phyllotreta striolata",        // 37: flea beetle
        "Mamestra brassicae",           // 38: cabbage army worm
        "Spodoptera exigua",            // 39: beet army worm
        "Scrobipalpa ocellatella",      // 40: Beet spot flies
        "Loxostege sticticalis",        // 41: meadow moth
        "Bothynoderes punctiventris",   // 42: beet weevil
        "Serica orientalis",            // 43: sericaorient alismots chulsky
        "Hypera postica",               // 44: alfalfa weevil
        "Heliothis viriplaca",          // 45: flax budworm
        "Adelphocoris lineolatus",      // 46: alfalfa plant bug
        "Lygus lineolaris",             // 47: tarnished plant bug
        "Locustoidea",                  // 48: Locustoidea
        "Lytta polita",                 // 49: lytta polita
        "Epicauta gorhami",             // 50: legume blister beetle
        "Meloidae",                     // 51: blister beetle
        "Therioaphis maculata",         // 52: therioaphis maculata Buckton
        "Odontothrips loti",            // 53: odontothrips loti
        "Thripidae",                    // 54: Thrips
        "Bruchophagus roddi",           // 55: alfalfa seed chalcid
        "Pieris canidia",               // 56: Pieris canidia
        "Apolygus lucorum",             // 57: Apolygus lucorum
        "Limacodidae",                  // 58: Limacodidae
        "Daktulosphaira vitifoliae",    // 59: Viteus vitifoliae
        "Colomerus vitis",              // 60: Colomerus vitis
        "Brevipalpus lewisi",           // 61: Brevipoalpus lewisi McGregor
        "Oides decempunctata",          // 62: oides decempunctata
        "Polyphagotarsonemus latus",    // 63: Polyphagotars onemus latus
        "Pseudococcus comstocki",       // 64: Pseudococcus comstocki Kuwana
        "Paranthrene regalis",          // 65: parathrene regalis
        "Ampelophaga rubiginosa",       // 66: Ampelophaga
        "Lycorma delicatula",           // 67: Lycorma delicatula
        "Xylotrechus quadripes",        // 68: Xylotrechus
        "Cicadella viridis",            // 69: Cicadella viridis
        "Miridae",                      // 70: Miridae
        "Trialeurodes vaporariorum",    // 71: Trialeurodes vaporariorum
        "Erythroneura apicalis",        // 72: Erythroneura apicalis
        "Papilio xuthus",               // 73: Papilio xuthus
        "Panonychus citri",             // 74: Panonchus citri McGregor
        "Phyllocoptruta oleivora",      // 75: Phyllocoptes oleiverus ashmead
        "Icerya purchasi",              // 76: Icerya purchasi Maskell
        "Unaspis yanonensis",           // 77: Unaspis yanonensis
        "Ceroplastes rubens",           // 78: Ceroplastes rubens
        "Chrysomphalus aonidum",        // 79: Chrysomphalus aonidum
        "Parlatoria zizyphi",           // 80: Parlatoria zizyphus Lucus
        "Nipaecoccus viridis",          // 81: Nipaecoccus vastalor
        "Aleurocanthus spiniferus",     // 82: Aleurocanthus spiniferus
        "Bactrocera minax",             // 83: Tetradacus c Bactrocera minax
        "Bactrocera dorsalis",          // 84: Dacus dorsalis(Hendel)
        "Bactrocera tsuneonis",         // 85: Bactrocera tsuneonis
        "Spodoptera litura",            // 86: Prodenia litura
        "Adris tyrannus",               // 87: Adristyrannus
        "Phyllocnistis citrella",       // 88: Phyllocnistis citrella Stainton
        "Toxoptera citricida",          // 89: Toxoptera citricidus
        "Toxoptera aurantii",           // 90: Toxoptera aurantii
        "Aphis spiraecola",             // 91: Aphis citricola Vander Goot
        "Scirtothrips dorsalis",        // 92: Scirtothrips dorsalis Hood
        "Dasineura sp.",                // 93: Dasineura sp
        "Lawana imitata",               // 94: Lawana imitata Melichar
        "Salurnis marginellus",         // 95: Salurnis marginella Guerr
        "Deporaus marginatus",          // 96: Deporaus marginatus Pascoe
        "Chlumetia transversa",         // 97: Chlumetia transversa
        "Idioscopus clypealis",         // 98: Mango flat beak leafhopper
        "Rhytidodera bowringii",        // 99: Rhytidodera bowrinii white
        "Sternochetus frigidus",        // 100: Sternochetus frigidus
        "Cicadellidae"                  // 101: Cicadellidae
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