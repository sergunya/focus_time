package com.focustime.nopplugin.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import java.awt.RenderingHints
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLayer
import javax.swing.SwingUtilities
import javax.swing.plaf.LayerUI

/**
 * Overlays editors to paint a gopher icon at the caret position.
 */
@Service(Service.Level.PROJECT)
class EditorGopherCursorInstaller(private val project: Project) {
    private val overlays = mutableMapOf<JComponent, EditorOverlay>()

    init {
        // Install on existing editors
        installOnExistingEditors()
        // Listen for future editors
        val listener = object : com.intellij.openapi.editor.event.EditorFactoryListener {
            override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                val editor = event.editor
                SwingUtilities.invokeLater { installOnEditor(editor) }
            }
        }
        EditorFactory.getInstance().addEditorFactoryListener(listener, project)
    }

    fun setEnabled(enabled: Boolean) {
        overlays.values.forEach { it.isEnabled = enabled; it.repaintTarget() }
    }

    private fun installOnExistingEditors() {
        for (editor in EditorFactory.getInstance().allEditors) {
            installOnEditor(editor)
        }
    }

    private fun installOnEditor(editor: Editor) {
        val comp = editor.contentComponent as? JComponent ?: return
        if (overlays.containsKey(comp)) return
        // Avoid double-wrapping
        if (comp is JLayer<*> || comp.parent is JLayer<*>) return

        val overlay = EditorOverlay(comp, editor)
        overlays[comp] = overlay
        overlay.install(true)
    }

    private class EditorOverlay(private val editorComponent: JComponent, private val editor: Editor) {
        var isEnabled: Boolean = true
        private var layer: JLayer<JComponent>? = null
        private var painter: CaretGopherPainter? = null

        fun install(enabled: Boolean) {
            isEnabled = enabled
            if (layer != null) return

            val parent = editorComponent.parent ?: return
            if (parent is JLayer<*>) return
            if (editorComponent is JLayer<*>) return

            painter = CaretGopherPainter(editorComponent, editor) { isEnabled }
            @Suppress("UNCHECKED_CAST")
            val jlayer = JLayer(editorComponent, painter)
            layer = jlayer

            try {
                val index = parent.getComponentZOrder(editorComponent)
                parent.remove(editorComponent)
                parent.add(jlayer, index)
                parent.revalidate()
                parent.repaint()
            } catch (_: Exception) {
                layer = null
                painter = null
            }
        }

        fun repaintTarget() {
            layer?.repaint()
        }
    }

    private class CaretGopherPainter(
        private val target: JComponent,
        private val editor: Editor,
        private val isEnabledProvider: () -> Boolean
    ) : LayerUI<JComponent>() {
        private val gopherOriginal by lazy { loadGopherIcon() }

        override fun paint(g: java.awt.Graphics, c: JComponent) {
            super.paint(g, c)
            if (!isEnabledProvider()) return
            if (c !is JLayer<*>) return
            val view = c.view as? JComponent ?: return
            if (!view.isShowing) return

            // Compute caret location
            val caret = editor.caretModel.primaryCaret
            val visPos = caret.visualPosition
            val pt = editor.visualPositionToXY(visPos)
            val lineHeight = editor.lineHeight

            val g2 = g.create() as java.awt.Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                val size = (lineHeight * 1.10).toInt().coerceAtLeast(10).coerceAtMost(lineHeight)
                val x = pt.x - size / 2
                val y = pt.y + (lineHeight - size) / 2

                // Draw the icon with its own alpha; do not paint any background to keep full transparency
                g2.drawImage(gopherOriginal, x, y, size, size, null)
            } finally {
                g2.dispose()
            }
        }

        private fun loadGopherIcon(): java.awt.Image {
            val url = javaClass.classLoader.getResource("icons/gopher_fast.png")
            return if (url != null) ImageIcon(url).image else placeholderImage()
        }

        private fun placeholderImage(): java.awt.Image {
            val img = java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g = img.graphics as java.awt.Graphics2D
            g.color = java.awt.Color(0, 150, 220)
            g.fillOval(2, 2, 20, 20)
            g.dispose()
            return img
        }
    }
}