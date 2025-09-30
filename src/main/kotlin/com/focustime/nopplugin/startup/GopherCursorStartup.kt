package com.focustime.nopplugin.startup

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectPostStartupActivity

class GopherCursorStartup : ProjectPostStartupActivity {
    override suspend fun execute(project: Project) {
        // Touch the service to ensure overlays are installed for existing terminals
        val installer = project.getService(GopherCursorInstaller::class.java) ?: return
        val enabled = GopherCursorSettings.getInstance().settingsState.enabled
        installer.setEnabled(enabled)
    }
}

