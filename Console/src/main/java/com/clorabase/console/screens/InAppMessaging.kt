package com.clorabase.console.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import com.clorabase.console.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.kohsuke.github.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Preview(showBackground = true)
@Composable
fun InAppMessagingScreen() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val isSendButtonEnabled = title.isNotBlank() && message.isNotBlank() && channel.isNotBlank()
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = compressImage(inputStream,50)
                        imageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Image chosen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to pick image: I/O Error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun resetForm() {
        title = ""
        message = ""
        channel = ""
        link = ""
        imageBase64 = null
        isSending = false
    }

    fun sendMessage() {
        if (isSending) return
        isSending = true

        coroutineScope.launch(Dispatchers.IO) {
            val jsonObject = JSONObject().apply {
                put("title", title)
                put("message", message)
                put("image", imageBase64)
                put("link", link)
                put("type", "simple")
            }

            val path = "${Globals.currentProject.value}/messages/" + message.hashCode();
            try {
                GithubUtils.create(jsonObject.toString().toByteArray(), path)
                resetForm()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Message sent!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: HttpException) {
                if (e.message?.contains("Invalid request.") == true) {
                    try {
                        GithubUtils.update(jsonObject.toString().toByteArray(), path)
                        withContext(Dispatchers.Main){
                            Toast.makeText(context,"Message sent", Toast.LENGTH_LONG).show();
                        }
                    } catch (deleteException: IOException) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to send message: ${deleteException.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: JSONException) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Horrible glitch! An error occurred.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                launch(Dispatchers.Main) {
                    isSending = false
                }
            }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.messaging),
            contentDescription = "Messaging",
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )

        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp)) {
            Text(
                text = "Insert a message into the message queue of the project. The message will be deleted once it is seen by the client",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                maxLines = 3,
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Button Link (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    if (imageBase64 == null)
                        imagePickerLauncher.launch("image/*")
                    else {
                        Toast.makeText(context, "Image removed", Toast.LENGTH_SHORT).show()
                        imageBase64 = null;
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp)
            ) {
                if (imageBase64 == null) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add image",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Header Image")
                } else {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove image",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Header Image")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { sendMessage() },
                shape = RectangleShape,
                enabled = isSendButtonEnabled && !isSending,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                if (isSending) {
                    CircularProgressIndicator()
                } else {
                    Text("Send Message")
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.Send,"Send");
                }
            }
        }
    }
}

fun compressImage(inputStream: InputStream, quality: Int): ByteArray {
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}