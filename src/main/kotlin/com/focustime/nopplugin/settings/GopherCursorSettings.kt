package com.focustime.nopplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "GopherCursorSettings", storages = [Storage("gopherCursorSettings.xml")])
class GopherCursorSettings : PersistentStateComponent<GopherCursorSettings.State> {
    data class State(var enabled: Boolean = true)

    var settingsState = State()

    override fun getState(): State = settingsState
    override fun loadState(state: State) { this.settingsState = state }

    companion object {
        fun getInstance(): GopherCursorSettings = service()
    }
}

