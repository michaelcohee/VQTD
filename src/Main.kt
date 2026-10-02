package vqtd

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class RoomTheme { BEIGE, DARK_GREEN }
private object RoomStyle { var theme = RoomTheme.BEIGE; var highContrast = false }
internal val ink get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color.BLACK; RoomStyle.highContrast -> Color.WHITE; RoomStyle.theme == RoomTheme.BEIGE -> Color(0x2D382B); else -> Color(0xEAF2EF) }
internal val dim get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0x303030); RoomStyle.highContrast -> Color(0xE0E0E0); RoomStyle.theme == RoomTheme.BEIGE -> Color(0x586454); else -> Color(0xB2C7BB) }
internal val canvas get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color.WHITE; RoomStyle.highContrast -> Color.BLACK; RoomStyle.theme == RoomTheme.BEIGE -> Color(0xF2EBD9); else -> Color(0x0E1D18) }
internal val panel get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0xE9E9E9); RoomStyle.highContrast -> Color(0x111111); RoomStyle.theme == RoomTheme.BEIGE -> Color(0xE5D8BB); else -> Color(0x18332A) }
internal val surface get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color.WHITE; RoomStyle.highContrast -> Color.BLACK; RoomStyle.theme == RoomTheme.BEIGE -> Color(0xFFF9EA); else -> Color(0x10251D) }
internal val borderColor get() = when { RoomStyle.highContrast -> ink; RoomStyle.theme == RoomTheme.BEIGE -> Color(0x687A5E); else -> Color(0x638776) }
internal val buttonColor get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0xD5D5D5); RoomStyle.highContrast -> Color(0x262626); RoomStyle.theme == RoomTheme.BEIGE -> Color(0xD7C69D); else -> Color(0x284B39) }
internal val green get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0x005A1C); RoomStyle.highContrast -> Color(0x71FF9A); RoomStyle.theme == RoomTheme.BEIGE -> Color(0x146235); else -> Color(0x75D79A) }
internal val cyan get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0x004C88); RoomStyle.highContrast -> Color(0x7CCBFF); RoomStyle.theme == RoomTheme.BEIGE -> Color(0x145D7A); else -> Color(0x7FCBE9) }
internal val amber get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0x765000); RoomStyle.highContrast -> Color(0xFFE477); RoomStyle.theme == RoomTheme.BEIGE -> Color(0x795000); else -> Color(0xF6D47A) }
internal val red get() = when { RoomStyle.highContrast && RoomStyle.theme == RoomTheme.BEIGE -> Color(0x990000); RoomStyle.highContrast -> Color(0xFF9090); RoomStyle.theme == RoomTheme.BEIGE -> Color(0x9A342B); else -> Color(0xF59B93) }

data class Candidate(val index: Int, val left: Int, val op: Char, val right: Int) {
    val result: Int get() = when (op) { '+' -> left + right; '-' -> left - right; else -> 0 }
    private fun width(n: Int): Int = max(1, 32 - n.coerceAtLeast(0).countLeadingZeroBits())
    val bits: Int get() = 2 + width(left) + width(right)
    val opcode: String get() = if (op == '+') "00 ADD" else "01 SUB"
    fun operandBits(n: Int): String = n.toString(2).padStart(width(n), '0')
    fun formula() = "($left $op $right = $result)  F($index)"
    fun encoding() = "$opcode  ${operandBits(left)}  ${operandBits(right)}"
}

data class GlossaryEntry(val symbol: String, val name: String, val formal: String, val plain: String, val code: String)

private val glossary = listOf(
    GlossaryEntry("F(i)", "Indexed candidate function", "VQTD's example maps index i to an expression and its result. The three F examples are not printed as equations in the papers.", "One possible route to target 4 in the class exercise.", "candidates[i]; Candidate.index"),
    GlossaryEntry("arg min", "Argument of the minimum", "arg minᵢ ℓ(F(i)) selects the index whose valid formula has least encoded length.", "Return which route is shortest, rather than returning its size.", "minWith(compareBy { it.bits })"),
    GlossaryEntry("ℓ(F)", "Encoded length", "The number of bits in a stated representation of formula F; VQTD currently uses an illustrative rule.", "How much space the class example costs.", "Candidate.bits"),
    GlossaryEntry("∈", "Membership", "x ∈ S states that x is an element of set S.", "x belongs to this collection.", "set.contains(x)"),
    GlossaryEntry("∀", "Universal quantifier", "∀x ∈ S, P(x) states that P holds for every x in S.", "Check every member.", "S.all { p(it) }"),
    GlossaryEntry("∃", "Existential quantifier", "∃x ∈ S : P(x) states that at least one member satisfies P.", "Find at least one match.", "S.any { p(it) }"),
    GlossaryEntry("⌈x⌉", "Ceiling", "The least integer greater than or equal to x.", "Round upward so the tail still has room.", "(n + divisor - 1) / divisor"),
    GlossaryEntry("GF(2⁸)", "Finite field", "A field of 256 byte-valued symbols with addition and multiplication closed in the field.", "The arithmetic world used by byte-level Reed–Solomon.", "gf_mul(a,b); a xor b"),
    GlossaryEntry("⊕", "Exclusive OR", "Addition in GF(2⁸): a ⊕ b.", "Combine bits without carry.", "a xor b; a ^ b"),
    GlossaryEntry("dₜ", "Data column vector", "dₜ = [D₀[t],D₁[t],D₂[t],D₃[t]]ᵀ.", "Four data symbols at one array position.", "arrayOf(d0[t], d1[t], d2[t], d3[t])"),
    GlossaryEntry("ᵀ", "Transpose", "Turns a row representation into a column representation or conversely.", "Rotate the orientation used by matrix multiplication.", "Shape/type decision; often no runtime operation"),
    GlossaryEntry("⊥", "Abstain in BRMR", "BRMR defines ⊥ as no answer in Ω(o)=H∪{⊥}; elsewhere the symbol may mean orthogonal.", "The rebuilder refuses to guess an owner.", "null / Optional.empty() / Option::None"),
    GlossaryEntry("V", "Vandermonde matrix", "V[r,c] = rᶜ over the selected field.", "Rows encode powers of a distinct field element.", "vandermonde[row][column] = gfPow(row,column)"),
    GlossaryEntry("V₀⁻¹", "Inverse of the top matrix", "The matrix which satisfies V₀⁻¹V₀ = I.", "The operation that converts Vandermonde rows into systematic form.", "invert(top)"),
    GlossaryEntry("G", "Systematic generator matrix", "G = V·V₀⁻¹, with its first four rows equal to I₄.", "Keep original data rows visible and generate parity rows below them.", "generator = multiply(v, invert(top))"),
    GlossaryEntry("I₄", "Four-dimensional identity", "A 4×4 matrix with ones on its main diagonal and zeros elsewhere.", "Multiplication leaves the four data values unchanged.", "identity(4)"),
    GlossaryEntry("S⁻¹", "Survivor inverse", "Inverse of the 4×4 generator submatrix selected by surviving share indices.", "Turn surviving rows back into the original four rows.", "invert(survivingRows)"),
    GlossaryEntry("H(x)", "Cryptographic hash", "A deterministic fixed-length commitment to x.", "A fingerprint used to detect changed content.", "sha256(bytes)"),
    GlossaryEntry("||", "Concatenation", "a || b is the ordered byte sequence a followed by b.", "Join fields without changing their order.", "buffer += a; buffer += b"),
    GlossaryEntry("leaf", "Merkle leaf", "BRMR: leaf(c,p)=H(00||len(c)||c||len(p)||p); RedTail-X itself does not specify this owner leaf.", "A hashed owner-map entry in BRMR.", "leafHash(childHandle, parentHandle)"),
    GlossaryEntry("root", "Merkle root", "The final hash obtained by repeatedly combining ordered child hashes.", "One digest representing the committed hierarchy.", "rootOf(leaves)"),
    GlossaryEntry("proof", "Merkle inclusion proof", "Sibling hashes and directions sufficient to recompute a root from one leaf.", "Evidence that a target belongs at a tree position.", "verify(root, leaf, proof)"),
    GlossaryEntry("coordinate", "Bound identity", "MRT-X identifies a share by ordered stripe and share index; BRMR identifies an owner by object handle and line.", "The address that distinguishes identical-looking values.", "stripeIndex + shareIndex; objectHandle + ownerLine"),
    GlossaryEntry("ℵ", "Aleph", "Infinite cardinal notation in mathematics; no ℵ formula was found in the three supplied papers.", "A proposed later question, not part of these reports.", "No implementation association yet"),
    GlossaryEntry("∞", "Infinity", "Unboundedness in mathematics; no infinity flag was defined in the supplied papers.", "Do not insert it into current storage metadata.", "No implementation association yet"),
    GlossaryEntry("X⚑", "Flagged X", "No flagged-X notation was defined in the supplied papers. BRMR uses X for a sealed original file.", "Keep this proposed symbol separate from BRMR's X.", "No implementation association yet"),
    GlossaryEntry("invariant", "Invariant", "A property required to remain true through every valid transformation.", "The rule the code must never break.", "require(result == target)"),
    GlossaryEntry("manifest", "Length and integrity record", "RedTail-X records original_len, share_len, stripe hash, and six share hashes. BRMR adds owner_root and sidecars in its later manifest.", "The map used to interpret stored values correctly.", "StripeManifest; BRMR FileManifest"),
    GlossaryEntry("S(o)", "Parent solution set", "BRMR: S(o) is the set of parents whose 350/360 references list child o.", "All parents that point back to this child.", "reverseIndex[childHandle]"),
    GlossaryEntry("R2u", "Unique mirror rebuilder", "R2u(o)=p when S(o)={p}; otherwise it returns ⊥.", "Accept one parent, abstain on zero or several.", "parents.singleOrNull()"),
    GlossaryEntry("X̂", "Repaired view", "BRMR's estimate of the sealed original X, rendered from E plus accepted owner-line changes.", "A separate view of repaired owner fields.", "renderSidecar(E, acceptedRows)"),
    GlossaryEntry("E", "Stored error file", "E=ERRₛ(X) in the sealed protocol; DarkRock stores E exactly, including converter errors.", "The bytes the storage layer promises to restore.", "STORE=E; RESTORE(STORE)=E"),
    GlossaryEntry("ρ", "Damage rate", "BRMR injects random same-length handle damage at probability ρ.", "How often a handle is changed in the random damage test.", "rng.nextDouble() < rho"),
    GlossaryEntry("C/W/Ab", "Correct / wrong / abstain", "BRMR scores answers as correct, wrong, or abstained before calculating precision and coverage.", "Count all three outcomes separately.", "ScoredResult enum"),
    GlossaryEntry("λ(o)", "Owner-line index (BRMR)", "BRMR p. 3: λ(o) is the index of o's owner line — the first code-330 value outside a 102 {…} group; owner(o) is the line at λ(o).", "Which line of the DXF holds this object's owner handle.", "ownerLineIndex[objectHandle]"),
    GlossaryEntry("λ₁…λ₄", "Your reserved proof labels", "Not defined in the three supplied papers. Distinct from BRMR's λ(o).", "Placeholders until you write their statements and sources.", "No implementation association yet"),
    GlossaryEntry("0x11d", "Field polynomial", "x⁸+x⁴+x³+x²+1, irreducible over GF(2); VQTD builds GF(2⁸) modulo it.", "The rule for folding a 9-bit overflow back into one byte.", "if (x and 0x100 != 0) x = x xor 0x11d"),
    GlossaryEntry("xtime", "Multiply by x (0x02)", "a·x = (a ≪ 1) mod p(x): shift left, XOR 0x11d if bit 8 is set.", "Doubling inside the field.", "GfTrace.xtime(a)"),
    GlossaryEntry("a⁻¹", "Field inverse", "For a ≠ 0, a²⁵⁵ = 1 so a⁻¹ = a²⁵⁴.", "The byte you multiply by to get back to 01.", "Gf256.power(a, 254)"),
    GlossaryEntry("Gauss–Jordan", "Row reduction", "Reduce [A | I] to [I | A⁻¹] with row swaps, scalings, and row additions.", "Undo the mixing one column at a time.", "RowReduceTrace.steps; Gf256.invert4()"),
    GlossaryEntry("MDS", "Maximum distance separable", "A k-of-n code where every k rows of G are invertible; for 4+2, all C(6,4)=15 choices.", "Any four survivors are enough, whichever two are lost.", "--check-math loops over all 15"),
    GlossaryEntry("H₈", "Byte entropy", "H₈ = −Σσ p(σ) log₂ p(σ) over 256 byte values; 0 ≤ H₈ ≤ 8 bits/byte.", "How evenly a sample uses the 256 byte values.", "EntropySample.h8"),
    GlossaryEntry("S, k_B", "Thermodynamic entropy", "Boltzmann S = k_B ln W; Gibbs S = −k_B Σ p ln p = (k_B ln 2)·H. k_B = 1.380649×10⁻²³ J/K.", "Same formula shape as H₈; bytes have no temperature.", "EntropyLesson.K_B"),
    GlossaryEntry("domain separation", "Typed hashing", "Distinct prefixes prevent one object type from being interpreted as another.", "Label each hash before combining its bytes.", "H(0x00||leaf); H(0x01||left||right)")
)

