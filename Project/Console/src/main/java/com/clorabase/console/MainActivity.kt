package com.clorabase.console

import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.clorabase.console.models.AppState
import com.clorabase.console.models.LoginState
import com.clorabase.console.models.MainViewModel
import com.clorabase.console.screens.*
import com.clorabase.console.theme.Black
import com.clorabase.console.theme.ClorabaseTheme
import com.clorabase.console.theme.Green
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (!isConnected) {
            AlertDialog.Builder(this)
                .setTitle("No internet connection")
                .setMessage("Please check your internet connection and try again.")
                .setPositiveButton("Retry") { _, _ -> recreate() }
                .setNegativeButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        val config = getSharedPreferences("main", 0)

        setContent {
            ClorabaseTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(config))
                val appState by viewModel.appState.collectAsState()
                val showAddProjectDialog by viewModel.showAddProjectDialog.collectAsState()
                val context = LocalContext.current

                when (appState) {
                    is AppState.Splash -> {
                        SplashScreen()
                    }
                    is AppState.Login -> {
                        LoginScreen(
                            viewModel = viewModel,
                            showToast = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    is AppState.Main -> {
                        val projects by viewModel.projects.collectAsState()
                        MainScreen(viewModel, projects)

                        if (showAddProjectDialog) {
                            AddProjectDialog(
                                viewModel = viewModel,
                                onDismiss = { viewModel.showAddProjectDialog.value = false },
                                onProjectAdded = { projectName ->
                                    Toast.makeText(context, "Project $projectName added", Toast.LENGTH_SHORT).show()
                                    (context as ComponentActivity).viewModelStore.clear();
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
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

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Error) {
            showToast((loginState as LoginState.Error).message)
            viewModel.resetLoginState()
        }
    }
}

@Composable
private fun SplashScreen() {
    val gradients = listOf(
        listOf(Color(0xFFE8CF70), Color(0xFFAAF8EB)),
        listOf(Color(0xFFFCF4DF), Color(0xFFD66FEE)),
        listOf(Color(0xFF84FFC9), Color(0xFFAAB2FF), Color(0xFFECA0FF)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradients.random()
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Clorabase Logo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading...",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator(color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, projects: List<String>) {
    val currentProject by Globals.currentProject.collectAsState()
    val screenTitle by viewModel.currentScreen.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scrollState = rememberScrollState()
    val bstack = remember { mutableStateListOf<Screen>(Screen.Home) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                Modifier.fillMaxWidth(0.7f),
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(Modifier.verticalScroll(scrollState)) {
                    DrawerHeader()
                    DrawerItems(viewModel) { icon ->
                        coroutineScope.launch { drawerState.close() }
                        when (icon) {
                            R.drawable.ic_db -> bstack.add(Screen.Database)
                            R.drawable.ic_storage -> bstack.add(Screen.Storage)
                            R.drawable.ic_push -> bstack.add(Screen.PushNotifications)
                            R.drawable.ic_chat -> bstack.add(Screen.InAppMessaging)
                            R.drawable.ic_update -> bstack.add(Screen.InAppUpdates)
                            R.drawable.ic_key -> bstack.add(Screen.Quota)
                            else -> bstack.add(Screen.Home)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = screenTitle) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        val context = LocalActivity.current as ComponentActivity;
                        ProjectDropdown(projects, currentProject) {
                            viewModel.onProjectSelected(it)
                            context.viewModelStore.clear();
                        }
                        IconButton({ viewModel.showAddProjectDialog.value = true }) {
                            Icon(Icons.Filled.Add, "Add project")
                        }
                    }
                )
            }
        ) { padding ->
            NavDisplay(
                backStack = bstack,
                modifier = Modifier.padding(padding),
                onBack = {
                    bstack.removeLastOrNull()
                    if (bstack.isNotEmpty()) viewModel.currentScreen.value = bstack.last().title
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<Screen.Home> {
                        HomeScreen {
                            bstack.add(it)
                            viewModel.currentScreen.value = it.title
                        }
                    }
                    entry<Screen.Database> { DatabaseScreen() }
                    entry<Screen.Storage> { StorageScreen() }
                    entry<Screen.InAppMessaging> { InAppMessagingScreen() }
                    entry<Screen.InAppUpdates> { UpdateScreen() }
                    entry<Screen.Quota> { QuotaScreen() }
                }
            )
        }
    }
}

@Composable
fun DrawerItems(model: MainViewModel, onItemSelected: (Int) -> Unit) {
    val currentScreen by model.currentScreen.collectAsState()
    val context = LocalContext.current

    val items = mapOf(
        Screen.Database.title to R.drawable.ic_db,
        Screen.Storage.title to R.drawable.ic_storage,
        Screen.InAppMessaging.title to R.drawable.ic_chat,
        Screen.InAppUpdates.title to R.drawable.ic_update,
        Screen.Quota.title to R.drawable.ic_key,
    )

    val local = mapOf(
        "ClorastoreDB" to R.drawable.ic_file,
        "CloremDB" to R.drawable.ic_object,
        "ClorographDB" to R.drawable.ic_graph,
    )

    val extras = mapOf(
        "Github" to "https://github.com/Clorabase/Clorabase",
        "Website" to "https://clorabase.github.io/",
        "Documentation" to "https://clorabase.github.io/docs/#/"
    )

    items.forEach { (screen, icon) ->
        NavigationDrawerItem(
            label = { Text(screen) },
            icon = { Icon(painterResource(id = icon), contentDescription = null) },
            selected = currentScreen == screen,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = Color.Black,
                unselectedIconColor = Color.Black
            ),
            onClick = { onItemSelected(icon) }
        )
    }

    HorizontalDivider()
    Text(
        text = "Local databases",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )

    local.forEach { (screen, icon) ->
        NavigationDrawerItem(
            label = { Text(screen) },
            icon = { Icon(painterResource(id = icon), contentDescription = null) },
            selected = false,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = Color.Black,
                unselectedIconColor = Color.Black
            ),
            onClick = {
                val url = "https://github.com/Clorabase/$screen"
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        )
    }

    HorizontalDivider()

    Text(
        text = "Extras",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )

    extras.forEach { (name, url) ->
        NavigationDrawerItem(
            label = { Text(name) },
            icon = {
                Icon(
                    painterResource(
                        id = when (name) {
                            "Github" -> R.drawable.ic_db
                            "Website" -> R.drawable.ic_web
                            "Documentation" -> R.drawable.ic_help
                            else -> 0
                        }
                    ), contentDescription = null
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = Color.Black,
                unselectedIconColor = Color.Black
            ),
            selected = false,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        )
    }
}

@Composable
fun ProjectDropdown(
    projects: List<String>,
    currentProject: String?,
    onProjectSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        Row(
            Modifier.clickable(onClick = { expanded = true }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                color = MaterialTheme.colorScheme.onBackground,
                text = currentProject ?: "Select Project",
                modifier = Modifier.padding(8.dp)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Dropdown"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project) },
                    onClick = {
                        onProjectSelected(project)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.header),
            contentDescription = "Header Background",
            modifier = Modifier.matchParentSize()
        )

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Clorabase Logo",
            modifier = Modifier.size(100.dp)
        )
    }
}

@Composable
fun AddProjectDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onProjectAdded: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var configureStorage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isCreateEnabled = projectName.isNotBlank() && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create new project",
                fontSize = 20.sp,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the name of new project. The name must be alphanumeric and can include underscores.",
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { newValue ->
                        projectName = newValue.filter { it.isLetterOrDigit() || it == '_' }
                        errorMessage = null
                    },
                    label = { Text("Enter project name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = configureStorage,
                        onCheckedChange = { configureStorage = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set-up blob storage", color = Color.Black)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    viewModel.createProject(
                        projectName = projectName,
                        configureStorage = configureStorage,
                        onSuccess = {
                            isLoading = false
                            onProjectAdded(projectName)
                        },
                        onError = { error ->
                            isLoading = false
                        }
                    )
                },
                enabled = isCreateEnabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Create project")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}