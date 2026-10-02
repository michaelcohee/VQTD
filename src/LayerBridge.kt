package vqtd

import java.awt.*
import javax.swing.*

// This slide composes two independent classroom operations. F(i) selects a
// description of the number 4; the numerical result is then used as one
// example byte in d[t]. No paper encodes the F(i) expression through G.
internal class LayerBridgePanel(private val threeD: Boolean) : JPanel(BorderLayout()) {
    private val hunt = RedTailHuntHeader()
    private val formula = FormulaBridgeCanvas()
    private val matrix3d = if (threeD) Matrix3DCanvas(compact = true) else null
    private val matrix2d = if (threeD) null else MatrixBridge2DCanvas()
    private val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formula, matrix3d ?: matrix2d!!)
    private var frame = 0
    private val lastStage = 7
    var stage = 0; private set

    init {
        preferredSize = Dimension(980, 610)
        split.resizeWeight = 0.42
        split.dividerLocation = 410
        split.border = null
        add(hunt, BorderLayout.NORTH)
        add(split, BorderLayout.CENTER)
        refreshTheme()
    }
    fun refreshTheme() {
        background = canvas; hunt.background = panel; formula.background = canvas
        matrix3d?.background = canvas; matrix2d?.background = canvas
        split.background = panel; repaint()
    }
    fun advanceStep(): Boolean {
        if (stage < lastStage) stage++
        matrix3d?.advanceStep()
        matrix3d?.bridgeStage = stage
        hunt.stage = stage
        formula.stage = stage; formula.layer = matrix3d?.activeSlice ?: 1
        matrix2d?.stage = stage
        repaint()
        return stage < lastStage
    }
    fun tick() {
        matrix3d?.tick(); frame++
        if (frame % 45 == 0 && stage < lastStage) stage++
        matrix3d?.bridgeStage = stage
        hunt.stage = stage
        formula.stage = stage; formula.layer = matrix3d?.activeSlice ?: 1
        repaint()
    }
    fun resetView() {
        stage = 0; frame = 0; hunt.stage = 0; formula.stage = 0; formula.layer = 1
        matrix3d?.resetView(); matrix2d?.stage = 0
        matrix3d?.bridgeStage = 0
        repaint()
    }
}

private class RedTailHuntHeader : JPanel() {
    var stage = 0; set(value) { field = value; repaint() }
    init { background = panel; preferredSize = Dimension(980, 112) }
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = raw.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.scale(width / 980.0, height / 112.0)
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 17); g.color = ink
            val title = "REDTAIL-X HUNT FIRST"
            val subtitleX = 15 + g.fontMetrics.stringWidth(title) + 17
            g.drawString(title, 15, 23)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 12); g.color = dim
            g.drawString("Worked 7-byte tail · real storage order before the classroom F(i) bridge", subtitleX, 22)
            val titles = arrayOf("0  LOCATE STRIPE", "1  CHECK HASHES", "2  SELECT SURVIVORS")
            val details = arrayOf("stripe 0: original_len=7; share_len=2", "hash fail: shares 1 and 4 → erasures", "verified rows: 0, 2, 3, 5")
            for (i in 0..2) {
                val x = 15 + i * 322
                g.color = surface; g.fillRoundRect(x, 35, 302, 64, 12, 12)
                g.color = if (stage == i) cyan else if (stage > i) green else borderColor
                g.stroke = BasicStroke(if (stage == i) 2.6f else 1.3f)
                g.drawRoundRect(x, 35, 302, 64, 12, 12)
                g.font = Font(Font.SANS_SERIF, Font.BOLD, 14); g.drawString(titles[i], x + 12, 59)
                g.font = Font(Font.SANS_SERIF, Font.PLAIN, 12); g.color = ink; g.drawString(details[i], x + 12, 82)
            }
        } finally { g.dispose() }
    }
}

