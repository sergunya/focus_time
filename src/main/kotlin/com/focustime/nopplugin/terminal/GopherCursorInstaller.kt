package com.focustime.nopplugin.terminal

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Component
import java.awt.Container
import java.awt.Rectangle
import java.awt.RenderingHints
import com.intellij.util.ui.UIUtil
import com.intellij.ui.JBColor
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

            // Skip if already wrapped in a JLayer or parent is JLayer
            if (terminalComponent is JLayer<*> || terminalComponent.parent is JLayer<*>) return@forEach

            val overlay = GopherCursorOverlay(terminalComponent)
            overlays[terminalComponent] = overlay
            overlay.install(true)
        }
    }

    private fun findTerminalLikeComponents(container: Container, results: MutableList<JComponent>) {
        for (child in container.components) {
            // Skip JLayer components
            if (child is JLayer<*>) {
                // But recurse into the view of the JLayer
                val view = child.view
                if (view is Container) {
                    findTerminalLikeComponents(view, results)
                }
                continue
            }

            if (child is JComponent) {
                val name = child.javaClass.name
                if (name.contains("TerminalPanel") || name.contains("JediTermWidget") || name.contains("TerminalWidget")) {
                    results.add(child)
                }
                findTerminalLikeComponents(child, results)
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

        // Validate parent exists and is not a JLayer
        val parent = terminalComponent.parent
        if (parent == null || parent is JLayer<*>) {
            return
        }

        // Ensure terminalComponent is not already wrapped
        if (terminalComponent is JLayer<*>) {
            return
        }

        painter = CursorPainter(terminalComponent) { isEnabled }
        @Suppress("UNCHECKED_CAST")
        val jlayer = JLayer(terminalComponent, painter)
        layer = jlayer

        try {
            val index = parent.getComponentZOrder(terminalComponent)
            parent.remove(terminalComponent)
            parent.add(jlayer, index)
            parent.revalidate()
            parent.repaint()
        } catch (e: Exception) {
            // If installation fails, clean up
            layer = null
            painter = null
        }
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

private class CursorPainter(private val target: JComponent, private val isEnabledProvider: () -> Boolean) : LayerUI<JComponent>() {
    private val gopherOriginal by lazy { loadGopherIcon() }

    override fun paint(g: java.awt.Graphics, c: JComponent) {
        super.paint(g, c)
        if (!isEnabledProvider()) return
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
            val w = (28 * scale).roundToInt()
            val h = (28 * scale).roundToInt()
            val x = caretRect.x + (caretRect.width - w) / 2
            val y = caretRect.y + (caretRect.height - h) / 2

            // Mask the classic terminal cursor under the icon to avoid bleed-through
            g2.color = view.background
            g2.fillRect(caretRect.x, caretRect.y, caretRect.width, caretRect.height)

            g2.drawImage(gopherOriginal, x, y, w, h, null)
        } finally {
            g2.dispose()
        }
    }

    private fun computeScale(cellW: Int, cellH: Int): Double {
        val base = minOf(cellW, cellH).toDouble()
        return (base / 16.0).coerceIn(0.75, 2.0)
    }

    private fun inferCaretRectangle(component: JComponent, cellW: Int, cellH: Int): Rectangle? {
        try {
            // Try to find a relevant terminal-like ancestor component first
            val panel = findAncestorWithName(component, "TerminalPanel")
                ?: findAncestorWithName(component, "JediTerm")
                ?: findAncestorWithName(component, "TerminalWidget")
                ?: return null

            // Strategy 1: common field names on panel
            fun readIntField(obj: Any, vararg candidates: String): Int? {
                val clazz = obj.javaClass
                for (name in candidates) {
                    try {
                        val f = clazz.getDeclaredField(name)
                        f.isAccessible = true
                        val v = f.get(obj)
                        if (v is Int) return v
                    } catch (_: NoSuchFieldException) {
                        // try next
                    } catch (_: Throwable) {
                        // ignore and try next
                    }
                }
                // Also attempt on all declared fields with contains to be resilient across versions
                try {
                    val fields = clazz.declaredFields
                    for (f in fields) {
                        val lname = f.name.lowercase()
                        if (candidates.any { lname.contains(it.lowercase()) }) {
                            try {
                                f.isAccessible = true
                                val v = f.get(obj)
                                if (v is Int) return v
                            } catch (_: Throwable) {
                                // continue
                            }
                        }
                    }
                } catch (_: Throwable) { }
                return null
            }

            fun callIntGetter(obj: Any, vararg candidates: String): Int? {
                val clazz = obj.javaClass
                for (name in candidates) {
                    try {
                        val m = clazz.getMethod(name)
                        val v = m.invoke(obj)
                        if (v is Int) return v
                    } catch (_: NoSuchMethodException) {
                        // try next
                    } catch (_: Throwable) {
                        // ignore
                    }
                }
                return null
            }

            val cx = readIntField(panel, "cursorX", "myCursorX", "caretX", "myCaretX")
                ?: callIntGetter(panel, "getCursorX", "getCaretX")
            val cy = readIntField(panel, "cursorY", "myCursorY", "caretY", "myCaretY")
                ?: callIntGetter(panel, "getCursorY", "getCaretY")

            if (cx != null && cy != null) {
                return Rectangle(cx * cellW, cy * cellH, cellW, cellH)
            }

            // Strategy 2: some implementations keep a caret object with x/y
            try {
                val caretField = panel.javaClass.declaredFields.firstOrNull { it.name.contains("caret", true) || it.name.contains("cursor", true) }
                if (caretField != null) {
                    caretField.isAccessible = true
                    val caretObj = caretField.get(panel)
                    if (caretObj != null) {
                        val ccx = readIntField(caretObj, "x", "column", "col") ?: callIntGetter(caretObj, "getX", "getColumn")
                        val ccy = readIntField(caretObj, "y", "line", "row") ?: callIntGetter(caretObj, "getY", "getLine", "getRow")
                        if (ccx != null && ccy != null) {
                            return Rectangle(ccx * cellW, ccy * cellH, cellW, cellH)
                        }
                    }
                }
            } catch (_: Throwable) { }
        } catch (_: Throwable) {
            // fall through to null
        }
        return null
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

    private fun placeholderImage(): java.awt.Image {
        val img = UIUtil.createImage(target, 24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.graphics as java.awt.Graphics2D
        g.color = JBColor(java.awt.Color(220, 0, 0), java.awt.Color(220, 0, 0))
        g.fillRoundRect(0, 0, 24, 24, 6, 6)
        g.dispose()
        return img
    }
}