private class Model {
    var candidates = listOf(Candidate(0, 2, '+', 2), Candidate(1, 14, '-', 10), Candidate(2, 8, '-', 4))
    var stage = 0
    val target: Int get() = candidates.first().result
    val valid: List<Candidate> get() = candidates.filter { it.result == target }
    val winner: Candidate get() = valid.minWith(compareBy<Candidate> { it.bits }.thenBy { it.index })
}

private class HuntCanvas(private val model: Model) : JPanel() {
    init { background = canvas; preferredSize = Dimension(980, 610) }
    private fun Graphics2D.text(s: String, x: Int, y: Int, color: Color = ink, size: Int = 15, bold: Boolean = false) {
        this.color = color; font = Font(Font.MONOSPACED, if (bold) Font.BOLD else Font.PLAIN, size); drawString(s, x, y)
    }
    private fun Graphics2D.box(x: Int, y: Int, w: Int, h: Int, color: Color, title: String) {
        this.color = surface; fillRoundRect(x,y,w,h,18,18)
        this.color = color; stroke = BasicStroke(2f); drawRoundRect(x,y,w,h,18,18); text(title,x+14,y+25,color,14,true)
    }
    private fun Graphics2D.arrow(x1:Int,y1:Int,x2:Int,y2:Int,color:Color) {
        this.color=color;stroke=BasicStroke(2.5f);drawLine(x1,y1,x2,y2)
        val a=Math.atan2((y2-y1).toDouble(),(x2-x1).toDouble());val n=10
        drawLine(x2,y2,(x2-n*Math.cos(a-.55)).toInt(),(y2-n*Math.sin(a-.55)).toInt())
        drawLine(x2,y2,(x2-n*Math.cos(a+.55)).toInt(),(y2-n*Math.sin(a+.55)).toInt())
    }
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw); val g=raw as Graphics2D;g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
        g.text("CLASS EXERCISE: SHORTEST FORM → COORDINATE → PROOF → REPAIR",24,32,ink,18,true)
        model.candidates.forEachIndexed { i,c ->
            val y=65+i*100;val active=model.stage>=1;val chosen=model.stage>=3&&c==model.winner;val discarded=model.stage>=4&&c!=model.winner
            val color=when{chosen->green;discarded->red;active->cyan;else->dim};g.box(25,y,255,76,color,"F(${c.index})")
            g.text(c.formula(),42,y+50,if(discarded)dim else ink,16,true)
            if(model.stage>=2){g.text("${c.bits} bits",205,y+50,color,13,true);g.arrow(280,y+38,355,y+38,color)}
            if(discarded){g.color=red;g.stroke=BasicStroke(2f);g.drawLine(40,y+52,190,y+40)}
        }
        if(model.stage>=2){g.box(355,92,190,222,amber,"BIT COST COMPARATOR");model.candidates.forEachIndexed{i,c->g.text("F(${c.index})  ${c.bits.toString().padStart(2)} bits",380,140+i*48,if(c==model.winner&&model.stage>=3)green else ink,15,c==model.winner&&model.stage>=3)};g.text("min(valid, bits, index)",373,292,dim,12)}
        if(model.stage>=3){g.arrow(545,203,610,203,green);g.box(610,155,320,96,green,"SELECTED SHORTEST PATH");g.text(model.winner.formula(),630,195,ink,17,true);g.text(model.winner.encoding(),630,225,green,13,true)}
        if(model.stage>=5){g.arrow(770,251,770,305,cyan);g.box(610,305,320,82,cyan,"MANIFEST COORDINATE");g.text("formula=F(${model.winner.index})  target=${model.target}",630,340);g.text("path=/hunt/target/${model.target}/F${model.winner.index}",630,365,dim,12)}
        if(model.stage>=6){
            g.arrow(610,347,548,415,amber);g.box(360,415,380,72,amber,"CONCEPT ONLY: A COMMITMENT COULD BIND IT");g.text("root → 4 → F(${model.winner.index})  (no paper commits F(i))",380,458,ink,14,true)
            g.arrow(550,487,550,525,cyan);g.box(360,525,380,62,cyan,"SURVIVING ROWS → INVERSE → REPAIR");g.text("location first; reconstruction second",405,565,ink,14,true)
        }
        g.text("Stage ${model.stage}/6",875,600,dim,12)
    }
}

// A rendered stack of 6×4 coefficient grids. Depth represents successive
// byte columns, not a third dimension in the Vandermonde matrix itself.
internal object Gf256 {
    fun multiply(a: Int, b: Int): Int {
        var x = a; var y = b; var result = 0
        repeat(8) {
            if (y and 1 != 0) result = result xor x
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11d
            y = y ushr 1
        }
        return result
    }
    fun power(a: Int, n: Int): Int { var v = 1; repeat(n) { v = multiply(v, a) }; return v }
    private fun inverse(a: Int): Int { require(a != 0); return power(a, 254) }
    fun invert4(source: Array<IntArray>): Array<IntArray> {
        val work = Array(4) { r -> IntArray(8) { c -> if (c < 4) source[r][c] else if (c - 4 == r) 1 else 0 } }
        for (column in 0..3) {
            val pivot = (column..3).firstOrNull { work[it][column] != 0 } ?: error("Singular Vandermonde slice")
            val tmp = work[column]; work[column] = work[pivot]; work[pivot] = tmp
            val factor = inverse(work[column][column])
            for (c in 0..7) work[column][c] = multiply(work[column][c], factor)
            for (r in 0..3) if (r != column) {
                val amount = work[r][column]
                for (c in 0..7) work[r][c] = work[r][c] xor multiply(amount, work[column][c])
            }
        }
        return Array(4) { r -> work[r].copyOfRange(4, 8) }
    }
    val generator: Array<IntArray> = run {
        val vandermonde = Array(6) { r -> IntArray(4) { c -> power(r + 1, c) } }
        val topInverse = invert4(Array(4) { vandermonde[it].clone() })
        Array(6) { r -> IntArray(4) { c -> (0..3).fold(0) { value, k -> value xor multiply(vandermonde[r][k], topInverse[k][c]) } } }
            .also { g -> require((0..3).all { r -> (0..3).all { c -> g[r][c] == if (r == c) 1 else 0 } }) }
    }
}

internal class Matrix3DCanvas(private val compact: Boolean = false) : JPanel() {
    private data class Projected(val row: Int, val column: Int, val slice: Int, val x: Int, val y: Int, val depth: Double, val size: Double)
    private data class RawPoint(val x: Double, val y: Double, val depth: Double, val size: Double)
    private val slices = 40
    private var scan = 0
    private var yaw = 0.55
    private var pitch = -0.23
    private var zoom = 1.0
    private var dragX = 0
    private var dragY = 0
    private var frame = 0
    private var selected: Projected? = null
    private var projectedNodes = emptyList<Projected>()
    var bridgeStage = 0; set(value) { field = value; repaint() }
    val activeSlice: Int get() = scan + 1

