package vqtd

import java.awt.*
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

// Math foundations for the classroom. Every number on these slides is
// computed here at runtime from the same Gf256 code the other slides use,
// so a learner can check the arithmetic instead of trusting typed-in values.

internal fun hex(v: Int) = v.toString(16).uppercase().padStart(2, '0')

// ---------------------------------------------------------------- GF(2⁸)

internal data class XtimeStep(val before: Int, val shifted: Int, val overflow: Boolean, val after: Int)

internal object GfTrace {
    const val POLY = 0x11d
    /** Multiply by x (= 0x02): shift left; if bit 8 appears, XOR the field polynomial. */
    fun xtime(a: Int): XtimeStep {
        val shifted = a shl 1
        val overflow = shifted and 0x100 != 0
        return XtimeStep(a, shifted, overflow, if (overflow) shifted xor POLY else shifted)
    }
    /** ×04 = ×x², i.e. two doublings. */
    fun times4(a: Int): List<XtimeStep> { val first = xtime(a); return listOf(first, xtime(first.after)) }
    fun bitString(value: Int, width: Int = 8): String {
        val s = (width - 1 downTo 0).joinToString("") { ((value shr it) and 1).toString() }
        return if (width == 9) "${s[0]} ${s.substring(1, 5)} ${s.substring(5)}" else "${s.substring(0, 4)} ${s.substring(4)}"
    }
    /** Shift-and-add ("Russian peasant") multiplication with every step written out. */
    fun multiplyTrace(a: Int, b: Int): Pair<Int, List<String>> {
        var x = a; var result = 0
        val lines = mutableListOf("a = 0x${hex(a)} = ${bitString(a)}     b = 0x${hex(b)} = ${bitString(b)}", "")
        for (i in 0..7) {
            if ((b shr i) and 1 == 1) {
                val next = result xor x
                lines += "bit $i of b = 1 → add a·x^$i:   ${hex(result)} ⊕ ${hex(x)} = ${hex(next)}"
                result = next
            } else lines += "bit $i of b = 0 → skip;           result stays ${hex(result)}"
            if (i < 7) {
                val step = xtime(x)
                lines += "      double a·x^$i: ${bitString(x)} → ${bitString(step.shifted, 9)}" +
                    if (step.overflow) "  bit 8 set → ⊕ 1 0001 1101 → ${hex(step.after)}" else "  → ${hex(step.after)}"
                x = step.after
            }
        }
        lines += ""; lines += "a · b = 0x${hex(result)}   (Gf256.multiply gives 0x${hex(Gf256.multiply(a, b))})"
        return result to lines
    }
    fun inverse(a: Int): Int { require(a in 1..255); return Gf256.power(a, 254) }
}

// ------------------------------------------------------- row reduction

internal class RowStep(
    val title: String, val detail: List<String>, val matrix: Array<IntArray>,
    val labels: List<String>, val pivot: Int, val changed: Set<Int>
)

internal object RowReduceTrace {
    val survivors = intArrayOf(0, 2, 3, 5)
    val data = intArrayOf(4, 0, 0, 0)
    val coded: IntArray by lazy {
        IntArray(6) { r -> (0..3).fold(0) { v, c -> v xor Gf256.multiply(Gf256.generator[r][c], data[c]) } }
    }
    val survivingCodes: IntArray get() = IntArray(4) { coded[survivors[it]] }
    val steps: List<RowStep> by lazy { build() }
    val inverse: Array<IntArray> get() = Array(4) { r -> steps.last().matrix[r].copyOfRange(4, 8) }
    val recovered: IntArray get() = IntArray(4) { r -> (0..3).fold(0) { v, k -> v xor Gf256.multiply(inverse[r][k], survivingCodes[k]) } }

    private fun snapshot(w: Array<IntArray>) = Array(4) { w[it].clone() }
    private fun build(): List<RowStep> {
        val w = Array(4) { r -> IntArray(8) { c -> if (c < 4) Gf256.generator[survivors[r]][c] else if (c - 4 == r) 1 else 0 } }
        val labels = survivors.map { "G$it" }.toMutableList()
        val out = mutableListOf(RowStep("Augment [A | I]",
            listOf("A = rows 0, 2, 3, 5 of G (the survivors).", "I = 4×4 identity; it records every row operation."),
            snapshot(w), labels.toList(), -1, emptySet()))
        for (col in 0..3) {
            val notes = mutableListOf<String>(); val changed = mutableSetOf(col)
            val pivot = (col..3).first { w[it][col] != 0 }
            if (pivot != col) {
                val t = w[col]; w[col] = w[pivot]; w[pivot] = t
                val l = labels[col]; labels[col] = labels[pivot]; labels[pivot] = l
                changed += pivot; notes += "swap rows $col ↔ $pivot (need a nonzero pivot)"
            }
            val pv = w[col][col]
            if (pv != 1) {
                val f = GfTrace.inverse(pv)
                for (c in 0..7) w[col][c] = Gf256.multiply(w[col][c], f)
                notes += "scale row $col by ${hex(pv)}⁻¹ = ${hex(f)}  (${hex(pv)}·${hex(f)} = 01)"
            }
            for (r in 0..3) if (r != col && w[r][col] != 0) {
                val amount = w[r][col]
                for (c in 0..7) w[r][c] = w[r][c] xor Gf256.multiply(amount, w[col][c])
                changed += r; notes += "row $r ⊕= ${hex(amount)}·row $col  (clears column $col)"
            }
            if (notes.isEmpty()) notes += "column $col is already clean"
            out += RowStep("Column $col", notes, snapshot(w), labels.toList(), col, changed)
        }
        return out
    }
}

