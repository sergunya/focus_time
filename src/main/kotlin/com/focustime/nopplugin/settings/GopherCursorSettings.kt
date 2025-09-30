package com.focustime.nopplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "GopherCursorSettings", storages = [Storage("gopherCursorSettings.xml")])
class GopherCursorSettings : PersistentStateComponent<GopherCursorSettings.State> {
    data class State(var enabled: Boolean = true)

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): GopherCursorSettings = service()
    }
}

