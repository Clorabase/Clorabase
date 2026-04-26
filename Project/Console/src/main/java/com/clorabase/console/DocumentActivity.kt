package com.clorabase.console

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.models.DataField
import com.clorabase.console.models.DocumentEvent
import com.clorabase.console.models.DocumentUiState
import com.clorabase.console.models.DocumentViewModel
import com.clorabase.console.models.FieldType
import com.clorabase.console.theme.ClorabaseTheme
import kotlinx.coroutines.flow.collectLatest

class DocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClorabaseTheme {
                val path = intent.getStringExtra("path") ?: ""
                val name = intent.getStringExtra("name") ?: ""
                val mode = intent.getStringExtra("mode") ?: "create"
                val viewModel: DocumentViewModel = viewModel()

                // Initialize once
                LaunchedEffect(Unit) {
                    viewModel.init(path, name, mode)
                }

                DocumentScreen(
                    viewModel = viewModel,
                    mode = mode,
                    onFinishActivity = { resultName, operation ->
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("name", resultName)
                            putExtra("operation", operation)
                        })
                        finish()
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel,
    mode: String,
    onFinishActivity: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()
    val context = LocalContext.current
    var showAddFieldDialog by remember { mutableStateOf(false) }

    // React to ViewModel events securely
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DocumentEvent.DocumentSaved -> {
                    Toast.makeText(context, "Document saved successfully", Toast.LENGTH_SHORT).show()
                    onFinishActivity(event.name, event.operation)
                }
                is DocumentEvent.DocumentDeleted -> {
                    Toast.makeText(context, "Document deleted successfully", Toast.LENGTH_SHORT).show()
                    onFinishActivity(event.name, event.operation)
                }
                is DocumentEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == "update") "Edit Document" else "Create Document") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.onSaveDocument() }) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_save), contentDescription = "Save document")
                    }
                    if (mode == "update") {
                        IconButton(onClick = { viewModel.onDeleteDocument() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete document")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFieldDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add field")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

                // Modernized Note Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Important Guidelines", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Key names cannot contain spaces.\n• Separate Array values with commas (e.g. v1,v2).\n• Nested objects are currently unsupported.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (val state = uiState) {
                    is DocumentUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DocumentUiState.Success -> {
                        if (state.fields.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "No fields added yet.\nTap '+' to add data.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                itemsIndexed(state.fields) { index, field ->
                                    val errorMsg = fieldErrors[field.key]
                                    DocumentField(
                                        field = field,
                                        errorMsg = errorMsg,
                                        onDelete = { viewModel.onDeleteField(index) },
                                        onKeyChange = { newKey -> viewModel.onFieldChange(index, newKey.replace(" ", ""), field.value) },
                                        onValueChange = { newValue -> viewModel.onFieldChange(index, field.key, newValue) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) } // Padding for FAB
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
    errorMsg: String?,
    onDelete: () -> Unit,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val isError = errorMsg != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = field.key,
                    onValueChange = onKeyChange,
                    label = { Text("Key") },
                    isError = isError,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = field.value,
                    onValueChange = onValueChange,
                    label = { Text(field.type.javaClass.simpleName) },
                    isError = isError,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f),
                    trailingIcon = {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Clear, contentDescription = "Delete field", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            AnimatedVisibility(visible = isError) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
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
        title = { Text("Select Field Type", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldTypeItem(icon = Icons.Default.List, title = "String", description = "Standard text value") { onTypeSelected(FieldType.String) }
                FieldTypeItem(icon = Icons.Default.Add, title = "Number", description = "Integers and decimals") { onTypeSelected(FieldType.Number) }
                FieldTypeItem(icon = Icons.Default.Check, title = "Boolean", description = "True or False") { onTypeSelected(FieldType.Boolean) }
                FieldTypeItem(icon = Icons.Default.List, title = "Array", description = "Comma-separated list") { onTypeSelected(FieldType.Array) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun FieldTypeItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}