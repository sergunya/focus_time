package com.focustime.nopplugin.settings

import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class GopherCursorConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var gopherCheckBox: JBCheckBox? = null
    private var darkenCheckBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Focus Time: Gopher Editor Cursor"

    override fun createComponent(): JComponent {
        val settings = GopherCursorSettings.getInstance()

        val gopherCb = JBCheckBox("Replace the editor caret with a Gopher icon")
        gopherCb.isSelected = settings.settingsState.enabled
        gopherCheckBox = gopherCb

        val darkenCb = JBCheckBox("Darken editor background while holding Control (after 5s)")
        darkenCb.isSelected = settings.settingsState.darkenOnControlEnabled
        darkenCheckBox = darkenCb

        val p = FormBuilder.createFormBuilder()
            .addComponent(gopherCb)
            .addComponent(darkenCb)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel = p
        return p
    }

    override fun isModified(): Boolean {
        val settings = GopherCursorSettings.getInstance()
        val gopherChanged = gopherCheckBox?.isSelected != settings.settingsState.enabled
        val darkenChanged = darkenCheckBox?.isSelected != settings.settingsState.darkenOnControlEnabled
        return gopherChanged || darkenChanged
    }

    override fun apply() {
        val settings = GopherCursorSettings.getInstance()
        val newGopherEnabled = gopherCheckBox?.isSelected ?: true
        val newDarkenEnabled = darkenCheckBox?.isSelected ?: true
        val gopherChanged = settings.settingsState.enabled != newGopherEnabled
        val darkenChanged = settings.settingsState.darkenOnControlEnabled != newDarkenEnabled

        settings.settingsState.enabled = newGopherEnabled
        settings.settingsState.darkenOnControlEnabled = newDarkenEnabled

        val installer = project.getService(EditorGopherCursorInstaller::class.java)
        if (gopherChanged) {
            installer?.setEnabled(newGopherEnabled)
        }
        if (darkenChanged) {
            // Request repaint of overlays to reflect new setting
            installer?.refreshOverlays()
        }
    }

    override fun reset() {
        val settings = GopherCursorSettings.getInstance()
        gopherCheckBox?.isSelected = settings.settingsState.enabled
        darkenCheckBox?.isSelected = settings.settingsState.darkenOnControlEnabled
    }

    override fun disposeUIResources() {
        panel = null
        gopherCheckBox = null
        darkenCheckBox = null
    }
}