// ------------------------------------------------------------- entropy

internal class EntropySample(val name: String, val note: String, val bytes: ByteArray) {
    val counts = IntArray(256).also { c -> bytes.forEach { c[it.toInt() and 0xff]++ } }
    val distinct = counts.count { it > 0 }
    val h8: Double = abs(counts.filter { it > 0 }.sumOf { val p = it.toDouble() / bytes.size; p * ln(p) / ln(2.0) })
    /** Display only: 16 buckets by high nibble. H₈ itself uses all 256 values. */
    val buckets = IntArray(16).also { b -> for (v in 0..255) b[v shr 4] += counts[v] }
    val tryZstd get() = h8 < EntropyLesson.GATE
}

internal object EntropyLesson {
    const val SAMPLE = 8192          // RedTail-X sampled 8 KiB per 256 KiB chunk
    const val GATE = 6.4             // bits/byte threshold reported in RedTail-X §5.4
    const val K_B = 1.380649e-23     // J/K, exact in the 2019 SI
    val samples: List<EntropySample> by lazy {
        val text = "Reed-Solomon shares rebuild exact bytes; hashes only check them. ".toByteArray()
        var state = 0x2545F491
        val random = ByteArray(SAMPLE) {
            state = state xor (state shl 13); state = state xor (state ushr 17); state = state xor (state shl 5)
            (state ushr 24).toByte()
        }
        listOf(
            EntropySample("A · constant", "8,192 copies of 'A'", ByteArray(SAMPLE) { 'A'.code.toByte() }),
            EntropySample("B · English text", "one sentence, repeated", ByteArray(SAMPLE) { text[it % text.size] }),
            EntropySample("C · pseudo-random", "xorshift32, fixed seed", random)
        )
    }
    fun f2(v: Double) = String.format("%.2f", v)
}

// ------------------------------------------------------------ 2D slides

internal fun Graphics2D.lessonText(s: String, x: Int, y: Int, color: Color = ink, size: Int = 16, bold: Boolean = false, mono: Boolean = false) {
    this.color = color; font = Font(if (mono) Font.MONOSPACED else Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, size); drawString(s, x, y)
}
internal fun Graphics2D.lessonPanel(x: Int, y: Int, w: Int, h: Int, accent: Color, thick: Boolean = true) {
    color = surface; fillRoundRect(x, y, w, h, 14, 14)
    color = accent; stroke = BasicStroke(if (thick) 2.2f else 1f); drawRoundRect(x, y, w, h, 14, 14)
}