    init {
        background = canvas
        preferredSize = Dimension(980, 610)
        toolTipText = "Drag to rotate • wheel to zoom • click a node for its coefficient"
        val mouse = object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) { dragX = e.x; dragY = e.y }
            override fun mouseDragged(e: java.awt.event.MouseEvent) {
                yaw += (e.x - dragX) * 0.007
                pitch = (pitch + (e.y - dragY) * 0.007).coerceIn(-1.2, 1.2)
                dragX = e.x; dragY = e.y; repaint()
            }
            override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
                zoom = (zoom * if (e.wheelRotation < 0) 1.1 else 0.9).coerceIn(0.45, 2.4)
                repaint()
            }
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                selected = projectedNodes.minByOrNull { (it.x - e.x) * (it.x - e.x) + (it.y - e.y) * (it.y - e.y) }
                    ?.takeIf { (it.x - e.x) * (it.x - e.x) + (it.y - e.y) * (it.y - e.y) < 225 }
                repaint()
            }
        }
        addMouseListener(mouse); addMouseMotionListener(mouse); addMouseWheelListener(mouse)
    }

    fun tick() { yaw += 0.004; frame++; if (frame % 4 == 0) scan = (scan + 1) % slices; repaint() }
    fun advanceStep() { scan = (scan + 1) % slices; repaint() }
    fun resetView() { scan = 0; yaw = 0.55; pitch = -0.23; zoom = 1.0; frame = 0; selected = null; repaint() }
    private fun rawPoint(r: Int, c: Int, z: Int): RawPoint {
        val wx = (c - 1.5) * 1.45
        val wy = (r - 2.5) * 1.05
        val wz = (z - (slices - 1) / 2.0) * 0.82
        val rx = cos(yaw) * wx + sin(yaw) * wz
        val rz = -sin(yaw) * wx + cos(yaw) * wz
        val ry = cos(pitch) * wy - sin(pitch) * rz
        val depth = sin(pitch) * wy + cos(pitch) * rz
        val perspective = 35.0 / (35.0 + depth).coerceAtLeast(8.0)
        return RawPoint(rx * perspective, ry * perspective, depth, perspective)
    }
    private fun tint(base: Color, alpha: Int) = Color(base.red, base.green, base.blue, alpha.coerceIn(0, 255))
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = (raw.create() as Graphics2D)
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val rawNodes = Array(slices) { z -> Array(6) { r -> Array(4) { c -> rawPoint(r, c, z) } } }
            val rawList = rawNodes.flatMap { plane -> plane.flatMap { it.asList() } }
            val minX = rawList.minOf { it.x }; val maxX = rawList.maxOf { it.x }
            val minY = rawList.minOf { it.y }; val maxY = rawList.maxOf { it.y }
            val scale = min((width - 70.0) / (maxX - minX), (height - 225.0) / (maxY - minY)) * zoom
            val centerY = (110.0 + height - 115.0) / 2.0
            val nodes = Array(slices) { z -> Array(6) { r -> Array(4) { c ->
                val p = rawNodes[z][r][c]
                Projected(r, c, z, (width / 2.0 + (p.x - (minX + maxX) / 2.0) * scale).toInt(),
                    (centerY + (p.y - (minY + maxY) / 2.0) * scale).toInt(), p.depth, p.size)
            } } }
            projectedNodes = nodes.flatMap { plane -> plane.flatMap { it.asList() } }

            // The dim grid shows the whole stack; the bright plane is the
            // byte column being inspected. Every node is G[row,column].
            g.stroke = BasicStroke(1f)
            for (z in 0 until slices) for (r in 0..5) for (c in 0..3) {
                val p = nodes[z][r][c]
                val base = if (r < 4) green else amber
                val alpha = if (z == scan) 180 else 28
                g.color = tint(base, alpha)
                if (c < 3) { val q = nodes[z][r][c + 1]; g.drawLine(p.x, p.y, q.x, q.y) }
                if (r < 5) { val q = nodes[z][r + 1][c]; g.drawLine(p.x, p.y, q.x, q.y) }
                if (z < slices - 1) { val q = nodes[z + 1][r][c]; g.drawLine(p.x, p.y, q.x, q.y) }
            }
            for (p in projectedNodes.sortedByDescending { it.depth }) {
                val active = p.slice == scan
                val base = if (p.row < 4) green else amber
                val radius = ((if (active) 5.0 else 2.5) * p.size).toInt().coerceIn(2, 11)
                g.color = tint(base, if (active) 255 else 88)
                g.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2)
                if (active) { g.color = tint(ink, 210); g.drawOval(p.x - radius - 2, p.y - radius - 2, radius * 2 + 4, radius * 2 + 4) }
            }
            g.font = Font(Font.SANS_SERIF, Font.BOLD, if(compact)16 else 19); g.color = ink
            g.drawString(if(compact)"6×4 GENERATOR STACK" else "VANDERMONDE → SYSTEMATIC GENERATOR", 22, 32)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, if(compact)12 else 14); g.color = dim
            g.drawString(if(compact)"G = V · V₀⁻¹;  c[t] = Gd[t]" else "6 output rows × 4 input columns × 40 byte positions", 22, 56)
            if(!compact)g.drawString("G = V · V₀⁻¹     c[t] = G · d[t]", 22, 79)
            g.color = green; g.drawString("● data rows 0–3", 22, height - 88)
            g.color = if(compact && bridgeStage >= 7)cyan else amber
            g.drawString(if(compact && bridgeStage >= 7)"rows 0,2,3,5 → recover d; check hash" else "● parity rows 4–5", 22, height - 66)
            g.color = dim
            if(compact && bridgeStage >= 5) {
                val output = Gf256.generator.joinToString(",") { row -> Gf256.multiply(row[0],4).toString(16).uppercase().padStart(2,'0') }
                g.drawString(if(bridgeStage >= 6)"d=[04,00,00,00] → c=[$output]" else "d=[04,00,00,00] → apply G", 22, height - 43)
            }
            if(!compact)g.drawString("Depth repeats the 6×4 grid across byte positions; it is not a third matrix axis.", 22, height - 43)
            g.drawString(if(compact)"Layer ${scan + 1}/$slices · drag / zoom / click" else "Drag: rotate    Wheel: zoom    Click: inspect    Active byte position: ${scan + 1}/$slices", 22, height - 20)
            selected?.let { p ->
                val value = Gf256.generator[p.row][p.column]
                val text = "G[${p.row},${p.column}] = 0x${value.toString(16).uppercase().padStart(2, '0')}   ·   byte position ${p.slice + 1}"
                g.font = Font(Font.MONOSPACED, Font.BOLD, 14)
                val boxWidth = g.fontMetrics.stringWidth(text) + 24
                g.color = surface; g.fillRoundRect(16, 95, boxWidth, 36, 12, 12)
                g.color = borderColor; g.drawRoundRect(16, 95, boxWidth, 36, 12, 12)
                g.color = ink; g.drawString(text, 28, 119)
            }
        } finally { g.dispose() }
    }
}

internal enum class LessonKind { FORMULA, MATRIX, MERKLE, REDTAIL, BRIDGE, FILEMAP, REEDSOLOMON, GFHAND, ROWREDUCE, ENTROPY }

