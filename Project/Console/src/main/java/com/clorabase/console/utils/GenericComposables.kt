package com.clorabase.console.utils


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clorabase.console.R

sealed class ListItem {
    data class Document(val doc: clorabase.sdk.java.database.Document) : ListItem()
    data class File(val name: String) : ListItem()
    data class Folder(val name: String) : ListItem()
}

@Composable
fun FileListItem(item: ListItem, onClick: (ListItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (item) {
            is ListItem.File -> R.drawable.ic_file
            is ListItem.Folder -> R.drawable.ic_folder
            is ListItem.Document -> R.drawable.ic_file
        }
        val name = when (item) {
            is ListItem.File -> item.name
            is ListItem.Folder -> item.name
            is ListItem.Document -> item.doc.name
        }

        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            Column {
                Text(text = "Enter a name for the new folder or collection. Names can only contain letters and numbers.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { newValue ->
                        folderName = newValue.filter { it.isLetterOrDigit() }
                    },
                    label = { Text("Folder/Collection Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onCreate(folderName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BrowsableListCommons(onBack : () -> Unit, onFolderCLick : () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clickable(onClick = onBack)
            , verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(end = 10.dp),
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Go back",
            )

            Text("Go back", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clickable(onClick = onFolderCLick)
            , verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(end = 10.dp),
                painter = painterResource(R.drawable.ic_folder),
                colorFilter = ColorFilter.tint(Color.Black),
                contentDescription = "Add collection",
            )

            Text("Add collection", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}