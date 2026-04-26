package com.clorabase.console.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.database.ClorastoreException
import clorabase.sdk.java.database.Collection
import clorabase.sdk.java.database.Document
import com.clorabase.console.Globals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DocumentUiState {
    data object Loading : DocumentUiState()
    data class Success(val fields: List<DataField>) : DocumentUiState()
    data class Error(val message: String) : DocumentUiState()
}

sealed class DocumentEvent {
    data class DocumentSaved(val name: String, val operation: String) : DocumentEvent()
    data class DocumentDeleted(val name: String, val operation: String) : DocumentEvent()
    data class ShowToast(val message: String) : DocumentEvent()
}

data class DataField(
    var key: String,
    var value: String,
    val type: FieldType
)

sealed class FieldType {
    data object String : FieldType()
    data object Number : FieldType()
    data object Boolean : FieldType()
    data object Array : FieldType()
}

class DocumentViewModel : ViewModel() {
    private lateinit var doc: Document
    private lateinit var root: Collection
    private var isEditMode: Boolean = false
    private var documentName: String = ""

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DocumentEvent>()
    val events: SharedFlow<DocumentEvent> = _events.asSharedFlow()

    // Map of specific field keys to error messages
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    fun init(path: String, name: String, mode: String) {
        root = Globals.clorabase.database
        doc = root.documentAt(path)
        documentName = name.removeSuffix(".doc")
        isEditMode = mode == "update"

        if (isEditMode) {
            loadDocument()
        } else {
            _uiState.value = DocumentUiState.Success(emptyList())
        }
    }

    private fun loadDocument() {
        _uiState.value = DocumentUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = doc.fetch() ?: emptyMap()
                val fields = data.filterKeys { it != "_timestamp" }.map { (key, value) ->
                    val type = when (value) {
                        is String -> FieldType.String
                        is Number -> FieldType.Number
                        is Boolean -> FieldType.Boolean
                        is List<*> -> FieldType.Array
                        else -> FieldType.String
                    }
                    DataField(key, value.toString().removePrefix("[").removeSuffix("]"), type)
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = DocumentUiState.Success(fields)
                }
            } catch (e: ClorastoreException) {
                withContext(Dispatchers.Main) {
                    _uiState.value = DocumentUiState.Error(e.message ?: "An error occurred while loading the document.")
                }
            }
        }
    }

    fun onAddField(type: FieldType) {
        val currentState = _uiState.value
        if (currentState is DocumentUiState.Success) {
            val list = currentState.fields.toMutableList()
            list.add(DataField(key = "", value = "", type = type))
            _uiState.value = currentState.copy(fields = list)
        }
    }

    fun onFieldChange(index: Int, key: String, value: String) {
        val currentState = _uiState.value
        if (currentState is DocumentUiState.Success) {
            val updatedFields = currentState.fields.toMutableList()
            val oldKey = updatedFields[index].key

            updatedFields[index] = updatedFields[index].copy(key = key, value = value)
            _uiState.value = currentState.copy(fields = updatedFields)

            // Clear error for this field if it's being edited
            if (_fieldErrors.value.containsKey(oldKey) || _fieldErrors.value.containsKey(key)) {
                val updatedErrors = _fieldErrors.value.toMutableMap()
                updatedErrors.remove(oldKey)
                updatedErrors.remove(key)
                _fieldErrors.value = updatedErrors
            }
        }
    }

    fun onDeleteField(index: Int) {
        val currentState = _uiState.value
        if (currentState is DocumentUiState.Success) {
            val updatedFields = currentState.fields.toMutableList()
            val removedKey = updatedFields[index].key
            updatedFields.removeAt(index)
            _uiState.value = currentState.copy(fields = updatedFields)

            // Clean up any lingering errors
            val updatedErrors = _fieldErrors.value.toMutableMap()
            updatedErrors.remove(removedKey)
            _fieldErrors.value = updatedErrors
        }
    }

    fun onSaveDocument() {
        val currentState = _uiState.value
        if (currentState !is DocumentUiState.Success) {
            viewModelScope.launch { _events.emit(DocumentEvent.ShowToast("Cannot save. Invalid state.")) }
            return
        }

        _uiState.value = DocumentUiState.Loading
        _fieldErrors.value = emptyMap() // Reset errors

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val errors = mutableMapOf<String, String>()
                val fieldsToSave = currentState.fields
                    .filter { it.key.isNotBlank() && it.value.isNotBlank() }
                    .associate { field ->
                        try {
                            field.key to when (field.type) {
                                is FieldType.String -> field.value
                                is FieldType.Number -> field.value.toDouble()
                                is FieldType.Boolean -> field.value.toBooleanStrict()
                                is FieldType.Array -> field.value.split(",").map { it.trim() }
                            }
                        } catch (e: Exception) {
                            errors[field.key] = "Invalid format for ${field.type.javaClass.simpleName}"
                            field.key to "" // Dummy value, will be aborted anyway
                        }
                    }

                if (errors.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _fieldErrors.value = errors
                        _uiState.value = currentState // Restore the success state to show fields
                        _events.emit(DocumentEvent.ShowToast("Please fix the highlighted errors"))
                    }
                    return@launch
                }

                if (isEditMode) {
                    doc.update(fieldsToSave)
                } else {
                    doc.setData(fieldsToSave)
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = DocumentUiState.Success(currentState.fields)
                    val operation = if (isEditMode) "update" else "create"
                    _events.emit(DocumentEvent.DocumentSaved(documentName, operation))
                }
            } catch (e: ClorastoreException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _events.emit(DocumentEvent.ShowToast(e.message ?: "An error occurred during save."))
                    _uiState.value = currentState // Revert to showing form
                }
            }
        }
    }

    fun onDeleteDocument() {
        _uiState.value = DocumentUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                doc.delete()
                withContext(Dispatchers.Main) {
                    _events.emit(DocumentEvent.DocumentDeleted(documentName, "delete"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _events.emit(DocumentEvent.ShowToast(e.message ?: "An error occurred while deleting."))
                    // Revert state if failed
                    loadDocument()
                }
            }
        }
    }
}