package vqtd

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// The learner's choices are kept in memory for this run. They are not a
// diagnosis, an automated assessment of prose, or telemetry.
data class LearningCard(
    val title: String,
    val source: String,
    val prompt: String,
    val choices: List<String>,
    val correct: Int,
    val evidence: String,
    val equation: String,
    val code: String,
    val explainPrompt: String
)

val learningCards = listOf(
    LearningCard("Four survivors", "RedTail-X v1.1, printed p. 2–3", "Two of six shares are lost. How many verified shares are enough to reconstruct the four data symbols?",
        listOf("Any two", "Any four", "All six"), 1, "A 4+2 MDS generator has four independent data dimensions. Any four surviving generator rows can be inverted; share and stripe hashes still check integrity.",
        "c = Gd; d = Gsurvivors⁻¹ csurvivors", "invert(fourSurvivingRows); verifyStripeHash()", "Why do four surviving rows work, and what does the hash check afterward?"),
    LearningCard("Variable tail", "RedTail-X v1.1, printed p. 3", "A final stripe has seven data bytes. What is the length of each of its six coded shares?",
        listOf("1 byte", "2 bytes", "7 bytes"), 1, "The share length is s=⌈7/4⌉=2 bytes. Four data shares have room for eight bytes, so the one pad byte is removed after decoding using original_len=7.",
        "s = ⌈t/4⌉; coded_tail = 6s", "shareLen = (originalLen + 3) / 4", "Why must the manifest store original_len as well as share_len?"),
    LearningCard("Exact CAD restore", "DarkRock CAD v1, printed p. 3–5", "A geometry key finds a visually similar drawing. What must happen before using it to restore original DXF bytes?",
        listOf("Accept the geometric match", "Verify literal edits or a byte-exact patch", "Normalize whitespace"), 1, "A geometry key can rank candidates. The stored literal data or verified patch must still reproduce the original bytes exactly. The A3 storage arm failed the held-out comparison.",
        "restore(stored representation) = original bytes", "candidate = geometryKeyLookup(); require(patch(candidate) == originalBytes)", "Why is a useful lookup key different from an exact storage format?"),
    LearningCard("Ambiguous owner", "BRMR, printed p. 4", "Two distinct parents refer back to the same child whose owner field is missing. What should R2u return?",
        listOf("The first parent", "The closest parent", "⊥ (abstain)"), 2, "R2u accepts only a singleton parent set. With two candidates, the mirror evidence is ambiguous; returning ⊥ avoids pretending that a tie break is proof.",
        "|S(o)| = 1 ⇒ owner; otherwise ⊥", "reverseIndex[child].singleOrNull()", "Explain why confidence in one candidate cannot replace a unique witness."),
    LearningCard("Merkle root", "BRMR, printed p. 5", "An owner-map Merkle root was committed at ingest. Which claim can an inclusion proof support?",
        listOf("The converter's owner was originally correct", "This owner row matches the committed ingest map", "The root can solve a missing owner alone"), 1, "The proof binds a row to the prior root. It detects later changes if the root is trusted; it cannot repair an error already present at ingest or infer a missing value by itself.",
        "leaf=H(00||row); node=H(01||left||right)", "verify(ownerRoot, ownerLeaf, siblingProof)", "What does the root remember, and what additional witness proposes a repair?"),
    LearningCard("Held-out reversal", "DarkRock CAD v1, printed p. 5", "A3 saved 9.7% on development data but used 8.1% more on held-out files. What is the supported conclusion?",
        listOf("A3 is a proven universal compressor", "A3 should be rejected as a storage format on this evidence", "The held-out measurement is irrelevant"), 1,
        "The development gain did not generalize. The report rejected A3 as a storage representation, while retaining a narrow geometry-key role for choosing a delta base.",
        "development −9.7% → held-out +8.1%", "compareProtectedBytes(development, heldOut)", "Why is a held-out reversal more informative than a promising development number?")
)

data class LearningAttempt(val cardIndex: Int, val choice: Int, val confidence: String, val correct: Boolean, val revisit: Boolean)

