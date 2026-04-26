package com.clorabase.console.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar

data class QueuedMessage(
    val id: String,
    val title: String,
    val expiry: Long,
    val githubPath: String
)

enum class DialogStyle {
    SIMPLE, COUPON, PROMO
}

class InAppMessagesViewModel : ViewModel() {
    var title by mutableStateOf("")
    var messageContent by mutableStateOf("")
    var link by mutableStateOf("")
    var imageURL by mutableStateOf("")
    var isSending by mutableStateOf(false)
    var showOnce by mutableStateOf(false)
    var expiryDays by mutableStateOf("7")
    var useImageUrl by mutableStateOf(true)
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var selectedStyle by mutableStateOf(DialogStyle.SIMPLE)
    
    var queuedMessages by mutableStateOf<List<QueuedMessage>>(emptyList())
    var isLoadingMessages by mutableStateOf(false)

    fun init() {
        fetchMessages()
    }

    fun fetchMessages() {
        val projectName = Globals.currentProject.value
        val path = "$projectName/messages/"
        isLoadingMessages = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = GithubUtils.listFiles(path) ?: emptyList()
                val messages = mutableListOf<QueuedMessage>()
                for (file in files) {
                    if (file.isFile && file.name.endsWith(".json")) {
                        try {
                            val data = GithubUtils.getJsonResponse(file.rawUrl)
                            messages.add(
                                QueuedMessage(
                                    id = file.sha,
                                    title = data["title"]?.toString() ?: "No Title",
                                    expiry = (data["expiry"] as? Number)?.toLong() ?: 0L,
                                    githubPath = path + file.name
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    queuedMessages = messages.sortedByDescending { it.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isLoadingMessages = false }
            }
        }
    }

    fun sendMessage(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isSending) return
        isSending = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val projectName = Globals.currentProject.value
                val messageId = System.currentTimeMillis()
                var finalImageUrl = if (useImageUrl) imageURL else ""

                if (!useImageUrl && selectedImageUri != null) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(selectedImageUri!!)
                    if (inputStream != null) {
                        val imageBytes = compressImage(inputStream, 80)
                        val imagePath = "$projectName/messages/images/img_$messageId.jpg"
                        GithubUtils.create(imageBytes, imagePath)
                        finalImageUrl = "https://raw.githubusercontent.com/${Globals.username}/Clorabase-projects/main/$imagePath"
                    }
                }

                val expiryMillis = if (expiryDays.isBlank()) 0L else {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, expiryDays.toInt())
                    calendar.timeInMillis
                }

                val payload = JSONObject().apply {
                    put("title", title)
                    put("message", messageContent)
                    put("image", finalImageUrl)
                    put("link", link)
                    put("timestamp", messageId)
                    put("expiry", expiryMillis)
                    put("show_once", showOnce)
                    put("style", selectedStyle.name)
                }.toString().toByteArray()

                val path = "$projectName/messages/msg_$messageId.json"
                val sha = GithubUtils.create(payload, path)

                withContext(Dispatchers.Main) {
                    val newMessage = QueuedMessage(
                        id = sha,
                        title = title,
                        expiry = expiryMillis,
                        githubPath = path
                    )
                    queuedMessages = (listOf(newMessage) + queuedMessages).sortedByDescending { it.id }
                    resetForm()
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            } finally {
                withContext(Dispatchers.Main) { isSending = false }
            }
        }
    }

    fun deleteMessage(githubPath: String, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GithubUtils.delete(githubPath)
                withContext(Dispatchers.Main) {
                    queuedMessages = queuedMessages.filter { it.githubPath != githubPath }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to delete message")
                }
            }
        }
    }

    private fun resetForm() {
        title = ""
        messageContent = ""
        link = ""
        imageURL = ""
        showOnce = false
        expiryDays = "7"
        selectedImageUri = null
        useImageUrl = true
        selectedStyle = DialogStyle.SIMPLE
    }

    private fun compressImage(inputStream: InputStream, quality: Int): ByteArray {
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
}
