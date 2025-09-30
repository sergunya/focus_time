package com.focustime.nopplugin.startup

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class GopherCursorStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = GopherCursorSettings.getInstance().settingsState

        // Initialize editor gopher cursor overlay
        project.getService(EditorGopherCursorInstaller::class.java)?.let { installer ->
            installer.setEnabled(settings.enabled)
        }
    }
}

