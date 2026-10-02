package vqtd

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// These are 3D teaching graphs projected with Java2D. Their depth expresses
// the order of operations or tree tiers; it does not add an algebraic axis to
// the equations in the papers.
internal data class SpatialNode(
    val id: String, val label: String, val detail: String,
    val x: Double, val y: Double, val z: Double,
    val appears: Int, val role: String = "data", val lost: Boolean = false, val lostFrom: Int = 4
)
internal data class SpatialEdge(val from: String, val to: String, val appears: Int, val role: String = "link")
// notes has one entry per stage, so a scene may have any number of stages.
// keyLabels names nodes whose labels stay visible after their stage passes.
internal data class SpatialScene(
    val title: String, val subtitle: String, val boundary: String,
    val notes: List<String>, val nodes: List<SpatialNode>, val edges: List<SpatialEdge>,
    val keyLabels: Set<String> = emptySet(),
    val yaw0: Double = 0.60, val pitch0: Double = -0.22,
    // When true, tiers older than the previous stage recede to faint ghosts
    // so the current step stays readable in dense scenes.
    val fadePast: Boolean = false
)

internal fun sceneFor(kind: LessonKind): SpatialScene = when (kind) {
    LessonKind.FORMULA -> formulaScene().let { it.copy(keyLabels = it.nodes.map { n -> n.id }.toSet()) }
    LessonKind.MERKLE -> merkleScene().copy(keyLabels = setOf("row0", "leaf0", "p0", "u0", "root", "proof", "repair"))
    LessonKind.REDTAIL -> redtailScene().copy(keyLabels = setOf("b0", "d0", "g", "s0", "s4", "s5", "survive", "verify"))
    LessonKind.FILEMAP -> fileMapScene().copy(keyLabels = setOf("file", "stripe0", "share0_0", "share0_4", "root0", "fileRoot", "rebuild"))
    LessonKind.REEDSOLOMON -> reedSolomonScene().let { sc -> sc.copy(keyLabels = setOf("d0", "v", "inverse", "g", "row0", "row4", "share1", "share4", "survivors", "recover") + sc.nodes.filter { it.id.startsWith("lambda") }.map { it.id }) }
    LessonKind.GFHAND -> gfHandScene()
    LessonKind.ROWREDUCE -> rowReduceScene()
    LessonKind.ENTROPY -> entropyScene()
    LessonKind.BRIDGE -> error("Layer Bridge has a side-by-side 3D matrix view")
    LessonKind.MATRIX -> error("Matrix has its own 3D lattice")
}

private fun formulaScene(): SpatialScene {
    val nodes = listOf(
        SpatialNode("f0", "F(0)", "2+2=4; 6 teaching bits", -5.0, -3.0, -8.0, 0),
        SpatialNode("f1", "F(1)", "14−10=4; 10 teaching bits", 0.0, 2.0, -8.0, 0),
        SpatialNode("f2", "F(2)", "8−4=4; 9 teaching bits", 5.0, -1.0, -8.0, 0),
        SpatialNode("target", "TARGET 4", "All three paths reach the same target", 0.0, 0.0, -4.0, 1, "proof"),
        SpatialNode("cost0", "6 bits", "F(0) under the illustrative cost rule", -5.0, -3.0, 0.0, 2),
        SpatialNode("cost1", "10 bits", "F(1) under the illustrative cost rule", 0.0, 2.0, 0.0, 2),
        SpatialNode("cost2", "9 bits", "F(2) under the illustrative cost rule", 5.0, -1.0, 0.0, 2),
        SpatialNode("win", "F(0) WINS", "arg min valid formula length = F(0)", -3.0, 2.0, 4.0, 3, "proof"),
        SpatialNode("omit", "LONGER OMIT", "F(1) and F(2) are longer here", 4.5, -5.0, 4.0, 4, "warning"),
        SpatialNode("manifest", "COORDINATE", "Conceptual manifest lookup in the class diagram", -2.0, -1.0, 8.0, 5, "proof"),
        SpatialNode("bridge", "PAPER BRIDGE", "Merkle and RS are distinct operations in the papers", 2.0, 2.0, 11.0, 6, "parity")
    )
    val edges = listOf(
        SpatialEdge("f0", "target", 1), SpatialEdge("f1", "target", 1), SpatialEdge("f2", "target", 1),
        SpatialEdge("f0", "cost0", 2), SpatialEdge("f1", "cost1", 2), SpatialEdge("f2", "cost2", 2),
        SpatialEdge("cost0", "win", 3, "proof"), SpatialEdge("cost1", "omit", 4, "warning"), SpatialEdge("cost2", "omit", 4, "warning"),
        SpatialEdge("win", "manifest", 5, "proof"), SpatialEdge("manifest", "bridge", 6, "parity")
    )
    return SpatialScene("F(i) CLASSROOM HUNT IN 3D", "Three expressions → one target → measured bit costs → selected path",
        "Class exercise only. The paper excluded shortest-form storage; no F(i) Merkle commitment is claimed.",
        listOf("Three candidate expressions all evaluate to 4.", "Validate each path against target 4.", "Compare the illustrative 6, 10, and 9 bit costs.",
            "Select F(0) under this classroom encoding.", "Mark the two longer descriptions as omitted here.", "Bind the selected description to a conceptual coordinate.",
            "Keep the later paper-specific Merkle and RS operations distinct."), nodes, edges)
}

