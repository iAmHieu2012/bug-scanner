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
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả chung") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = identification, onValueChange = { identification = it }, label = { Text("Đặc điểm nhận dạng") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = danger, onValueChange = { danger = it }, label = { Text("Mức độ nguy hại") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Cách xử lý") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                
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
                    val result = bugInfo?.copy(
                        name = name,
                        englishName = englishName,
                        description = description,
                        identification = identification,
                        treatment = treatment,
                        danger = danger,
                        imageUrl = imageUrl
                    ) ?: BugInfo(
                        id = scientificName.replace(" ", "_"),
                        name = name,
                        englishName = englishName,
                        scientificName = scientificName,
                        description = description,
                        identification = identification,
                        treatment = treatment,
                        danger = danger,
                        imageUrl = imageUrl,
                        wikiUrl = ""
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