// Staged paper diagrams share one player. The Merkle owner map belongs to
// BRMR; RedTail-X uses share and stripe hashes, not this Merkle owner root.
private class Lesson2DCanvas(private val kind: LessonKind) : JPanel() {
    var stage = 0; private set
    init { background = canvas; preferredSize = Dimension(980, 610) }
    fun next(): Boolean { if (stage < 6) stage++; repaint(); return stage < 6 }
    fun reset() { stage = 0; repaint() }
    private fun Graphics2D.text(s: String, x: Int, y: Int, color: Color = ink, size: Int = 16, bold: Boolean = false) {
        this.color = color; font = Font(Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, size); drawString(s, x, y)
    }
    private fun Graphics2D.box(x: Int, y: Int, w: Int, h: Int, title: String, detail: String, accent: Color, bright: Boolean = true) {
        color = surface; fillRoundRect(x, y, w, h, 16, 16)
        color = if (bright) accent else borderColor; stroke = BasicStroke(if (bright) 2.5f else 1f); drawRoundRect(x, y, w, h, 16, 16)
        text(title, x + 12, y + 27, if (bright) accent else dim, 15, true)
        text(detail, x + 12, y + 53, ink, 13)
    }
    private fun Graphics2D.link(x1: Int, y1: Int, x2: Int, y2: Int, accent: Color) {
        color = accent; stroke = BasicStroke(2.3f); drawLine(x1, y1, x2, y2)
        val a = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        drawLine(x2, y2, (x2 - 9 * cos(a - 0.55)).toInt(), (y2 - 9 * sin(a - 0.55)).toInt())
        drawLine(x2, y2, (x2 - 9 * cos(a + 0.55)).toInt(), (y2 - 9 * sin(a + 0.55)).toInt())
    }
    override fun paintComponent(raw: Graphics) {
        super.paintComponent(raw)
        val g = raw.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.scale(width / 980.0, height / 610.0)
            when (kind) {
                LessonKind.MATRIX -> drawMatrix(g)
                LessonKind.MERKLE -> drawMerkle(g)
                LessonKind.REDTAIL -> drawRedTail(g)
                LessonKind.FILEMAP -> drawFileMap(g)
                LessonKind.REEDSOLOMON -> drawReedSolomon(g)
                LessonKind.BRIDGE -> Unit
                else -> Unit
            }
            g.text("Stage $stage/6  •  Next reveals one claim; Play walks the same steps.", 24, 593, dim, 13)
        } finally { g.dispose() }
    }
    private fun drawMatrix(g: Graphics2D) = with(g) {
        text("VANDERMONDE → 4+2 GENERATOR", 24, 34, ink, 21, true)
        text("One byte column t: d[t] ∈ GF(2⁸)⁴ → c[t] ∈ GF(2⁸)⁶", 24, 61, dim, 15)
        for (r in 0..3) box(35, 125 + r * 70, 155, 57, "d$r[t]", "input byte", green, stage >= 1)
        if (stage >= 2) {
            box(335, 115, 280, 324, "G = V · V₀⁻¹", "6 rows × 4 columns", cyan)
            for (r in 0..5) {
                val values = Gf256.generator[r].joinToString("  ") { it.toString(16).uppercase().padStart(2, '0') }
                text("${if (r < 4) "D" else "P"}${if (r < 4) r else r - 4}   $values", 360, 193 + r * 39, if (r < 4) green else amber, 15, true)
            }
            link(190, 265, 335, 265, cyan)
        }
        if (stage >= 3) { link(615, 265, 725, 265, cyan); text("c = Gd", 632, 245, ink, 15, true) }
        if (stage >= 3) for (r in 0..5) box(725, 109 + r * 62, 195, 54,
            "c$r[t]", if (r < 4) "systematic data" else "parity byte", if (r < 4) green else amber, r < 4 || stage >= 4)
        if (stage >= 5) {
            box(48, 485, 385, 72, "ERASURE HUNT", "Choose any 4 surviving output rows.", red)
            box(520, 485, 407, 72, "INVERSE", "Solve d = Gsurvivors⁻¹ · csurvivors", green, stage >= 6)
            link(433, 521, 520, 521, green)
        }
        if (stage == 0) text("Begin with the four input nodes. The formula hunt F(i) is a separate exercise.", 35, 485, dim, 15)
    }
    private fun drawMerkle(g: Graphics2D) = with(g) {
        text("BRMR OWNER MAP → MERKLE ROOT", 24, 34, ink, 21, true)
        text("A commitment detects later owner-map changes; the reverse-edge witness proposes repair.", 24, 61, dim, 15)
        text("leaf(c,p)=H(00||len(c)||c||len(p)||p);  node=H(01||left||right)", 24, 84, dim, 13)
        val leaves = arrayOf("(child A, parent P)", "(child B, parent P)", "(child C, parent Q)", "(child D, parent Q)")
        for (i in 0..3) box(35 + i * 235, 438, 205, 72, "owner row $i", leaves[i], green, stage >= 1)
        if (stage >= 2) for (i in 0..3) {
            val x = 35 + i * 235
            box(x + 32, 338, 142, 57, "leaf $i", "H(00 || row)", cyan)
            link(x + 103, 438, x + 103, 395, cyan)
        }
        if (stage >= 3) for (i in 0..1) {
            val x = 153 + i * 470
            box(x, 225, 200, 62, "parent hash $i", "H(01 || left || right)", amber)
            link(138 + i * 470, 338, x + 42, 287, amber)
            link(373 + i * 470, 338, x + 158, 287, amber)
        }
        if (stage >= 4) {
            box(390, 108, 200, 68, "OWNER ROOT", "committed at ingest", green)
            link(253, 225, 442, 176, green); link(723, 225, 538, 176, green)
        }
        if (stage >= 5) {
            text("If owner row 1 changes later, its leaf no longer recomputes this root.", 46, 542, red, 15, true)
            text("A proof uses sibling hashes along one leaf-to-root path: O(log n).", 46, 562, ink, 14)
        }
        if (stage >= 6) {
            color = surface; fillRoundRect(49, 515, 890, 54, 12, 12)
            text("REPAIR: |S(o)|=1 proposes one parent; verify evidence; render a separate sidecar view.", 62, 548, green, 15, true)
        }
        if (stage == 0) text("Reveal rows → leaves → parents → root → damage check → separate repair.", 35, 552, dim, 15)
    }
    private fun drawRedTail(g: Graphics2D) = with(g) {
        text("REDTAIL-X → VARIABLE TAIL AND TWO-LOSS REPAIR", 24, 34, ink, 21, true)
        text("Worked 7-byte tail: s = ⌈7/4⌉ = 2 bytes per share; six shares store 12 bytes.", 24, 61, dim, 15)
        box(35, 222, 155, 78, "INPUT TAIL", "7 exact bytes", green, stage >= 1)
        if (stage >= 2) {
            link(190, 260, 260, 260, cyan)
            for (r in 0..3) box(260, 105 + r * 100, 180, 70, "D$r", if (r == 3) "1 byte + 0 pad" else "2 data bytes", green)
        }
        if (stage >= 3) {
            link(440, 260, 520, 260, cyan)
            box(520, 217, 145, 82, "G = [I₄; C]", "c[t] = Gd[t]", cyan)
        }
        if (stage >= 4) {
            link(665, 260, 720, 260, amber)
            for (r in 0..5) box(720, 72 + r * 70, 202, 58, if (r < 4) "share $r  DATA" else "share $r  PARITY",
                "2 bytes + hash", if (r < 4) green else amber, stage < 5 || r != 1 && r != 4)
        }
        if (stage >= 5) {
            text("× share 1 and share 4 lost; 4 verified shares remain.", 35, 523, red, 16, true)
            text("Reject corrupt share hashes before selecting the four survivor rows.", 35, 548, ink, 14)
        }
        if (stage >= 6) {
            color = surface; fillRoundRect(29, 492, 925, 74, 12, 12)
            text("INVERT 4 ROWS → rebuild data → truncate padding to 7 bytes → verify stripe hash.", 42, 529, green, 16, true)
            text("The manifest keeps original_len=7 and share_len=2; a hash detects errors but does not solve for bytes.", 42, 551, dim, 13)
        }
        if (stage == 0) text("The 1 MiB stripe is an implementation size; the tail rule applies to its final partial array.", 35, 528, dim, 15)
    }
    private fun drawFileMap(g: Graphics2D) = with(g) {
        text("FILE SHARES → MERKLE SCOPE (PROPOSED)", 24, 34, ink, 21, true)
        text("Each node commits to a region below it; recovery still needs coded share bytes.", 24, 61, dim, 15)
        box(365, 80, 250, 60, "FILE BYTES", "ordered stripe sequence", green)
        if (stage >= 1) for (s in 0..1) { val x=90+s*540; link(490,140,x+130,170,cyan); box(x,170,260,60,"STRIPE $s","one file region",cyan) }
        if (stage >= 2) for (s in 0..1) { val x=90+s*540; link(x+130,230,x+130,250,amber); box(x,250,260,60,"4 DATA + 2 PARITY","G applies per byte column",amber) }
        if (stage >= 3) for (s in 0..1) { val x=90+s*540; link(x+130,310,x+130,330,cyan); box(x,330,260,60,"SHARE + META LEAVES","hash bytes and coordinates",cyan) }
        if (stage >= 4) for (s in 0..1) { val x=90+s*540; link(x+130,390,x+130,410,green); box(x,410,260,60,"STRIPE ROOT $s","parent hashes over leaves",green) }
        if (stage >= 5) { link(220,470,455,490,green); link(760,470,525,490,green); box(385,490,210,60,"FILE ROOT","H(01 || root0 || root1)",green) }
        if (stage >= 6) text("Four verified shares rebuild bytes; compare a trusted digest. The root alone cannot rebuild.",62,569,green,14,true)
        if (stage == 0) text("Two illustrative stripes; proposed Merkle layer over RedTail-X share records.",70,530,dim,15)
    }
    private fun drawReedSolomon(g: Graphics2D) = with(g) {
        text("REED–SOLOMON 4+2: MATRIX, ERASURES, RECOVERY",24,34,ink,21,true)
        text("GF(2⁸), one byte column. Four data symbols make six coded symbols.",24,60,dim,15)
        text("Your λ₁ λ₂ λ₃ λ₄: proof statements reserved; definitions needed. (BRMR's λ(o) is different: the owner-line index.)",24,86,dim,12)
        for(r in 0..3) box(30,125+r*83,155,58,"d$r","data byte",green,stage>=1)
        if(stage>=2){link(185,270,250,270,cyan);box(250,150,260,205,"V → G","G = V · Vtop⁻¹",cyan); for(r in 0..3)text("row $r: ${Gf256.generator[r].joinToString(" ") { it.toString(16).padStart(2,'0') }}",267,226+r*31,ink,15)}
        if(stage>=3){link(510,260,595,260,cyan);text("c = Gd",522,236,ink,15,true);for(r in 0..5)box(605,82+r*66,215,54,"c$r",if(r<4)"data share" else "parity share",if(r<4)green else amber)}
        if(stage>=4){box(31,480,330,67,"ERASE 1 AND 4","absence / hash mismatch",red)}
        if(stage>=5){box(381,480,510,67,"SELECT 0, 2, 3, 5","four surviving generator rows",cyan)}
        if(stage>=6){color=surface;fillRoundRect(31,551,910,32,8,8);text("Invert selected 4×4 rows → recover d₀…d₃; verify reconstructed stripe hash.",43,573,green,14,true)}
        if(stage==0)text("Reveal input, generator, codeword, loss, survivors, and recovery.",35,536,dim,15)
    }
}

private fun label(text:String)=JLabel(text).apply{foreground=ink;font=Font(Font.SANS_SERIF,Font.BOLD,13)}
private fun field(value:String)=JTextField(value,5).apply{background=surface;foreground=ink;caretColor=ink;border=BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor),EmptyBorder(6,8,6,8))}

// One entry per Hunt Map slide. Each slide owns a 2D player and a 3D player,
// so the shared controls look up the current entry instead of branching on
// every lesson kind. Adding a slide means adding one entry to buildSlides().
private class Slide(
    val kind: LessonKind, val title: String, val intro: String, val group: String,
    val flat: JComponent, val deep: JComponent,
    val next2d: () -> Boolean, val reset2d: () -> Unit, val status2d: () -> String,
    val next3d: () -> Unit, val reset3d: () -> Unit, val tick3d: () -> Unit,
    val retheme: () -> Unit = { flat.background = canvas; deep.background = canvas }
)

private class App : JFrame("VQTD — Visual Quasi-Thermodynamics") {
    private val model = Model()
    private val status = JLabel("Illustrative F(i) exercise; the paper excluded shortest-form storage.")
    private val learningProgress = LearningProgress()
    private val slides: List<Slide> = buildSlides()
    private var threeDMode = false
    private var lesson = LessonKind.FORMULA
    private val current: Slide get() = slides.first { it.kind == lesson }
    private val timer = Timer(850) { advanceCurrent() }
    private val spatialTimer = Timer(50) { current.tick3d() }
    private var selectedTab = 0
    private val binary3d = Spatial3DCanvas(binaryScene(model.candidates))

    init { defaultCloseOperation = EXIT_ON_CLOSE; minimumSize = Dimension(1100, 760); rebuild(); pack(); setLocationRelativeTo(null) }

    private fun buildSlides(): List<Slide> {
        val papers = "PAPERS"; val math = "MATH FOUNDATIONS"
        fun staged(kind: LessonKind, title: String, intro: String, steps: String): Slide {
            val flat = Lesson2DCanvas(kind); val deep = Spatial3DCanvas(kind)
            return Slide(kind, title, intro, papers, flat, deep, flat::next, flat::reset, { "$title stage ${flat.stage}/6: $steps" },
                { deep.advanceStep() }, deep::resetView, deep::tick)
        }
        fun foundation(kind: LessonKind, title: String, intro: String, steps: String): Slide {
            val flat = MathLesson2DCanvas(kind); val deep = Spatial3DCanvas(kind)
            return Slide(kind, title, intro, math, flat, deep, flat::next, flat::reset, { "$title stage ${flat.stage}/${flat.lastStage}: $steps" },
                { deep.advanceStep() }, deep::resetView, deep::tick)
        }
        val formula2d = HuntCanvas(model); val formula3d = Spatial3DCanvas(LessonKind.FORMULA)
        val matrix2d = Lesson2DCanvas(LessonKind.MATRIX); val matrix3d = Matrix3DCanvas()
        val bridge2d = LayerBridgePanel(false); val bridge3d = LayerBridgePanel(true)
        return listOf(
            Slide(LessonKind.FORMULA, "F(i) Hunt", "Class exercise: compare F(0), F(1), F(2).", papers, formula2d, formula3d,
                { if (model.stage < 6) model.stage++; model.stage < 6 }, { model.stage = 0 }, { "Formula hunt stage ${model.stage}/6. Classroom exercise only." },
                { formula3d.advanceStep() }, formula3d::resetView, formula3d::tick),
            Slide(LessonKind.MATRIX, "Matrix", "Map four data rows to six coded rows.", papers, matrix2d, matrix3d,
                matrix2d::next, matrix2d::reset, { "Matrix 2D stage ${matrix2d.stage}/6: data, generator, parity, survivors, inverse." },
                matrix3d::advanceStep, matrix3d::resetView, matrix3d::tick),
            staged(LessonKind.MERKLE, "Merkle Roots", "BRMR: trace owner rows to a committed Merkle root.", "commitment checks; mirror proposes repair."),
            staged(LessonKind.REDTAIL, "RedTail-X", "RedTail-X: follow variable tail coding and two-loss repair.", "code, survive, reconstruct, verify."),
            Slide(LessonKind.BRIDGE, "Layer Bridge", "Formula result 4 becomes one example data byte in a matrix layer.", papers, bridge2d, bridge3d,
                bridge2d::advanceStep, bridge2d::resetView, { "Layer Bridge stage ${bridge2d.stage}/7: RedTail-X hunt → class formula → matrix byte." },
                { bridge3d.advanceStep() }, bridge3d::resetView, bridge3d::tick, retheme = { bridge2d.refreshTheme(); bridge3d.refreshTheme() }),
            staged(LessonKind.FILEMAP, "File Scope", "Proposed file-share Merkle scope: root commits; shares rebuild.", "coded shares → commitments → verified rebuild."),
            staged(LessonKind.REEDSOLOMON, "Reed–Solomon", "Reed–Solomon: four symbols, six shares, any four recover.", "generator → losses → inverse."),
            foundation(LessonKind.GFHAND, "GF(2⁸) by Hand", "Why 52·04 = 55: shift, overflow, XOR with 0x11d.", "shift → overflow → reduce."),
            foundation(LessonKind.ROWREDUCE, "Row Reduction", "Invert survivor rows 0, 2, 3, 5 by Gauss–Jordan, then recover d.", "one pivot column per step."),
            foundation(LessonKind.ENTROPY, "Entropy H₈", "Three byte samples, the 6.4 gate, and the thermodynamics link.", "samples → gate → physics → boundary.")
        )
    }

