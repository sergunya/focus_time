package com.focustime.nopplugin.settings

import com.focustime.nopplugin.editor.EditorCursorColorManager
import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.focustime.nopplugin.terminal.GopherCursorInstaller
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class GopherCursorConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var checkBox: JBCheckBox? = null
    private var colorPanel: JPanel? = null
    private var selectedColor: Color? = null

    override fun getDisplayName(): String = "Focus Time: Gopher Terminal Cursor"

    override fun createComponent(): JComponent {
        val settings = GopherCursorSettings.getInstance()

        val cb = JBCheckBox("Replace blinking terminal cursor with Gopher icon")
        cb.isSelected = settings.settingsState.enabled
        checkBox = cb

        // Create color picker panel
        val storedColor = Color(settings.settingsState.cursorColor, true)
        selectedColor = storedColor

        val colorPreview = JPanel()
        colorPreview.background = storedColor
        colorPreview.preferredSize = Dimension(40, 25)
        colorPreview.border = BorderFactory.createLineBorder(JBColor.GRAY)
        colorPanel = colorPreview

        val colorButton = JButton("Choose Color")
        colorButton.addActionListener {
            val newColor = JColorChooser.showDialog(
                panel,
                "Choose Cursor Color",
                selectedColor ?: Color.WHITE
            )
            if (newColor != null) {
                selectedColor = newColor
                colorPanel?.background = newColor
            }
        }

        val colorRow = JPanel()
        colorRow.layout = BoxLayout(colorRow, BoxLayout.X_AXIS)
        colorRow.add(JBLabel("Cursor Color: "))
        colorRow.add(Box.createHorizontalStrut(10))
        colorRow.add(colorPreview)
        colorRow.add(Box.createHorizontalStrut(10))
        colorRow.add(colorButton)
        colorRow.alignmentX = JComponent.LEFT_ALIGNMENT

        val p = FormBuilder.createFormBuilder()
            .addComponent(cb)
            .addVerticalGap(10)
            .addComponent(colorRow)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel = p
        return p
    }

    override fun isModified(): Boolean {
        val settings = GopherCursorSettings.getInstance()
        val enabledChanged = checkBox?.isSelected != settings.settingsState.enabled
        val colorChanged = selectedColor?.let { 
            it.rgb != Color(settings.settingsState.cursorColor, true).rgb 
        } ?: false
        return enabledChanged || colorChanged
    }

    override fun apply() {
        val settings = GopherCursorSettings.getInstance()
        val newEnabled = checkBox?.isSelected ?: true
        val enabledChanged = settings.settingsState.enabled != newEnabled

        val newColor = selectedColor?.rgb ?: 0xFFFFFFFF.toInt()
        val colorChanged = settings.settingsState.cursorColor != newColor

        settings.settingsState.enabled = newEnabled
        settings.settingsState.cursorColor = newColor

        val terminalInstaller = project.getService(GopherCursorInstaller::class.java)
        val editorManager = project.getService(EditorCursorColorManager::class.java)
        val editorInstaller = project.getService(EditorGopherCursorInstaller::class.java)
        if (enabledChanged) {
            terminalInstaller?.setEnabled(newEnabled)
            editorManager?.setEnabled(newEnabled)
            editorInstaller?.setEnabled(newEnabled)
        }
        if (colorChanged) {
            val awtColor = Color(newColor, true)
            terminalInstaller?.updateColor(awtColor)
            editorManager?.updateColor(awtColor)
            editorInstaller?.updateColor(awtColor)
        }
    }

    override fun reset() {
        val settings = GopherCursorSettings.getInstance()
        checkBox?.isSelected = settings.settingsState.enabled
        selectedColor = Color(settings.settingsState.cursorColor, true)
        colorPanel?.background = selectedColor
    }

    override fun disposeUIResources() {
        panel = null
        checkBox = null
        colorPanel = null
        selectedColor = null
    }
}

