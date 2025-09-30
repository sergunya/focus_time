package com.focustime.nopplugin.settings

import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class GopherCursorConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var checkBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Focus Time: Gopher Terminal Cursor"

    override fun createComponent(): JComponent {
        val p = JPanel()
        p.layout = BoxLayout(p, BoxLayout.Y_AXIS)
        val cb = JBCheckBox("Replace blinking terminal cursor with Gopher icon")
        p.add(cb)
        panel = p
        checkBox = cb
        val settings = GopherCursorSettings.getInstance()
        cb.isSelected = settings.state.enabled
        return p
    }

    override fun isModified(): Boolean {
        val settings = GopherCursorSettings.getInstance()
        return checkBox?.isSelected != settings.state.enabled
    }

    override fun apply() {
        val settings = GopherCursorSettings.getInstance()
        val newVal = checkBox?.isSelected ?: true
        val changed = settings.state.enabled != newVal
        settings.state.enabled = newVal
        if (changed) {
            project.getService(GopherCursorInstaller::class.java)?.setEnabled(newVal)
        }
    }

    override fun reset() {
        val settings = GopherCursorSettings.getInstance()
        checkBox?.isSelected = settings.state.enabled
    }

    override fun disposeUIResources() {
        panel = null
        checkBox = null
    }
}

