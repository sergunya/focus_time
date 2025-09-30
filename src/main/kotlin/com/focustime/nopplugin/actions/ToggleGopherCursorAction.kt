package com.focustime.nopplugin.actions

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.focustime.nopplugin.editor.EditorCursorColorManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleGopherCursorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = GopherCursorSettings.getInstance()
        settings.settingsState.enabled = !settings.settingsState.enabled
        val enabled = settings.settingsState.enabled
        project.getService(GopherCursorInstaller::class.java)?.setEnabled(enabled)
        project.getService(EditorGopherCursorInstaller::class.java)?.setEnabled(enabled)
        project.getService(EditorCursorColorManager::class.java)?.setEnabled(enabled)
    }
}

