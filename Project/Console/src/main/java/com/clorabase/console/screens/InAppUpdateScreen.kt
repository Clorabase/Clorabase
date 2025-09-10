package com.clorabase.console.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clorabase.console.R
import com.clorabase.console.models.UpdateViewModel
import java.net.MalformedURLException
import java.net.URL

@Preview(showBackground = true)
@Composable
fun UpdateScreen() {
    val viewModel: UpdateViewModel = viewModel()
    val showDialog = remember { mutableStateOf(false) }
    val packageName = remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()){

        FloatingActionButton(
            onClick = { showDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(3f)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Push new update")
        }

        if (packageName.value.isBlank())
            AppsList(viewModel,packageName)
        else
            MainContent(viewModel,packageName.value)

        if (showDialog.value) {
            val context = LocalContext.current;
            PushUpdateDialog(
                onDismiss = { showDialog.value = false },
                onPushUpdate = { versionCode, versionName, pName, downloadUrl, updateType ->
                    viewModel.pushUpdate(versionCode, versionName, pName, downloadUrl, updateType)
                    showDialog.value = false
                    Toast.makeText(context,"Update pushed successfully",Toast.LENGTH_SHORT).show();                }
            )
        }
    }
}

@Composable
fun AppsList(viewModel: UpdateViewModel, packageName: MutableState<String>) {
    var fetching by remember { mutableStateOf(false) };
    val apps = remember { mutableStateOf(listOf("")) };

    Column(Modifier.padding(16.dp)) {
        Text(
            "Choose your app,",
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.anton)),
            fontSize = 30.sp
        )

        Text(
            "List of the apps you have in your project. Click on the app package to view it's update details",
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Spacer(Modifier.height(10.dp))
        HorizontalDivider()

        if (fetching)
            CircularProgressIndicator()
        else {
            LazyColumn {
                items(apps.value){
                    Row(Modifier.fillMaxWidth().padding(10.dp).clickable {
                        packageName.value = it;
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_android), tint = Color.Green, contentDescription = "App")
                        Text(text = it, fontSize = 18.sp, modifier = Modifier.padding(6.dp).fillMaxWidth());
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.fetchApps {
                apps.value = it;
                fetching = false;
            }
        }
    }
}

@Composable
fun MainContent(viewModel: UpdateViewModel,packageName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        LaunchedEffect(Unit) {
            viewModel.fetchUpdateDetails(packageName)
        }

        when {
            viewModel.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (viewModel.error.equals("No update details found."))
                        Text("No update information available. Push a new update to get started.")
                    else
                        Text("Error: ${viewModel.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            viewModel.updateInfo != null -> {
                UpdateDetailsCard(viewModel.updateInfo!!)
                Spacer(modifier = Modifier.height(24.dp))
                ChangelogDisplay(viewModel.changelog)
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No update information available. Push a new update to get started.")
                }
            }
        }
    }
}

@Composable
fun UpdateDetailsCard(info: UpdateViewModel.UpdateInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Latest App Update", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            UpdateDetailRow("Version Name:", info.versionName)
            UpdateDetailRow("Version Code:", info.versionCode.toString())
            UpdateDetailRow("Update Type:", info.updateType) // Added Update Type
            UpdateDetailRow("Release Date:", info.releaseDate)
            UpdateDetailRow("Total Downloads:", info.downloadCount.toString())
        }
    }
}

@Composable
fun UpdateDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.width(150.dp))
        Text(value, color = Color.Black) // Used Color.Black
    }
}

@Composable
fun ChangelogDisplay(changelog: String?) {
    Column {
        Text("Changelogs", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(changelog ?: "No changelog.", modifier = Modifier.padding(16.dp), color = Color.Black) // Used Color.Black
        }
    }
}

// Inside your PushUpdateDialog.kt file

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushUpdateDialog(
    onDismiss: () -> Unit,
    onPushUpdate: (Int, String, String, String?, String) -> Unit
) {
    var versionCode by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var updateType by remember { mutableStateOf("Flexible") }
    var expanded by remember { mutableStateOf(false) }
    val updateTypes = listOf("Flexible", "Urgent")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Push New Update") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it.filter { char -> char.isLetterOrDigit() || char == '.' } },
                    label = { Text("Version Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { versionCode = it.filter { char -> char.isDigit() } },
                    label = { Text("Version Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it.filter { char -> char.isLetterOrDigit() || char == '.' } },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("Download URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = updateType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Update Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        updateTypes.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    updateType = selection
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val context = LocalContext.current;
            TextButton(
                onClick = {
                    val code = versionCode.toIntOrNull()
                    if (code != null && versionName.isNotBlank() && packageName.isNotBlank() && downloadUrl.isNotBlank()) {
                        try {
                            URL(downloadUrl);
                            onPushUpdate(code, versionName, packageName, downloadUrl, updateType)
                        } catch (e : MalformedURLException){
                            Toast.makeText(context,"Invalid URL",Toast.LENGTH_SHORT).show();
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Push Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}