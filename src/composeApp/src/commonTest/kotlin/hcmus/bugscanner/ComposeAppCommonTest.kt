package hcmus.bugscanner

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.toBugInfo
import hcmus.bugscanner.ui.auth.AuthValidation
import hcmus.bugscanner.ui.chat.ChatPromptSuggestions
import hcmus.bugscanner.ui.components.isKnownCanvasUnsafeImageUrl
import hcmus.bugscanner.ui.home.AppTab
import hcmus.bugscanner.ui.home.appTabFromHash
import hcmus.bugscanner.ui.home.toHashRoute
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import hcmus.bugscanner.ui.layout.classifyAdaptiveWidth
import hcmus.bugscanner.ui.scan.ScanRuntimeBackend
import hcmus.bugscanner.ui.scan.ScanRuntimeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
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
        assertTrue(prompts.any { it.contains("côn trùng", ignoreCase = true) })
        assertEquals(
            "Cung cấp cho tôi thông tin chi tiết và cách xử lý bọ xít?",
            ChatPromptSuggestions.detailPrompt("bọ xít")
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
            wikiUrl = "https://example.com/wiki"
        )

        val bug = history.toBugInfo()

        assertEquals("inat-42", bug.id)
        assertEquals("Bướm phượng", bug.name)
        assertEquals("Swallowtail butterfly", bug.englishName)
        assertEquals("Papilio demoleus", bug.scientificName)
        assertEquals("Loài bướm phổ biến.", bug.description)
        assertEquals("Không cần xử lý.", bug.treatment)
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
        assertTrue(prompt.contains("YOLO"))
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
        assertTrue(isKnownCanvasUnsafeImageUrl("https://static.inaturalist.org/photos/42/medium.jpg"))
        assertTrue(isKnownCanvasUnsafeImageUrl("https://via.placeholder.com/600x400.png?text=Chua+co+anh"))
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
