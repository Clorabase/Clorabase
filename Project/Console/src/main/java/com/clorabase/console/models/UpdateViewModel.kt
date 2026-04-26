package com.clorabase.console.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UpdateScreenState {
    object AppList : UpdateScreenState()
    data class AppDetails(val packageName: String) : UpdateScreenState()
}

class UpdateViewModel : ViewModel() {
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    val apps = mutableStateListOf<String>()

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var currentScreen by mutableStateOf<UpdateScreenState>(UpdateScreenState.AppList)
        private set

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val releaseDate: String,
        val downloadCount: Int,
        val updateType: String,
        var changelog: String? = null
    )

    fun navigateToDetails(packageName: String) {
        currentScreen = UpdateScreenState.AppDetails(packageName)
        fetchUpdateDetails(packageName)
    }

    fun navigateBack() {
        currentScreen = UpdateScreenState.AppList
        updateInfo = null
        error = null
    }

    fun fetchUpdateDetails(packageName: String) {
        error = null
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val project = Globals.currentProject.value ?: return@launch
                val versionJsonPath = "$project/updates/$packageName/version.json"
                val changelogPath = "$project/updates/$packageName/changelogs.txt"

                val versionJson = GithubUtils.getImmediateRaw(versionJsonPath, GithubUtils.getLatestCommit(project))
                val jsonObject = JSONObject(String(versionJson))

                val info = UpdateInfo(
                    versionCode = jsonObject.optInt("code", 0),
                    versionName = jsonObject.optString("name", "N/A"),
                    releaseDate = jsonObject.optString("date", "N/A"),
                    downloadCount = jsonObject.optInt("downloadCount", 0),
                    updateType = jsonObject.optString("mode", "N/A")
                )

                try {
                    val changelogBytes = GithubUtils.getRaw(changelogPath)
                    info.changelog = String(changelogBytes)
                } catch (e: FileNotFoundException) {
                    info.changelog = "No changelogs available."
                }

                withContext(Dispatchers.Main) {
                    updateInfo = info
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    error = when (e) {
                        is UnknownHostException -> "Check your internet connection."
                        is FileNotFoundException -> "No update details found."
                        is IOException -> "Failed to fetch update details: ${e.message}"
                        else -> "An unexpected error occurred."
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun pushUpdate(
        versionCode: Int,
        versionName: String,
        packageName: String,
        downloadUrl: String?,
        updateType: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            error = null
            try {
                val releaseDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val project = Globals.currentProject.value ?: return@launch
                val versionJsonPath = "$project/updates/$packageName/version.json"

                val newUpdateJson = JSONObject().apply {
                    put("code", versionCode)
                    put("name", versionName)
                    put("package", packageName)
                    put("downloadUrl", downloadUrl)
                    put("downloadCount", 0)
                    put("mode", updateType.lowercase(Locale.ROOT))
                    put("date", releaseDate)
                }

                if (GithubUtils.exists(versionJsonPath)) {
                    GithubUtils.update(newUpdateJson.toString().toByteArray(), versionJsonPath)
                } else {
                    GithubUtils.create(newUpdateJson.toString().toByteArray(), versionJsonPath)
                    withContext(Dispatchers.Main) {
                        if (!apps.contains(packageName)) apps.add(packageName)
                    }
                }

                // Refresh details if currently viewing this app
                if (currentScreen is UpdateScreenState.AppDetails && (currentScreen as UpdateScreenState.AppDetails).packageName == packageName) {
                    fetchUpdateDetails(packageName)
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to push update: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun fetchApps() {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val project = Globals.currentProject.value ?: return@launch
                val list = try {
                    GithubUtils.listFiles("$project/updates").map { it.name }
                } catch (e: FileNotFoundException) {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    apps.clear()
                    apps.addAll(list)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun deleteUpdate(packageName: String, onFailed: (e: Exception) -> Unit) {
        isLoading = true;
        viewModelScope.launch(Dispatchers.IO) {
            val project = Globals.currentProject.value ?: return@launch
            val base = "$project/updates/$packageName"
            try {
                GithubUtils.delete("$base/version.json")
                if (GithubUtils.exists("$base/changelogs.txt")) {
                    GithubUtils.delete("$base/changelogs.txt")
                }
                withContext(Dispatchers.Main) {
                    apps.remove(packageName)
                    isLoading = false;
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailed(e)
                    isLoading = false;
                    error = "Failed to delete update: ${e.message}"
                }
            }
        }
    }
}