private fun merkleScene(): SpatialScene {
    val nodes = mutableListOf<SpatialNode>()
    val edges = mutableListOf<SpatialEdge>()
    for (i in 0..7) {
        val x = (i - 3.5) * 2.1
        nodes += SpatialNode("row$i", "ROW $i", "child $i → committed parent ${if (i < 4) "P" else "Q"}", x, -3.0, -9.0, 0)
        nodes += SpatialNode("leaf$i", "LEAF $i", "H(00 || len(child) || child || len(parent) || parent)", x, -1.0, -5.0, 1, "proof")
        edges += SpatialEdge("row$i", "leaf$i", 1)
    }
    for (i in 0..3) {
        val x = (i - 1.5) * 4.2
        nodes += SpatialNode("p$i", "PAIR $i", "H(01 || left || right)", x, 1.0, -1.0, 2, "parity")
        edges += SpatialEdge("leaf${2 * i}", "p$i", 2)
        edges += SpatialEdge("leaf${2 * i + 1}", "p$i", 2)
    }
    for (i in 0..1) {
        val x = (i - 0.5) * 8.4
        nodes += SpatialNode("u$i", "BRANCH $i", "Hash of two ordered pair hashes", x, 3.0, 3.0, 3, "parity")
        edges += SpatialEdge("p${2 * i}", "u$i", 3)
        edges += SpatialEdge("p${2 * i + 1}", "u$i", 3)
    }
    nodes += SpatialNode("root", "OWNER ROOT", "Owner map commitment recorded at ingest", 0.0, 5.0, 7.0, 4, "proof")
    edges += SpatialEdge("u0", "root", 4, "proof"); edges += SpatialEdge("u1", "root", 4, "proof")
    nodes += SpatialNode("proof", "INCLUSION", "Sibling path recomputes the trusted root in O(log n)", -5.0, 0.0, 10.0, 5, "proof")
    edges += SpatialEdge("leaf2", "p1", 5, "proof")
    edges += SpatialEdge("p1", "u0", 5, "proof")
    edges += SpatialEdge("u0", "root", 5, "proof")
    nodes += SpatialNode("repair", "SIDECAR VIEW", "Unique mirror proposes owner; stored E stays byte-exact", 5.0, 0.0, 10.0, 6, "parity")
    return SpatialScene("BRMR OWNER MERKLE TREE IN 3D", "Eight sample rows rise through leaf, pair, branch, and root tiers",
        "The root checks a prior commitment. It cannot fix a converter error already present at ingest.",
        listOf("Owner rows are committed before later damage.", "Domain-separated leaf hashes bind child and parent handles.",
            "Ordered pairs combine into parent hashes.", "Two larger branches combine the pair hashes.", "One root commits to the owner map at ingest.",
            "An inclusion proof follows sibling hashes to that root.", "R2u proposes a unique owner; a separate sidecar renders accepted repair."), nodes, edges)
}