internal class MathLesson2DCanvas(private val kind: LessonKind) : JPanel() {
    var stage = 0; private set
    val lastStage = 6
    init { background = canvas; preferredSize = Dimension(980, 610) }
    fun next(): Boolean { if (stage < lastStage) stage++; repaint(); return stage < lastStage }
    fun reset() { stage = 0; repaint() }

    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = raw.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.scale(width / 980.0, height / 610.0)
            when (kind) {
                LessonKind.GFHAND -> drawGf(g)
                LessonKind.ROWREDUCE -> drawRowReduce(g)
                LessonKind.ENTROPY -> drawEntropy(g)
                else -> Unit
            }
            g.lessonText("Stage $stage/$lastStage  •  Next reveals one step; every value is computed live.", 24, 600, dim, 12)
        } finally { g.dispose() }
    }

    private fun claim(g: Graphics2D, lines: List<String>, x: Int = 600, y: Int = 92, w: Int = 360) {
        g.lessonPanel(x, y, w, 30 + lines.size * 24, cyan)
        lines.forEachIndexed { i, s -> g.lessonText(s, x + 14, y + 28 + i * 24, if (i == 0) cyan else ink, if (i == 0) 15 else 14, i == 0) }
    }

    /** Draws one byte (or a 9-bit intermediate) as cells; bit 8 is red when it overflows. */
    private fun bitRow(g: Graphics2D, x: Int, y: Int, label: String, value: Int, width: Int, accent: Color, note: String = "") {
        g.lessonText(label, x, y + 24, accent, 15, true, true)
        val start = x + 120
        for (i in 0 until width) {
            val bit = width - 1 - i
            val one = (value shr bit) and 1 == 1
            val cx = start + (9 - width + i) * 38
            val cellColor = if (bit == 8 && one) red else if (one) accent else borderColor
            g.color = surface; g.fillRoundRect(cx, y, 34, 34, 8, 8)
            g.color = cellColor; g.stroke = BasicStroke(if (one) 2.4f else 1f); g.drawRoundRect(cx, y, 34, 34, 8, 8)
            g.lessonText(if (one) "1" else "0", cx + 11, y + 24, if (one) cellColor else dim, 17, one, true)
        }
        if (note.isNotEmpty()) g.lessonText(note, start + 9 * 38 + 8, y + 24, accent, 14, true)
    }

    private fun drawGf(g: Graphics2D) {
        val p0 = GfTrace.times4(0x52); val p1 = GfTrace.times4(0xF7)
        g.lessonText("GF(2⁸) BY HAND: WHERE DOES 55 COME FROM?", 24, 34, ink, 21, true)
        g.lessonText("P0 row = 52 F7 02 A6,  d = [04,00,00,00]ᵀ  →  c₄ = 52·04 ⊕ F7·00 ⊕ 02·00 ⊕ A6·00", 24, 61, dim, 15)
        g.lessonText("bit:   8    7    6    5    4    3    2    1    0", 144, 100, dim, 12, false, true)
        bitRow(g, 24, 110, "0x52", 0x52, 8, ink, if (stage >= 2) "× 04 = × x²" else "")
        if (stage >= 3) bitRow(g, 24, 165, "×2", p0[0].shifted, 9, cyan, "→ 0x${hex(p0[0].after)}")
        if (stage >= 4) bitRow(g, 24, 220, "×2", p0[1].shifted, 9, cyan, "overflow!")
        if (stage >= 5) {
            bitRow(g, 24, 275, "⊕ 0x11d", GfTrace.POLY, 9, amber)
            g.color = ink; g.stroke = BasicStroke(1.5f); g.drawLine(144, 318, 144 + 9 * 38, 318)
            bitRow(g, 24, 326, "=", p0[1].after, 9, green, "= 0x${hex(p0[1].after)} ✓")
        }
        if (stage >= 6) {
            g.lessonPanel(24, 390, 560, 150, amber)
            g.lessonText("P1:  F7·04  (same recipe, both doublings overflow)", 38, 418, amber, 15, true)
            g.lessonText("F7 = ${GfTrace.bitString(0xF7)}", 38, 446, ink, 14, false, true)
            g.lessonText("×2 → ${GfTrace.bitString(p1[0].shifted, 9)}  ⊕ 11D → 0x${hex(p1[0].after)}", 38, 470, ink, 14, false, true)
            g.lessonText("×2 → ${GfTrace.bitString(p1[1].shifted, 9)}  ⊕ 11D → 0x${hex(p1[1].after)}", 38, 494, ink, 14, false, true)
            g.lessonText("c = [04, 00, 00, 00, ${hex(p0[1].after)}, ${hex(p1[1].after)}]  — matches Layer Bridge", 38, 526, green, 14, true)
        }
        claim(g, when (stage) {
            0 -> listOf("THE QUESTION", "Layer Bridge showed P0 → 55.", "Only one term is nonzero, so", "c₄ is just 52 · 04. But how?")
            1 -> listOf("ADDITION IS XOR", "Bits add with no carry: 1 ⊕ 1 = 0.", "a · 00 = 00 and x ⊕ 00 = x,", "so the three zero terms vanish.")
            2 -> listOf("MULTIPLY BY 04", "A byte is a polynomial with 0/1", "coefficients: 0x52 = x⁶+x⁴+x.", "04 = x², so ×04 means ×x twice;", "×x is a one-bit left shift.")
            3 -> listOf("FIRST DOUBLING", "52 → ${hex(p0[0].after)}. Bit 8 stayed 0,", "so the result still fits in a byte.", "No reduction needed.")
            4 -> listOf("SECOND DOUBLING", "${hex(p0[0].after)} → ${GfTrace.bitString(p0[1].shifted, 9)}.", "Bit 8 is 1 (red): nine bits do not", "fit in a 256-element field.")
            5 -> listOf("REDUCE WITH 0x11d", "0x11d = x⁸+x⁴+x³+x²+1. In this field", "x⁸ = x⁴+x³+x²+1, so XOR it in:", "bit 8 cancels and 0x55 remains.")
            else -> listOf("YOUR TURN", "Redo P1 = F7·04 on paper first.", "Then try any pair in Code Bridge →", "GF(2⁸) calculator, which prints", "every shift and XOR.")
        }, y = 110)
    }

    private fun drawRowReduce(g: Graphics2D) {
        val steps = RowReduceTrace.steps
        val step = steps[minOf(stage, steps.size - 1)]
        g.lessonText("ROW REDUCTION: INVERT THE FOUR SURVIVORS", 24, 34, ink, 21, true)
        g.lessonText("Gauss–Jordan on [A | I] over GF(2⁸). Subtracting = adding = XOR.", 24, 61, dim, 15)
        val x0 = 92; val y0 = 128; val cw = 52; val gap = 22
        g.lessonText("A", x0 + 2 * cw - 4, y0 - 12, green, 15, true)
        g.lessonText(if (stage >= 5) "A⁻¹" else "I  (records operations)", x0 + 4 * cw + gap + 40, y0 - 12, if (stage >= 5) green else cyan, 15, true)
        for (r in 0..3) {
            val y = y0 + r * 58
            val rowChanged = r in step.changed && stage in 1..4
            g.lessonText(step.labels[r], 30, y + 30, if (rowChanged) amber else dim, 15, true, true)
            for (c in 0..7) {
                val x = x0 + c * cw + if (c >= 4) gap else 0
                val v = step.matrix[r][c]
                val isPivot = stage in 1..4 && r == step.pivot && c == step.pivot
                val accent = when {
                    isPivot -> amber
                    stage >= 5 && c >= 4 -> green
                    rowChanged -> amber
                    v == 0 -> borderColor
                    c < 4 -> green else -> cyan
                }
                g.color = surface; g.fillRoundRect(x, y, cw - 6, 44, 8, 8)
                g.color = accent; g.stroke = BasicStroke(if (isPivot) 3.2f else if (v != 0) 2f else 1f); g.drawRoundRect(x, y, cw - 6, 44, 8, 8)
                g.lessonText(hex(v), x + 9, y + 28, if (v == 0) dim else ink, 16, v != 0, true)
            }
        }
        g.color = dim; g.stroke = BasicStroke(1.5f); g.drawLine(x0 + 4 * cw + gap / 2 - 3, y0 - 4, x0 + 4 * cw + gap / 2 - 3, y0 + 4 * 58)
        if (stage >= 6) {
            val c = RowReduceTrace.survivingCodes; val d = RowReduceTrace.recovered
            g.lessonPanel(24, 380, 560, 120, green)
            g.lessonText("RECOVER THE DATA", 38, 408, green, 15, true)
            g.lessonText("c_surv = [c0, c2, c3, c5] = [${c.joinToString(", ") { hex(it) }}]", 38, 436, ink, 14, false, true)
            g.lessonText("d = A⁻¹ · c_surv  = [${d.joinToString(", ") { hex(it) }}]ᵀ", 38, 462, ink, 14, true, true)
            g.lessonText(if (d.contentEquals(RowReduceTrace.data)) "Original d = [04,00,00,00] restored ✓" else "MISMATCH", 38, 488, green, 14, true)
        }
        claim(g, when (stage) {
            0 -> listOf("SET UP", "Shares 1 and 4 are lost. Stack the", "G rows of shares 0, 2, 3, 5 as A,", "and put I beside it.") + step.detail.drop(1)
            in 1..4 -> listOf(step.title.uppercase()) + step.detail.flatMap { it.split("  ") }
            5 -> listOf("DONE", "The left half is now I₄. Every", "operation was also applied to I,", "so the right half is A⁻¹.")
            else -> listOf("WHY ANY FOUR WORK", "All 15 four-row choices of G", "are invertible (MDS property).", "Try another choice: --check-math", "verifies all 15.")
        }, x = 600, y = 92, w = 360)
    }

    private fun drawEntropy(g: Graphics2D) {
        val samples = EntropyLesson.samples
        g.lessonText("ENTROPY H₈: HOW MIXED ARE THE BYTES?", 24, 34, ink, 21, true)
        g.lessonText("H₈ = −Σ p(σ) log₂ p(σ) over 256 byte values · p(σ) = count(σ)/8192 · 0 ≤ H₈ ≤ 8", 24, 61, dim, 15)
        val colors = listOf(green, cyan, amber)
        for ((i, s) in samples.withIndex()) {
            val x = 24 + i * 318; val shown = stage >= i + 1
            g.lessonPanel(x, 82, 300, 300, if (shown) colors[i] else borderColor, shown)
            g.lessonText(s.name, x + 14, 108, if (shown) colors[i] else dim, 15, true)
            g.lessonText(s.note, x + 14, 128, dim, 12)
            if (!shown) continue
            val max = s.buckets.max().coerceAtLeast(1)
            for (b in 0..15) {
                val h = (s.buckets[b] * 150.0 / max).toInt()
                g.color = colors[i]; g.fillRect(x + 22 + b * 16, 300 - h, 12, h)
            }
            g.color = dim; g.drawLine(x + 18, 300, x + 282, 300)
            g.lessonText("byte value buckets 0x0_ … 0xF_", x + 50, 318, dim, 11)
            g.lessonText("H₈ = ${EntropyLesson.f2(s.h8)} bits/byte", x + 14, 346, ink, 16, true)
            g.lessonText("${s.distinct} of 256 values used", x + 14, 368, dim, 12)
            if (stage >= 4) g.lessonText(if (s.tryZstd) "< 6.4 → try zstd" else "≥ 6.4 → skip zstd", x + 165, 368, if (s.tryZstd) green else red, 13, true)
        }
        when {
            stage >= 6 -> {
                g.lessonPanel(24, 398, 932, 178, red)
                g.lessonText("BOUNDARY: THE \"QUASI\" IN QUASI-THERMODYNAMICS", 38, 426, red, 15, true)
                g.lessonText("Same mathematics, different object. A file's bytes have no temperature or energy; H₈ is a property", 38, 452, ink, 14)
                g.lessonText("of their frequency counts. RedTail-X used H₈ only to decide whether to try compression (with a", 38, 474, ink, 14)
                g.lessonText("periodicity check, keeping the smaller verified output). It never decides Reed–Solomon recovery.", 38, 496, ink, 14)
                g.lessonText("Boltzmann S = k_B ln W   ·   Gibbs S = −k_B Σ p ln p   ·   S = (k_B ln 2) · H", 38, 530, cyan, 15, true, true)
                g.lessonText("Display groups 256 values into 16 buckets; H₈ itself is computed over all 256.", 38, 558, dim, 12)
            }
            stage >= 5 -> {
                g.lessonPanel(24, 398, 932, 178, cyan)
                g.lessonText("THE THERMODYNAMICS LINK", 38, 426, cyan, 15, true)
                g.lessonText("Boltzmann (1877):  S = k_B ln W,  W = number of equally likely microstates", 38, 456, ink, 15, false, true)
                g.lessonText("Gibbs:             S = −k_B Σ p ln p     ← same shape as H₈ = −Σ p log₂ p", 38, 482, ink, 15, false, true)
                g.lessonText("Convert:           S = (k_B ln 2) · H    (bits → J/K)", 38, 508, ink, 15, false, true)
                g.lessonText("Uniform bytes: W = 256 → H = log₂ 256 = 8 bits → S = 8·k_B·ln 2 ≈ ${String.format("%.2e", 8 * EntropyLesson.K_B * ln(2.0))} J/K", 38, 540, green, 14, true)
            }
            stage >= 4 -> {
                g.lessonPanel(24, 398, 932, 120, green)
                g.lessonText("THE REDTAIL-X GATE (printed p. 5, §5.4)", 38, 426, green, 15, true)
                g.lessonText("One 8 KiB sample per 256 KiB chunk. If H₈ < 6.4 bits/byte (or the sample is strongly periodic),", 38, 456, ink, 14)
                g.lessonText("try zstd level 3 and keep whichever verified representation is smaller. High-entropy data", 38, 478, ink, 14)
                g.lessonText("such as already-compressed or encrypted bytes is stored as is.", 38, 500, ink, 14)
            }
            else -> claim(g, when (stage) {
                0 -> listOf("WHAT H₈ MEASURES", "How evenly a sample uses the", "256 byte values. One value only:", "0 bits. All 256 equally: 8 bits.")
                1 -> listOf("SAMPLE A", "One value, p = 1: −1·log₂1 = 0.", "Perfectly predictable bytes.", "0·log₂0 := 0 for unused values.")
                2 -> listOf("SAMPLE B", "Text uses ${samples[1].distinct} distinct values,", "unevenly: H₈ = ${EntropyLesson.f2(samples[1].h8)}.", "Predictable → compressible.")
                else -> listOf("SAMPLE C", "Nearly flat over all 256 values:", "H₈ = ${EntropyLesson.f2(samples[2].h8)}, close to the max 8.", "(Finite samples sit just below 8.)")
            }, x = 24, y = 398, w = 600)
        }
    }
}

