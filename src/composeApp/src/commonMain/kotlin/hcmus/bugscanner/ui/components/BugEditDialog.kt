package hcmus.bugscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo

/**
 * Hộp thoại (Dialog) cho phép Admin thêm mới hoặc chỉnh sửa bài viết Bách khoa.
 *
 * @param bugInfo Dữ liệu côn trùng cần sửa. Nếu null thì là chế độ Thêm mới.
 * @param onDismiss Callback khi đóng hộp thoại.
 * @param onSave Callback khi lưu thành công, trả về [BugInfo] đã cập nhật.
 * @param onDelete Callback khi Admin xác nhận xóa bài viết (tuỳ chọn).
 */
@Composable
fun BugEditDialog(
    bugInfo: BugInfo?,
    onDismiss: () -> Unit,
    onSave: (BugInfo) -> Unit,
    onDelete: ((BugInfo) -> Unit)? = null
) {
    var name by remember { mutableStateOf(bugInfo?.name ?: "") }
    var englishName by remember { mutableStateOf(bugInfo?.englishName ?: "") }
    var scientificName by remember { mutableStateOf(bugInfo?.scientificName ?: "") }
    var description by remember { mutableStateOf(bugInfo?.description ?: "") }
    var identification by remember { mutableStateOf(bugInfo?.identification ?: "") }
    var treatment by remember { mutableStateOf(bugInfo?.treatment ?: "") }
    var danger by remember { mutableStateOf(bugInfo?.danger ?: "") }
    var imageUrl by remember { mutableStateOf(bugInfo?.imageUrl ?: "") }
    var harmfulnessLevel by remember { mutableStateOf(bugInfo?.harmfulnessLevel ?: "") }
    var affectedCrops by remember { mutableStateOf(bugInfo?.affectedCrops?.joinToString("\n") ?: "") }
    var hostPlants by remember { mutableStateOf(bugInfo?.hostPlants?.joinToString("\n") ?: "") }
    var damageSymptoms by remember { mutableStateOf(bugInfo?.damageSymptoms?.joinToString("\n") ?: "") }
    var identificationTips by remember { mutableStateOf(bugInfo?.identificationTips?.joinToString("\n") ?: "") }
    var whereToFind by remember { mutableStateOf(bugInfo?.whereToFind?.joinToString("\n") ?: "") }
    var season by remember { mutableStateOf(bugInfo?.season ?: "") }
    var safeActions by remember { mutableStateOf(bugInfo?.safeActions?.joinToString("\n") ?: "") }
    var ipmNotes by remember { mutableStateOf(bugInfo?.ipmNotes?.joinToString("\n") ?: "") }
    var searchTokens by remember { mutableStateOf(bugInfo?.searchTokens?.joinToString(", ") ?: "") }
    var wikiUrl by remember { mutableStateOf(bugInfo?.wikiUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bugInfo == null) "Thêm bài viết mới" else "Chỉnh sửa bài viết") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên tiếng Việt") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = englishName, onValueChange = { englishName = it }, label = { Text("Tên tiếng Anh") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = scientificName, onValueChange = { scientificName = it }, label = { Text("Tên khoa học") }, modifier = Modifier.fillMaxWidth(), enabled = bugInfo == null)
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Link Ảnh (URL)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wikiUrl, onValueChange = { wikiUrl = it }, label = { Text("Wikipedia URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả chung") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = identification, onValueChange = { identification = it }, label = { Text("Đặc điểm nhận dạng") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = danger, onValueChange = { danger = it }, label = { Text("Mức độ nguy hại") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = harmfulnessLevel, onValueChange = { harmfulnessLevel = it }, label = { Text("Cấp độ (crop_pest, human_pest, ...)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Cách xử lý") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                
                Text("Các trường danh sách (xuống dòng để chia mục):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(value = affectedCrops, onValueChange = { affectedCrops = it }, label = { Text("Cây trồng bị ảnh hưởng") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = hostPlants, onValueChange = { hostPlants = it }, label = { Text("Cây ký chủ") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = damageSymptoms, onValueChange = { damageSymptoms = it }, label = { Text("Triệu chứng gây hại") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = identificationTips, onValueChange = { identificationTips = it }, label = { Text("Mẹo nhận dạng") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = whereToFind, onValueChange = { whereToFind = it }, label = { Text("Nơi thường tìm thấy") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = safeActions, onValueChange = { safeActions = it }, label = { Text("Hành động an toàn") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = ipmNotes, onValueChange = { ipmNotes = it }, label = { Text("Ghi chú IPM") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                
                OutlinedTextField(value = season, onValueChange = { season = it }, label = { Text("Mùa vụ xuất hiện") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = searchTokens, onValueChange = { searchTokens = it }, label = { Text("Từ khóa tìm kiếm (cách nhau dấu phẩy)") }, modifier = Modifier.fillMaxWidth())

                if (bugInfo != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onDelete(bugInfo) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Xóa bài viết", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = (bugInfo ?: BugInfo.empty().copy(
                        id = scientificName.lowercase().replace(" ", "_"),
                        scientificName = scientificName
                    )).copy(
                        name = name,
                        englishName = englishName,
                        description = description,
                        identification = identification,
                        treatment = treatment,
                        danger = danger,
                        imageUrl = imageUrl,
                        wikiUrl = wikiUrl,
                        harmfulnessLevel = harmfulnessLevel,
                        season = season,
                        affectedCrops = affectedCrops.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        hostPlants = hostPlants.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        damageSymptoms = damageSymptoms.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        identificationTips = identificationTips.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        whereToFind = whereToFind.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        safeActions = safeActions.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        ipmNotes = ipmNotes.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                        searchTokens = searchTokens.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                    onSave(result)
                },
                enabled = name.isNotBlank() && scientificName.isNotBlank()
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
