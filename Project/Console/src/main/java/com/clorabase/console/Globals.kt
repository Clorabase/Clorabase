package com.clorabase.console

import clorabase.sdk.java.Clorabase
import kotlinx.coroutines.flow.MutableStateFlow

object Globals {
    const val VERSION : String = "0.6"
    val currentProject = MutableStateFlow<String?>(null)
    lateinit var username : String
    lateinit var token : String
    lateinit var clorabase : Clorabase

    fun initClorabase(){
        clorabase = Clorabase.getInstance(username, token, currentProject.value!!)
    }
}