// ------------------------------------------------------------ 3D scenes

private fun bitNodes(prefix: String, value: Int, width: Int, y: Double, z: Double, appears: Int, onRole: String, header: String, headerDetail: String, redOverflow: Boolean = true): List<SpatialNode> {
    val nodes = mutableListOf(SpatialNode("${prefix}_h", header, headerDetail, -9.6, y, z, appears, "proof"))
    for (bit in 0 until width) {
        val one = (value shr bit) and 1 == 1
        val role = if (bit == 8 && one && redOverflow) "warning" else if (one) onRole else "zero"
        nodes += SpatialNode("${prefix}_$bit", if (one) "1" else "0", "bit $bit (x^$bit term) of 0x${value.toString(16).uppercase()}", (3.5 - bit) * 1.45, y, z, appears, role)
    }
    return nodes
}

internal fun gfHandScene(): SpatialScene {
    val p0 = GfTrace.times4(0x52); val p1 = GfTrace.times4(0xF7)
    val nodes = mutableListOf<SpatialNode>(); val edges = mutableListOf<SpatialEdge>()
    val ya = -5.0; val yb = 6.5
    nodes += bitNodes("a0", 0x52, 8, ya, -10.0, 0, "proof", "0x52", "P0 coefficient for d0")
    nodes += bitNodes("a1", p0[0].shifted, 9, ya, -5.0, 1, "proof", "×2 → ${hex(p0[0].after)}", "First doubling: left shift, bit 8 is 0")
    nodes += bitNodes("a2", p0[1].shifted, 9, ya, 0.0, 2, "proof", "×2 → 1${hex(p0[1].shifted and 0xff)}", "Second doubling: bit 8 overflows")
    nodes += bitNodes("ap", GfTrace.POLY, 9, ya, 5.0, 3, "parity", "⊕ 0x11D", "Field polynomial x⁸+x⁴+x³+x²+1", redOverflow = false)
    nodes += bitNodes("ar", p0[1].after, 8, ya, 10.0, 4, "data", "= ${hex(p0[1].after)}", "52·04 in GF(2⁸)")
    for (bit in 0..7) { edges += SpatialEdge("a0_$bit", "a1_${bit + 1}", 1); edges += SpatialEdge("a1_$bit", "a2_${bit + 1}", 2) }
    for (bit in 0..7) { edges += SpatialEdge("a2_$bit", "ar_$bit", 4, "proof"); edges += SpatialEdge("ap_$bit", "ar_$bit", 4, "parity") }
    nodes += bitNodes("b0", 0xF7, 8, yb, -10.0, 5, "parity", "0xF7", "P1 coefficient for d0")
    nodes += bitNodes("b1", p1[0].shifted, 9, yb, -5.0, 5, "parity", "×2", "Shift: bit 8 overflows")
    nodes += bitNodes("b2", p1[0].after, 8, yb, 0.0, 5, "parity", "⊕11D → ${hex(p1[0].after)}", "Reduced after first doubling")
    nodes += bitNodes("b3", p1[1].shifted, 9, yb, 5.0, 5, "parity", "×2", "Shift: bit 8 overflows again")
    nodes += bitNodes("b4", p1[1].after, 8, yb, 10.0, 5, "parity", "⊕11D → ${hex(p1[1].after)}", "F7·04 in GF(2⁸)")
    for (bit in 0..7) { edges += SpatialEdge("b0_$bit", "b1_${bit + 1}", 5, "parity"); edges += SpatialEdge("b2_$bit", "b3_${bit + 1}", 5, "parity") }
    nodes += SpatialNode("code", "c = [04,00,00,00,${hex(p0[1].after)},${hex(p1[1].after)}]", "Matches the Layer Bridge codeword", 9.0, 0.8, 14.0, 6, "proof")
    edges += SpatialEdge("ar_h", "code", 6, "proof"); edges += SpatialEdge("b4_h", "code", 6, "proof")
    return SpatialScene("GF(2⁸) BY HAND IN 3D", "Depth = one multiplication step · each node is one bit (x⁸ … x⁰)",
        "Computed live by Gf256 with polynomial 0x11d, the classroom field construction used across VQTD.",
        listOf("0x52 as eight bits: each bit is a coefficient of x⁷ … x⁰.",
            "×02 shifts every bit one place left: 52 → ${hex(p0[0].after)}, bit 8 stays 0.",
            "Shift again: bit 8 (red) overflows — nine bits do not fit the field.",
            "Bring in the field polynomial 0x11d = x⁸+x⁴+x³+x²+1.",
            "XOR cancels bit 8 and leaves 0x${hex(p0[1].after)}: P0 = 52·04 = ${hex(p0[1].after)}.",
            "P1: F7 overflows on both doublings → ${hex(p1[0].after)} → ${hex(p1[1].after)}.",
            "Both parity bytes match the Layer Bridge codeword."),
        nodes, edges, nodes.map { it.id }.toSet(), yaw0 = 0.22, pitch0 = -0.42)
}

