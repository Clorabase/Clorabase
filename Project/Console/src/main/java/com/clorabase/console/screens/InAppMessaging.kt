package com.clorabase.console.screens

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.R
import com.clorabase.console.models.DialogStyle
import com.clorabase.console.models.InAppMessagesViewModel
import com.clorabase.console.models.QueuedMessage

@Preview(showBackground = true)
@Composable
fun InAppMessagingScreen(viewModel: InAppMessagesViewModel = viewModel()) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Image(
                painter = painterResource(id = R.drawable.messaging),
                contentDescription = "Messaging",
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
        }

        item {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Publish In-App Message",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add a new message to the project's queue for all users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.messageContent,
                    onValueChange = { viewModel.messageContent = it },
                    label = { Text("Message Body") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = viewModel.link,
                    onValueChange = { viewModel.link = it },
                    label = { Text("Action Link (URL)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = viewModel.showOnce, onCheckedChange = { viewModel.showOnce = it })
                    Text("Show only once per user")
                }

                OutlinedTextField(
                    value = viewModel.expiryDays,
                    onValueChange = { viewModel.expiryDays = it.filter { char -> char.isDigit() } },
                    label = { Text("Expires in (days)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dialog Style", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DialogStyle.values().forEach { style ->
                        RadioButton(
                            selected = viewModel.selectedStyle == style,
                            onClick = { viewModel.selectedStyle = style }
                        )
                        Text(style.name, modifier = Modifier.padding(end = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Message Image", fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = viewModel.useImageUrl, onClick = { viewModel.useImageUrl = true })
                    Text("Image URL")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = !viewModel.useImageUrl, onClick = { viewModel.useImageUrl = false })
                    Text("Upload Image")
                }

                if (viewModel.useImageUrl) {
                    OutlinedTextField(
                        value = viewModel.imageURL,
                        onValueChange = { viewModel.imageURL = it },
                        label = { Text("Direct Image URL") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Button(onClick = { launcher.launch("image/*") }) {
                            Text(if (viewModel.selectedImageUri == null) "Select Image" else "Change Image")
                        }
                        if (viewModel.selectedImageUri != null) {
                            Text(
                                " Image Selected",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.sendMessage(context,
                            onSuccess = {
                                Toast.makeText(context, "Message published successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { Toast.makeText(context, "Failed: $it", Toast.LENGTH_LONG).show() }
                        )
                    },
                    shape = RectangleShape,
                    enabled = viewModel.title.isNotBlank() && viewModel.messageContent.isNotBlank() && !viewModel.isSending,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (viewModel.isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Publish Message")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Message Queue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (viewModel.isLoadingMessages) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp).fillMaxWidth().size(32.dp))
            }
        } else if (viewModel.queuedMessages.isEmpty()) {
            item {
                Text("No messages in queue", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
        } else {
            items(viewModel.queuedMessages) { message ->
                MessageItem(message, onDelete = {
                    viewModel.deleteMessage(message.githubPath) {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        
        item { 
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MessageItem(message: QueuedMessage, onDelete: () -> Unit) {
    val expiryText = if (message.expiry == 0L) {
        "Never"
    } else {
        DateUtils.getRelativeTimeSpanString(message.expiry).toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = "Expires: $expiryText", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
