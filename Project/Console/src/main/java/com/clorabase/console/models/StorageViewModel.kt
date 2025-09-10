package com.clorabase.console.models

import android.app.AlertDialog
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.traceEventEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.Clorabase
import clorabase.sdk.java.Reason
import clorabase.sdk.java.storage.ClorabaseStorage
import clorabase.sdk.java.storage.ProgressListener
import clorabase.sdk.java.storage.StorageException
import com.clorabase.console.Globals
import com.clorabase.console.utils.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.UnknownHostException
import java.util.*
import androidx.core.net.toUri

sealed class StorageUiState {
    data object Loading : StorageUiState()
    data class Success(val files: List<ListItem>) : StorageUiState()
    data class Error(val message: String) : StorageUiState()
}

class StorageViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    private val history = Stack<ClorabaseStorage>()
    private var currentStorage: ClorabaseStorage

    init {
        currentStorage = Globals.clorabase.storage
        fetchFiles(currentStorage)
    }

    private fun fetchFiles(storage: ClorabaseStorage) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = StorageUiState.Loading
            try {
                val fileNames = storage.listFiles()
                val files = fileNames.map { name ->
                    when {
                        name.endsWith(".ptr") -> ListItem.File(name)
                        name.contains(".") -> ListItem.File(name)
                        else -> ListItem.Folder(name)
                    }
                }
                _uiState.value = StorageUiState.Success(files)
            } catch (e: StorageException) {
                if (e.cause is FileNotFoundException)
                    _uiState.value = StorageUiState.Success(listOf())
                else
                    _uiState.value = StorageUiState.Error(e.message!!)
            }
        }
    }

    fun navigateIntoFolder(folderName: String) {
        history.push(currentStorage)
        currentStorage = currentStorage.directory(folderName)
        fetchFiles(currentStorage)
    }

    fun navigateBack() {
        if (history.isNotEmpty()) {
            currentStorage = history.pop()
            fetchFiles(currentStorage)
        }
    }

    fun deleteFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (fileName.endsWith(".ptr")) {
                    currentStorage.deleteBlob(fileName.removeSuffix(".ptr"))
                } else {
                    currentStorage.delete(fileName)
                }
                _uiState.value = StorageUiState.Success(
                    (uiState.value as? StorageUiState.Success)?.files?.filterNot {
                        it is ListItem.File && it.name == fileName
                    } ?: listOf()
                )
            } catch (e: StorageException) {
                _uiState.value = StorageUiState.Error("Failed to delete file.")
            }
        }
    }

    fun downloadFile(context: Context, fileName: String, onDownloadComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (fileName.endsWith(".ptr")) {
                    val downloadManager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = currentStorage.getBlobDownloadURL(fileName).toString().toUri()
                    val request = DownloadManager.Request(uri)
                        .setTitle(fileName)
                        .setDescription("Downloading file from Clorabase storage...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "Clorabase/${fileName.removeSuffix(".ptr")}"
                        )

                    val id = downloadManager.enqueue(request)
                    withContext(Dispatchers.Main) {
                        onDownloadComplete("Your download has been started, check the notification")
                    }
                } else {
                    saveFileToDownloads(
                        currentStorage.getFile(fileName).inputStream(),
                        fileName
                    ) { success, msg ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onDownloadComplete(msg);
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onDownloadComplete("Failed to download file.")
                }
            }
        }
    }

    fun uploadFile(context: Context, file: Uri) {
        val pd = ProgressDialog(context)
        pd.setTitle("Uploading file")
        pd.setMessage("Please wait while the file is being uploaded...")
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setCancelable(true)
        pd.show()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = getFileNameFromUri(file, context);
                val inputStream = context.contentResolver.openInputStream(file)!!
                currentStorage.addFile(inputStream.readBytes(), fileName)

                val currentList = (uiState.value as StorageUiState.Success).files.toMutableList();
                currentList.add(ListItem.File(fileName))
                _uiState.value = StorageUiState.Success(currentList);

                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: StorageException) {
                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    Toast.makeText(context, "File upload failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun uploadBlob(context: Context, file: Uri) {
        val pd = ProgressDialog(context)
        pd.setTitle("Uploading Blob")
        pd.setMessage("Please wait while the blob is being uploaded...")
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        pd.setCancelable(true)
        pd.max = 100
        pd.show()

        viewModelScope.launch(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(file)
            val fileName = getFileNameFromUri(file, context);
            try {
                currentStorage.uploadBlob(inputStream!!, fileName, object : ProgressListener {

                    override fun onProgress(bytesRead: Long, totalBytes: Long) {
                        val progress = (bytesRead * 100 / totalBytes).toInt()
                        viewModelScope.launch(Dispatchers.Main) {
                            pd.progress = progress
                        }
                    }

                    override fun onError(e: Exception) {
                        val message = when (e) {
                            is UnknownHostException -> "No internet connection."
                            is StorageException -> when (e.reason) {
                                Reason.DOCUMENT_SIZE_EXCEEDED -> "File size exceeds the limit."
                                Reason.TOO_MANY_REQUESTS -> "Storage quota exceeded."
                                Reason.File_ALREADY_EXISTS -> "You have already uploaded this BLOB. Please upload the existing .ptr file of the blob instead"
                                else -> "Storage error: ${e.message}"
                            }

                            else -> "Error: ${e.message}"
                        }

                        viewModelScope.launch(Dispatchers.Main) {
                            pd.dismiss()
                            if (message.startsWith("You have already uploaded")) {
                                AlertDialog.Builder(context)
                                    .setTitle("Blobl already exists")
                                    .setMessage(message)
                                    .setPositiveButton("Okey", null)
                                    .show();
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                            }
                        }
                    }


                    override fun onComplete() {
                        viewModelScope.launch(Dispatchers.Main) {
                            pd.dismiss()
                            Toast.makeText(
                                context,
                                "Blob uploaded successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        val currentList = (uiState.value as StorageUiState.Success).files.toMutableList();
                        currentList.add(ListItem.File("$fileName.ptr"))
                        _uiState.value = StorageUiState.Success(currentList);
                    }
                })
            } catch (e: StorageException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Blob upload failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri, context: Context): String {
        var fileName: String = "unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }


    private fun saveFileToDownloads(
        inputStream: InputStream,
        fileName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val clorabaseDir = File(downloadsDir, "Clorabase")
        if (!clorabaseDir.exists()) {
            if (!clorabaseDir.mkdirs()) {
                onComplete(false, "Failed to create directory.")
                return
            }
        }

        val file = File(clorabaseDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024) // 4KB buffer
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }

            onComplete(true, "File saved successfully to Downloads")
            inputStream.close();
        } catch (e: IOException) {
            Log.e("FileSaveFunction", "Error saving file: ${e.message}", e)
            onComplete(false, "Error saving file: ${e.message}")
        }
    }
}