private class FormulaBridgeCanvas : JPanel() {
    var stage = 0; set(value) { field = value; repaint() }
    var layer = 1; set(value) { field = value; repaint() }
    init { background = canvas; preferredSize = Dimension(410, 610) }
    private fun Graphics2D.line(value: String, x: Int, y: Int, size: Int = 15, bold: Boolean = false, color: Color = ink) {
        this.color = color; font = Font(Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, size); drawString(value, x, y)
    }
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = raw.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.scale(width / 410.0, height / 610.0)
            g.line("FORMULA SIDE", 20, 34, 19, true)
            g.line(if(stage < 3)"Waiting for verified survivors" else "Three routes, same result", 20, 58, 14, false, dim)
            if (stage < 3) {
                g.color = surface; g.fillRoundRect(19, 105, 370, 185, 14, 14)
                g.color = cyan; g.stroke = BasicStroke(2f); g.drawRoundRect(19, 105, 370, 185, 14, 14)
                g.line("HUNT THE STORAGE TARGET", 35, 145, 17, true, cyan)
                g.line("First: locate the stripe.", 35, 182, 15)
                g.line("Then: reject bad share hashes.", 35, 213, 15)
                g.line("Keep any four verified shares.", 35, 244, 15)
                g.line("Next comes the F(i) teaching exercise.", 20, 347, 14, false, dim)
                g.line("No shortest-form rule runs in RedTail-X.", 20, 585, 12, false, dim)
                return
            }
            val examples = arrayOf("F(0) = 2 + 2 = 4", "F(1) = 14 − 10 = 4", "F(2) = 8 − 4 = 4")
            val costs = arrayOf("6 teaching bits", "10 teaching bits", "9 teaching bits")
            for (i in 0..2) {
                val y = 83 + i * 92
                g.color = surface; g.fillRoundRect(19, y, 370, 78, 14, 14)
                g.color = if (stage >= 4 && i == 0) green else if (stage >= 4) dim else borderColor
                g.stroke = BasicStroke(if (stage >= 4 && i == 0) 2.5f else 1.2f)
                g.drawRoundRect(19, y, 370, 78, 14, 14)
                g.line(examples[i], 32, y + 30, 17, true)
                g.line(costs[i], 32, y + 56, 13, false, if (stage >= 4 && i == 0) green else dim)
            }
            g.line("All three validate target 4.", 22, 381, 14, true, cyan)
            if (stage >= 4) g.line("Class arg min chooses F(0).", 22, 407, 14, true, green)
            if (stage >= 5) {
                g.color = surface; g.fillRoundRect(19, 425, 370, 93, 14, 14)
                g.color = green; g.stroke = BasicStroke(2.3f); g.drawRoundRect(19, 425, 370, 93, 14, 14)
                g.line("VALUE BRIDGE → 0x04", 32, 458, 17, true, green)
                g.line("Use 4 as one example data byte.", 32, 487, 13)
                g.line("Current matrix layer: $layer/40", 32, 506, 12, false, dim)
            }
            if (stage >= 5) g.line("d[t] = [04, 00, 00, 00]ᵀ", 22, 545, 15, true, cyan)
            g.line("Teaching composition; not a storage format.", 20, 585, 12, false, dim)
        } finally { g.dispose() }
    }
}

private class MatrixBridge2DCanvas : JPanel() {
    var stage = 0; set(value) { field = value; repaint() }
    init { background = canvas; preferredSize = Dimension(570, 610) }
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = raw.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.scale(width / 570.0, height / 610.0)
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 19); g.color = ink; g.drawString("MATRIX SIDE · ONE BYTE LAYER", 20, 34)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 14); g.color = dim; g.drawString("G = V · V₀⁻¹    c[t] = Gd[t]", 20, 58)
            if (stage >= 5) { g.color = cyan; g.drawString("d[t] = [04, 00, 00, 00]ᵀ", 20, 91) }
            g.font = Font(Font.MONOSPACED, Font.BOLD, 13); g.color = dim
            g.drawString("ROW     COL 0  COL 1  COL 2  COL 3    c[t]", 20, 127)
            for (r in 0..5) {
                val y = 143 + r * 63
                g.color = surface; g.fillRoundRect(17, y, 535, 54, 10, 10)
                g.color = if (r < 4) green else amber
                g.stroke = BasicStroke(2f); g.drawRoundRect(17, y, 535, 54, 10, 10)
                val values = Gf256.generator[r]
                val output = Gf256.multiply(values[0], 4)
                g.font = Font(Font.MONOSPACED, Font.BOLD, 14)
                g.drawString(if (r < 4) "D$r" else "P${r - 4}", 31, y + 34)
                g.color = ink
                values.forEachIndexed { c, v -> g.drawString(v.toString(16).uppercase().padStart(2, '0'), 122 + c * 82, y + 34) }
                if (stage >= 6) { g.color = if (r < 4) green else amber; g.drawString("→  ${output.toString(16).uppercase().padStart(2, '0')}", 462, y + 34) }
            }
            if (stage >= 6) {
                g.font = Font(Font.SANS_SERIF, Font.BOLD, 14); g.color = cyan
                g.drawString("One sample column: c[t] = G · [04,00,00,00]ᵀ", 20, 548)
            }
            if (stage >= 7) {
                g.font = Font(Font.SANS_SERIF, Font.PLAIN, 13); g.color = dim
                g.drawString("Rows 0, 2, 3, 5 recover d[t]; verify the stripe hash.", 20, 575)
            }
        } finally { g.dispose() }
    }
}
