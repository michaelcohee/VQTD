package vqtd

// Source pages are the printed page numbers in Papers/, not PDF viewer indices.
data class LessonFormula(
    val paper: String,
    val page: String,
    val topic: String,
    val expression: String,
    val classroom: String,
    val codeBridge: String,
    val status: String = "Reported formula"
)

val paperFormulas = listOf(
    // RedTail-X v1.1: code and layout are distinct from the excluded explorations.
    LessonFormula("RedTail-X v1.1", "2 §2.1", "Field and data vector", "𝔽 = GF(2⁸),   d ∈ 𝔽⁴", "Each column has four byte symbols. Arithmetic is in a 256-element field.", "MRT-X: galois_8::ReedSolomon; C++: gf_mul and four data arrays"),
    LessonFormula("RedTail-X v1.1", "2 §2.1", "Systematic code", "c = Gd,   G = [I₄; C] ∈ 𝔽⁶ˣ⁴", "The first four outputs reproduce data; the last two provide parity. Any four independent rows solve for d.", "MRT-X: ReedSolomon::new(4,2); C++: generator() and invert(submatrix)"),
    LessonFormula("RedTail-X v1.1", "2 §2.1", "Nominal storage rate", "n/k = 6/4 = 1.5", "Six stored shares for every four data shares means 50% parity overhead for a full stripe.", "MRT-X: DATA_SHARDS=4, PARITY_SHARDS=2"),
    LessonFormula("RedTail-X v1.1", "2 §2.2 Eq. 1", "Byte entropy", "H₈(x) = −Σσ p(σ) log₂ p(σ),   0 ≤ H₈ ≤ 8", "A compression gate may inspect byte diversity. This number does not control Reed–Solomon recovery.", "Paper compression experiment; MRT-X storage.rs has no entropy gate"),
    LessonFormula("RedTail-X v1.1", "2 §2.2", "Zero-frequency convention", "0 · log₂(0) := 0", "Absent byte values contribute zero to Shannon entropy.", "When count==0, skip the term; avoid log(0)"),
    LessonFormula("RedTail-X v1.1", "2 §3", "Alternative P and Q parity", "P = ⊕ᵢ dᵢ,   Q = ⊕ᵢ gⁱ dᵢ", "A tested RAID-6 style parity pair. The paper retained library Reed–Solomon for production.", "Do not substitute this for MRT-X's current Vandermonde generator", "Tested alternative"),
    LessonFormula("RedTail-X v1.1", "3 §4.1", "Full and tail lengths", "q = ⌊L / 2²⁰⌋,   t = L mod 2²⁰", "Process q full 1 MiB stripes, then a tail only when t>0.", "MRT-X: STRIPE_SIZE = 4 × 256 KiB; chunks(STRIPE_SIZE)"),
    LessonFormula("RedTail-X v1.1", "3 §4.1 Eq. 2", "Variable tail rule", "s = ⌈t/4⌉,   coded_tail = 6s", "Use six short arrays for the final partial stripe. The 1 MiB limit is a processing choice; the rule acts on the array tail.", "MRT-X: share_len = original_len.div_ceil(4); C++: (len+3)/4"),
    LessonFormula("RedTail-X v1.1", "3 §4.1", "Tail padding bound", "6⌈t/4⌉ ≤ 1.5t + 4.5 bytes", "At most three zero data bytes are introduced by rounding the tail share length.", "MRT-X: last data share is zero padded, then output is truncated to original_len"),
    LessonFormula("RedTail-X v1.1", "3 §4.2", "Decoder length invariant", "share_len = ⌈original_len/4⌉", "The manifest must remember original_len because up to four tail sizes share one rounded s.", "MRT-X: StripeManifest::validate()"),
    LessonFormula("RedTail-X v1.1", "3 §4.2", "Reconstruction invariant", "valid shares ≥ 4; truncate to original_len; hash(rebuilt)=original_hash", "Hash each share, mark bad ones as erasures, solve from four, trim padding, then verify original bytes.", "MRT-X: reconstruct() and SHA-256 checks"),
    LessonFormula("RedTail-X v1.1", "4 §5.2 Table 1", "Measured ratio", "1,218,969,600 → 1,000,400,844 bytes; reduction 17.93%", "The 213 PDF corpus measured less zero-fill storage. This is one corpus on one M1 volume.", "Benchmark result, not a universal guarantee", "Measured result"),
    LessonFormula("RedTail-X v1.1", "5 §5.4", "Compression gate", "attempt zstd if H₈(sample)<6.4 or periodicity is strong", "An 8 KiB start sample of each 256 KiB chunk chose whether to try compression; the smaller verified output was kept.", "Paper pipeline only; MRT-X copier does not compress files", "Measured policy"),
    LessonFormula("RedTail-X v1.1", "5 §5.5", "Repair helper path", "choose latency-weighted shortest path", "A simulation selected helpers across network links; no network repair was run.", "No corresponding MRT-X network path", "Simulation"),
    LessonFormula("RedTail-X v1.1", "6 §6", "Polynomial canonical form", "P(x)=Σₑ cₑxᵉ; discard |cₑ|<10⁻⁹", "Combining and sorting terms preserves polynomial value but loses byte order. The paper excluded it as file storage.", "Do not use for byte-exact restore", "Excluded formulation"),
    LessonFormula("RedTail-X v1.1", "6 §6", "N*Tropy", "Hₙ = H₈ / log₂ N", "The corpus-normalized score moved when corpus size changed even though byte entropy barely changed.", "Excluded from compression gate", "Excluded formulation"),
    LessonFormula("RedTail-X v1.1", "6 §6", "Clamped estimator", "p̃σ = clamp(pσ, ε, 1−ε)", "Clamping all 256 frequencies breaks Σp=1 unless they are renormalized. The score can exceed 8.", "Use 0·log(0)=0 or renormalize", "Excluded formulation"),
    LessonFormula("RedTail-X v1.1", "6 §6", "Logit edge case", "logit(p)=ln(p/(1−p))", "The expression is undefined at p=0 and p=1.", "Guard endpoints if studying it; not used in MRT-X", "Excluded formulation"),
    LessonFormula("RedTail-X v1.1", "6 §6", "Shortest form hunt", "select shortest byte-length equivalent form + control", "This resembles your F(0), F(1), F(2) exercise. It ran faster but stored more bytes and skipped zstd verification; the paper excluded it.", "VQTD F(i) scene is a classroom model, not active MRT-X encoding", "Excluded formulation"),
    LessonFormula("RedTail-X v1.1", "7 §7", "Overlapping loss limit", "lost shares > 2 in one stripe ⇒ stop repair", "A final audit catches residual damage; a third overlapping loss cannot be solved by 4+2.", "MRT-X: reconstruct rejects more than two lost shares"),

    // DarkRock CAD: mostly accounting definitions and measured comparisons, not a new codec equation.
    LessonFormula("DarkRock CAD v1", "3 §3.1", "Exact restore invariant", "restore(stored representation) = original bytes", "A geometry key may suggest a match, but only verified literal edits may reconstruct the original file.", "A3 candidate lookup then byte comparison; C2 verified patch"),
    LessonFormula("DarkRock CAD v1", "3 §3.1", "Storage arms", "A1=fixed chunks; A2=exact normalization; A3=geometry key; C1=FastCDC; C2=delta", "Compare five storage representations under the same RedTail-X 4+2 protection layer.", "Named benchmark arms; no corresponding VQTD file processing"),
    LessonFormula("DarkRock CAD v1", "4 §4.1", "Protected byte ledger", "protected bytes = payload + edits + refs + manifests + catalog", "Count everything required to restore files after deleting derived indexes.", "A derived index qualifies only after delete/rebuild/verify test"),
    LessonFormula("DarkRock CAD v1", "5 §5.2 Table 2", "Held-out reversal", "A3 vs A1: −9.7% development → +8.1% held-out", "A positive development result reversed on the 132-file held-out set. A3 was rejected as a storage format.", "Do not teach geometry-key canonicalization as an accepted codec", "Measured negative result"),
    LessonFormula("DarkRock CAD v1", "5 §5.4 Table 3", "Base selection gain", "C2-key vs C2-name: 18,169,422 vs 18,507,450 bytes", "The geometry key helped choose a delta base while exact patch verification preserved bytes; gain was 1.8% on 47 real drawings.", "Key ranks candidates; patch verification decides acceptance", "Measured narrow gain"),
    LessonFormula("DarkRock CAD v1", "4 §4.3", "Erasure test matrix", "6 single losses + 15 distinct pairs = 21 patterns", "Every arm was byte checked across all one- and two-share losses in 4+2.", "Combinations C(6,1)+C(6,2)"),

    // BRMR: object-level recovery is a separate layer above byte-exact storage.
    LessonFormula("BRMR 2026", "3 §3", "Byte-line decomposition", "X = ℓ₀ || '\\n' || … || ℓₘ₋₁; |X|=Σ|ℓⱼ|+(m−1)", "Treat the DXF as exact bytes split into lines. Retain carriage returns during owner-line replacement.", "Scanner tracks line positions and CRLF"),
    LessonFormula("BRMR 2026", "3 §3", "Owner-line index", "λ(o) = index of o's owner line; owner(o) = ℓ at λ(o)", "λ(o) points at the line holding the owner handle: the first 330 code outside a 102 {…} group. Not the same as your reserved λ₁…λ₄.", "Scanner records ownerLineIndex per object"),
    LessonFormula("BRMR 2026", "3 §3", "Handle universe", "H(X)={h(o):o∈X}; Ω(o)=H∪{⊥}", "Every known handle is a possible owner; ⊥ means abstain.", "Build handle index; Option<Handle> for proposals"),
    LessonFormula("BRMR 2026", "3 §4.1", "Detect owner holes", "D(E)={o∈OBJECTS(E): owner(o)∈{0,∅} or owner(o)∉H(E)}", "Find missing or invalid owner fields before proposing a repair.", "Detector reads only error file E"),
    LessonFormula("BRMR 2026", "4 §4.2", "Mirror identity", "owner(o)=p ⇔ (350|360,h(o))∈ref(p)", "The parent lists its child even when the child's owner field was lost.", "Reverse index from parent references to child handles"),
    LessonFormula("BRMR 2026", "4 §4.2", "Unique parent set", "S(o)={p∈H:(350|360,h(o))∈ref(p)}; |S(o)|=1 ⇒ answer", "Accept exactly one mirrored parent. Zero or several means abstain.", "R2u: singleOrNull()"),
    LessonFormula("BRMR 2026", "4 §4.3", "Neighbor witness R1", "R1(o)=owner(oⱼ), j=argmin |i−j| among same-type valid objects", "The nearest object's owner is only a guess; the paper keeps R1 diagnostic and out of the accepting set.", "R1 diagnostic only"),
    LessonFormula("BRMR 2026", "4 §4.3", "Mirror and reactor witnesses", "R2: back-reference; R3: first valid reactor; R2u=p iff S(o)={p}", "R2 reads a reverse edge; R3 is a partial second witness; R2u removes the unsafe tie break.", "Rebuilder returns candidate or ⊥"),
    LessonFormula("BRMR 2026", "4 §4.4", "Best of three", "B(o)=majority value if count≥2, else firstπ(R2,R3,R1)", "A control arm combined three proposals, then fell back by priority when none agreed.", "π = R2 ≻ R3 ≻ R1", "Evaluated control"),
    LessonFormula("BRMR 2026", "4 §4.4", "Strict witness pair", "Sᵢⱼ(o)=Rᵢ(o) if Rᵢ=Rⱼ≠⊥; else ⊥", "Two witnesses answer only on agreement. Disagreement costs coverage but avoids ungrounded guesses.", "if (a != null && a == b) a else null"),
    LessonFormula("BRMR 2026", "4 §4.4", "Fallback pair", "Fᵢⱼ(o)=Sᵢⱼ(o) if answered; else first priority answer", "Fallback increases coverage and can inherit the higher priority witness's mistakes.", "Pair agreement followed by priority fallback", "Evaluated control"),
    LessonFormula("BRMR 2026", "5 §4.4", "Witness ladder", "L(o): agree R2u/R3; else R2u+type gate; else R3+type gate; else ⊥", "Ask more witnesses only when needed. The type gate did not catch same-type forged mirrors.", "Branch by agreement, absence, type gate, then abstain"),
    LessonFormula("BRMR 2026", "5 §4.4", "Owner Merkle commitment", "leaf(c,p)=H(00||len(c)||c||len(p)||p); node(l,r)=H(01||l||r)", "A pre-ingest owner map root can catch later changes. Domain prefixes distinguish leaves from nodes.", "owner_root; leaf hash; node hash; inclusion proof"),
    LessonFormula("BRMR 2026", "5 §4.4", "Inclusion proof", "verify(root,leaf(o,p),π)∈{0,1}; proof size O(log n)", "Sibling hashes and directions prove an owner row was committed before damage.", "Merkle verify(root, leaf, proof)"),
    LessonFormula("BRMR 2026", "5 §4.5", "Sidecar view invariant", "X̂_A=E[owner lines ← answers]; STORE=E; RESTORE(STORE)=E", "The repaired view changes accepted owner fields, while stored converter bytes remain exactly E.", "Sidecar overlays owner lines; storage hash stays unchanged"),
    LessonFormula("BRMR 2026", "6 §5.1", "Sealed error injection", "E=ERRₛ(X), owner line ← '0'||CR; |X|−|E|=Σ(|owner|−1)", "A seeded black box hides true owners until the frozen rebuilder has produced its answers.", "Replace only selected owner value lines; compare after unseal"),
    LessonFormula("BRMR 2026", "6 §5.3", "Scores", "C=Σ1[A=T]; W=Σ1[A∉{T,⊥}]; Ab=N−C−W", "Count correct answers, wrong answers, and abstentions separately.", "Per-field scored result enum"),
    LessonFormula("BRMR 2026", "6 §5.3", "Precision and coverage", "precision=C/(C+W); coverage=(C+W)/N", "Precision asks whether answered fields were correct; coverage asks how often the rule answered.", "Guard zero denominator in precision"),
    LessonFormula("BRMR 2026", "6 §5.3", "Whole-file exactness", "SHA256(X̂_A)=SHA256(X) ⇔ all injected owners correct", "Because the injection touched only owner lines, a file hash match tests the whole restored file.", "Compare rebuilt and sealed file digests after unseal"),
    LessonFormula("BRMR 2026", "9 §7.1", "Strict pair error bound", "W(Sᵢⱼ)≤#{o:Rᵢ(o)=Rⱼ(o) wrong and non-⊥}", "A strict pair can be wrong only when its two witnesses agree on the same wrong value.", "Count coincident wrong proposals", "Lemma"),
    LessonFormula("BRMR 2026", "9 §7.1", "Strict coverage identity", "C(S₂ⱼ)=C(Rⱼ) when R2 is always correct", "On the tested injection, strict pair coverage equals the other witness's agreement rate.", "R2/R3 agreement counted per field", "Conditional lemma"),
    LessonFormula("BRMR 2026", "9 §7.1", "Fallback collapse", "Ab(R2)=0 ⇒ F12=F23=R2", "If top priority always answers, fallback returns it whenever the pair has no answer.", "Priority behavior under stated test condition", "Conditional lemma"),
    LessonFormula("BRMR 2026", "9 §7.1", "Mirror-free pair risk", "W(F13)=#{o:R3=⊥ and R1 wrong}=7", "Without the back-reference witness, fallback inherits neighbor guesses where reactors are absent.", "Do not promote R1 to acceptance", "Measured lemma"),
    LessonFormula("BRMR 2026", "9 §7.3", "Check strength", "pass(o)={p∈Ω(o):check(p)}; accept when |pass(o)|=1", "A check that permits thousands of candidates is weak. A unique reverse-edge check can decide.", "Candidate filter then cardinality check"),
    LessonFormula("BRMR 2026", "10 §7.4", "Witness majority threshold", "n≥2f+1", "A majority needs three independent witnesses to outvote one faulty witness; two only detect disagreement.", "Select odd witness count; independence still matters"),
    LessonFormula("BRMR 2026", "10 §7.6", "Rule-of-three uncertainty", "p̂₉₅≈3/n: 3/555≈0.54%; 3/36≈8.3%", "Zero observed errors does not imply zero risk. Correlated fields make the file-level bound more honest.", "Report both field and file denominators", "Approximate bound"),
    LessonFormula("BRMR 2026", "8 §6.8", "Forged-mirror result", "priority 59 wrong; ladder 19 wrong; ladder+parity 0 wrong", "A second witness reduced errors; the pre-ingest commitment resolved the remaining forged cases in this test.", "Measured seeded box, not a universal rate", "Measured result")
)