private fun redtailScene(): SpatialScene {
    val nodes = mutableListOf<SpatialNode>()
    val edges = mutableListOf<SpatialEdge>()
    for (i in 0..6) nodes += SpatialNode("b$i", "BYTE $i", "One of seven original tail bytes", (i - 3) * 2.0, 0.0, -9.0, 0)
    for (i in 0..3) {
        nodes += SpatialNode("d$i", "D$i", if (i == 3) "One data byte plus one zero pad" else "Two data bytes", (i - 1.5) * 4.0, 0.0, -5.0, 1)
        for (byte in (i * 2)..min(i * 2 + 1, 6)) edges += SpatialEdge("b$byte", "d$i", 1)
    }
    nodes += SpatialNode("g", "G = [I₄; C]", "Apply the 4+2 systematic generator per byte column", 0.0, 1.0, -1.0, 2, "proof")
    for (i in 0..3) edges += SpatialEdge("d$i", "g", 2)
    for (i in 0..5) {
        nodes += SpatialNode("s$i", "SHARE $i", if (i < 4) "Systematic data share" else "Parity share", (i - 2.5) * 2.7, 0.0, 3.0, 3,
            if (i < 4) "data" else "parity", lost = i == 1 || i == 4)
        edges += SpatialEdge("g", "s$i", 3, if (i < 4) "link" else "parity")
    }
    nodes += SpatialNode("survive", "4 SURVIVORS", "Shares 0, 2, 3, and 5 remain after two losses", 0.0, 1.0, 7.0, 5, "proof")
    for (i in listOf(0, 2, 3, 5)) edges += SpatialEdge("s$i", "survive", 5, "proof")
    nodes += SpatialNode("verify", "VERIFY 7 BYTES", "Invert survivor rows, truncate padding, check stripe hash", 0.0, 1.5, 11.0, 6, "parity")
    edges += SpatialEdge("survive", "verify", 6, "parity")
    return SpatialScene("REDTAIL-X 4+2 TAIL IN 3D", "Seven original bytes → four short data shares → six coded shares → repair",
        "Worked example: t=7, s=⌈7/4⌉=2. Six 2-byte shares store 12 coded bytes before metadata.",
        listOf("Seven bytes enter the final partial stripe.", "Split into four 2-byte data shares; pad the last with one zero.",
            "Apply the systematic generator G to each byte column.", "Produce four data shares and two parity shares.", "Lose shares 1 and 4; hashes identify bad or missing shares.",
            "Select four verified survivors and invert their generator rows.", "Rebuild, truncate to original_len=7, and verify the stripe hash."), nodes, edges)
}

