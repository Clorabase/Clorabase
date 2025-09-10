package com.clorabase.console

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.models.MainViewModel
import com.clorabase.console.screens.DatabaseScreen
import com.clorabase.console.screens.HomeScreen
import com.clorabase.console.screens.InAppMessagingScreen
import com.clorabase.console.screens.QuotaScreen
import com.clorabase.console.screens.Screen
import com.clorabase.console.screens.StorageScreen
import com.clorabase.console.screens.UpdateScreen
import com.clorabase.console.theme.ClorabaseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.util.Date
import kotlin.random.Random


val showDialog = mutableStateOf(false)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (isConnected) {
            val config = getSharedPreferences("main", 0)
            if (config.getString("token", null) == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                setContent {
                    ClorabaseTheme {
                        var isLoaded by remember { mutableStateOf(false) }
                        val model: MainViewModel = viewModel(factory = MainViewModel.Factory(config))
                        val projects by model.projects.collectAsState();


                        model.fetchProjects({
                            AlertDialog.Builder(this)
                                .setTitle("Error")
                                .setMessage(it)
                                .setPositiveButton("Retry") { _, _ -> recreate() }
                                .setNegativeButton("Exit") { _, _ -> finish() }
                                .setCancelable(false)
                                .show()
                        }, {
                            isLoaded = true;
                        });

                        if (isLoaded) {
                            if (projects.isEmpty())
                                showDialog.value = true
                            else
                                MainScreen(model, projects)
                        } else {
                            SplashScreen()
                        }

                        if (showDialog.value) {
                            val context = LocalContext.current
                            AddProjectDialog(onProjectAdded = { projectName ->
                                model._projects.value.add(projectName)
                                model.onProjectSelected(projectName)
                                showDialog.value = false

                                Toast.makeText(context, "Project $projectName added", Toast.LENGTH_SHORT).show()
                                (context as Activity).recreate();
                            },{
                                showDialog.value = false
                            })
                        }
                    }
                }
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("No internet connection")
                .setMessage("Please check your internet connection and try again.")
                .setPositiveButton("retry") { _, _ -> recreate() }
                .setNegativeButton("exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

}

@Composable
private fun SplashScreen() {
    val gradiets = listOf(
        listOf(Color(0xFFE8CF70),Color(0xFFAAF8EB)),
        listOf(Color(0xFFFCF4DF),Color(0xFFD66FEE)),
        listOf(Color(0xFF84FFC9),Color(0xFFAAB2FF),Color(0xFFECA0FF)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradiets.random()
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
                "Fetching projects...",
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
    val scrollState = rememberScrollState();
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
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        ProjectDropdown(projects, currentProject, {
                            viewModel.onProjectSelected(it);
                            viewModel.currentScreen.value = Screen.Home.title;

                            if (bstack.size > 1)
                                bstack.removeLastOrNull();
                        })
                        IconButton({
                            showDialog.value = true
                        }){
                            Icon(Icons.Filled.Add,"Add project");
                        }
                    }
                )
            }
        ) { padding ->
            NavDisplay(
                backStack = bstack,
                modifier = Modifier.padding(padding),
                onBack = {
                    bstack.removeLastOrNull();
                    viewModel.currentScreen.value = bstack.last().title;
                },
                entryDecorators = listOf(
                    rememberSavedStateNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    rememberSceneSetupNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<Screen.Home>{
                        HomeScreen {
                            bstack.add(it)
                            viewModel.currentScreen.value = it.title;
                        };
                    }

                    entry<Screen.Database> {
                        viewModel.currentScreen.value = it.title;
                        DatabaseScreen()
                    }

                    entry<Screen.Storage> {
                        viewModel.currentScreen.value = it.title;
                         StorageScreen()
                    }

                    entry<Screen.InAppMessaging> {
                        viewModel.currentScreen.value = it.title;
                        InAppMessagingScreen()
                    }

                    entry<Screen.InAppUpdates> {
                        viewModel.currentScreen.value = it.title;
                        UpdateScreen()
                    }

                    entry<Screen.Quota> {
                        viewModel.currentScreen.value = it.title;
                        QuotaScreen()
                    }
                }
            )
        }
    }
}


@Composable
fun DrawerItems(model: MainViewModel, onItemSelected: (Int) -> Unit) {
    val currentScreen by model.currentScreen.collectAsState()
    val context = LocalContext.current

    val items = mapOf<String,Int>(
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
        "Website" to "https://clorabase.netlify.app",
        "Documentation" to "https://clorabase-docs.netlify.app/"
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
            onClick = {
                onItemSelected(icon)
            }
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
                val url = "https://github.com/Clorabase/$screen";
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
            painter = painterResource(id = R.drawable.header), // background image
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
    onProjectAdded: (String) -> Unit,
    onDismiss: () -> Unit
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
                    Text("Configure storage", color = Color.Black)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    createProject(projectName, configureStorage,
                        onSuccess = {
                            isLoading = false
                            errorMessage = null
                            onProjectAdded(projectName)
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
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


fun createProject(
    projectName: String,
    configureStorage: Boolean,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val config = JSONObject()
    config.put("project", projectName)
    config.put("created", Date().toString())
    config.put("isStorageConfigured",configureStorage)
    config.put("version", "0.4")
    config.put("storageBucketName",projectName)


    CoroutineScope(Dispatchers.IO).launch {
        try {
            GithubUtils.create(config.toString().toByteArray(), "$projectName/config.json");
            if (configureStorage)
                GithubUtils.repo.createRelease(projectName)
                    .name("$projectName Store room")
                    .body(String(GithubUtils.getRaw("https://github.com/Clorabase/Clorabase/docs/release.md")))
                    .create();
            withContext(Dispatchers.Main){
                onSuccess()
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                if (e is FileAlreadyExistsException)
                    onError("Project with this name already exists")
                else
                    onError("Failed to create project: ${e.message}")
            }
        }
    }
}