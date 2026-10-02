# VQTD paper guide for the class

This guide was checked against the three PDFs in `Papers/`. Page numbers below
are the printed numbers on the pages. The app's **Paper Math** tab gives each
formula a source location, plain explanation, code association, and status.

## 1. RedTail-X v1.1

Source: `Papers/RedTail-X_Technical_Report_v1.1.pdf`, DOI
`10.5281/zenodo.23060238`.

### Coding algebra, printed page 2

```text
𝔽 = GF(2⁸)
d ∈ 𝔽⁴
c = Gd
G = [I₄; C] ∈ 𝔽⁶ˣ⁴
n/k = 6/4 = 1.5
```

Teach four byte symbols becoming six shares. The first four generator rows
leave data visible; the final two generate parity. Any four of the six rows
must be invertible for two erasures to be repairable. The paper states the
evaluated implementation used the `reed-solomon-erasure` crate's systematic
Vandermonde construction. The exact `V[r,c]=r^c` expression used in the C++
port comes from that implementation, not as a displayed formula in the report.

The page also mentions a separately tested RAID-6 style construction:

```text
P = ⊕ᵢ dᵢ
Q = ⊕ᵢ gⁱdᵢ
```

That construction was not selected for production (§5.3).

### Classroom Reed–Solomon and file-scope views

The app's Reed–Solomon slide expands the 4+2 algebra above into a concrete
`G = V · Vtop⁻¹` teaching construction, two erasures, four selected surviving
rows, and a 4×4 inverse. Its displayed coefficients come from the app's
GF(2⁸) example; they are not claimed to match the crate's internal matrix.
The File Scope slide draws a proposed Merkle tree above share and metadata
hashes for two example stripes. This file-share tree is **not** an implemented
or published RedTail-X feature. It is also distinct from BRMR's published
Merkle root over DXF owner rows. Hashes can detect a mismatch and commit to
subtrees; Reed–Solomon share bytes enable reconstruction. The PDFs supplied
here do not define four numbered λ₁…λ₄ proofs, so the app reserves those
labels without attributing statements to them.

### Entropy gate, printed pages 2 and 5

```text
H₈(x) = −Σσ p(σ) log₂ p(σ),  0 ≤ H₈ ≤ 8
0 · log₂(0) := 0
```

This measured whether to attempt compression. In the reported policy, one
8 KiB start sample per 256 KiB chunk triggered zstd level 3 when entropy was
below 6.4 bits per byte or the sample had strong periodicity. The smaller
verified representation was kept. Entropy is separate from erasure recovery.

### Variable final shares, printed page 3

```text
q = ⌊L / 2²⁰⌋
t = L mod 2²⁰
s = ⌈t/4⌉
coded tail bytes = 6s ≤ 1.5t + 4.5
```

For a full stripe, shares are 256 KiB each. For a partial final stripe, six
shares are sized to `s`; at most three data bytes are zero-filled in total.
The manifest stores `original_len`, `share_len`, the stripe hash, and six
share hashes. Reconstruction checks the length and hashes, uses four valid
shares, truncates to `original_len`, then verifies original bytes.

The 1 MiB stripe size was a chosen implementation parameter. RedTail-X's
contribution is the final-share layout rule, not a new Reed–Solomon code.

### Measurements and excluded proposals, printed pages 4–6

On 213 PDF files (666,933,563 input bytes), emitted bytes changed from
1,218,969,600 at fixed share size to 1,000,400,844 with variable tails, a
17.93% reduction in this corpus. The report notes single-pass timings and
one-volume limitations.

Section 6 reports four excluded formulations:

| Exploration | Formula or rule | Why excluded |
|---|---|---|
| Polynomial canonicalization | `P(x)=Σ cₑxᵉ`, drop `|cₑ|<10⁻⁹` | Preserved polynomial value while risking loss of original byte order |
| N*Tropy | `Hₙ=H₈/log₂N` | Score shifted with corpus size despite nearly unchanged byte entropy |
| Clamped entropy | `p̃σ=clamp(pσ,ε,1−ε)` | Clamped frequencies no longer summed to one without renormalization |
| Shortest form | Keep control plus shortest equivalent byte representation | Measured more stored bytes and skipped zstd verification |

The paper also notes `logit(p)=ln(p/(1−p))` is undefined at `p=0` or `1`.

## 2. DarkRock CAD v1

Source: `Papers/DarkRock_CAD_Technical_Report_v1.pdf`, DOI
`10.5281/zenodo.23078389`.

The report compares storage representations for byte-exact DXF restore. Its
equations are mostly operational invariants and measured ratios rather than a
new coding matrix.

```text
restore(stored representation) = original bytes
protected bytes = payload + edits + refs + manifests + catalog
single/two-share loss patterns = C(6,1) + C(6,2) = 21
```

The geometry key can propose a candidate, but literal stored bytes or a
verified patch decide exactness. A3 used 9.7% fewer protected bytes than A1
on 44 development files, then 8.1% more on 132 held-out files. The report
rejects A3 as a storage format. Using the key only to choose a C2 delta base
saved 1.8% versus filename-family selection on 47 real drawings. This is a
narrow measured result with unmeasured key computation cost.

## 3. BRMR

Source:
`Papers/BRMR-Field-Level-Repair-of-Converter-Lost-Owner-Handles-in-DXF.pdf`,
DOI `10.5281/zenodo.23103307`.

