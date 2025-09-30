package com.focustime.nopplugin.actions

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleGopherCursorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = GopherCursorSettings.getInstance()
        settings.settingsState.enabled = !settings.settingsState.enabled
        project.getService(GopherCursorInstaller::class.java)?.setEnabled(settings.settingsState.enabled)
    }
}

