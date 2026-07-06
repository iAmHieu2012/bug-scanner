package hcmus.bugscanner

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ConfidencePolicy
import hcmus.bugscanner.domain.model.GeminiContent
import hcmus.bugscanner.domain.model.GeminiInlineData
import hcmus.bugscanner.domain.model.GeminiPart
import hcmus.bugscanner.domain.model.GeminiRequest
import hcmus.bugscanner.domain.model.HarmfulnessLevel
import hcmus.bugscanner.domain.model.HarmfulnessPolicy
import hcmus.bugscanner.domain.model.Instruction
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.toBugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import hcmus.bugscanner.ui.auth.AuthValidation
import hcmus.bugscanner.ui.auth.AuthErrorPolicy
import hcmus.bugscanner.ui.chat.ChatPromptSuggestions
import hcmus.bugscanner.ui.chat.ChatContextResolver
import hcmus.bugscanner.ui.chat.ChatPayloadPolicy
import hcmus.bugscanner.ui.chat.ChatRagContextPolicy
import hcmus.bugscanner.data.remote.ApiKeyPolicy
import hcmus.bugscanner.data.remote.NetworkPolicy
import hcmus.bugscanner.ui.encyclopedia.SearchQueryPolicy
import hcmus.bugscanner.ui.components.isKnownCanvasUnsafeImageUrl
import hcmus.bugscanner.ui.detail.DetailSectionTextPolicy
import hcmus.bugscanner.ui.home.AppTab
import hcmus.bugscanner.ui.home.appTabFromHash
import hcmus.bugscanner.ui.home.toHashRoute
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import hcmus.bugscanner.ui.layout.classifyAdaptiveWidth
import hcmus.bugscanner.ui.scan.ScanRuntimeBackend
import hcmus.bugscanner.ui.scan.ScanRuntimeStatus
import hcmus.bugscanner.ui.text.MarkdownBlock
import hcmus.bugscanner.ui.text.MarkdownEmphasis
import hcmus.bugscanner.ui.text.SimpleMarkdown
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    @Test
    fun confidencePolicyMapsScoresToFarmerReadableLevels() {
        assertEquals("Độ tin cậy cao", ConfidencePolicy.explain(0.91f).label)
        assertEquals("Độ tin cậy trung bình", ConfidencePolicy.explain(0.62f).label)
        assertEquals("Độ tin cậy thấp", ConfidencePolicy.explain(0.31f).label)
        assertTrue(ConfidencePolicy.explain(0.31f).guidance.contains("chụp lại", ignoreCase = true))
    }

    @Test
    fun harmfulnessPolicyMapsKnownAndUnknownValuesSafely() {
        assertEquals(HarmfulnessLevel.CROP_PEST, HarmfulnessPolicy.fromValue("crop_pest"))
        assertEquals(HarmfulnessLevel.BENEFICIAL, HarmfulnessPolicy.fromValue("beneficial"))
        assertEquals(HarmfulnessLevel.UNKNOWN, HarmfulnessPolicy.fromValue("unexpected"))
        assertEquals("Có thể gây hại cây trồng", HarmfulnessPolicy.fromValue("crop_pest").label)
    }

    @Test
    fun authValidationRejectsInvalidInputBeforeNetworkCall() {
        assertEquals("Vui lòng nhập email.", AuthValidation.validate("", "123456")?.message)
        assertEquals("Email không hợp lệ.", AuthValidation.validate("not-an-email", "123456")?.message)
        assertEquals("Mật khẩu cần ít nhất 6 ký tự.", AuthValidation.validate("user@example.com", "123")?.message)
        assertNull(AuthValidation.validate("user@example.com", "123456"))
    }

    @Test
    fun chatPromptSuggestionsProvideUsefulBugScannerStarters() {
        val prompts = ChatPromptSuggestions.defaultPrompts

        assertTrue(prompts.size >= 3)
        assertFalse(prompts.any { it.contains("này", ignoreCase = true) })
        assertTrue(prompts.any { it.contains("ảnh", ignoreCase = true) || it.contains("chụp", ignoreCase = true) })
        assertEquals(
            "Cung cấp cho tôi thông tin chi tiết và cách xử lý bọ xít?",
            ChatPromptSuggestions.detailPrompt("bọ xít")
        )
    }

    @Test
    fun chatPromptSuggestionsUseActiveBugContext() {
        val bug = BugInfo.empty().copy(name = "Rầy nâu", scientificName = "Nilaparvata lugens")

        val prompts = ChatPromptSuggestions.promptsForBug(bug)

        assertTrue(prompts.any { it.contains("Rầy nâu") })
        assertTrue(prompts.any { it.contains("gây hại", ignoreCase = true) })
        assertFalse(prompts.any { it == "Côn trùng này có nguy hiểm với người không?" })
    }


    @Test
    fun chatContextResolverQueriesFirestoreAndPrefersDatabaseRecord() = runBlocking {
        val scannedBug = BugInfo.empty().copy(
            name = "Tên từ kết quả scan",
            scientificName = "Cnaphalocrocis medinalis",
            imageUrl = "https://example.com/scan.jpg"
        )
        val databaseBug = BugInfo.empty().copy(
            name = "Sâu cuốn lá lúa nhỏ",
            scientificName = "Cnaphalocrocis medinalis",
            description = "Thông tin lấy từ Firestore.",
            imageUrl = "https://example.com/database.jpg"
        )
        val repository = FakeEncyclopediaRepository(databaseBug)

        val resolved = ChatContextResolver.resolve(repository, scannedBug)

        assertEquals(listOf("Cnaphalocrocis medinalis"), repository.scientificNameQueries)
        assertEquals("Sâu cuốn lá lúa nhỏ", resolved?.name)
        assertEquals("Thông tin lấy từ Firestore.", resolved?.description)
        assertEquals("https://example.com/scan.jpg", resolved?.imageUrl)
    }

    @Test
    fun chatRagContextPolicyInjectsVietnameseFirestoreContext() {
        val bug = BugInfo.empty().copy(
            name = "Sâu cuốn lá lúa nhỏ",
            englishName = "Rice leaf roller",
            scientificName = "Cnaphalocrocis medinalis",
            description = "Gây hại trên lá lúa.",
            identification = "Ấu trùng cuốn lá thành ống.",
            danger = "Làm giảm diện tích quang hợp.",
            harmfulnessLevel = HarmfulnessLevel.CROP_PEST.value,
            treatment = "Theo dõi mật độ và bảo vệ thiên địch.",
            wikiUrl = "https://example.com/source"
        )

        val instruction = ChatRagContextPolicy.systemInstruction(bug)

        assertTrue(instruction.contains("Ngữ cảnh từ cơ sở dữ liệu BugScanner"))
        assertTrue(instruction.contains("Sâu cuốn lá lúa nhỏ"))
        assertTrue(instruction.contains("Cnaphalocrocis medinalis"))
        assertTrue(instruction.contains("Gây hại trên lá lúa"))
        assertTrue(instruction.contains("Có thể gây hại cây trồng"))
        assertTrue(instruction.contains("Luôn trả lời bằng tiếng Việt"))
    }

    @Test
    fun chatRagContextPolicyUsesFarmerFacingContextAndHidesTechnicalJargon() {
        val bug = BugInfo.empty().copy(
            name = "Rầy nâu",
            englishName = "Brown planthopper",
            scientificName = "Nilaparvata lugens",
            description = "Rầy nâu là sâu hại quan trọng trên lúa.",
            identification = "Mã lớp YOLO/IP102: 7.",
            danger = "Có thể gây cháy rầy.",
            treatment = "Theo dõi ruộng thường xuyên.",
            affectedCrops = listOf("Lúa"),
            damageSymptoms = listOf("Cây lúa vàng, khô từng chòm"),
            safeActions = listOf("Kiểm tra mật độ rầy ở gốc lúa"),
            sourceRefs = listOf("https://example.com/source")
        )

        val instruction = ChatRagContextPolicy.systemInstruction(bug)

        assertTrue(instruction.contains("Cây trồng thường gặp: Lúa"))
        assertTrue(instruction.contains("Dấu hiệu gây hại: Cây lúa vàng, khô từng chòm"))
        assertTrue(instruction.contains("Việc nên làm:"))
        assertTrue(instruction.contains("Kiểm tra mật độ rầy ở gốc lúa"))
        assertFalse(instruction.contains("YOLO"))
        assertFalse(instruction.contains("IP102"))
        assertFalse(instruction.contains("GBIF"))
    }

    @Test
    fun chatRagContextPolicyDoesNotInventContextWhenBugIsMissing() {
        val instruction = ChatRagContextPolicy.systemInstruction(null)

        assertTrue(instruction.contains("Luôn trả lời bằng tiếng Việt"))
        assertFalse(instruction.contains("Ngữ cảnh từ cơ sở dữ liệu Firestore"))
    }

    @Test
    fun chatPayloadPolicyKeepsOnlyRecentConversationTurns() {
        val history = (1..12).map { index ->
            GeminiContent(
                role = if (index % 2 == 0) "model" else "user",
                parts = listOf(GeminiPart(text = "message $index"))
            )
        }

        val trimmed = ChatPayloadPolicy.trimHistory(history)

        assertEquals(8, trimmed.size)
        assertEquals("message 5", trimmed.first().parts.first().text)
        assertEquals("message 12", trimmed.last().parts.first().text)
    }

    @Test
    fun chatPayloadPolicyRejectsOversizedInlineImages() {
        assertTrue(ChatPayloadPolicy.acceptsInlineImage(ByteArray(ChatPayloadPolicy.MAX_INLINE_IMAGE_BYTES)))
        assertFalse(ChatPayloadPolicy.acceptsInlineImage(ByteArray(ChatPayloadPolicy.MAX_INLINE_IMAGE_BYTES + 1)))
    }

    @Test
    fun geminiInlineImagePayloadUsesRestApiFieldNames() {
        val request = GeminiRequest(
            systemInstruction = Instruction(parts = GeminiPart(text = "system")),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(text = "con gi day"),
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = "base64"
                            )
                        )
                    )
                )
            )
        )

        val json = Json { explicitNulls = false }.encodeToString(request)

        assertTrue(json.contains(""""systemInstruction""""))
        assertTrue(json.contains(""""inline_data""""))
        assertTrue(json.contains(""""mime_type""""))
        assertFalse(json.contains(""""inlineData""""))
        assertFalse(json.contains(""""mimeType""""))
    }

    @Test
    fun simpleMarkdownParsesGeminiStyleBulletsAndEmphasis() {
        val blocks = SimpleMarkdown.parse(
            """
            Bà con có thể kiểm tra:
            - **Có ích:** thường ăn sâu hại.
            - *Gây hại* thường để lại vết cắn.
            """.trimIndent()
        )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.Bullet)
        assertEquals(MarkdownEmphasis.BOLD, blocks[1].spans.first().emphasis)
        assertEquals("Có ích:", blocks[1].spans.first().text)
        assertEquals(MarkdownEmphasis.ITALIC, blocks[2].spans.first().emphasis)
        assertEquals("Gây hại", blocks[2].spans.first().text)
    }

    @Test
    fun detailSectionTextPolicyKeepsLabelsInlineWithBulletBody() {
        val items = DetailSectionTextPolicy.sectionItems(
            "Ưu tiên quản lý dịch hại tổng hợp: quan sát mật độ, bảo vệ thiên địch; Chụp thêm ảnh rõ hơn"
        )

        assertEquals(2, items.size)
        assertEquals("Ưu tiên quản lý dịch hại tổng hợp", items[0].label)
        assertEquals("quan sát mật độ, bảo vệ thiên địch", items[0].body)
        assertEquals("Ưu tiên quản lý dịch hại tổng hợp: quan sát mật độ, bảo vệ thiên địch", items[0].fullText)
        assertEquals("", items[1].label)
        assertEquals("Chụp thêm ảnh rõ hơn", items[1].body)
    }

    @Test
    fun apiKeyPolicyRejectsBlankAndPlaceholderValues() {
        assertFalse(ApiKeyPolicy.isConfigured(""))
        assertFalse(ApiKeyPolicy.isConfigured("   "))
        assertFalse(ApiKeyPolicy.isConfigured("your_groq_api_key"))
        assertFalse(ApiKeyPolicy.isConfigured("AIzaSy..."))
        assertTrue(ApiKeyPolicy.isConfigured("real-key-value"))
    }

    @Test
    fun networkPolicyUsesBoundedTimeouts() {
        assertEquals(15_000L, NetworkPolicy.REQUEST_TIMEOUT_MS)
        assertEquals(15_000L, NetworkPolicy.CONNECT_TIMEOUT_MS)
        assertEquals(20_000L, NetworkPolicy.SOCKET_TIMEOUT_MS)
    }

    @Test
    fun searchQueryPolicySkipsGroqForEnglishAndScientificQueries() {
        assertFalse(SearchQueryPolicy.shouldTranslateWithGroq("stink bug"))
        assertFalse(SearchQueryPolicy.shouldTranslateWithGroq("Apis mellifera"))
        assertTrue(SearchQueryPolicy.shouldTranslateWithGroq("bọ xít"))
        assertTrue(SearchQueryPolicy.shouldTranslateWithGroq("ong mật"))
    }

    @Test
    fun searchQueryPolicyMatchesVietnameseNamesWithoutAccents() {
        val bugs = listOf(
            BugInfo.empty().copy(
                name = "Bọ hung cánh nâu",
                englishName = "Serica orientalismots chulsky",
                scientificName = "Serica orientalis"
            ),
            BugInfo.empty().copy(
                name = "Rầy nâu",
                englishName = "Brown planthopper",
                scientificName = "Nilaparvata lugens"
            )
        )

        assertEquals(listOf("Bọ hung cánh nâu"), SearchQueryPolicy.filterBugs(bugs, "bo hung").map { it.name })
        assertEquals(listOf("Rầy nâu"), SearchQueryPolicy.filterBugs(bugs, "planthopper").map { it.name })
        assertEquals(listOf("Rầy nâu"), SearchQueryPolicy.filterBugs(bugs, "lugens").map { it.name })
    }

    @Test
    fun searchQueryPolicyMatchesFarmerSearchTokensAndHostCrops() {
        val bugs = listOf(
            BugInfo.empty().copy(
                name = "Rầy nâu",
                scientificName = "Nilaparvata lugens",
                affectedCrops = listOf("Lúa"),
                damageSymptoms = listOf("Cháy rầy"),
                searchTokens = listOf("ray nau", "sau hai lua", "chay ray")
            ),
            BugInfo.empty().copy(
                name = "Bọ trĩ vàng",
                scientificName = "Thrips flavus",
                affectedCrops = listOf("Rau màu")
            )
        )

        assertEquals(listOf("Rầy nâu"), SearchQueryPolicy.filterBugs(bugs, "sau hai lua").map { it.name })
        assertEquals(listOf("Rầy nâu"), SearchQueryPolicy.filterBugs(bugs, "cháy rầy").map { it.name })
        assertEquals(listOf("Bọ trĩ vàng"), SearchQueryPolicy.filterBugs(bugs, "rau màu").map { it.name })
    }

    @Test
    fun searchQueryPolicyDoesNotMatchGenericLongTextForShortQueries() {
        val bugs = listOf(
            BugInfo.empty().copy(
                name = "Bướm phượng",
                scientificName = "Papilio demoleus",
                description = "Loài này thuộc bộ Lepidoptera."
            ),
            BugInfo.empty().copy(
                name = "Bọ hung cánh nâu",
                scientificName = "Serica orientalis"
            )
        )

        assertEquals(listOf("Bọ hung cánh nâu"), SearchQueryPolicy.filterBugs(bugs, "bo").map { it.name })
    }

    @Test
    fun bugInfoCombinesLegacyImageUrlWithGalleryUrls() {
        val bug = BugInfo.empty().copy(
            imageUrl = "https://example.com/main.jpg",
            imageUrls = listOf(
                "https://example.com/main.jpg",
                " ",
                "https://via.placeholder.com/600x400.png",
                "https://example.com/second.jpg"
            )
        )

        assertEquals(
            listOf("https://example.com/main.jpg", "https://example.com/second.jpg"),
            bug.displayImageUrls()
        )
    }

    @Test
    fun bugInfoCanSkipFailedGalleryUrlsWhenPickingDisplayImages() {
        val bug = BugInfo.empty().copy(
            imageUrl = "https://example.com/dead.jpg",
            imageUrls = listOf(
                "https://example.com/dead.jpg",
                "https://example.com/working.jpg",
                "https://example.com/third.jpg"
            )
        )

        assertEquals(
            listOf("https://example.com/working.jpg", "https://example.com/third.jpg"),
            bug.displayImageUrls(excludedUrls = setOf("https://example.com/dead.jpg"))
        )
    }

    @Test
    fun legacyHistoryRecordStillConvertsToBugInfo() {
        val history = ScanHistory(
            bugName = "bọ xít",
            imageUrl = "https://example.com/bug.jpg"
        )

        val bug = history.toBugInfo()

        assertEquals("bọ xít", bug.id)
        assertEquals("bọ xít", bug.name)
        assertEquals("bọ xít", bug.scientificName)
        assertEquals("https://example.com/bug.jpg", bug.imageUrl)
    }

    @Test
    fun enrichedHistoryRecordPreservesDetailMetadata() {
        val history = ScanHistory(
            bugId = "inat-42",
            bugName = "Bướm phượng",
            englishName = "Swallowtail butterfly",
            scientificName = "Papilio demoleus",
            imageUrl = "https://example.com/papilio.jpg",
            confidence = 0.86f,
            source = ScanSource.INATURALIST.value,
            description = "Loài bướm phổ biến.",
            identification = "Cánh có vệt vàng đen.",
            danger = "Không nguy hiểm.",
            treatment = "Không cần xử lý.",
            harmfulnessLevel = HarmfulnessLevel.BENEFICIAL.value,
            wikiUrl = "https://example.com/wiki"
        )

        val bug = history.toBugInfo()

        assertEquals("inat-42", bug.id)
        assertEquals("Bướm phượng", bug.name)
        assertEquals("Swallowtail butterfly", bug.englishName)
        assertEquals("Papilio demoleus", bug.scientificName)
        assertEquals("Loài bướm phổ biến.", bug.description)
        assertEquals("Không cần xử lý.", bug.treatment)
        assertEquals(HarmfulnessLevel.BENEFICIAL.value, bug.harmfulnessLevel)
        assertEquals("https://example.com/wiki", bug.wikiUrl)
    }

    @Test
    fun detailPromptIncludesBugContextAndScanMetadata() {
        val bug = BugInfo.empty().copy(
            name = "Bọ xít xanh",
            scientificName = "Nezara viridula",
            identification = "Cơ thể xanh, dạng khiên.",
            danger = "Có thể gây hại cây trồng.",
            treatment = "Loại bỏ bằng biện pháp cơ học."
        )

        val prompt = ChatPromptSuggestions.detailPrompt(
            bug = bug,
            confidence = 0.91f,
            source = ScanSource.YOLO
        )

        assertTrue(prompt.contains("Bọ xít xanh"))
        assertTrue(prompt.contains("Nezara viridula"))
        assertFalse(prompt.contains("YOLO"))
        assertFalse(prompt.contains("IP102"))
        assertTrue(prompt.contains("91%"))
        assertTrue(prompt.contains("Cơ thể xanh"))
        assertTrue(prompt.contains("Không dùng Markdown"))
    }

    @Test
    fun adaptiveLayoutUsesStableWebBreakpoints() {
        assertEquals(AdaptiveLayoutSize.COMPACT, classifyAdaptiveWidth(599f))
        assertEquals(AdaptiveLayoutSize.MEDIUM, classifyAdaptiveWidth(600f))
        assertEquals(AdaptiveLayoutSize.MEDIUM, classifyAdaptiveWidth(999f))
        assertEquals(AdaptiveLayoutSize.EXPANDED, classifyAdaptiveWidth(1000f))
    }

    @Test
    fun appTabsMapToStableHashRoutes() {
        assertEquals("#/scan", AppTab.SCAN.toHashRoute())
        assertEquals("#/history", AppTab.HISTORY.toHashRoute())
        assertEquals("#/encyclopedia", AppTab.WIKI.toHashRoute())
        assertEquals("#/chat", AppTab.CHATBOT.toHashRoute())
        assertEquals(AppTab.WIKI, appTabFromHash("#/encyclopedia"))
        assertEquals(AppTab.SCAN, appTabFromHash("#/unknown"))
        assertEquals(AppTab.SCAN, appTabFromHash(""))
    }

    @Test
    fun canvasUnsafeImagePolicyIdentifiesKnownCrossOriginHosts() {
        assertTrue(isKnownCanvasUnsafeImageUrl("https://via.placeholder.com/600x400.png?text=Chua+co+anh"))
        assertFalse(isKnownCanvasUnsafeImageUrl("https://static.inaturalist.org/photos/42/medium.jpg"))
        assertFalse(isKnownCanvasUnsafeImageUrl("https://example.com/bug.jpg"))
        assertFalse(isKnownCanvasUnsafeImageUrl(""))
    }

    @Test
    fun scanRuntimeReadyStateControlsLiveDetection() {
        val webGl = ScanRuntimeStatus.Ready(ScanRuntimeBackend.WEBGL, liveDetectionSupported = true)
        val wasm = ScanRuntimeStatus.Ready(ScanRuntimeBackend.WASM, liveDetectionSupported = false)

        assertTrue(webGl.supportsLiveDetection)
        assertFalse(wasm.supportsLiveDetection)
        assertFalse(ScanRuntimeStatus.PermissionDenied().supportsLiveDetection)
        assertEquals("WebGL", webGl.backendLabel)
        assertEquals("WASM", wasm.backendLabel)
    }

}

private class FakeEncyclopediaRepository(
    private val databaseBug: BugInfo?
) : EncyclopediaRepository {
    val scientificNameQueries = mutableListOf<String>()

    override suspend fun getExploreInsects(searchQuery: String, limit: Int): List<BugInfo> = emptyList()

    override suspend fun getBugByName(name: String): BugInfo? = null

    override suspend fun getBugByScientificName(scientificName: String): BugInfo? {
        scientificNameQueries += scientificName
        return databaseBug
    }

    override suspend fun prefetchDatabase() = Unit

    override suspend fun saveBugToFirebase(bug: BugInfo): Boolean = true
}