// Proposed teaching composition: RedTail-X already records per-share and
// stripe hashes; a new Merkle layer over those records is illustrated here.
// BRMR's published Merkle root commits an owner map, not file-share bytes.
private fun fileMapScene(): SpatialScene {
    val nodes = mutableListOf<SpatialNode>()
    val edges = mutableListOf<SpatialEdge>()
    nodes += SpatialNode("file", "FILE", "Original bytes split into ordered stripes", 0.0, -2.0, -12.0, 0)
    for (s in 0..1) {
        val base = if (s == 0) -6.0 else 6.0
        nodes += SpatialNode("stripe$s", "STRIPE $s", "One file region; each byte column uses G", base, -1.0, -8.0, 1, "proof")
        edges += SpatialEdge("file", "stripe$s", 1)
        for (r in 0..5) {
            val x = base + (r - 2.5) * 1.3
            nodes += SpatialNode("share${s}_$r", "S$s:$r", "Stored share $r of stripe $s; G row $r spans its byte columns", x, -1.0, -4.0, 2,
                if (r < 4) "data" else "parity", lost = s == 0 && (r == 1 || r == 4))
            edges += SpatialEdge("stripe$s", "share${s}_$r", 2)
        }
        for (i in 0..7) {
            val x = base + (i - 3.5) * 1.3
            val detail = when (i) {
                6 -> "Hash of stripe $s manifest lengths and coordinates"
                7 -> "Hash of stripe $s original data bytes"
                else -> "Hash of share $i bytes and stripe/share coordinate"
            }
            nodes += SpatialNode("leaf${s}_$i", if (i < 6) "H(S$s:$i)" else if (i == 6) "H(M$s)" else "H(D$s)", detail, x, 0.0, 0.0, 3, "proof")
            if (i < 6) edges += SpatialEdge("share${s}_$i", "leaf${s}_$i", 3, "hash")
        }
        for (p in 0..3) {
            val x = base + (p - 1.5) * 2.6
            nodes += SpatialNode("pair${s}_$p", "PAIR $s:$p", "Commits to two descendant leaves", x, 1.0, 4.0, 4, "parity")
            edges += SpatialEdge("leaf${s}_${2 * p}", "pair${s}_$p", 4, "hash")
            edges += SpatialEdge("leaf${s}_${2 * p + 1}", "pair${s}_$p", 4, "hash")
        }
        for (b in 0..1) {
            val x = base + (b - 0.5) * 5.2
            nodes += SpatialNode("branch${s}_$b", "BRANCH $s:$b", "Commits to four descendant leaves", x, 2.0, 7.0, 4, "parity")
            edges += SpatialEdge("pair${s}_${2 * b}", "branch${s}_$b", 4, "hash")
            edges += SpatialEdge("pair${s}_${2 * b + 1}", "branch${s}_$b", 4, "hash")
        }
        nodes += SpatialNode("root$s", "STRIPE ROOT $s", "Commits to eight leaves: six shares and two metadata records", base, 3.0, 10.0, 5, "proof")
        edges += SpatialEdge("branch${s}_0", "root$s", 5, "hash")
        edges += SpatialEdge("branch${s}_1", "root$s", 5, "hash")
    }
    nodes += SpatialNode("fileRoot", "FILE ROOT", "Commits to both stripe roots; a digest, not a file copy", 0.0, 4.0, 13.0, 5, "proof")
    edges += SpatialEdge("root0", "fileRoot", 5, "hash")
    edges += SpatialEdge("root1", "fileRoot", 5, "hash")
    nodes += SpatialNode("rebuild", "REBUILD + VERIFY", "Four valid shares rebuild bytes; then compare hashes", 0.0, 0.0, 16.0, 6, "parity")
    for (r in listOf(0, 2, 3, 5)) edges += SpatialEdge("share0_$r", "rebuild", 6, "proof")
    return SpatialScene("FILE SHARES + MERKLE SCOPE IN 3D", "Two example stripes; inspect a node to see the region it commits to",
        "Proposed file-share Merkle layer. RedTail-X uses share/stripe hashes; BRMR's Merkle root commits an owner map.",
        listOf("Begin with the original file bytes.", "Split the file into ordered stripes.", "G produces four data and two parity shares per stripe.",
            "Hash share bytes and bound coordinates into leaves; add metadata leaves.", "Parent hashes commit to descendant leaf regions.",
            "Stripe roots feed a file root; an internal node commits to a subtree, not bytes.",
            "Rebuild from four valid shares, then verify bytes and commitments."), nodes, edges)
}

private fun reedSolomonScene(): SpatialScene {
    val nodes = mutableListOf<SpatialNode>()
    val edges = mutableListOf<SpatialEdge>()
    for (c in 0..3) nodes += SpatialNode("d$c", "d$c", "Input byte $c in GF(2^8)", (c-1.5)*3.0, -3.0, -9.0, 0)
    nodes += SpatialNode("v", "V", "Vandermonde rows evaluated at six distinct field elements", -3.0, 0.0, -5.0, 1, "proof")
    nodes += SpatialNode("inverse", "Vtop⁻¹", "Invert the top four Vandermonde rows to make the code systematic", 3.0, 0.0, -5.0, 1, "proof")
    nodes += SpatialNode("g", "G=V·Vtop⁻¹", "First four generator rows form I₄; final two rows are parity", 0.0, 1.0, -2.0, 2, "proof")
    edges += SpatialEdge("v", "g", 2); edges += SpatialEdge("inverse", "g", 2)
    for (c in 0..3) edges += SpatialEdge("d$c", "g", 2)
    for (r in 0..5) {
        val x = (r-2.5)*2.8
        val row = Gf256.generator[r].joinToString(" ") { it.toString(16).uppercase().padStart(2,'0') }
        nodes += SpatialNode("row$r", "G[$r,*]", "GF(2^8) coefficients: $row", x, 0.0, 1.5, 3, if (r < 4) "data" else "parity")
        nodes += SpatialNode("share$r", "c$r", if (r < 4) "Systematic data share $r" else "Parity share $r", x, 0.0, 5.0, 3,
            if (r < 4) "data" else "parity", lost = r == 1 || r == 4)
        edges += SpatialEdge("g", "row$r", 3); edges += SpatialEdge("row$r", "share$r", 3)
    }
    nodes += SpatialNode("survivors", "0,2,3,5", "Four verified surviving rows; losses are 1 and 4", 0.0, 1.0, 9.0, 5, "proof")
    for (r in listOf(0,2,3,5)) edges += SpatialEdge("share$r", "survivors", 5, "proof")
    nodes += SpatialNode("recover", "Gsurv⁻¹", "Invert the selected 4×4 matrix to recover four input bytes", 0.0, 2.0, 12.0, 6, "parity")
    edges += SpatialEdge("survivors", "recover", 6, "parity")
    for (i in 1..4) nodes += SpatialNode("lambda$i", "λ$i ?", "Reserved proof label: supply the definition and source before claiming a result", (i-2.5)*3.0, -3.0, 13.5, 6, "proof")
    return SpatialScene("REED–SOLOMON 4+2 IN 3D", "Rotate the generator and inspect rows; red shares mark two erasures",
        "Algebra rebuilds bytes; hashes check damage. Your λ₁…λ₄ await definitions (BRMR's λ(o) is an owner-line index).",
        listOf("Four bytes form the data vector d.", "Six distinct field points form V; invert its top four rows.",
            "G=V·Vtop⁻¹ makes the first four output rows systematic.", "Each G row produces one coded share byte.",
            "Shares 1 and 4 are lost; their absence alone does not erase the file.", "Select intact rows 0, 2, 3, 5.",
            "Invert those four rows, recover d, and verify the stripe digest."), nodes, edges)
}

