package com.clorabase.console

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.models.DataField
import com.clorabase.console.models.DocumentUiState
import com.clorabase.console.models.DocumentViewModel
import com.clorabase.console.models.FieldType
import com.clorabase.console.theme.ClorabaseTheme
import org.json.JSONArray


public class DocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClorabaseTheme {
                val path = intent.getStringExtra("path") ?: ""
                val name = intent.getStringExtra("name") ?: ""
                val mode = intent.getStringExtra("mode") ?: "view"
                val viewModel: DocumentViewModel = viewModel()
                viewModel.init(path, mode)


                DocumentScreen(
                    viewModel = viewModel,
                    mode = mode,
                    onDocumentSaved = {
                        Toast.makeText(this, "Document saved successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("name", name.removeSuffix(".doc"))
                            putExtra("operation", mode);
                        })
                        finish()
                    },
                    onDocumentDeleted = {
                        Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("name", name.removeSuffix(".doc"))
                            putExtra("operation", "delete");
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel = viewModel(),
    mode: String = "create",
    onDocumentSaved: () -> Unit = {},
    onDocumentDeleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") };

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == "edit") "Edit document" else "Create document") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.onSaveDocument(
                            onSuccess = { onDocumentSaved() },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_save), contentDescription = "Save document")
                    }
                    if (mode == "edit") {
                        IconButton(onClick = {
                            viewModel.onDeleteDocument(
                                onSuccess = { onDocumentDeleted() },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete document")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFieldDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add field")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("**NOTE**", fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp));
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = """
                        1. Document key should not contain spaces.
                        2. For array type, use comma (,) to separate values. e.g., value1,value2,value3
                        3. Nested objects are not supported yet.
                    """.trimIndent(),
                        color = Color.Black,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (val state = uiState) {
                    is DocumentUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DocumentUiState.Success -> {
                        LazyColumn {
                            itemsIndexed(state.fields) { index, field ->
                                DocumentField(
                                    field = field,
                                    onDelete = {
                                        viewModel.onDeleteField(index)
                                    },
                                    onKeyChange = { newKey ->
                                        viewModel.onFieldChange(index, newKey.replace(" ",""), field.value)
                                    },
                                    onValueChange = { newValue ->
                                        viewModel.onFieldChange(index, field.key, newValue);
                                    },
                                    errorMessage = viewModel.errorMessage
                                )
                            }
                        }
                    }
                    is DocumentUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddFieldDialog) {
        AddDocumentFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onTypeSelected = { type ->
                viewModel.onAddField(type)
                showAddFieldDialog = false
            }
        )
    }
}


@Composable
fun DocumentField(
    field: DataField,
    onDelete: () -> Unit,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    errorMessage: MutableState<String>,
) {
    val isError = errorMessage.value.isNotBlank() && errorMessage.value.contains(field.key);

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = field.key,
                singleLine = true,
                onValueChange = onKeyChange,
                label = { Text("Key") },
                isError = isError,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = field.value,
                onValueChange = onValueChange,
                isError = isError,
                label = { Text("Value (${field.type.javaClass.simpleName})") },
                modifier = Modifier.weight(2f)
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete field", tint = Color.Black)
            }
        }

        if (isError) {
            Text(
                text = errorMessage.value,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}


@Composable
fun AddDocumentFieldDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (FieldType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Field Type") },
        text = {
            Column {
                TextButton(onClick = { onTypeSelected(FieldType.String) }) {
                    Text("String")
                }
                TextButton(onClick = { onTypeSelected(FieldType.Number) }) {
                    Text("Number")
                }
                TextButton(onClick = { onTypeSelected(FieldType.Boolean) }) {
                    Text("Boolean")
                }
                TextButton(onClick = { onTypeSelected(FieldType.Array) }) {
                    Text("Array")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}