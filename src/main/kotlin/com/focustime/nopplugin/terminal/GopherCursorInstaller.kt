package com.focustime.nopplugin.terminal

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Component
import java.awt.Container
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLayer
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.plaf.LayerUI
import kotlin.math.roundToInt

@Service(Service.Level.PROJECT)
class GopherCursorInstaller(private val project: Project) : Disposable {

    private val overlays = mutableMapOf<JComponent, GopherCursorOverlay>()

    init {
        installOnExistingTerminalTabs()
        subscribeForFutureTerminalTabs()
    }

    fun setEnabled(enabled: Boolean) {
        overlays.values.forEach { it.isEnabled = enabled; it.repaintTarget() }
    }

    private fun installOnExistingTerminalTabs() {
        SwingUtilities.invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal") ?: return@invokeLater
            toolWindow.contentManager.contents.forEach { content ->
                scanAndInstallOverlay(content.component)
            }
        }
    }

    private fun subscribeForFutureTerminalTabs() {
        val timer = Timer(1500) {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal") ?: return@Timer
            toolWindow.contentManager.contents.forEach { content ->
                scanAndInstallOverlay(content.component)
            }
        }
        timer.isRepeats = true
        timer.start()
    }

    private fun scanAndInstallOverlay(root: Component) {
        if (root !is Container) return
        val terminals = mutableListOf<JComponent>()
        findTerminalLikeComponents(root, terminals)
        terminals.forEach { terminalComponent ->
            if (overlays.containsKey(terminalComponent)) return@forEach
            val overlay = GopherCursorOverlay(terminalComponent)
            overlays[terminalComponent] = overlay
            overlay.install(true)
        }
    }

    private fun findTerminalLikeComponents(container: Container, results: MutableList<JComponent>) {
        for (child in container.components) {
            if (child is JComponent) {
                val name = child.javaClass.name
                if (name.contains("TerminalPanel") || name.contains("JediTermWidget") || name.contains("TerminalWidget")) {
                    results.add(child)
                }
                if (child is Container) {
                    findTerminalLikeComponents(child, results)
                }
            }
        }
    }

    override fun dispose() {
        overlays.values.forEach { it.uninstall() }
        overlays.clear()
    }
}

private class GopherCursorOverlay(private val terminalComponent: JComponent) {
    var isEnabled: Boolean = true
    private var layer: JLayer<JComponent>? = null
    private var painter: CursorPainter? = null

    fun install(enabled: Boolean) {
        isEnabled = enabled
        if (layer != null) return
        painter = CursorPainter(terminalComponent)
        @Suppress("UNCHECKED_CAST")
        val jlayer = JLayer(terminalComponent, painter)
        layer = jlayer
        val parent = terminalComponent.parent
        val index = parent.getComponentZOrder(terminalComponent)
        parent.remove(terminalComponent)
        parent.add(jlayer, index)
        parent.revalidate()
        parent.repaint()
    }

    fun uninstall() {
        val jlayer = layer ?: return
        val parent = jlayer.parent ?: return
        parent.remove(jlayer)
        parent.add(terminalComponent)
        parent.revalidate()
        parent.repaint()
        layer = null
        painter = null
    }

    fun repaintTarget() {
        layer?.repaint()
    }
}

private class CursorPainter(target: JComponent) : LayerUI<JComponent>() {
    private val gopher by lazy { loadGopherIcon() }

    override fun paint(g: java.awt.Graphics, c: JComponent) {
        super.paint(g, c)
        if (c !is JLayer<*>) return
        val view = c.view as? JComponent ?: return
        if (!view.isShowing) return

        val fm = view.getFontMetrics(view.font)
        val cellW = fm.charWidth('W').coerceAtLeast(1)
        val cellH = (fm.height * 1.1).roundToInt().coerceAtLeast(1)

        val caretRect = inferCaretRectangle(view, cellW, cellH) ?: return

        val g2 = g.create() as java.awt.Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            val scale = computeScale(cellW, cellH)
            val w = (24 * scale).roundToInt()
            val h = (24 * scale).roundToInt()
            val x = caretRect.x + (caretRect.width - w) / 2
            val y = caretRect.y + (caretRect.height - h) / 2
            g2.drawImage(gopher, x, y, w, h, null)
        } finally {
            g2.dispose()
        }
    }

    private fun computeScale(cellW: Int, cellH: Int): Double {
        val base = minOf(cellW, cellH).toDouble()
        return (base / 16.0).coerceIn(0.75, 2.0)
    }

    private fun inferCaretRectangle(component: JComponent, cellW: Int, cellH: Int): Rectangle? {
        return try {
            val panel = findAncestorWithName(component, "TerminalPanel") ?: return null
            val clazz = panel.javaClass
            val xField = clazz.declaredFields.firstOrNull { it.name.contains("cursorX", true) }
            val yField = clazz.declaredFields.firstOrNull { it.name.contains("cursorY", true) }
            if (xField != null && yField != null) {
                xField.isAccessible = true
                yField.isAccessible = true
                val cx = (xField.get(panel) as? Int) ?: return null
                val cy = (yField.get(panel) as? Int) ?: return null
                Rectangle(cx * cellW, cy * cellH, cellW, cellH)
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun findAncestorWithName(component: JComponent, nameFragment: String): JComponent? {
        var current: Component? = component
        while (current != null) {
            if (current is JComponent && current.javaClass.name.contains(nameFragment)) return current
            current = current.parent
        }
        return null
    }

    private fun loadGopherIcon(): java.awt.Image {
        val url = javaClass.classLoader.getResource("icons/gopher_fast.png")
        return if (url != null) ImageIcon(url).image else placeholderImage()
    }

    private fun placeholderImage(): BufferedImage {
        val img = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(220, 0, 0)
        g.fillRoundRect(0, 0, 24, 24, 6, 6)
        g.dispose()
        return img
    }
}

