package com.clorabase.console.screens

import androidx.navigation3.runtime.NavKey
import clorabase.sdk.java.database.Collection

sealed class Screen(val title: String) : NavKey{

    data object Home : Screen("Home")
    data object Database : Screen("Clorabase Database")
    data object Storage : Screen("Clorabase Storage")
    data object PushNotifications : Screen("Clorabase Push")
    data object InAppMessaging : Screen("In-App Messaging")
    data object InAppUpdates : Screen("In-App Updates")
    data object Documentation : Screen("Documentation")
    data object Quota : Screen("Quota & Usage")
    data class CreateDocument(val name : String, val collection : Collection) : Screen("Create Document")
}