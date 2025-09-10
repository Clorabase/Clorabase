package com.clorabase.console.models

import androidx.compose.runtime.mutableStateOf
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
import java.util.Stack


sealed class DatabaseUiState {
    data object Loading : DatabaseUiState()
    data class Success(val files: List<ListItem>) : DatabaseUiState()
    data class Error(val message: String) : DatabaseUiState()
}

class DatabaseViewModel : ViewModel() {
    private var collection: Collection

    private val _uiState = MutableStateFlow<DatabaseUiState>(DatabaseUiState.Loading)
    val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()

    private val history = Stack<List<ListItem>>()
    var currentPath = mutableStateOf("");

    init {
        collection = Globals.clorabase.database
        fetchFiles(collection)
    }

    private fun fetchFiles(collection: Collection) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = DatabaseUiState.Loading
                val files = mutableListOf<ListItem>()
                collection.documents.forEach {
                    files.add(ListItem.Document(it))
                }
                collection.collections.forEach{
                    files.add(ListItem.Folder(it))
                }

                _uiState.value = DatabaseUiState.Success(files.toList())
            } catch (e: ClorastoreException) {
                val errorMessage : String = when (e.reason){
                    Reason.NOT_EXISTS -> "Collection does not exist."
                    Reason.DOCUMENT_SIZE_EXCEEDED -> "Database size exceeded. Please delete the largest documents."
                    Reason.UNKNOWN -> "Unknown error occurred. Report on GitHub."
                    else -> {"An error occurred: ${e.reason}"}
                }

                if (e.reason == Reason.NOT_EXISTS)
                    _uiState.value = DatabaseUiState.Success(listOf())
                else
                    _uiState.value = DatabaseUiState.Error(errorMessage)
            }
        }
    }

    fun navigateIntoCollection(collectionName: String) {
        val currentState = _uiState.value
        if (currentState is DatabaseUiState.Success) {
            history.push(currentState.files);
            currentPath.value = currentPath.value + "/" + collectionName
            collection = collection.collection(collectionName);
            fetchFiles(collection)
        }
    }


    fun navigateBack() {
        if (history.isNotEmpty()) {
            _uiState.value = DatabaseUiState.Success(history.pop())
            currentPath.value = currentPath.value.removeSuffix("/").substringBeforeLast("/")
            collection = collection.parent;
        }
    }

    fun onNewCollectionClick(name : String) {
        _uiState.value = DatabaseUiState.Success(listOf());
        currentPath.value = currentPath.value + "/" + name
    }

    fun handleDocumentResult(name: String,operation : String) {
        val currentState = _uiState.value
        if (currentState is DatabaseUiState.Success) {
            val list = currentState.files.toMutableList()
            if (operation == "create") {
                list.add(ListItem.Document(collection.document(name)))
            } else if (operation == "delete") {
                list.removeIf { it is ListItem.Document && it.doc.name == name + ".doc" }
            }
            _uiState.value = DatabaseUiState.Success(list)
        }
    }
}