    private fun rebuild(selected: Int = 0) {
        contentPane.removeAll(); contentPane.layout = BorderLayout(); contentPane.background = canvas
        slides.forEach { it.retheme() }
        binary3d.background = canvas
        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply { background = panel; border = EmptyBorder(7, 12, 7, 12) }
        bar.add(label("CLASSROOM THEME"))
        fun roomButton(title: String, action: () -> Unit) = JButton(title).apply { background = buttonColor; foreground = ink; isFocusPainted = false; addActionListener { action() } }
        bar.add(roomButton("Beige") { RoomStyle.theme = RoomTheme.BEIGE; rebuild(selectedTab) })
        bar.add(roomButton("Dark Green") { RoomStyle.theme = RoomTheme.DARK_GREEN; rebuild(selectedTab) })
        bar.add(roomButton(if (RoomStyle.highContrast) "High Contrast: ON" else "High Contrast: OFF") { RoomStyle.highContrast = !RoomStyle.highContrast; rebuild(selectedTab) })
        contentPane.add(bar, BorderLayout.NORTH)
        val tabs = JTabbedPane().apply {
            background = panel; foreground = ink
            addTab("Hunt Map", huntTab())
            addTab("Learning Lab", LearningLabPanel(learningProgress, LabColors(canvas, panel, surface, ink, dim, green, red, borderColor)))
            addTab("Paper Math", paperMathTab()); addTab("Binary Lens", binaryTab()); addTab("Glossary & Symbols", glossaryTab())
            addTab("Lecture Board", lectureTab()); addTab("Code Bridge", codeTab()); addTab("Paper Notes", notesTab())
        }
        tabs.selectedIndex = selected.coerceIn(0, tabs.tabCount - 1)
        tabs.addChangeListener { selectedTab = tabs.selectedIndex }
        contentPane.add(tabs, BorderLayout.CENTER)
        contentPane.revalidate(); contentPane.repaint()
    }

