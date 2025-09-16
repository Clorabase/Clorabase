package com.clorabase.console.models

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit
import clorabase.sdk.java.Clorabase
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import kotlinx.coroutines.withContext
import java.io.IOException

open class MainViewModel(private val config: SharedPreferences) : ViewModel() {
    val _projects = MutableStateFlow<MutableList<String>>(mutableListOf())
    val projects: StateFlow<List<String>> = _projects.asStateFlow()
    val currentScreen = MutableStateFlow("Home")

    init {
        Globals.username = config.getString("username", null) ?: ""
        Globals.token = config.getString("token",  null) ?: ""
    }

    fun fetchProjects(onError : (String) -> Unit = {}, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GithubUtils.init(Globals.token, Globals.username)
                _projects.value = GithubUtils.listFiles("")
                    .filter { !it.isFile }
                    .map { it.name }
                    .toMutableList()

                Globals.currentProject.value = config.getString("activeProject", projects.value.firstOrNull())
                if (Globals.currentProject.value != null){ // Just created the repo
                    Globals.clorabase = Clorabase.getInstance(
                        Globals.username,
                        Globals.token,
                        Globals.currentProject.value!!
                    )
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e : Exception) {
                withContext(Dispatchers.Main){onError("Failed to load projects: ${e.message}")}
            }
        }
    }

    fun onProjectSelected(project: String) {
        Globals.currentProject.value = project
        config.edit { putString("activeProject", project) };
    }


    class Factory(private val config: SharedPreferences) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(config) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}