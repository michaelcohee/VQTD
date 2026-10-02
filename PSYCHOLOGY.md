# Psychology layer for the VQTD classroom

This is the teaching design behind the app's **Learning Lab**, not a
psychological model of a storage codec and not a claim that its math simulates
a brain. It precedes any proposed neuromorphic programming work.

## The learning loop

1. **Predict.** Before revealing a matrix step or an owner-repair decision,
   ask the learner what can be recovered and how sure they are. The app has
   six paper-backed cards from RedTail-X, DarkRock CAD, and BRMR.
2. **Reveal one step.** Animate one row, witness, or proof at a time. Keep the
   goal and the currently used symbols visible.
3. **Explain.** Ask the learner to state why the step is valid. For example,
   “Why are any four surviving shares enough?” or “Why does `|S(o)|=1`
   permit an answer?”
4. **Revisit.** A studied card can be selected for a new attempt with the
   answer hidden. The app shows feedback and the paper location after that
   attempt.

The first two steps are a design response to the demands of learning a new
symbol system. Worked examples can reduce the processing spent on searching
for a solution while a learner builds a usable schema [1]. Explaining a
worked step in one's own words can connect that step to its principle [2].
Repeated retrieval can improve later recall compared with repeated study
alone [3]. These findings motivate the controls; they do not establish a
measured learning gain for VQTD until the app is evaluated.

## Confidence and evidence

The Learning Lab records **unsure**, **somewhat sure**, or **very sure** before
each reveal. After feedback, it shows the learner's prediction beside the
actual evidence and reports how many very-sure predictions were correct in
this session. In BRMR, a learner may feel confident about an owner
while the rule must still return `⊥` because the witness set is ambiguous.
This makes the difference between a judgment and a sufficient check visible.
Metacognitive judgments can guide study, but may be biased [4].

Good questions for this layer:

- Which four of six rows survived, and what can they reconstruct?
- Does a hash match prove the intended owner, or only match a prior commitment?
- If two owner witnesses disagree, should the rebuilder guess or abstain?
- Why did an apparent development-set compression gain reverse on held-out
  data in DarkRock CAD?

## Interface principles

- Keep **Play**, **Pause**, **Next**, and **Reset** available. Let the learner
  control pacing and return to a previous explanation.
- Keep a visible stage marker and a short line stating the current claim.
- Offer plain language, equation, and code views for the same concept.
- Use color and text labels together; never make color the only cue.
- Offer a quiet presentation without motion or timed questions. These are
  interface options for anyone, not a diagnosis or a fixed learning-style
  assignment.

The Learning Lab itself has no timer. Its choices and written explanations
remain in memory during the running app session; there is no file upload,
telemetry, or automatic grading of prose. The explanation is saved so the
learner can compare their own wording with the paper-backed evidence later.

## Possible later bridge to neuromorphic work

The class can compare **state → input/evidence → update → output** with an
engineered adaptive system. That is an analogy to investigate, not an
equivalence between human learning and Reed–Solomon or Merkle algorithms.
Any future neuromorphic lesson should identify its actual computational model
and testable predictions before making claims about cognition.

## Sources

1. Sweller, J. (1988), “Cognitive Load During Problem Solving: Effects on
   Learning,” *Cognitive Science* 12, 257–285.
   https://onlinelibrary.wiley.com/doi/10.1207/s15516709cog1202_4
2. Chi, M. T. H. et al. (1989), “Self-Explanations: How Students Study and Use
   Examples in Learning to Solve Problems,” *Cognitive Science* 13, 145–182.
   https://doi.org/10.1207/s15516709cog1302_1
3. Karpicke, J. D. and Roediger, H. L. (2008), “The Critical Importance of
   Retrieval for Learning,” *Science* 319, 966–968.
   https://doi.org/10.1126/science.1152408
4. Metcalfe, J. (2009), “Metacognitive Judgments and Control of Study,”
   *Current Directions in Psychological Science* 18, 159–163.
   https://pmc.ncbi.nlm.nih.gov/articles/PMC2742428/
