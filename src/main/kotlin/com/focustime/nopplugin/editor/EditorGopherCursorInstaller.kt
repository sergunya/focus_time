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

    fun refreshOverlays() {
        overlays.values.forEach { it.repaintTarget() }
    }

    private fun installOnExistingEditors() {
        for (editor in EditorFactory.getInstance().allEditors) {
            installOnEditor(editor)
        }
    }

    private fun installOnEditor(editor: Editor) {
        val comp = editor.contentComponent
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
        private var timer: javax.swing.Timer? = null
        private var controlHeld: Boolean = false
        private var darkenStartTimestamp: Long = 0L
        private var currentAlpha: Float = 0f

        override fun installUI(c: JComponent) {
            super.installUI(c)
            if (c is JLayer<*>) {
                // Listen for key events to detect Control hold
                c.layerEventMask = java.awt.AWTEvent.KEY_EVENT_MASK
                @Suppress("UNCHECKED_CAST")
                startTimer(c as JLayer<out JComponent>)
            }
        }

        override fun uninstallUI(c: JComponent) {
            if (c is JLayer<*>) {
                c.layerEventMask = 0
            }
            controlHeld = false
            darkenStartTimestamp = 0L
            currentAlpha = 0f
            stopTimer()
            target.repaint()
            super.uninstallUI(c)
        }

        override fun eventDispatched(e: java.awt.AWTEvent, l: JLayer<out JComponent>) {
            if (e is java.awt.event.KeyEvent) {
                when (e.id) {
                    java.awt.event.KeyEvent.KEY_PRESSED -> {
                        if (e.keyCode == java.awt.event.KeyEvent.VK_CONTROL && !controlHeld) {
                            controlHeld = true
                            darkenStartTimestamp = System.currentTimeMillis()
                            currentAlpha = 0f
                            l.repaint()
                        }
                    }
                    java.awt.event.KeyEvent.KEY_RELEASED -> {
                        if (e.keyCode == java.awt.event.KeyEvent.VK_CONTROL) {
                            controlHeld = false
                            darkenStartTimestamp = 0L
                            currentAlpha = 0f
                            l.repaint()
                            l.view?.repaint()
                        }
                    }
                }
            }
        }

        private fun startTimer(layer: JLayer<out JComponent>) {
            stopTimer()
            // Timer only ensures occasional repaints while layer is active
            timer = javax.swing.Timer(50) {
                layer.repaint()
            }.also { it.start() }
        }

        private fun stopTimer() {
            timer?.stop()
            timer = null
        }



        override fun paint(g: java.awt.Graphics, c: JComponent) {
            super.paint(g, c)
            if (c !is JLayer<*>) return
            val view = c.view as? JComponent ?: return
            if (!view.isShowing) return

            val g2 = g.create() as java.awt.Graphics2D
            try {
                // Darken editor background while Control is held, honoring settings
                try {
                    val settings = com.focustime.nopplugin.settings.GopherCursorSettings.getInstance().settingsState
                    if (settings.darkenOnControlEnabled && isEnabledProvider() && controlHeld) {
                        val now = System.currentTimeMillis()
                        val delay = settings.darkenStartDelayMs.toLong().coerceAtLeast(0L)
                        val maxAlpha = settings.darkenMaxAlpha.coerceIn(0f, 1f)
                        val elapsed = now - darkenStartTimestamp
                        val base = if (elapsed > delay) (elapsed - delay).toFloat() else 0f
                        // Linearly ramp to maxAlpha over ~2 seconds after delay
                        val targetAlpha = if (base <= 0f) 0f else (base / 2000f * maxAlpha).coerceIn(0f, maxAlpha)
                        currentAlpha = targetAlpha
                        if (currentAlpha > 0f) {
                            val oldComposite = g2.composite
                            g2.composite = java.awt.AlphaComposite.SrcOver.derive(currentAlpha)
                            g2.color = java.awt.Color(0, 0, 0)
                            g2.fillRect(0, 0, view.width, view.height)
                            g2.composite = oldComposite
                        }
                    }
                } catch (_: Throwable) {
                    // Ignore settings errors
                }

                // Then optionally paint gopher icon if enabled
                if (isEnabledProvider()) {
                    // Compute caret location
                    val caret = editor.caretModel.primaryCaret
                    val visPos = caret.visualPosition
                    val pt = editor.visualPositionToXY(visPos)
                    val lineHeight = editor.lineHeight

                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    val size = (lineHeight * 1.10).toInt().coerceAtLeast(10).coerceAtMost(lineHeight)
                    val x = pt.x - size / 2
                    val y = pt.y + (lineHeight - size) / 2

                    // Draw the icon with its own alpha; do not paint any background to keep full transparency
                    g2.drawImage(gopherOriginal, x, y, size, size, null)
                }
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