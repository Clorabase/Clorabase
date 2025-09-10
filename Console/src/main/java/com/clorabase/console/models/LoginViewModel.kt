package com.clorabase.console.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpException
import java.net.UnknownHostException

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val token: String, val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    fun onTokenChange(newToken: String) {
        _token.value = newToken
    }

    fun login() {
        if (_loginState.value is LoginState.Loading) {
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val github = GitHub.connectUsingOAuth(_token.value)
                val username = github.myself.login
                try {
                    github.createRepository("Clorabase-projects")
                        .owner(username)
                        .autoInit(true)
                        .description("This repo is created by clorabase console and is totally of internal use.")
                        .create()
                } catch (e: HttpException) {
                    Log.e("LoginViewModel", "Repo creation failed, but may already exist.", e)
                }

                _loginState.value = LoginState.Success(_token.value, username)
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = when (e) {
                    is UnknownHostException -> "Check your internet connection!"
                    else -> "Invalid token!"
                }
                _loginState.value = LoginState.Error(errorMessage)
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}