internal fun rowReduceScene(): SpatialScene {
    val steps = RowReduceTrace.steps
    val nodes = mutableListOf<SpatialNode>(); val edges = mutableListOf<SpatialEdge>()
    fun x(c: Int) = (c - 3.5) * 1.7 + if (c >= 4) 0.9 else -0.9
    fun y(r: Int) = (r - 1.5) * 1.5
    for ((k, step) in steps.withIndex()) {
        val z = k * 3.6 - 10.0
        for (r in 0..3) for (c in 0..7) {
            val v = step.matrix[r][c]
            val role = when {
                k in 1..4 && r == step.pivot && c == step.pivot -> "warning"
                v == 0 -> "zero"
                k in 1..4 && r in step.changed -> "parity"
                c < 4 -> "data" else -> "proof"
            }
            nodes += SpatialNode("m${k}_${r}_$c", hex(v), "${step.title}: row $r (${step.labels[r]}), column $c = 0x${hex(v)}", x(c), y(r), z, k, role)
            if (c < 7) edges += SpatialEdge("m${k}_${r}_$c", "m${k}_${r}_${c + 1}", k)
        }
        nodes += SpatialNode("m${k}_h", if (k == 0) "[A | I]" else "col ${k - 1}", step.detail.joinToString("; "), -9.8, -3.6, z, k, "proof")
    }
    val zi = steps.size * 3.6 - 10.0 + 1.0
    for (r in 0..3) for (c in 0..3) {
        val v = RowReduceTrace.inverse[r][c]
        nodes += SpatialNode("inv_${r}_$c", hex(v), "A⁻¹[$r,$c] = 0x${hex(v)}", x(c + 4), y(r), zi, 5, if (v == 0) "zero" else "data")
    }
    nodes += SpatialNode("inv_h", "A⁻¹", "Right half after the left half became I₄", x(4) - 2.5, -3.6, zi, 5, "proof")
    val zd = zi + 5.0
    val cs = RowReduceTrace.survivingCodes; val ds = RowReduceTrace.recovered
    for (r in 0..3) {
        nodes += SpatialNode("c$r", "c${RowReduceTrace.survivors[r]}=${hex(cs[r])}", "Surviving share byte", -7.0, y(r), zd, 6, "parity")
        nodes += SpatialNode("d$r", "d$r=${hex(ds[r])}", "Recovered data byte", 7.0, y(r), zd, 6, "data")
    }
    for (r in 0..3) for (k in 0..3) if (RowReduceTrace.inverse[r][k] != 0) edges += SpatialEdge("c$k", "d$r", 6, "proof")
    val labels = nodes.filter { it.appears >= 5 || it.id.endsWith("_h") }.map { it.id }.toSet()
    return SpatialScene("ROW REDUCTION IN 3D", "Depth = one Gauss–Jordan column · the current plane shows its values",
        "Survivor rows 0, 2, 3, 5 of the classroom generator; amber = changed row, red = pivot.",
        listOf("Stack G rows 0, 2, 3, 5 beside the identity: [A | I].") +
            steps.drop(1).map { "${it.title}: ${it.detail.joinToString("; ")}" } +
            listOf("Left half is now I₄, so the right half is A⁻¹.",
                "d = A⁻¹·[${cs.joinToString(",") { hex(it) }}] = [${ds.joinToString(",") { hex(it) }}] — the data is back."),
        nodes, edges, labels, yaw0 = 0.38, pitch0 = -0.30, fadePast = true)
}

