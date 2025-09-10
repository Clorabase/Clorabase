package com.clorabase.console.models


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.Clorabase
import clorabase.sdk.java.database.ClorastoreException
import clorabase.sdk.java.database.Collection
import clorabase.sdk.java.database.Document
import com.clorabase.console.Globals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

sealed class DocumentUiState {
    object Loading : DocumentUiState()
    data class Success(val fields: MutableList<DataField>) : DocumentUiState()
    data class Error(val message: String) : DocumentUiState()
}

data class DataField(
    var key: String,
    var value: String,
    val type: FieldType
)

sealed class FieldType {
    object String : FieldType()
    object Number : FieldType()
    object Boolean : FieldType()
    object Array : FieldType()
}

class DocumentViewModel : ViewModel() {
    private lateinit var doc : Document
    private lateinit var root : Collection
    public val errorMessage = mutableStateOf<String>("")
    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()
    private var isEditMode: Boolean = false;

    public fun init(path: String, mode: String) {
        root = Globals.clorabase.database
        doc = root.documentAt(path)
        isEditMode = mode == "update"
        if (isEditMode) {
            loadDocument()
        } else {
            _uiState.value = DocumentUiState.Success(mutableListOf())
        }
    }

    private fun loadDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = doc.fetch() ?: emptyMap()
                val fields = data.filterKeys { it != "_timestamp" }.map { (key, value) ->
                    val type = when (value) {
                        is String -> FieldType.String
                        is Number -> FieldType.Number
                        is Boolean -> FieldType.Boolean
                        is List<*> -> FieldType.Array
                        else -> FieldType.String // Default to String for other types
                    }
                    DataField(key, value.toString().removePrefix("[").removeSuffix("]"), type)
                }.toMutableList()
                _uiState.value = DocumentUiState.Success(fields)
            } catch (e: ClorastoreException) {
                _uiState.value = DocumentUiState.Error(e.message ?: "An error occurred while loading the document.")
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
            updatedFields[index] = updatedFields[index].copy(key = key, value = value)
            _uiState.value = currentState.copy(fields = updatedFields)
        }
    }

    fun onDeleteField(index: Int) {
        val currentState = _uiState.value
        if (currentState is DocumentUiState.Success) {
            val updatedFields = currentState.fields.toMutableList()
            updatedFields.removeAt(index)
            _uiState.value = currentState.copy(fields = updatedFields)
        }
    }

    fun onSaveDocument(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is DocumentUiState.Success) {
            onError("Cannot save. Invalid state.")
            return
        }

        _uiState.value = DocumentUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fieldsToSave = currentState.fields.filter {
                    it.key.isNotBlank() && it.value.isNotBlank()
                }.associate { field ->
                    try {
                        field.key to when (field.type) {
                            is FieldType.String -> field.value
                            is FieldType.Number -> field.value.toDouble()
                            is FieldType.Boolean -> field.value.toBooleanStrict()
                            is FieldType.Array -> field.value.split(",").map { it.trim() }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.value = currentState;
                        errorMessage.value = "Error parsing field '${field.key}': ${e.message}"
                        return@launch;
                    }
                }

                if (isEditMode) {
                    doc.update(fieldsToSave)
                } else {
                    doc.setData(fieldsToSave)
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                    _uiState.value = DocumentUiState.Success(currentState.fields)
                }
            } catch (e: ClorastoreException) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    onError(e.message ?: "An error occurred during save.")
                    _uiState.value = DocumentUiState.Error(e.message ?: "An error occurred during save.")
                }
            }
        }
    }

    fun onDeleteDocument(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.value = DocumentUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                doc.delete()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "An error occurred while deleting the document.")
                }
            }
        }
    }
}