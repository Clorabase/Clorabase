package com.clorabase.console.screens


import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.DocumentActivity
import com.clorabase.console.R
import com.clorabase.console.models.DatabaseUiState
import com.clorabase.console.models.DatabaseViewModel
import com.clorabase.console.theme.Black
import com.clorabase.console.utils.BrowsableListCommons
import com.clorabase.console.utils.FileListItem
import com.clorabase.console.utils.FolderDialog
import com.clorabase.console.utils.ListItem

@Composable
@Preview(showBackground = true)
fun DatabaseScreen() {
    val viewModel: DatabaseViewModel = viewModel()
    var showDocumentDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState();

    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp)
    ) {

        Text(
            text = "Add or delete document",
            fontSize = 30.sp,
            fontFamily = FontFamily(Font(R.font.anton)),
            color = Black,
            modifier = Modifier.padding(5.dp)
        )

        Text(
            text = "In our no-sql database, you can store your data in an easy and secure way. " +
                    "Each document is a JSON file that can contain any data you want. " +
                    "You can create, read, update, and delete documents and collections as needed.",
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(10.dp),
        )

        Text(
            text = "PATH : ${viewModel.currentPath.value}",
            color = Color.Red,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(10.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))

        when (val state = uiState) {
            is DatabaseUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DatabaseUiState.Success -> {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == -1){
                        val name = it.data?.getStringExtra("name")
                        val operation = it.data?.getStringExtra("operation")
                        viewModel.handleDocumentResult(name!!,operation!!);
                    }
                }

                if (showDocumentDialog)
                    AddDocumentDialog(
                        onDismissRequest = { showDocumentDialog = false },
                        onCreate = { name, path ->
                            launcher.launch(
                                Intent(context, DocumentActivity::class.java).apply {
                                    putExtra("name", name)
                                    putExtra("path", "$path/$name")
                                    putExtra("mode", "create")
                                }
                            )
                        }
                    )

                if (showCollectionDialog)
                    FolderDialog(
                        onDismiss = { showCollectionDialog = false },
                        onCreate = {viewModel.onNewCollectionClick(it)}
                    )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp)
                ) {
                    item {
                        BrowsableListCommons(
                            onBack = {viewModel.navigateBack()},
                            onFolderCLick = {
                                showCollectionDialog = true;
                            }
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .clickable(onClick = { showDocumentDialog = true }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.padding(end = 10.dp),
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add document",
                            )

                            Text("Add document", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(state.files) {
                        FileListItem(it){
                            when (it) {
                                is ListItem.Document -> {
                                    launcher.launch(Intent(context, DocumentActivity::class.java).apply {
                                        putExtra("path", it.doc.path)
                                        putExtra("name", it.doc.name)
                                        putExtra("mode", "update")
                                    })
                                }
                                is ListItem.Folder -> {
                                    viewModel.navigateIntoCollection(it.name);
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            is DatabaseUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message)
                }
            }
        }
    }
}


@Composable
fun AddDocumentDialog(onDismissRequest: () -> Unit, onCreate: (String,String) -> Unit) {
    var docName by remember { mutableStateOf("") }
    val viewModel: DatabaseViewModel = viewModel()
    val error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create Document", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it.filter { it.isDigit() || it.isLetter() } },
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (docName.isNotBlank()) {
                            onCreate("$docName.doc",viewModel.currentPath.value);
                            onDismissRequest()
                        }
                    }) {
                        Text("Create")
                    }

                    if (error.isNotBlank()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}