    private fun huntTab(): JPanel {
        val cards = JPanel(CardLayout()).apply { background = canvas; slides.forEach { add(it.flat, "${it.kind}2d"); add(it.deep, "${it.kind}3d") } }
        fun showCard() = (cards.layout as CardLayout).show(cards, "$lesson${if (threeDMode) "3d" else "2d"}")
        showCard()
        val p = JPanel(BorderLayout()).apply { background = canvas; border = EmptyBorder(10, 10, 10, 10); add(cards, BorderLayout.CENTER) }
        fun button(t: String, action: () -> Unit) = JButton(t).apply { isFocusPainted = false; background = buttonColor; foreground = ink; addActionListener { action() } }
        val lessonButtons = mutableMapOf<LessonKind, JButton>()
        fun select(which: LessonKind) {
            timer.stop(); spatialTimer.stop(); lesson = which; showCard()
            lessonButtons.forEach { (key, b) -> b.border = BorderFactory.createLineBorder(if (key == which) green else borderColor, if (key == which) 2 else 1) }
            refresh(current.intro)
        }
        val slideRows = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); background = panel; border = EmptyBorder(3, 10, 3, 10) }
        for ((group, members) in slides.groupBy { it.group }) {
            val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 3)).apply { background = panel; add(label(group.padEnd(18))) }
            members.forEach { s -> lessonButtons[s.kind] = button(s.title) { select(s.kind) }.also(row::add) }
            slideRows.add(row)
        }
        lessonButtons.forEach { (key, b) -> b.border = BorderFactory.createLineBorder(if (key == lesson) green else borderColor, if (key == lesson) 2 else 1) }
        p.add(slideRows, BorderLayout.NORTH)
        val controls = JPanel(FlowLayout(FlowLayout.LEFT)).apply { background = panel; border = EmptyBorder(7, 10, 7, 10) }
        controls.add(button("Reset") { timer.stop(); spatialTimer.stop(); if (threeDMode) current.reset3d() else current.reset2d(); refresh("This slide is reset.") })
        controls.add(button("Next") { if (threeDMode) { current.next3d(); refresh("Advanced one step in this 3D scene.") } else advanceCurrent() })
        controls.add(button("Play") { if (threeDMode) { timer.stop(); spatialTimer.start(); refresh("Playing this 3D scene.") } else { spatialTimer.stop(); timer.start(); refresh("Playing this 2D lesson one claim at a time.") } })
        controls.add(button("Pause") { timer.stop(); spatialTimer.stop(); refresh("Paused.") })
        lateinit var viewButton: JButton
        viewButton = button(if (threeDMode) "2D View" else "3D View") {
            timer.stop(); spatialTimer.stop(); threeDMode = !threeDMode
            showCard()
            viewButton.text = if (threeDMode) "2D View" else "3D View"
            refresh("${if (threeDMode) "3D" else "2D"} view: ${current.title}.")
        }
        controls.add(viewButton)
        status.foreground = amber; controls.add(status); p.add(controls, BorderLayout.SOUTH); return p
    }
    private fun advanceCurrent() { if (!current.next2d()) timer.stop(); refresh(current.status2d()) }
    private fun refresh(s: String) { status.text = s; slides.forEach { it.flat.repaint(); it.deep.repaint() } }
    private fun paperMathTab():JPanel {
        val listModel=DefaultListModel<LessonFormula>()
        fun load(query:String="") {
            listModel.clear()
            paperFormulas.filter { query.isBlank() || listOf(it.paper,it.page,it.topic,it.expression,it.classroom,it.status).any { value -> value.contains(query,true) } }
                .forEach(listModel::addElement)
        }
        load()
        val detail=JTextArea().apply{isEditable=false;lineWrap=true;wrapStyleWord=true;background=surface;foreground=ink;font=Font(Font.SANS_SERIF,Font.PLAIN,17);border=EmptyBorder(22,24,22,24)}
        val source=JLabel("Select an equation or measured relationship.").apply{foreground=dim;border=EmptyBorder(8,12,8,12)}
        val list=JList(listModel).apply{
            background=surface;foreground=ink;selectionBackground=green;selectionForeground=if(RoomStyle.theme==RoomTheme.BEIGE)Color.WHITE else Color.BLACK
            font=Font(Font.SANS_SERIF,Font.PLAIN,15);fixedCellHeight=54
            cellRenderer=object:DefaultListCellRenderer(){override fun getListCellRendererComponent(l:JList<*>?,v:Any?,i:Int,selected:Boolean,focus:Boolean):Component{
                val e=v as LessonFormula
                return super.getListCellRendererComponent(l,"${e.paper}  ·  ${e.topic}",i,selected,focus).apply{font=Font(Font.SANS_SERIF,Font.PLAIN,14);border=EmptyBorder(7,10,7,10);background=if(selected)green else surface;foreground=if(selected)if(RoomStyle.theme==RoomTheme.BEIGE)Color.WHITE else Color.BLACK else ink}
            }}
            addListSelectionListener{if(!it.valueIsAdjusting){val e=selectedValue?:return@addListSelectionListener
                source.text="${e.paper}  |  printed page ${e.page}  |  ${e.status}"
                detail.text="${e.topic}\n\n${e.expression}\n\nTEACH IT\n${e.classroom}\n\nCODE ASSOCIATION\n${e.codeBridge}\n\nSOURCE\nPapers/${when(e.paper){"RedTail-X v1.1"->"RedTail-X_Technical_Report_v1.1.pdf";"DarkRock CAD v1"->"DarkRock_CAD_Technical_Report_v1.pdf";else->"BRMR-Field-Level-Repair-of-Converter-Lost-Owner-Handles-in-DXF.pdf"}}\nPrinted page ${e.page}"
                detail.caretPosition=0
            }}
        }
        val search=field("").apply{columns=26}
        val top=JPanel(FlowLayout(FlowLayout.LEFT)).apply{background=panel;border=EmptyBorder(7,10,7,10);add(label("SOURCE FORMULAS"));add(search);add(JButton("Filter").apply{background=buttonColor;foreground=ink;addActionListener{load(search.text);if(listModel.size()>0)list.selectedIndex=0}});add(JButton("All").apply{background=buttonColor;foreground=ink;addActionListener{search.text="";load();list.selectedIndex=0}})}
        search.addActionListener{load(search.text);if(listModel.size()>0)list.selectedIndex=0}
        val split=JSplitPane(JSplitPane.HORIZONTAL_SPLIT,JScrollPane(list),JScrollPane(detail)).apply{resizeWeight=.38;dividerLocation=390;border=null}
        val p=JPanel(BorderLayout()).apply{background=canvas;add(top,BorderLayout.NORTH);add(split,BorderLayout.CENTER);add(source,BorderLayout.SOUTH)}
        list.selectedIndex=0
        return p
    }
    private fun binaryTab(): JPanel {
        val flat = JPanel(); flat.layout = BoxLayout(flat, BoxLayout.Y_AXIS); flat.background = canvas; flat.border = EmptyBorder(25, 30, 25, 30)
        flat.add(label("CLASS EXERCISE — the RedTail-X report excluded shortest-form storage")); flat.add(Box.createVerticalStrut(18))
        model.candidates.forEach { c -> flat.add(JPanel(GridLayout(2, 1)).apply { background = panel; border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(if (c == model.winner) green else borderColor, 2), EmptyBorder(12, 15, 12, 15)); add(label("${c.formula()}     ${c.bits} bits")); add(JLabel(c.encoding()).apply { foreground = if (c == model.winner) green else dim; font = Font(Font.MONOSPACED, Font.PLAIN, 16) }) }); flat.add(Box.createVerticalStrut(12)) }
        flat.add(JTextArea("Illustrative rule:\n  2 opcode bits + minimal unsigned width(left) + minimal unsigned width(right)\n\nShortest valid candidate under this rule:\n  F(0) = ADD 2 2 = 6 bits\n\nClass tie rule: smaller F index wins.\n\nRedTail-X v1.1 §6 tested and excluded a shortest-form representation.\nThese particular F(i) examples and bit costs are supplied for teaching;\nthey are not a published wire format or active MRT-X storage rule.").apply { isEditable = false; background = canvas; foreground = ink; font = Font(Font.MONOSPACED, Font.PLAIN, 15) })
        // 2D / 3D toggle, matching the Hunt Map controls.
        val cards = JPanel(CardLayout()).apply { background = canvas; add(flat, "2d"); add(binary3d, "3d") }
        val spin = Timer(50) { binary3d.tick() }
        fun button(t: String, action: () -> Unit) = JButton(t).apply { isFocusPainted = false; background = buttonColor; foreground = ink; addActionListener { action() } }
        val controls = JPanel(FlowLayout(FlowLayout.LEFT)).apply { background = panel; border = EmptyBorder(7, 10, 7, 10) }
        lateinit var view: JButton
        var deep = false
        view = button("3D View") { deep = !deep; spin.stop(); (cards.layout as CardLayout).show(cards, if (deep) "3d" else "2d"); view.text = if (deep) "2D View" else "3D View" }
        controls.add(view)
        controls.add(button("Reset") { spin.stop(); binary3d.resetView() })
        controls.add(button("Next") { binary3d.advanceStep() })
        controls.add(button("Play") { spin.start() })
        controls.add(button("Pause") { spin.stop() })
        return JPanel(BorderLayout()).apply { background = canvas; add(cards, BorderLayout.CENTER); add(controls, BorderLayout.SOUTH) }
    }
    private fun codeTab(): JPanel {
        val text = """// Classroom shortest-form experiment; excluded from RedTail-X production
val valid = candidates.filter { it.result == target }
val selected = valid.minWith(compareBy({ it.encodedBits }, { it.index }))

// Actual RedTail-X layout (MRT-X src/storage.rs; cpp/mrtx.cpp)
shareLen = ceil(originalLen / 4)
encoded = reedSolomon4plus2(dataShares, shareLen)
assert(reconstruct(anyFour(encoded)) == originalBytes)

// Separate BRMR owner-repair layer (paper §4)
parents = reverseIndex[childHandle]
answer = parents.singleOrNull() // null = abstain (⊥)
leaf = H(0x00 || len(child) || child || len(parent) || parent)
assert(verify(ownerRoot, leaf, proof))

// Preserve stored converter bytes; render accepted owner changes as a view.
assert(restore(storedBytes) == storedBytes)

// ---- RUNNABLE IN THIS APP: VQTD's own GF(2⁸) multiply (src/Main.kt) ----
fun multiply(a: Int, b: Int): Int {
    var x = a; var y = b; var result = 0
    repeat(8) {
        if (y and 1 != 0) result = result xor x   // add (XOR) this power of a
        x = x shl 1                                // a · x
        if (x and 0x100 != 0) x = x xor 0x11d      // reduce: x⁸ = x⁴+x³+x²+1
        y = y ushr 1                               // next bit of b
    }
    return result
}
"""
        val code = JTextArea(text).apply { isEditable = false; background = surface; foreground = green; font = Font(Font.MONOSPACED, Font.PLAIN, 14); border = EmptyBorder(16, 18, 16, 18); caretPosition = 0 }
        val output = JTextArea("Enter two bytes in hex and press Multiply.\nThe trace below is produced by running the code on the left.").apply { isEditable = false; background = surface; foreground = ink; font = Font(Font.MONOSPACED, Font.PLAIN, 14); border = EmptyBorder(16, 18, 16, 18) }
        val a = field("52"); val b = field("04")
        fun parse(f: JTextField): Int? = f.text.trim().removePrefix("0x").toIntOrNull(16)?.takeIf { it in 0..255 }
        fun run(action: (Int, Int) -> String) { val x = parse(a); val y = parse(b); output.text = if (x == null || y == null) "Use two hex bytes 00–FF, e.g. 52 and 04." else action(x, y); output.caretPosition = 0 }
        fun button(t: String, action: () -> Unit) = JButton(t).apply { isFocusPainted = false; background = buttonColor; foreground = ink; addActionListener { action() } }
        val calc = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = panel; border = EmptyBorder(7, 10, 7, 10)
            add(label("GF(2⁸) CALCULATOR   a =")); add(a); add(label("b =")); add(b)
            add(button("Multiply") { run { x, y -> GfTrace.multiplyTrace(x, y).second.joinToString("\n") } })
            add(button("Add (XOR)") { run { x, y -> "a ⊕ b, bit by bit, no carries:\n\n  ${GfTrace.bitString(x)}   (0x${hex(x)})\n⊕ ${GfTrace.bitString(y)}   (0x${hex(y)})\n= ${GfTrace.bitString(x xor y)}   (0x${hex(x xor y)})\n\nSubtraction is the same operation: a ⊕ b ⊕ b = a." } })
            add(button("Inverse of a") { run { x, _ -> if (x == 0) "0 has no inverse: 0 · anything = 0." else { val inv = GfTrace.inverse(x); "a⁻¹ = a²⁵⁴ (every nonzero a has a²⁵⁵ = 1)\n\n0x${hex(x)}⁻¹ = 0x${hex(inv)}\ncheck: 0x${hex(x)} · 0x${hex(inv)} = 0x${hex(Gf256.multiply(x, inv))}" } } })
            add(button("Try 52·04") { a.text = "52"; b.text = "04"; run { x, y -> GfTrace.multiplyTrace(x, y).second.joinToString("\n") } })
            add(button("Try F7·04") { a.text = "F7"; b.text = "04"; run { x, y -> GfTrace.multiplyTrace(x, y).second.joinToString("\n") } })
        }
        a.addActionListener { run { x, y -> GfTrace.multiplyTrace(x, y).second.joinToString("\n") } }; b.addActionListener { run { x, y -> GfTrace.multiplyTrace(x, y).second.joinToString("\n") } }
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(code), JScrollPane(output)).apply { resizeWeight = 0.5; dividerLocation = 560; border = null }
        return JPanel(BorderLayout()).apply { background = canvas; border = EmptyBorder(10, 10, 10, 10); add(calc, BorderLayout.NORTH); add(split, BorderLayout.CENTER) }
    }
    private fun glossaryTab():JPanel {
        val headers=arrayOf("Symbol", "Professor's term", "Formal meaning", "Plain-language bridge", "Code association")
        val tableModel=object:DefaultTableModel(headers,0){override fun isCellEditable(r:Int,c:Int)=false}
        fun load(filter:String="") {
            tableModel.rowCount=0
            glossary.filter { filter.isBlank() || listOf(it.symbol,it.name,it.formal,it.plain,it.code).any { value -> value.contains(filter,true) } }
                .forEach { tableModel.addRow(arrayOf(it.symbol,it.name,it.formal,it.plain,it.code)) }
        }
        load()
        val table=JTable(tableModel).apply {
            background=surface;foreground=ink;gridColor=borderColor;selectionBackground=green;selectionForeground=if(RoomStyle.theme==RoomTheme.BEIGE)Color.WHITE else Color.BLACK
            font=Font(Font.SANS_SERIF,Font.PLAIN,13);rowHeight=62;autoResizeMode=JTable.AUTO_RESIZE_OFF
            columnModel.getColumn(0).preferredWidth=75;columnModel.getColumn(1).preferredWidth=185;columnModel.getColumn(2).preferredWidth=390;columnModel.getColumn(3).preferredWidth=320;columnModel.getColumn(4).preferredWidth=340
            tableHeader.background=buttonColor;tableHeader.foreground=ink;tableHeader.font=Font(Font.SANS_SERIF,Font.BOLD,13)
            val wrap=object:DefaultTableCellRenderer(){override fun getTableCellRendererComponent(t:JTable,v:Any?,selected:Boolean,focus:Boolean,row:Int,col:Int):Component{val area=JTextArea(v?.toString()?:"");area.lineWrap=true;area.wrapStyleWord=true;area.margin=Insets(6,7,6,7);area.font=if(col==0)Font(Font.SERIF,Font.BOLD,18) else Font(Font.SANS_SERIF,Font.PLAIN,13);area.background=if(selected)t.selectionBackground else t.background;area.foreground=if(selected)t.selectionForeground else if(col==0)amber else t.foreground;return area}}
            for(i in 0 until columnCount)columnModel.getColumn(i).cellRenderer=wrap
        }
        val search=field("").apply { columns=30 }
        val searchButton=JButton("Filter").apply{background=buttonColor;foreground=ink;addActionListener{load(search.text)}}
        search.addActionListener{load(search.text)}
        val top=JPanel(FlowLayout(FlowLayout.LEFT)).apply{background=panel;border=EmptyBorder(8,10,8,10);add(label("Search symbols, meaning, or code:"));add(search);add(searchButton);add(JButton("Show all").apply{background=buttonColor;foreground=ink;addActionListener{search.text="";load()}})}
        return JPanel(BorderLayout()).apply{background=canvas;add(top,BorderLayout.NORTH);add(JScrollPane(table).apply{viewport.background=canvas;border=EmptyBorder(10,10,10,10)},BorderLayout.CENTER)}
    }
    private fun lectureTab():JPanel {
        val text="""VQTD — PROFESSOR BOARD / THREE-PAPER COURSE

LEARNING OBJECTIVES

By the end of this lesson, a student should be able to:
1. Explain c=Gd in a systematic 4+2 Reed–Solomon code.
2. Derive s=⌈t/4⌉ and coded tail=6s from a partial stripe.
3. Distinguish compression/shortest-form experiments from the accepted layout.
4. Explain why a geometry key may choose candidates without controlling restore.
5. Use |S(o)|=1 to accept a unique DXF parent and ⊥ to abstain.
6. Explain what BRMR's owner Merkle root can prove after ingest.

PAPER 1 — REDTAIL-X v1.1 (printed pages 2–7)

Four data symbols d∈GF(2⁸)⁴ produce six stored symbols c=Gd.
The first four rows of G are I₄; any four of six rows can recover d.
Nominal coded rate = 6/4 = 1.5.

For a final t-byte tail: s=⌈t/4⌉ and coded bytes=6s.
The decoder checks share_len, rejects bad share hashes, uses four valid
shares, truncates to original_len, and checks the stripe hash.

H₈=-Σp log₂p is a separate compression gate. Section 6 excludes
polynomial canonicalization, N*Tropy, clamped entropy without
renormalization, and shortest-form storage.

PAPER 2 — DARKROCK CAD v1 (printed pages 3–6)

Every storage arm must restore original bytes exactly. The geometry key
suggests matches; raw bytes or verified edits provide exactness.
Protected bytes count payloads, edits, references, manifests, and catalog.
A3's −9.7% development result reversed to +8.1% on held-out data.
C2 delta patching was smallest. Geometry keys helped select a C2 base
by 1.8% on 47 real drawings, without becoming the restore format.

PAPER 3 — BRMR (printed pages 3–10)

E is the stored converter output. A separate X̂ is the repaired view.
D(E) finds invalid owner fields. S(o) lists parents that refer back to o.
R2u answers only when |S(o)|=1; otherwise it returns ⊥.
Strict pairs answer on witness agreement. The ladder escalates when
witnesses disagree. The owner map Merkle root detects changes after
ingest; it cannot correct a converter error already present at ingest.

The stored-byte invariant is STORE=E and RESTORE(STORE)=E.

CLASS EXERCISE — F(i), DISTINCT FROM THE ACCEPTED STORAGE CODE

Target: y = 4

Candidate set C = {F(0), F(1), F(2)}
F(0) = 2 + 2  = 4
F(1) = 14 − 10 = 4
F(2) = 8 − 4  = 4

Validity predicate: valid(F(i)) ⇔ F(i) = y
Selection: i* = arg min { ℓ(F(i)) | F(i) = y }

Under the visible teaching encoding:
ℓ(F(0)) = 6, ℓ(F(1)) = 10, ℓ(F(2)) = 9
Therefore i* = 0 in this invented encoding. The examples are useful to
learn arg min, but RedTail-X §6 reports shortest-form storage as excluded.

THREE LAYERS

RedTail-X: encoded arrays + per-share hashes → recover exact stored bytes.
DarkRock CAD: lookup or delta choice → verify exact file restoration.
BRMR: detect missing owner → unique mirror/witnesses → sidecar view.

ESTABLISHED MATHEMATICS

• arg min returns an argument/index, while min returns the minimum value.
• GF(2⁸) addition is XOR.
• A systematic Reed–Solomon generator keeps original data rows visible.
• A Merkle proof establishes inclusion in a prior commitment.
• A hash or Merkle root detects change; it does not by itself reveal the
  correct missing value.

PAPER-BOUND DISCIPLINE

• The 1 MiB stripe is an implementation size in the measured prototype.
• RedTail-X is a storage layout result, not a new Reed–Solomon code.
• BRMR repairs selected owner fields in a sidecar view; storage keeps E.
• A unique mirror is a measured condition for this corpus, not a DXF law.

NOT DEFINED BY THESE THREE PAPERS

• ALEPH as a project operation
• X carrying an infinity flag
• A normative encoding for the F(0), F(1), F(2) example

MATH FOUNDATIONS (Hunt Map → second row; each has 2D and 3D)

GF(2⁸) by Hand   0x52·04: shift, shift, bit 8 overflows, XOR 0x11d → 0x55.
                 F7·04 overflows twice → F3 → FB. Parity bytes [55, FB].
Row Reduction    [A | I] for survivor rows 0, 2, 3, 5 → [I | A⁻¹];
                 d = A⁻¹·[04, 00, 00, FB] = [04, 00, 00, 00].
Entropy H₈       constant 0.00, text ≈ 4, random ≈ 7.98 bits/byte;
                 gate at 6.4; S = (k_B ln 2)·H links to Gibbs entropy.
Code Bridge      GF(2⁸) calculator runs the same multiply code.

CLASS QUESTIONS

0a. Why does XOR count as addition in GF(2⁸)?
0b. Why must the row-reduction pivot be nonzero, and what fixes a zero?
0c. Why is H₈ a gate for compression but irrelevant to recovery?
1. Why does the tail need original_len if share_len is already stored?
2. Which four rows can solve for d after two erasures?
3. What made A3's development gain misleading?
4. Why is |S(o)|=1 stronger than “the proposed owner exists”?
5. What can an ingest-time owner root detect, and what can it not fix?
6. Why does precision differ from coverage when a rebuilder abstains?
"""
        return JPanel(BorderLayout()).apply{background=canvas;border=EmptyBorder(18,25,18,25);add(JScrollPane(JTextArea(text).apply{isEditable=false;background=canvas;foreground=ink;font=Font(Font.MONOSPACED,Font.PLAIN,15);caretPosition=0}).apply{border=null;viewport.background=canvas},BorderLayout.CENTER)}
    }
    private fun notesTab():JPanel{
        val text="""SOURCE MAP

Papers/RedTail-X_Technical_Report_v1.1.pdf
  RedTail-X: variable final shares; 4+2 layout; entropy gate;
  negative results for shortest-form and polynomial canonicalization.

Papers/DarkRock_CAD_Technical_Report_v1.pdf
  Exact CAD restore, protected-byte ledger, rejected A3 format,
  narrow geometry-key benefit as delta-base selector.

Papers/BRMR-Field-Level-Repair-of-Converter-Lost-Owner-Handles-in-DXF.pdf
  Unique reverse-edge owner recovery, witness ladder, owner-map Merkle
  commitment, and a sidecar repaired view that preserves stored bytes.

CLASSROOM-ONLY EXAMPLE

F(0) = 2 + 2 = 4
F(1) = 14 - 10 = 4
F(2) = 8 - 4 = 4

All paths reach 4. The pictured bit-cost rule is illustrative.
The RedTail-X paper tested and excluded a shortest-form storage rule.

DISTINCT OPERATIONS

Formula hunt   compares descriptions in the class exercise.
RS manifest    records original length and six share hashes.
Merkle proof   checks inclusion against a committed root.
RS inverse     reconstructs data from surviving generator rows.
BRMR mirror    proposes a DXF owner from a reverse edge.
BRMR sidecar   renders a repaired view without editing storage.

OPEN PAPER QUESTIONS

• Exact definition of your future ALEPH and infinity-flag notation
• Whether a revised shortest-form method can beat the measured baseline
• How to authenticate owner commitments against an active adversary
"""
        return JPanel(BorderLayout()).apply{background=canvas;border=EmptyBorder(25,30,25,30);add(JTextArea(text).apply{isEditable=false;lineWrap=true;wrapStyleWord=true;background=canvas;foreground=ink;font=Font(Font.MONOSPACED,Font.PLAIN,16)},BorderLayout.CENTER)}
    }
}