### Exact bytes and candidate space, printed page 3

```text
X = ℓ₀ || '\n' || ℓ₁ || ... || ℓₘ₋₁
|X| = Σⱼ |ℓⱼ| + (m−1)
H(X) = {h(o) : o ∈ X}
Ω(o) = H ∪ {⊥}
D(E) = {o ∈ OBJECTS(E) : owner(o) ∈ {0,∅} or owner(o) ∉ H(E)}
```

Here `⊥` explicitly means **abstain**. `X` is the sealed original and `E` is
the converter output with selected owner fields lost. BRMR works from `E`.

### Reverse-edge witnesses, printed page 4

```text
owner(o)=p ⇔ (350|360,h(o)) ∈ ref(p)
S(o) = {p ∈ H : (350|360,h(o)) ∈ ref(p)}
|S(o)|=1 ⇒ unique recovery
R2u(o) = p if S(o)={p}; otherwise ⊥
```

The paper also defines R1 as a nearest valid same-type neighbor, R2 as a
back-reference, and R3 as a reactor. R1 is diagnostic only, since a neighbor
need not share the correct owner. A strict pair answers only when two witnesses
agree; a fallback pair uses priority if they do not. The ladder on page 5
escalates when witnesses disagree and otherwise abstains.

### Owner-line index λ(o), printed page 3

BRMR defines `λ(o)` as the index of object o's owner line (the first code-330
value outside a `102 {…}` group), and `owner(o)` as the handle on that line.
This is the only λ in the three papers. The numbered λ₁…λ₄ proofs proposed for
VQTD are a separate, still-undefined set of labels.

### Owner commitment and separate view, printed page 5

```text
leaf(c,p) = H(00 || |c| || c || |p| || p)
node(l,r) = H(01 || l || r)
verify(root,leaf(o,p),π) ∈ {0,1}
proof size = O(log n)
STORE = E
RESTORE(STORE) = E
```

The owner-map root commits to what was stored at ingest. It can detect later
damage, including forged owner changes, if the commitment itself is trusted.
It cannot fix the converter's mistake that existed before ingest. Accepted
owner changes are rendered in a separate sidecar view `X̂`; they do not alter
the stored converter bytes.

### Scoring and uncertainty, printed pages 6, 9–10

```text
C = Σ 1[A(o)=T(o)]
W = Σ 1[A(o)∉{T(o),⊥}]
Ab = N−C−W
precision = C/(C+W)
coverage = (C+W)/N
pass(o) = {p∈Ω(o): check(p)}
n ≥ 2f+1
p̂₉₅ ≈ 3/n
```

These distinguish an incorrect answer from an abstention, require a strong
uniqueness check, describe the number of witnesses needed to outvote faults,
and give an approximate upper error-rate bound after zero observed errors.
The report warns that 555 fields are correlated within 36 files; the per-file
`3/36 ≈ 8.3%` figure is more honest than treating all fields as independent.

## Classroom F(i) exercise

The three expressions supplied for VQTD are:

```text
F(0) = 2 + 2 = 4
F(1) = 14 − 10 = 4
F(2) = 8 − 4 = 4
```

VQTD's `2 opcode bits + operand widths` rule is an invented classroom encoding
that teaches `arg min`. These examples do not appear as such in the PDFs. The
shortest-form storage proposal in RedTail-X §6 was tested and excluded. There
is no established Merkle commitment to the winning F(i) in these reports.

The app's **Layer Bridge** composes two separate lessons: evaluate and select
one F(i) description of the number 4, then place that numerical value in a
single example data-byte position `d[t]=[04,00,00,00]ᵀ`. The systematic
generator computes `c[t]=Gd[t]` for that byte column. This is a classroom
composition, not a claim that RedTail-X encodes shortest-form expressions or
that the F(i) formulas are Vandermonde coefficients.

The bridge now begins with the RedTail-X recovery hunt: read the stripe's
manifest lengths and coordinate, mark shares 1 and 4 as erasures after hash
checks in the worked example, and use verified shares 0, 2, 3, and 5. Those
four generator rows can reconstruct the example data column. The F(i) display
starts only after that paper-backed share selection.

`ℵ` and an infinity-flagged `X` are not defined by the supplied three papers.
BRMR's `X` denotes the sealed original file, with `X̂` for a rebuilt view.

## Math foundations taught in the app

These slides teach established mathematics used by the papers; they are not
claims made by the papers.

- **GF(2⁸) arithmetic.** Addition is XOR. Multiplying by `x` (0x02) is a left
  shift followed by XOR with `0x11d` when bit 8 is set. In the classroom
  generator, `52·04 = 55` and `F7·04 = FB`, giving
  `c = Gd = [04,00,00,00,55,FB]ᵀ` for `d = [04,00,00,00]ᵀ`.
- **Inversion by row reduction.** `[A | I] → [I | A⁻¹]` for survivor rows
  0, 2, 3, 5; `A⁻¹·[04,00,00,FB]ᵀ = [04,00,00,00]ᵀ`. All 15 four-row subsets of
  the 6×4 generator are invertible (the MDS property behind "any four of six").
- **Entropy.** `H₈` (RedTail-X p. 2, Eq. 1) has the same form as Gibbs entropy
  `S = −k_B Σ p ln p`; `S = (k_B ln 2)·H`. The analogy is in the mathematics only.
  RedTail-X uses H₈ as a compression gate (p. 5), never for recovery.
