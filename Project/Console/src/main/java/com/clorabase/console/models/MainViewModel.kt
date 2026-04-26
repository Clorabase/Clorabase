package com.clorabase.console.models

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.Clorabase
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import com.clorabase.console.screens.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.util.Date

sealed class AppState {
    data object Splash : AppState()
    data object Login : AppState()
    data object Main : AppState()
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Error(val message: String) : LoginState()
}

class MainViewModel(private val config: SharedPreferences) : ViewModel() {
    private val _appState = MutableStateFlow<AppState>(AppState.Splash)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // Login States
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    // Main States
    private val _projects = MutableStateFlow<MutableList<String>>(mutableListOf())
    val projects: StateFlow<List<String>> = _projects.asStateFlow()
    
    val currentScreen = MutableStateFlow(Screen.Home.title)
    val showAddProjectDialog = MutableStateFlow(false)

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val savedToken = config.getString("token", null)
        val savedUsername = config.getString("username", null)
        
        if (!savedToken.isNullOrEmpty() && !savedUsername.isNullOrEmpty()) {
            Globals.token = savedToken
            Globals.username = savedUsername
            fetchProjects()
        } else {
            _appState.value = AppState.Login
        }
    }

    fun onTokenChange(newToken: String) {
        _token.value = newToken
    }

    fun login() {
        if (_loginState.value is LoginState.Loading) return

        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GithubUtils.init(_token.value)
                GithubUtils.createClorabaseRepo()
                val username = GithubUtils.username
                
                // Save to SharedPreferences & Globals
                config.edit {
                    putString("token", _token.value)
                    putString("username", username)
                }
                Globals.token = _token.value
                Globals.username = username

                withContext(Dispatchers.Main) {
                    _loginState.value = LoginState.Idle
                    fetchProjects() // Proceed to fetch data
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loginState.value = LoginState.Error(e.message ?: "Login failed")
                }
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun fetchProjects(onError: (String) -> Unit = {}) {
        _appState.value = AppState.Splash
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GithubUtils.init(Globals.token)
                val fetchedProjects = GithubUtils.listFiles("")
                    .filter { !it.isFile }
                    .map { it.name }
                    .toMutableList()

                _projects.value = fetchedProjects
                val activeProject = config.getString("activeProject", fetchedProjects.firstOrNull())
                Globals.currentProject.value = activeProject

                if (activeProject != null) {
                    Globals.initClorabase()
                }

                withContext(Dispatchers.Main) {
                    if (fetchedProjects.isEmpty()) {
                        showAddProjectDialog.value = true
                    }
                    _appState.value = AppState.Main
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e.message?.contains("Bad credentials") == true) {
                        logout()
                    }
                    onError("Failed to load projects: ${e.message}")
                }
            }
        }
    }

    fun createProject(projectName: String, configureStorage: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val jsonConfig = JSONObject().apply {
            put("project", projectName)
            put("created", Date().toString())
            put("blobConfigured", configureStorage)
            put("version", Globals.VERSION)
            put("storageBucketName", projectName)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                GithubUtils.create(jsonConfig.toString().toByteArray(), "$projectName/config.json")
                if (configureStorage) {
                    GithubUtils.createStorageRelease(projectName);
                }
                withContext(Dispatchers.Main) {
                    _projects.value.add(projectName)
                    showAddProjectDialog.value = false
                    onProjectSelected(projectName);
                    onSuccess()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    when (e) {
                        is FileAlreadyExistsException -> onError("Project with this name already exists")
                        is FileNotFoundException -> createProject(projectName, configureStorage, onSuccess, onError) // Retry logic
                        else -> onError("Failed to create project: ${e.message}")
                    }
                }
            }
        }
    }

    fun onProjectSelected(project: String) {
        currentScreen.value = Screen.Home.title
        Globals.currentProject.value = project
        config.edit { putString("activeProject", project) }
        _appState.value = AppState.Splash
        viewModelScope.launch(Dispatchers.IO) {
            Globals.initClorabase()
            _appState.value = AppState.Main
        }
    }

    private fun logout() {
        config.edit { clear() }
        Globals.token = ""
        Globals.username = ""
        _appState.value = AppState.Login
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