private fun renderMatrixPreview() {
    // The preview path also checks that every four generator rows are usable.
    val g = Gf256.generator
    for (a in 0..2) for (b in a + 1..3) for (c in b + 1..4) for (d in c + 1..5)
        Gf256.invert4(arrayOf(g[a], g[b], g[c], g[d]))
    val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val scene = Matrix3DCanvas().apply { setSize(1280, 800) }
    val graphics = image.createGraphics()
    try { scene.paint(graphics) } finally { graphics.dispose() }
    val output = java.io.File("build/matrix-preview.png")
    output.parentFile.mkdirs()
    javax.imageio.ImageIO.write(image, "png", output)
    println("Rendered ${output.absolutePath}; all 15 four-row survivor sets are invertible.")
}

private fun renderSlidesPreview() {
    for ((name, kind) in listOf("matrix-2d" to LessonKind.MATRIX, "merkle-roots" to LessonKind.MERKLE, "redtail-x" to LessonKind.REDTAIL, "file-scope" to LessonKind.FILEMAP, "reed-solomon" to LessonKind.REEDSOLOMON)) {
        val scene = Lesson2DCanvas(kind).apply { setSize(1280, 800); repeat(6) { next() } }
        val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try { scene.paint(graphics) } finally { graphics.dispose() }
        val output = java.io.File("build/$name-preview.png")
        output.parentFile.mkdirs()
        javax.imageio.ImageIO.write(image, "png", output)
        println("Rendered ${output.absolutePath}")
    }
}

private fun render3DSlidesPreview() {
    for ((name, kind) in listOf("formula" to LessonKind.FORMULA, "merkle-roots" to LessonKind.MERKLE, "redtail-x" to LessonKind.REDTAIL, "file-scope" to LessonKind.FILEMAP, "reed-solomon" to LessonKind.REEDSOLOMON)) {
        val scene = Spatial3DCanvas(kind).apply { setSize(1280, 800); repeat(6) { advanceStep() } }
        val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try { scene.paint(graphics) } finally { graphics.dispose() }
        val output = java.io.File("build/$name-3d-preview.png")
        output.parentFile.mkdirs()
        javax.imageio.ImageIO.write(image, "png", output)
        println("Rendered ${output.absolutePath}")
    }
}

private fun renderLayerBridgePreview() {
    fun layoutTree(component: Component) {
        if (component is Container) { component.doLayout(); component.components.forEach(::layoutTree) }
    }
    for (mode in listOf(false, true)) {
        val scene = LayerBridgePanel(mode).apply { setSize(1280, 800); repeat(7) { advanceStep() } }
        layoutTree(scene)
        val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try { scene.paint(graphics) } finally { graphics.dispose() }
        val output = java.io.File("build/layer-bridge-${if(mode)"3d" else "2d"}-preview.png")
        output.parentFile.mkdirs()
        javax.imageio.ImageIO.write(image, "png", output)
        println("Rendered ${output.absolutePath}")
    }
}

private fun checkLayerBridge() {
    val data = intArrayOf(4, 0, 0, 0)
    val generator = Gf256.generator
    val coded = IntArray(6) { r -> (0..3).fold(0) { value, c -> value xor Gf256.multiply(generator[r][c], data[c]) } }
    require(coded.sliceArray(0..3).contentEquals(data))
    for (a in 0..2) for (b in a + 1..3) for (c in b + 1..4) for (d in c + 1..5) {
        val survivorRows = intArrayOf(a, b, c, d)
        val inverse = Gf256.invert4(Array(4) { generator[survivorRows[it]] })
        val rebuilt = IntArray(4) { r -> (0..3).fold(0) { value, k -> value xor Gf256.multiply(inverse[r][k], coded[survivorRows[k]]) } }
        require(rebuilt.contentEquals(data))
    }
    println("Layer Bridge: 04,00,00,00 → ${coded.joinToString(",") { it.toString(16).uppercase().padStart(2,'0') }}; all 15 survivor sets rebuilt the input.")
}

private fun renderLabPreview() {
    fun layoutTree(component: Component) {
        if (component is Container) {
            component.doLayout()
            component.components.forEach(::layoutTree)
        }
    }
    for ((theme, contrast) in listOf(RoomTheme.BEIGE to false, RoomTheme.DARK_GREEN to false, RoomTheme.DARK_GREEN to true)) {
        RoomStyle.theme = theme; RoomStyle.highContrast = contrast
        val scene = LearningLabPanel(LearningProgress(), LabColors(canvas, panel, surface, ink, dim, green, red, borderColor))
        scene.setSize(1280, 800); layoutTree(scene)
        val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try { scene.paint(graphics) } finally { graphics.dispose() }
        val output = java.io.File("build/learning-lab-${theme.name.lowercase()}${if(contrast)"-high-contrast" else ""}-preview.png")
        output.parentFile.mkdirs()
        javax.imageio.ImageIO.write(image, "png", output)
        println("Rendered ${output.absolutePath}")
    }
    RoomStyle.highContrast = false
}

