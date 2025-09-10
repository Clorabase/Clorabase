package com.clorabase.console

import clorabase.sdk.java.Clorabase
import kotlinx.coroutines.flow.MutableStateFlow

object Globals {
    val currentProject = MutableStateFlow<String?>(null)
    lateinit var username : String
    lateinit var token : String
    lateinit var clorabase : Clorabase
}