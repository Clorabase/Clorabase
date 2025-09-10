package com.clorabase.console

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clorabase.console.theme.Black
import com.clorabase.console.theme.ClorabaseTheme
import com.clorabase.console.theme.Green
import com.clorabase.console.models.LoginState
import com.clorabase.console.models.LoginViewModel

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClorabaseTheme {
                val loginViewModel: LoginViewModel by viewModels();

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { token, username ->
                        getSharedPreferences("main",0).edit().apply {
                            putString("token", token)
                            putString("username", username)
                            apply()
                        }
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    },
                    showToast = { message ->
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (token: String, username: String) -> Unit,
    showToast: (message: String) -> Unit
) {
    val token by viewModel.token.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFDBFFDB)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app),
                contentDescription = "App Icon",
                modifier = Modifier.padding(bottom = 50.dp).size(150.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome!",
                fontSize = 30.sp,
                color = Black,
                fontFamily = FontFamily(Font(R.font.anton))
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Please login with your github token to continue",
                fontSize = 18.sp,
                color = Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { viewModel.onTokenChange(it) },
                label = { Text("Github Token") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.login() },
                enabled = loginState !is LoginState.Loading,
                shape = RectangleShape,
                modifier = Modifier.wrapContentSize()
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        color = Green,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Login")
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Login Arrow"
                        )
                    }
                }
            }
        }
    }

    when (val state = loginState) {
        is LoginState.Success -> {
            onLoginSuccess(state.token, state.username)
            viewModel.resetLoginState()
        }

        is LoginState.Error -> {
            showToast(state.message)
            viewModel.resetLoginState()
        }

        else -> Unit
    }
}

@Preview
@Composable
private fun Screen() {
    ClorabaseTheme {
        LoginScreen(
            viewModel = LoginViewModel(),
            onLoginSuccess = { _, _ -> },
            showToast = {}
        )
    }
}