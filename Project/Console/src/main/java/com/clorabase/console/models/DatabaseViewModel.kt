package com.clorabase.console.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.Reason
import clorabase.sdk.java.database.ClorastoreException
import clorabase.sdk.java.database.Collection
import com.clorabase.console.Globals
import com.clorabase.console.utils.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DatabaseUiState {
    data object Loading : DatabaseUiState()
    data class Success(val files: List<ListItem>) : DatabaseUiState()
    data class Error(val message: String) : DatabaseUiState()
}

class DatabaseViewModel : ViewModel() {
    private var collection: Collection

    private val _uiState = MutableStateFlow<DatabaseUiState>(DatabaseUiState.Loading)
    val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()

    private val history = ArrayDeque<List<ListItem>>()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    init {
        collection = Globals.clorabase.database // Root collection first time when screen is opened
        fetchFiles(collection)
    }

    private fun fetchFiles(collection: Collection) {
        _uiState.value = DatabaseUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = mutableListOf<ListItem>()
                collection.documents.forEach {
                    files.add(ListItem.Document(it))
                }
                collection.collections.forEach {
                    files.add(ListItem.Folder(it))
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = DatabaseUiState.Success(files.toList())
                }
            } catch (e: ClorastoreException) {
                val errorMessage: String = when (e.reason) {
                    Reason.NOT_EXISTS -> "Collection does not exist."
                    Reason.DOCUMENT_SIZE_EXCEEDED -> "Database size exceeded. Please delete the largest documents."
                    Reason.UNKNOWN -> "Unknown error occurred. Report on GitHub."
                    else -> "An error occurred: ${e.reason}"
                }

                withContext(Dispatchers.Main) {
                    if (e.reason == Reason.NOT_EXISTS)
                        _uiState.value = DatabaseUiState.Success(emptyList())
                    else
                        _uiState.value = DatabaseUiState.Error(errorMessage)
                }
            }
        }
    }

    fun navigateIntoCollection(collectionName: String) {
        val currentState = _uiState.value
        if (currentState is DatabaseUiState.Success) {
            history.addLast(currentState.files)
            _currentPath.value = _currentPath.value + "/" + collectionName
            collection = collection.collection(collectionName)
            fetchFiles(collection)
        }
    }

    fun navigateBack() {
        if (history.isNotEmpty()) {
            _uiState.value = DatabaseUiState.Success(history.removeLast())
            _currentPath.value = _currentPath.value.removeSuffix("/").substringBeforeLast("/")
            collection = collection.parent
        }
    }

    fun onNewCollectionClick(name: String) {
        _uiState.value = DatabaseUiState.Success(emptyList())
        _currentPath.value = _currentPath.value + "/" + name
        viewModelScope.launch(Dispatchers.IO) {
            try {
                collection = collection.collection(name)
                collection.document("index").setData(mapOf("collection" to name))
                withContext(Dispatchers.Main) {
                    handleDocumentResult("index", "create")
                }
            } catch (e: ClorastoreException) {
                if (e.reason == Reason.UNKNOWN) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = DatabaseUiState.Error("Unknown error occurred. Report on GitHub.")
                    }
                }
            }
        }
    }

    fun handleDocumentResult(name: String, operation: String) {
        val currentState = _uiState.value
        if (currentState is DatabaseUiState.Success) {
            val list = currentState.files.toMutableList()
            if (operation == "create") {
                list.add(ListItem.Document(collection.document(name)))
            } else if (operation == "delete") {
                list.removeIf { it is ListItem.Document && it.doc.name == name }
            }
            _uiState.value = DatabaseUiState.Success(list)
        }
    }
}