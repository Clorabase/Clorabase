package com.clorabase.console.screens


import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.R
import com.clorabase.console.models.StorageUiState
import com.clorabase.console.models.StorageViewModel
import com.clorabase.console.theme.Black
import com.clorabase.console.utils.BrowsableListCommons
import com.clorabase.console.utils.FileListItem
import com.clorabase.console.utils.FolderDialog
import com.clorabase.console.utils.ListItem

@Composable
@Preview(showBackground = true)
fun StorageScreen() {
    val viewModel: StorageViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<ListItem.File?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Upload or delete files",
                fontSize = 30.sp,
                fontFamily = FontFamily(Font(R.font.anton)),
                color = Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Manage your app's file storage here. Upload, download, and remove files seamlessly." +
                        "You can either choose to upload a file blob or small file (max 25MB).",
            )

            Spacer(modifier = Modifier.height(25.dp))

            when (val state = uiState) {
                is StorageUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is StorageUiState.Success -> {

                    val showDialog = remember { mutableStateOf(false) }
                    if (showDialog.value) {
                        FolderDialog(
                            onDismiss = { showDialog.value = false },
                            onCreate = { folderName ->
                                viewModel.navigateIntoFolder(folderName)
                                showDialog.value = false
                            }
                        )
                    }

                    Box {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                BrowsableListCommons(
                                    onBack = {viewModel.navigateBack()},
                                    onFolderCLick = {
                                        showDialog.value = true;
                                    }
                                )
                            }

                            items(state.files) { fileItem ->
                                FileListItem(fileItem) { clickedItem ->
                                    when (clickedItem) {
                                        is ListItem.File -> {
                                            selectedFile = clickedItem
                                            showContextMenu = true
                                        }

                                        is ListItem.Folder -> {
                                            viewModel.navigateIntoFolder(clickedItem.name)
                                        }

                                        is ListItem.Document -> {}
                                    }
                                }
                            }
                        }

                        val isBlob = remember { mutableStateOf(false) }
                        val filePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                if (isBlob.value)
                                    viewModel.uploadBlob(context, uri)
                                else
                                    viewModel.uploadFile(context, uri);
                            }
                        }

                        UploadFAB(
                            onSimpleUploadClick = {
                                isBlob.value = false
                                filePicker.launch(arrayOf("*/*"))
                            },
                            onBlobUploadClick = {
                                isBlob.value = true;
                                filePicker.launch(arrayOf("*/*"))
                            },
                            Modifier.align(Alignment.BottomEnd))
                    }
                }

                is StorageUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }

    if (showContextMenu && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text("File Options") },
            text = { Text("Choose an option for ${selectedFile!!.name}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.downloadFile(context,selectedFile!!.name) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    showContextMenu = false
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(selectedFile!!.name)
                    showContextMenu = false
                }) {
                    Text("Delete")
                }
            }
        )
    }
}


@Composable
fun UploadFAB(
    onSimpleUploadClick: () -> Unit,
    onBlobUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInHorizontally { it / 2 }, // Enter from halfway up
            exit = fadeOut() + slideOutHorizontally { it * 2 }
        ) {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    text = { Text("Small file") },
                    icon = {Icon(painter = painterResource(R.drawable.ic_file), contentDescription = "Simple Upload")},
                    onClick = onSimpleUploadClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExtendedFloatingActionButton(
                    text = { Text("Large binary file") },
                    icon = {Icon(painter = painterResource(R.drawable.blob), contentDescription = "Large Upload")},
                    onClick = onBlobUploadClick
                )

            }
        }

        ExtendedFloatingActionButton(
            text = { Text("Upload file") },
            icon = {Icon(imageVector = Icons.Filled.Add, contentDescription = "Large Upload")},
            onClick = {expanded = !expanded}
        )
    }
}