private fun checkLearningLab() {
    val progress = LearningProgress()
    val lab = LearningLabPanel(progress, LabColors(canvas, panel, surface, ink, dim, green, red, borderColor))
    fun <T : Component> find(type: Class<T>, root: Container = lab, predicate: (T) -> Boolean): T? {
        for (child in root.components) {
            if (type.isInstance(child)) {
                val value = type.cast(child)
                if (predicate(value)) return value
            }
            if (child is Container) find(type, child, predicate)?.let { return it }
        }
        return null
    }
    find(JRadioButton::class.java) { it.text == "Any four" }!!.doClick()
    find(JButton::class.java) { it.text == "Reveal evidence" }!!.doClick()
    require(progress.phase == 1 && progress.attempts.single().correct)
    find(JButton::class.java) { it.text == "Explain this step" }!!.doClick()
    require(progress.phase == 2)
    find(JButton::class.java) { it.text == "Save explanation & next card" }!!.doClick()
    require(progress.studied.contains(0) && progress.cardIndex == 1)
    find(JButton::class.java) { it.text == "Revisit selected" }!!.doClick()
    require(progress.revisit && progress.cardIndex == 0 && progress.phase == 0)
    find(JRadioButton::class.java) { it.text == "Any two" }!!.doClick()
    find(JButton::class.java) { it.text == "Reveal evidence" }!!.doClick()
    require(progress.attempts.size == 2 && !progress.attempts.last().correct && progress.attempts.last().revisit)
    println("Learning Lab predict → reveal → explain → revisit check passed.")
}

private fun writePreview(component: JComponent, name: String) {
    component.setSize(1280, 800)
    val image = java.awt.image.BufferedImage(1280, 800, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try { component.paint(graphics) } finally { graphics.dispose() }
    val output = java.io.File("build/$name-preview.png")
    output.parentFile.mkdirs()
    javax.imageio.ImageIO.write(image, "png", output)
    println("Rendered ${output.absolutePath}")
}

private fun renderMathPreview() {
    for ((name, kind) in listOf("gf-by-hand" to LessonKind.GFHAND, "row-reduction" to LessonKind.ROWREDUCE, "entropy" to LessonKind.ENTROPY)) {
        val flat = MathLesson2DCanvas(kind); repeat(flat.lastStage) { flat.next() }
        writePreview(flat, name)
        val deep = Spatial3DCanvas(kind); repeat(deep.lastStage) { deep.advanceStep() }
        writePreview(deep, "$name-3d")
    }
    val binary = Spatial3DCanvas(binaryScene(Model().candidates)); repeat(binary.lastStage) { binary.advanceStep() }
    writePreview(binary, "binary-lens-3d")
}

// Independent checks for the math-foundation slides. Each require() states
// the property a learner should be able to verify by hand.
private fun checkMath() {
    // 1. GF(2⁸) by hand matches the generator's own multiply.
    require(GfTrace.times4(0x52).last().after == 0x55) { "52·04 should be 55" }
    require(GfTrace.times4(0xF7).last().after == 0xFB) { "F7·04 should be FB" }
    require(GfTrace.times4(0x52).map { it.overflow } == listOf(false, true))
    require(GfTrace.times4(0xF7).map { it.overflow } == listOf(true, true))
    for (a in 0..255) for (b in 0..255) require(GfTrace.multiplyTrace(a, b).first == Gf256.multiply(a, b)) { "trace mismatch $a·$b" }
    for (a in 1..255) require(Gf256.multiply(a, GfTrace.inverse(a)) == 1) { "no inverse for $a" }
    // 2. Row reduction ends in [I | A⁻¹] and recovers d.
    val last = RowReduceTrace.steps.last().matrix
    require((0..3).all { r -> (0..3).all { c -> last[r][c] == if (r == c) 1 else 0 } }) { "left half is not I₄" }
    val reference = Gf256.invert4(Array(4) { Gf256.generator[RowReduceTrace.survivors[it]] })
    require((0..3).all { r -> RowReduceTrace.inverse[r].contentEquals(reference[r]) }) { "inverse differs from Gf256.invert4" }
    require(RowReduceTrace.recovered.contentEquals(RowReduceTrace.data)) { "did not recover d" }
    var mds = 0
    for (a in 0..2) for (b in a + 1..3) for (c in b + 1..4) for (d in c + 1..5) { Gf256.invert4(arrayOf(Gf256.generator[a], Gf256.generator[b], Gf256.generator[c], Gf256.generator[d])); mds++ }
    require(mds == 15)
    // 3. Entropy samples land where the lesson says.
    val (constant, text, random) = EntropyLesson.samples
    require(constant.h8 == 0.0 && constant.tryZstd)
    require(text.h8 in 3.0..5.0 && text.tryZstd) { "text H₈ = ${text.h8}" }
    require(random.h8 in 7.9..8.0 && !random.tryZstd) { "random H₈ = ${random.h8}" }
    // 4. Every 3D scene has one note per stage and only valid edges.
    for (scene in listOf(gfHandScene(), rowReduceScene(), entropyScene(), binaryScene(Model().candidates)) + listOf(LessonKind.FORMULA, LessonKind.MERKLE, LessonKind.REDTAIL, LessonKind.FILEMAP, LessonKind.REEDSOLOMON).map(::sceneFor)) {
        val ids = scene.nodes.map { it.id }.toSet()
        require(ids.size == scene.nodes.size) { "duplicate node id in ${scene.title}" }
        require(scene.edges.all { it.from in ids && it.to in ids }) { "dangling edge in ${scene.title}" }
        val maxStage = scene.nodes.maxOf { it.appears }
        require(maxStage < scene.notes.size) { "${scene.title}: stage $maxStage has no note" }
    }
    println("Math check passed: 52·04=55, F7·04=FB; 65,536 traced products and 255 inverses agree; " +
        "row reduction recovers [${RowReduceTrace.recovered.joinToString(",") { hex(it) }}]; 15/15 survivor sets invertible; " +
        "H₈ = ${EntropyLesson.f2(constant.h8)} / ${EntropyLesson.f2(text.h8)} / ${EntropyLesson.f2(random.h8)}; all 3D scenes consistent.")
}

// UI smoke test and screenshot set: opens the real window (needs a display,
// e.g. xvfb-run), clicks through tabs and slides, and saves what it shows.
// --preview-app writes build/app-*.png; --screenshots writes docs/screenshots.
private fun renderAppPreview(dir: String = "build", gallery: Boolean = false) {
    lateinit var app: JFrame
    fun <T : Component> find(type: Class<T>, root: Container, predicate: (T) -> Boolean): T? {
        for (child in root.components) {
            if (type.isInstance(child) && predicate(type.cast(child))) return type.cast(child)
            if (child is Container) find(type, child, predicate)?.let { return it }
        }
        return null
    }
    fun showing(text: String) = find(JButton::class.java, app.contentPane) { it.text == text && it.isShowing }
    fun click(text: String) = SwingUtilities.invokeAndWait { showing(text)!!.doClick() }
    fun shot(name: String) {
        Thread.sleep(300)
        SwingUtilities.invokeAndWait {
            val pane = app.contentPane as JComponent
            val image = java.awt.image.BufferedImage(pane.width, pane.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics(); try { pane.paint(g) } finally { g.dispose() }
            val output = java.io.File(if (gallery) "$dir/$name.png" else "$dir/app-$name-preview.png"); output.parentFile.mkdirs()
            javax.imageio.ImageIO.write(image, "png", output); println("Rendered ${output.absolutePath}")
        }
    }
    fun tab(title: String) = SwingUtilities.invokeAndWait { val tabs = find(JTabbedPane::class.java, app.contentPane) { true }!!; tabs.selectedIndex = (0 until tabs.tabCount).first { tabs.getTitleAt(it) == title } }
    fun view(threeD: Boolean) { var holder = false; SwingUtilities.invokeAndWait { holder = showing(if (threeD) "3D View" else "2D View") != null }; if (holder) click(if (threeD) "3D View" else "2D View") }
    fun slide(title: String, threeD: Boolean, steps: Int) { tab("Hunt Map"); click(title); view(threeD); click("Reset"); repeat(steps) { click("Next") } }
    SwingUtilities.invokeAndWait { app = App().apply { setSize(1440, 900); isVisible = true } }
    if (!gallery) {
        shot("hunt-map")
        slide("Row Reduction", false, 2); shot("row-reduction-step2")
        slide("Row Reduction", true, 3); shot("row-reduction-3d-step3")
        tab("Binary Lens"); click("3D View"); repeat(6) { click("Next") }; shot("binary-lens")
        tab("Code Bridge"); click("Try F7·04"); shot("code-bridge")
        tab("Glossary & Symbols"); shot("glossary")
    } else {
        slide("Reed–Solomon", false, 6); shot("01-reed-solomon-2d")
        slide("Reed–Solomon", true, 6); shot("02-reed-solomon-3d")
        slide("Layer Bridge", false, 7); shot("03-layer-bridge")
        slide("GF(2⁸) by Hand", false, 6); shot("04-gf-by-hand")
        slide("GF(2⁸) by Hand", true, 6); shot("05-gf-by-hand-3d")
        slide("Row Reduction", false, 2); shot("06-row-reduction")
        slide("Row Reduction", true, 6); shot("07-row-reduction-3d")
        slide("Entropy H₈", false, 6); shot("08-entropy")
        slide("Merkle Roots", true, 6); shot("09-merkle-3d")
        slide("RedTail-X", false, 6); shot("10-redtail-x")
        tab("Learning Lab"); shot("11-learning-lab")
        tab("Paper Math"); shot("12-paper-math")
        tab("Code Bridge"); click("Try 52·04"); shot("13-code-bridge-calculator")
        tab("Glossary & Symbols"); shot("14-glossary")
        click("Dark Green"); slide("Matrix", true, 12); shot("15-matrix-3d-dark-green")
        slide("File Scope", true, 6); shot("16-file-scope-3d-dark-green")
    }
    SwingUtilities.invokeAndWait { app.dispose() }
    System.exit(0)
}

fun main(args:Array<String>){
    when {
        args.contentEquals(arrayOf("--preview-math")) -> renderMathPreview()
        args.contentEquals(arrayOf("--check-math")) -> checkMath()
        args.contentEquals(arrayOf("--preview-app")) -> renderAppPreview()
        args.contentEquals(arrayOf("--screenshots")) -> renderAppPreview("docs/screenshots", gallery = true)
        args.contentEquals(arrayOf("--self-test")) -> { checkMath(); checkLayerBridge(); checkLearningLab() }
        args.contentEquals(arrayOf("--preview-matrix")) -> renderMatrixPreview()
        args.contentEquals(arrayOf("--preview-slides")) -> renderSlidesPreview()
        args.contentEquals(arrayOf("--preview-3d-slides")) -> render3DSlidesPreview()
        args.contentEquals(arrayOf("--preview-layer-bridge")) -> renderLayerBridgePreview()
        args.contentEquals(arrayOf("--check-layer-bridge")) -> checkLayerBridge()
        args.contentEquals(arrayOf("--preview-lab")) -> renderLabPreview()
        args.contentEquals(arrayOf("--check-learning-lab")) -> checkLearningLab()
        else -> SwingUtilities.invokeLater{App().isVisible=true}
    }
}
