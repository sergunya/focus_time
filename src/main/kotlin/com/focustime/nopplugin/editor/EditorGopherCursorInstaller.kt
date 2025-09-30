package com.focustime.nopplugin.editor

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
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

    fun updateColor(color: Color) {
        overlays.values.forEach { it.updateColor(color); it.repaintTarget() }
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

        fun updateColor(color: Color) {
            painter?.updateColor(color)
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
        private var currentColor: Color = Color(GopherCursorSettings.getInstance().settingsState.cursorColor, true)
        private var tintedGopher: java.awt.Image? = null

        fun updateColor(color: Color) {
            currentColor = color
            tintedGopher = null
        }

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
                val size = (lineHeight * 0.95).toInt().coerceAtLeast(8)
                val x = pt.x - size / 2
                val y = pt.y + (lineHeight - size) / 2

                val imageToDraw = getTintedGopher(size, size)
                g2.drawImage(imageToDraw, x, y, size, size, null)
            } finally {
                g2.dispose()
            }
        }

        private fun getTintedGopher(w: Int, h: Int): java.awt.Image {
            if (currentColor.red > 250 && currentColor.green > 250 && currentColor.blue > 250) {
                return gopherOriginal
            }
            val cached = tintedGopher
            if (cached != null && cached.getWidth(null) == w && cached.getHeight(null) == h) {
                return cached
            }
            val buffered = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = buffered.createGraphics()
            g.drawImage(gopherOriginal, 0, 0, w, h, null)
            g.composite = AlphaComposite.SrcAtop
            g.color = currentColor
            g.fillRect(0, 0, w, h)
            g.dispose()
            tintedGopher = buffered
            return buffered
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