internal fun entropyScene(): SpatialScene {
    val samples = EntropyLesson.samples
    val roles = listOf("data", "proof", "parity")
    val nodes = mutableListOf<SpatialNode>(); val edges = mutableListOf<SpatialEdge>()
    for ((i, s) in samples.withIndex()) {
        val z = -7.0 + i * 7.0; val max = s.buckets.max().coerceAtLeast(1)
        nodes += SpatialNode("h$i", "${s.name.substringBefore(" ·")}: H₈=${EntropyLesson.f2(s.h8)}", "${s.name}; ${s.distinct} of 256 values used", -8.8, 1.0, z, i + 1, roles[i])
        for (b in 0..15) if (s.buckets[b] > 0) {
            val top = -sqrt(s.buckets[b].toDouble() / max) * 6.0
            nodes += SpatialNode("base${i}_$b", "", "bucket 0x${b.toString(16).uppercase()}_", (b - 7.5) * 0.95, 0.0, z, i + 1, "zero")
            nodes += SpatialNode("top${i}_$b", "", "bucket 0x${b.toString(16).uppercase()}_: ${s.buckets[b]} of 8192 bytes", (b - 7.5) * 0.95, top, z, i + 1, roles[i])
            edges += SpatialEdge("base${i}_$b", "top${i}_$b", i + 1, roles[i])
        }
        nodes += SpatialNode("g$i", if (s.tryZstd) "< 6.4: try zstd" else "≥ 6.4: skip", "RedTail-X gate at 6.4 bits/byte", 9.5, 1.0, z, 4, if (s.tryZstd) "data" else "warning")
    }
    nodes += SpatialNode("gibbs", "S = (k_B ln 2)·H", "Gibbs S = −k_B Σ p ln p has the same form as H₈", 0.0, -11.0, 12.0, 5, "proof")
    nodes += SpatialNode("boltz", "W=256 → H=8 bits", "Boltzmann S = k_B ln W for W equally likely states", -6.0, -11.0, 12.0, 5, "proof")
    nodes += SpatialNode("quasi", "QUASI: no temperature", "Same mathematical form; bytes have no energy. H₈ only gates compression.", 6.0, -11.0, 12.0, 6, "warning")
    edges += SpatialEdge("boltz", "gibbs", 5, "proof"); edges += SpatialEdge("gibbs", "quasi", 6, "warning")
    val labels = nodes.filter { it.label.isNotEmpty() }.map { it.id }.toSet()
    return SpatialScene("ENTROPY H₈ IN 3D", "Depth = sample · bar height = share of bytes per bucket (√ scale)",
        "H₈ uses all 256 values; 16 buckets are for display. The gate rule is from RedTail-X §5.4.",
        listOf("H₈ measures how evenly 8 KiB of bytes use the 256 possible values.",
            "Constant bytes: one value, p = 1, H₈ = ${EntropyLesson.f2(samples[0].h8)} bits/byte.",
            "Repeated English text: ${samples[1].distinct} values, unevenly, H₈ = ${EntropyLesson.f2(samples[1].h8)}.",
            "Pseudo-random bytes: nearly flat, H₈ = ${EntropyLesson.f2(samples[2].h8)} (max 8).",
            "Gate: H₈ < 6.4 → try zstd; otherwise store as is.",
            "Gibbs S = −k_B Σ p ln p has H's shape: S = (k_B ln 2)·H.",
            "Same form, different object: bytes have no temperature — the 'quasi'."),
        nodes, edges, labels, yaw0 = 0.42, pitch0 = -0.30)
}

