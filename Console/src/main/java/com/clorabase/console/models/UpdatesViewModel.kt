package com.clorabase.console.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import clorabase.sdk.java.utils.GithubUtils
import com.clorabase.console.Globals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdateViewModel : ViewModel() {
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    var changelog by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)


    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val releaseDate: String,
        val downloadCount: Int,
        val updateType: String
    )

    fun fetchUpdateDetails(packageName : String) {
        viewModelScope.launch(Dispatchers.IO) {
            error = null
            try {
                val project = Globals.currentProject.value ?: return@launch
                val versionJsonPath = "$project/updates/$packageName/version.json"
                val changelogPath = "$project/updates/$packageName/changelogs.txt"

                val versionJson = GithubUtils.getImmediateRaw(versionJsonPath,GithubUtils.getLatestCommit(project))
                val jsonObject = JSONObject(String(versionJson))

                updateInfo = UpdateInfo(
                    versionCode = jsonObject.optInt("code", 0),
                    versionName = jsonObject.optString("name", "N/A"),
                    releaseDate = jsonObject.optString("date", "N/A"),
                    downloadCount = jsonObject.optInt("downloadCount", 0),
                    updateType = jsonObject.optString("mode","N/A")
                )

                try {
                    val changelogBytes = GithubUtils.getRaw(changelogPath)
                    changelog = String(changelogBytes)
                } catch (e: FileNotFoundException) {
                    changelog = "No changelogs available."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = when (e) {
                    is UnknownHostException -> "Check your internet connection."
                    is FileNotFoundException -> "No update details found."
                    is IOException -> "Failed to fetch update details: ${e.message}"
                    else -> "An unexpected error occurred."
                }
            } finally {
                isLoading = false
            }
        }
    }


    fun pushUpdate(
        versionCode: Int,
        versionName: String,
        packageName: String,
        downloadUrl: String?,
        updateType: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            error = null
            try {
                val releaseDate = SimpleDateFormat("dd-MM-yyyy",Locale.getDefault()).format(Date());
                val project = Globals.currentProject.value ?: return@launch
                val versionJsonPath = "$project/updates/$packageName/version.json"
                val newUpdateJson = JSONObject().apply {
                    put("code", versionCode)
                    put("name", versionName)
                    put("package", packageName)
                    put("downloadUrl", downloadUrl)
                    put("downloadCount", 0)
                    put("mode", updateType.lowercase(Locale.ROOT))
                    put("date",releaseDate)
                }

                // Check if the file exists to determine whether to create or update
                val fileExists = GithubUtils.exists(versionJsonPath)
                if (fileExists) {
                    GithubUtils.update(newUpdateJson.toString().toByteArray(), versionJsonPath)
                } else {
                    GithubUtils.create(newUpdateJson.toString().toByteArray(), versionJsonPath)
                }

                // Update local cache immediately
                updateInfo = UpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    releaseDate = releaseDate,
                    downloadCount = 0,
                    updateType = updateType
                )

                try {
                    val changelogBytes = GithubUtils.getRaw("$project/updates/$packageName/changelogs.md")
                    changelog = String(changelogBytes)
                } catch (e: FileNotFoundException) {
                    changelog = "No changelogs available."
                }
            } catch (e: Exception) {
                error = "Failed to push update: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchApps(onAppsFetched : (List<String>) -> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            val apps = try {
                GithubUtils.listFiles(Globals.currentProject.value + "/updates").map { it.name }
            } catch (e : FileNotFoundException){
                mutableListOf("No app found")
            }

            onAppsFetched(apps);
        }
    }
}