internal class Spatial3DCanvas(private val scene: SpatialScene) : JPanel() {
    constructor(kind: LessonKind) : this(sceneFor(kind))
    private data class RawPoint(val x: Double, val y: Double, val depth: Double, val perspective: Double)
    private data class ScreenPoint(val node: SpatialNode, val x: Int, val y: Int, val depth: Double, val perspective: Double)
    private val byId = scene.nodes.associateBy { it.id }
    var stage = 0; private set
    val lastStage: Int get() = scene.notes.size - 1
    private var yaw = scene.yaw0
    private var pitch = scene.pitch0
    private var zoom = 1.0
    private var frame = 0
    private var dragX = 0
    private var dragY = 0
    private var selectedId: String? = null
    private var screenPoints = emptyList<ScreenPoint>()

    init {
        background = canvas; preferredSize = Dimension(980, 610)
        toolTipText = "Drag to rotate • wheel to zoom • click a node to inspect"
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { dragX = e.x; dragY = e.y }
            override fun mouseDragged(e: MouseEvent) {
                yaw += (e.x - dragX) * 0.007
                pitch = (pitch + (e.y - dragY) * 0.007).coerceIn(-1.15, 1.15)
                dragX = e.x; dragY = e.y; repaint()
            }
            override fun mouseWheelMoved(e: MouseWheelEvent) {
                zoom = (zoom * if (e.wheelRotation < 0) 1.1 else 0.9).coerceIn(0.45, 2.4); repaint()
            }
            override fun mouseClicked(e: MouseEvent) {
                selectedId = screenPoints.filter { it.node.appears <= stage }
                    .minByOrNull { (it.x - e.x) * (it.x - e.x) + (it.y - e.y) * (it.y - e.y) }
                    ?.takeIf { (it.x - e.x) * (it.x - e.x) + (it.y - e.y) * (it.y - e.y) < 400 }?.node?.id
                repaint()
            }
        }
        addMouseListener(mouse); addMouseMotionListener(mouse); addMouseWheelListener(mouse)
    }
    fun tick() { yaw += 0.004; frame++; if (frame % 45 == 0 && stage < lastStage) stage++; repaint() }
    fun advanceStep(): Boolean { if (stage < lastStage) stage++; repaint(); return stage < lastStage }
    fun resetView() { stage = 0; yaw = scene.yaw0; pitch = scene.pitch0; zoom = 1.0; frame = 0; selectedId = null; repaint() }
    private fun rawPoint(n: SpatialNode): RawPoint {
        val rx = cos(yaw) * n.x + sin(yaw) * n.z
        val rz = -sin(yaw) * n.x + cos(yaw) * n.z
        val ry = cos(pitch) * n.y - sin(pitch) * rz
        val depth = sin(pitch) * n.y + cos(pitch) * rz
        val perspective = 30.0 / (30.0 + depth).coerceAtLeast(7.0)
        return RawPoint(rx * perspective, ry * perspective, depth, perspective)
    }
    private fun roleColor(role: String): Color = when (role) { "proof" -> cyan; "parity" -> amber; "warning" -> red; "zero" -> dim; else -> green }
    private fun tint(c: Color, alpha: Int) = Color(c.red, c.green, c.blue, alpha.coerceIn(0, 255))
    override fun paintComponent(original: Graphics) {
        super.paintComponent(original)
        val g = original.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val raw = scene.nodes.associate { it.id to rawPoint(it) }
            val minX = raw.values.minOf { it.x }; val maxX = raw.values.maxOf { it.x }
            val minY = raw.values.minOf { it.y }; val maxY = raw.values.maxOf { it.y }
            val scale = min((width - 100.0) / (maxX - minX).coerceAtLeast(1.0),
                (height - 225.0) / (maxY - minY).coerceAtLeast(1.0)) * zoom
            val centerY = (100.0 + height - 125.0) / 2.0
            screenPoints = scene.nodes.map { n ->
                val p = raw.getValue(n.id)
                ScreenPoint(n, (width / 2.0 + (p.x - (minX + maxX) / 2.0) * scale).toInt(),
                    (centerY + (p.y - (minY + maxY) / 2.0) * scale).toInt(), p.depth, p.perspective)
            }
            val screenById = screenPoints.associateBy { it.node.id }
            g.stroke = BasicStroke(2.1f)
            for (edge in scene.edges) if (edge.appears <= stage) {
                val a = screenById.getValue(edge.from); val b = screenById.getValue(edge.to)
                val accent = roleColor(edge.role)
                g.color = tint(accent, if (edge.appears == stage) 210 else if (scene.fadePast && edge.appears < stage - 1) 22 else 95)
                g.drawLine(a.x, a.y, b.x, b.y)
            }
            for (p in screenPoints.filter { it.node.appears <= stage }.sortedByDescending { it.depth }) {
                val n = p.node
                val accent = if (n.lost && stage >= n.lostFrom) red else roleColor(n.role)
                val active = n.appears == stage || n.id == selectedId
                val ghost = scene.fadePast && !active && n.appears < stage - 1 && n.id !in scene.keyLabels
                val radius = ((if (active) 12.0 else if (ghost) 4.0 else 8.0) * p.perspective).toInt().coerceIn(if (ghost) 2 else 5, 24)
                if (ghost) { g.color = tint(accent, 45); g.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2); continue }
                g.color = tint(accent, if (active) 82 else 42)
                g.fillOval(p.x - radius - 5, p.y - radius - 5, radius * 2 + 10, radius * 2 + 10)
                g.color = accent; g.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2)
                g.color = tint(ink, 220); g.stroke = BasicStroke(1.5f); g.drawOval(p.x - radius, p.y - radius, radius * 2, radius * 2)
                val keyLabel = n.id in scene.keyLabels
                if ((active || keyLabel) && n.label.isNotEmpty()) {
                    g.font = Font(Font.SANS_SERIF, Font.BOLD, if (active) 13 else 11)
                    val textWidth = g.fontMetrics.stringWidth(n.label)
                    val x = if (p.x + radius + textWidth + 12 > width - 14) p.x - radius - textWidth - 8 else p.x + radius + 6
                    val y = p.y - 4
                    g.color = tint(surface, 215); g.fillRoundRect(x - 3, y - g.fontMetrics.ascent, textWidth + 6, g.fontMetrics.height, 6, 6)
                    g.color = ink; g.drawString(n.label, x, y)
                }
            }
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 19); g.color = ink; g.drawString(scene.title, 22, 31)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 14); g.color = dim; g.drawString(scene.subtitle, 22, 54)
            g.color = ink; g.drawString("Stage $stage/$lastStage  ·  ${scene.notes[stage]}", 22, height - 87)
            g.color = dim; g.drawString("Drag: rotate    Wheel: zoom    Click: inspect    Next: reveal another tier", 22, height - 59)
            g.drawString(scene.boundary, 22, height - 29)
            selectedId?.let { id ->
                val node = byId.getValue(id)
                val text = "${node.label}: ${node.detail}"
                g.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
                val boxWidth = min(width - 32, g.fontMetrics.stringWidth(text) + 26)
                g.color = surface; g.fillRoundRect(16, 70, boxWidth, 35, 12, 12)
                g.color = borderColor; g.drawRoundRect(16, 70, boxWidth, 35, 12, 12)
                g.color = ink; g.drawString(text, 28, 93)
            }
        } finally { g.dispose() }
    }
}