/** Binary Lens in 3D: the invented classroom encoding, one bit per node. */
internal fun binaryScene(candidates: List<Candidate>): SpatialScene {
    val nodes = mutableListOf<SpatialNode>(); val edges = mutableListOf<SpatialEdge>()
    val winner = candidates.minWith(compareBy<Candidate> { it.bits }.thenBy { it.index })
    for ((i, c) in candidates.withIndex()) {
        val z = (i - 1) * 6.0
        nodes += SpatialNode("h$i", "F(${c.index}): ${c.left}${c.op}${c.right}", c.formula(), -10.5, 0.0, z, 0, "proof")
        val fields = listOf(c.opcode.substring(0, 2) to ("parity" to 1), c.operandBits(c.left) to ("proof" to 2), c.operandBits(c.right) to ("data" to 3))
        var j = 0; var prev: String? = null
        for ((bits, meta) in fields) for (ch in bits) {
            val id = "b${i}_$j"
            nodes += SpatialNode(id, ch.toString(), "bit $j of F(${c.index}); ${when (meta.second) { 1 -> "opcode"; 2 -> "left operand ${c.left}"; else -> "right operand ${c.right}" }}",
                (j - 4.5) * 1.3, 0.0, z, meta.second, meta.first)
            if (prev != null) edges += SpatialEdge(prev, id, meta.second, meta.first)
            prev = id; j++
        }
        nodes += SpatialNode("t$i", "${c.bits} bits", "2 + width(${c.left}) + width(${c.right})", 9.5, -1.5, z, 4, if (c == winner) "data" else "zero")
    }
    nodes += SpatialNode("win", "arg min → F(${winner.index})", "Shortest valid formula under the invented rule", 9.5, -6.0, 0.0, 5, "data")
    for (i in candidates.indices) edges += SpatialEdge("t$i", "win", 5, if (candidates[i] == winner) "proof" else "zero")
    nodes += SpatialNode("excl", "RedTail-X §6: excluded", "Shortest-form storage stored more bytes and skipped verification", -2.0, -6.0, 9.0, 6, "warning")
    edges += SpatialEdge("win", "excl", 6, "warning")
    return SpatialScene("BINARY LENS IN 3D", "Depth = candidate · amber opcode, cyan left operand, green right operand",
        "Invented classroom encoding; not a RedTail-X wire format.",
        listOf("Three candidate formulas, all equal to 4.", "Each starts with a 2-bit opcode: 00 ADD, 01 SUB.",
            "Then the left operand in its minimal width.", "Then the right operand in its minimal width.",
            "Count bits: 2 + width(left) + width(right).", "arg min picks the shortest valid formula.",
            "The paper tested shortest-form storage and excluded it."),
        nodes, edges, nodes.map { it.id }.toSet())
}