class LearningProgress {
    var cardIndex = 0
    var phase = 0 // 0 predict; 1 reveal; 2 explain
    var revisit = false
    var choice = -1
    var confidence = "Unsure"
    val attempts = mutableListOf<LearningAttempt>()
    val studied = linkedSetOf<Int>()
    val drafts = mutableMapOf<Int, String>()
    val explanations = mutableMapOf<Int, String>()
    fun moveTo(index: Int, recall: Boolean = false) {
        cardIndex = index; phase = 0; revisit = recall; choice = -1; confidence = "Unsure"
    }
}

data class LabColors(val canvas: Color, val panel: Color, val surface: Color, val ink: Color, val dim: Color, val accent: Color, val caution: Color, val border: Color)

class LearningLabPanel(private val progress: LearningProgress, private val colors: LabColors) : JPanel(BorderLayout()) {
    private val body = JPanel()
    private val status = JLabel()
    init {
        background = colors.canvas
        border = EmptyBorder(12, 15, 12, 15)
        body.layout = BoxLayout(body, BoxLayout.Y_AXIS)
        body.background = colors.canvas
        add(JScrollPane(body).apply { border = null; viewport.background = colors.canvas; verticalScrollBar.unitIncrement = 20 }, BorderLayout.CENTER)
        add(status.apply { foreground = colors.dim; border = EmptyBorder(8, 10, 4, 10) }, BorderLayout.SOUTH)
        render()
    }
    private fun label(value: String, size: Int = 16, bold: Boolean = false, color: Color = colors.ink) = JLabel(value).apply {
        foreground = color; font = Font(Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, size)
        alignmentX = LEFT_ALIGNMENT
    }
    private fun button(value: String, action: () -> Unit) = JButton(value).apply {
        background = colors.panel; foreground = colors.ink; isFocusPainted = false; addActionListener { action() }
    }
    private fun paragraph(value: String, color: Color = colors.ink, size: Int = 16, height: Int = 86) = JTextArea(value).apply {
        isEditable = false; lineWrap = true; wrapStyleWord = true; background = colors.surface; foreground = color
        font = Font(Font.SANS_SERIF, Font.PLAIN, size); border = EmptyBorder(12, 14, 12, 14)
        alignmentX = LEFT_ALIGNMENT; preferredSize = Dimension(600, height); maximumSize = Dimension(Int.MAX_VALUE, height)
    }
    private fun row(vararg components: JComponent) = JPanel(FlowLayout(FlowLayout.LEFT, 9, 5)).apply {
        background = colors.canvas; alignmentX = LEFT_ALIGNMENT; maximumSize = Dimension(Int.MAX_VALUE, 48); components.forEach(::add)
    }
    private fun gap(height: Int = 12) { body.add(Box.createVerticalStrut(height)) }
    private fun refresh() { render(); revalidate(); repaint() }
    private fun render() {
        body.removeAll()
        val card = learningCards[progress.cardIndex]
        body.add(label("LEARNING LAB  ·  ${if (progress.revisit) "REVISIT" else "FIRST PASS"}  ·  CARD ${progress.cardIndex + 1}/${learningCards.size}", 17, true, colors.accent)); gap(6)
        body.add(label(card.title, 24, true)); gap(5)
        body.add(label(card.source, 13, false, colors.dim)); gap(14)
        body.add(paragraph("PREDICT\n${card.prompt}")); gap(8)

        val group = ButtonGroup()
        for ((index, choiceText) in card.choices.withIndex()) {
            val option = JRadioButton(choiceText).apply {
                background = colors.canvas; foreground = colors.ink; font = Font(Font.SANS_SERIF, Font.PLAIN, 15)
                isSelected = progress.choice == index; isEnabled = progress.phase == 0
                addActionListener { progress.choice = index }
                alignmentX = LEFT_ALIGNMENT
            }
            group.add(option); body.add(option)
        }
        gap(8)
        val confidence = JComboBox(arrayOf("Unsure", "Somewhat sure", "Very sure")).apply {
            selectedItem = progress.confidence; isEnabled = progress.phase == 0
            background = colors.surface; foreground = colors.ink
            addActionListener { progress.confidence = selectedItem as String }
        }
        body.add(row(label("Confidence before reveal:"), confidence)); gap(4)

        if (progress.phase == 0) {
            body.add(row(button("Reveal evidence") {
                if (progress.choice < 0) { status.text = "Choose a prediction first."; return@button }
                progress.attempts += LearningAttempt(progress.cardIndex, progress.choice, progress.confidence, progress.choice == card.correct, progress.revisit)
                progress.phase = 1; refresh()
            }))
        } else {
            val attempt = progress.attempts.last { it.cardIndex == progress.cardIndex && it.revisit == progress.revisit }
            val result = if (attempt.correct) "Your prediction matched the evidence." else "Your prediction differed from the evidence."
            body.add(paragraph("REVEAL\n$result  You marked: ${attempt.confidence}.\nAnswer: ${card.choices[card.correct]}\n\n${card.evidence}", if (attempt.correct) colors.accent else colors.caution, height = 150)); gap(8)
            body.add(paragraph("EQUATION  ${card.equation}\nCODE  ${card.code}", colors.ink, 14)); gap(8)
            if (progress.phase == 1) body.add(row(button("Explain this step") { progress.phase = 2; refresh() }))
            if (progress.phase == 2) {
                body.add(label("EXPLAIN IT IN YOUR OWN WORDS", 15, true, colors.accent)); gap(4)
                body.add(paragraph(card.explainPrompt)); gap(7)
                val writing = JTextArea(progress.drafts[progress.cardIndex] ?: "", 4, 48).apply {
                    lineWrap = true; wrapStyleWord = true; background = colors.surface; foreground = colors.ink; caretColor = colors.ink
                    font = Font(Font.SANS_SERIF, Font.PLAIN, 15); border = BorderFactory.createLineBorder(colors.border)
                    document.addDocumentListener(object : DocumentListener {
                        private fun save() { progress.drafts[progress.cardIndex] = text }
                        override fun insertUpdate(e: DocumentEvent) = save()
                        override fun removeUpdate(e: DocumentEvent) = save()
                        override fun changedUpdate(e: DocumentEvent) = save()
                    })
                }
                body.add(JScrollPane(writing).apply { alignmentX = LEFT_ALIGNMENT; maximumSize = Dimension(Int.MAX_VALUE, 110) }); gap(7)
                body.add(row(button("Save explanation & next card") {
                    progress.explanations[progress.cardIndex] = writing.text
                    progress.studied += progress.cardIndex
                    progress.moveTo((progress.cardIndex + 1) % learningCards.size)
                    refresh()
                }))
            }
        }
        gap(12)
        val studied = progress.studied.toList()
        if (studied.isNotEmpty()) {
            val chooser = JComboBox(studied.map { "${it + 1}. ${learningCards[it].title}" }.toTypedArray()).apply { background = colors.surface; foreground = colors.ink }
            body.add(row(label("REVISIT WITH ANSWER HIDDEN", 14, true), chooser, button("Revisit selected") {
                progress.moveTo(studied[chooser.selectedIndex], true); refresh()
            }))
        }
        body.add(row(button("Next question") { progress.moveTo((progress.cardIndex + 1) % learningCards.size); refresh() },
            button("Read teaching method") { JOptionPane.showMessageDialog(this,
                "Predict → reveal one paper-backed step → explain → revisit.\n" +
                "Confidence is recorded separately from correctness. Written explanations are saved locally in this session; the app does not grade prose.\n" +
                "Research notes and links: PSYCHOLOGY.md", "Teaching method", JOptionPane.INFORMATION_MESSAGE) }))
        val first = progress.attempts.count { !it.revisit }; val recalls = progress.attempts.count { it.revisit }
        val high = progress.attempts.filter { it.confidence == "Very sure" }
        status.text = "Session: $first first-pass predictions · $recalls revisits · very-sure correct ${high.count { it.correct }}/${high.size}. No data leaves this app."
        body.revalidate(); body.repaint()
    }
}
