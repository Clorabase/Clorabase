package com.clorabase.console.models

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.Reason
import clorabase.sdk.java.storage.ClorabaseStorage
import clorabase.sdk.java.storage.ProgressListener
import clorabase.sdk.java.storage.StorageException
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import com.clorabase.console.utils.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.UnknownHostException

sealed class StorageUiState {
    data object Loading : StorageUiState()
    data class Success(val files: List<ListItem>) : StorageUiState()
    data class Error(val message: String) : StorageUiState()
}

sealed class StorageEvent {
    data class ShowToast(val message: String) : StorageEvent()
    data class ShowErrorDialog(val title: String, val message: String) : StorageEvent()
    data object ShowBlobConfigDialog : StorageEvent()
}

class StorageViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StorageEvent>()
    val events: SharedFlow<StorageEvent> = _events.asSharedFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val history = ArrayDeque<ClorabaseStorage>()
    private var currentStorage: ClorabaseStorage

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    init {
        currentStorage = Globals.clorabase.storage
        fetchFiles(currentStorage)
    }

    private fun fetchFiles(storage: ClorabaseStorage) {
        _uiState.value = StorageUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileNames = storage.listFiles()
                val files = fileNames.map { name ->
                    when {
                        name.endsWith(".ptr") -> ListItem.File(name)
                        name.contains(".") -> ListItem.File(name)
                        else -> ListItem.Folder(name)
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = StorageUiState.Success(files)
                }
            } catch (e: StorageException) {
                withContext(Dispatchers.Main) {
                    if (e.reason == Reason.NOT_EXISTS)
                        _uiState.value = StorageUiState.Success(emptyList())
                    else
                        _uiState.value = StorageUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun navigateIntoFolder(folderName: String) {
        history.addLast(currentStorage)
        currentStorage = currentStorage.directory(folderName)
        fetchFiles(currentStorage)
        _currentPath.value = _currentPath.value + "/" + folderName
    }

    fun navigateBack() {
        if (history.isNotEmpty()) {
            currentStorage = history.removeLast()
            fetchFiles(currentStorage)
            _currentPath.value = _currentPath.value.removeSuffix("/").substringBeforeLast("/")
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

                withContext(Dispatchers.Main) {
                    val currentState = _uiState.value
                    if (currentState is StorageUiState.Success) {
                        _uiState.value = StorageUiState.Success(
                            currentState.files.filterNot { it is ListItem.File && it.name == fileName }
                        )
                    }
                }
            } catch (e: StorageException) {
                _events.emit(StorageEvent.ShowToast("Failed to delete file."))
            }
        }
    }

    // Still requires Context for DownloadManager, which is acceptable in Android MVVM for System Services
    fun downloadFile(context: Context, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (fileName.endsWith(".ptr")) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = currentStorage.getBlobDownloadURL(fileName).toString().toUri()
                    val request = DownloadManager.Request(uri)
                        .setTitle(fileName)
                        .setDescription("Downloading file from Clorabase storage...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "Clorabase/${fileName.removeSuffix(".ptr")}"
                        )

                    downloadManager.enqueue(request)
                    _events.emit(StorageEvent.ShowToast("Your download has been started, check the notification"))
                } else {
                    saveFileToDownloads(currentStorage.getFile(fileName).inputStream(), fileName) { success, msg ->
                        viewModelScope.launch {
                            _events.emit(StorageEvent.ShowToast(msg))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(StorageEvent.ShowToast("Failed to download file."))
            }
        }
    }

    fun uploadFile(inputStream: InputStream, fileName: String) {
        _uploadProgress.value = 0
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentStorage.uploadFile(inputStream, fileName, object : ProgressListener {
                    override fun onProgress(bytesRead: Long, totalBytes: Long) {
                        _uploadProgress.value = (bytesRead * 100 / totalBytes).toInt()
                    }

                    override fun onComplete(result: String?) {
                        _uploadProgress.value = null
                    }

                    override fun onError(e: Exception?) {
                        val message = when (e) {
                            is UnknownHostException -> "No internet connection."
                            is StorageException -> when (e.reason) {
                                Reason.DOCUMENT_SIZE_EXCEEDED -> "File size exceeds the limit."
                                Reason.TOO_MANY_REQUESTS -> "Storage quota exceeded."
                                Reason.File_ALREADY_EXISTS -> "You have already uploaded this file. Delete the file with the same name from the storage and try again."
                                else -> "Storage error: ${e.message}"
                            }
                            else -> "Error: ${e?.message}"
                        }

                        viewModelScope.launch {
                            _uploadProgress.value = null
                            if (message.startsWith("You have already uploaded")) {
                                _events.emit(StorageEvent.ShowErrorDialog("File already exists", message))
                            } else {
                                _events.emit(StorageEvent.ShowToast(message))
                            }
                        }
                    }
                })

                withContext(Dispatchers.Main) {
                    val currentState = _uiState.value
                    if (currentState is StorageUiState.Success) {
                        val currentList = currentState.files.toMutableList()
                        currentList.add(ListItem.File(fileName))
                        _uiState.value = StorageUiState.Success(currentList)
                    }
                    _uploadProgress.value = null
                    _events.emit(StorageEvent.ShowToast("File uploaded successfully"))
                }
            } catch (e: StorageException) {
                withContext(Dispatchers.Main) {
                    _uploadProgress.value = null
                    _events.emit(StorageEvent.ShowToast("File upload failed: ${e.message}"))
                }
            }
        }
    }

    fun uploadBlob(inputStream: InputStream, fileName: String) {
        _uploadProgress.value = 0
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentStorage.uploadBlob(inputStream, fileName, object : ProgressListener {

                    override fun onProgress(bytesRead: Long, totalBytes: Long) {
                        _uploadProgress.value = (bytesRead * 100 / totalBytes).toInt()
                    }

                    override fun onError(e: Exception) {
                        e.printStackTrace()
                        val message = when (e) {
                            is FileNotFoundException -> "BLOB Storage not configured for this project"
                            is UnknownHostException -> "No internet connection."
                            is StorageException -> when (e.reason) {
                                Reason.DOCUMENT_SIZE_EXCEEDED -> "File size exceeds the limit."
                                Reason.TOO_MANY_REQUESTS -> "Storage quota exceeded."
                                Reason.File_ALREADY_EXISTS -> "You have already uploaded this BLOB. Please upload the existing .ptr file of the blob instead"
                                else -> "Storage error: ${e.message}"
                            }
                            else -> "Error: ${e.message}"
                        }

                        viewModelScope.launch {
                            _uploadProgress.value = null
                            if (e is FileNotFoundException) {
                                _events.emit(StorageEvent.ShowBlobConfigDialog)
                            } else {
                                _events.emit(StorageEvent.ShowErrorDialog("Error occurred", message))
                            }
                        }
                    }

                    override fun onComplete(result: String) {
                        viewModelScope.launch {
                            _uploadProgress.value = null
                            _events.emit(StorageEvent.ShowToast("Blob uploaded successfully"))

                            val currentState = _uiState.value
                            if (currentState is StorageUiState.Success) {
                                val currentList = currentState.files.toMutableList()
                                currentList.add(ListItem.File("$fileName.ptr"))
                                _uiState.value = StorageUiState.Success(currentList)
                            }
                        }
                    }
                })
            } catch (e: StorageException) {
                withContext(Dispatchers.Main) {
                    _uploadProgress.value = null
                    _events.emit(StorageEvent.ShowToast("Blob upload failed: ${e.message}"))
                }
            }
        }
    }

    fun configureBlob(onRetryUpload: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = Globals.currentProject.value
            try {
                GithubUtils.createStorageRelease(project!!)
                GithubUtils.updateJSON(project + "/config.json") {
                    it["blobConfigured"] = true
                }
                withContext(Dispatchers.Main) {
                    onRetryUpload()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(StorageEvent.ShowToast(e.message ?: "Configuration failed"))
            }
        }
    }

    private fun saveFileToDownloads(
        inputStream: InputStream,
        fileName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val clorabaseDir = File(downloadsDir, "Clorabase")

        if (!clorabaseDir.exists() && !clorabaseDir.mkdirs()) {
            onComplete(false, "Failed to create directory.")
            return
        }

        val file = File(clorabaseDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            onComplete(true, "File saved successfully to Downloads")
            inputStream.close()
        } catch (e: IOException) {
            Log.e("FileSaveFunction", "Error saving file: ${e.message}", e)
            onComplete(false, "Error saving file: ${e.message}")
        }
    }

    fun getFileNameFromUri(uri: Uri, context: Context): String {
        var fileName = "unknown"
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
}