package com.focustime.nopplugin.startup

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class GopherCursorStartup : StartupActivity {
    override fun runActivity(project: Project) {
        val settings = GopherCursorSettings.getInstance().settingsState

        // Initialize terminal gopher cursor overlay if present
        project.getService(GopherCursorInstaller::class.java)?.let { installer ->
            installer.setEnabled(settings.enabled)
        }

        // Initialize editor gopher cursor overlay
        project.getService(EditorGopherCursorInstaller::class.java)?.let { installer ->
            installer.setEnabled(settings.enabled)
        }
    }
}

