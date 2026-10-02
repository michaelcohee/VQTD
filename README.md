# VQTD — Visual Quasi-Thermodynamics

**Software:** [![DOI](https://zenodo.org/badge/1402391643.svg)](https://doi.org/10.5281/zenodo.23111776) VQTD (this repository) · [![DOI](https://zenodo.org/badge/1391584742.svg)](https://doi.org/10.5281/zenodo.23111770) [DarkRock](https://github.com/michaelcohee/DarkRock) code

**Reports:**

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.23060238.svg)](https://doi.org/10.5281/zenodo.23060238) RedTail-X: Variable-Length Final Shares for 4+2 Reed–Solomon Erasure-Coded Storage

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.23078388.svg)](https://doi.org/10.5281/zenodo.23078388) Exact Family Patching versus Geometry-Key Canonicalization for Deduplicating DXF Drawings: A Pre-Registered Evaluation

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.23103307.svg)](https://doi.org/10.5281/zenodo.23103307) Back-Reference Mirror Recovery (BRMR): Field-Level Repair of Converter-Lost Owner Handles in DXF

Author: Michael Cohee · [![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE) · [![ORCID](https://img.shields.io/badge/ORCID-0009--0005--1502--5125-A6CE39?logo=orcid&logoColor=white)](https://orcid.org/0009-0005-1502-5125)

VQTD is an offline Kotlin/Swing classroom app for tracing equations from the
three papers in `Papers/` into plain explanations and code associations. Its
visual map also demonstrates the proposed shortest-form formula hunt.

It does not read user files, inspect disks, connect to a network, or alter an
MRT-X archive. The first version is a teaching model built around:

```text
(2 + 2 = 4)   F(0)
(14 - 10 = 4) F(1)
(8 - 4 = 4)   F(2)
```

All three functions reach the same target. The visual exercise encodes each
candidate, compares its bit cost, selects the shortest valid formula, and marks
longer descriptions as discarded. These F(i) examples are supplied for the
class; the RedTail-X report tested and excluded a shortest-form storage rule.

![VQTD overview: sixteen views of the classroom app](docs/screenshots/00-overview.png)

## Screenshots

| | |
|---|---|
| ![Reed–Solomon 4+2 in 2D](docs/screenshots/01-reed-solomon-2d.png) **Reed–Solomon 4+2**: generator, two erasures, four survivors | ![Reed–Solomon in 3D](docs/screenshots/02-reed-solomon-3d.png) **The same lesson in 3D**: drag to rotate, click a node to inspect |
| ![GF(2⁸) by hand](docs/screenshots/04-gf-by-hand.png) **GF(2⁸) by Hand**: why 52·04 = 55, one bit at a time | ![GF(2⁸) by hand in 3D](docs/screenshots/05-gf-by-hand-3d.png) **One depth tier per multiplication step** |
| ![Row reduction](docs/screenshots/06-row-reduction.png) **Row Reduction**: Gauss–Jordan with a forced row swap | ![Row reduction in 3D](docs/screenshots/07-row-reduction-3d.png) **One plane per pivot column**; data recovered as [04,00,00,00] |
| ![Entropy](docs/screenshots/08-entropy.png) **Entropy H₈**: three samples, the 6.4 gate, and the thermodynamics boundary | ![Layer Bridge](docs/screenshots/03-layer-bridge.png) **Layer Bridge**: RedTail-X hunt, then the F(i) value as one data byte |
| ![Merkle tree in 3D](docs/screenshots/09-merkle-3d.png) **BRMR owner Merkle tree** in 3D | ![RedTail-X variable tail](docs/screenshots/10-redtail-x.png) **RedTail-X**: 7-byte tail and two-loss repair |
| ![Learning Lab](docs/screenshots/11-learning-lab.png) **Learning Lab**: predict, reveal, explain, revisit | ![Paper Math](docs/screenshots/12-paper-math.png) **Paper Math**: page-cited formula catalog |
| ![Code Bridge calculator](docs/screenshots/13-code-bridge-calculator.png) **Code Bridge**: runnable GF(2⁸) calculator with full trace | ![Glossary](docs/screenshots/14-glossary.png) **Glossary & Symbols**: symbol, term, formal, plain, code |
| ![Matrix 3D, dark green theme](docs/screenshots/15-matrix-3d-dark-green.png) **Generator stack** across 40 byte positions (Dark Green theme) | ![File scope 3D, dark green](docs/screenshots/16-file-scope-3d-dark-green.png) **Proposed file-share Merkle scope** (Dark Green theme) |

Screenshots are produced by the app itself: `xvfb-run java -jar build/vqtd.jar --screenshots`
(or `java -jar build/vqtd.jar --screenshots` on a desktop) rewrites `docs/screenshots/`.

## Quick start

```sh
./build.sh                                            # needs kotlinc and a JDK 11+
java -jar build/vqtd.jar                              # open the classroom
java -Djava.awt.headless=true -jar build/vqtd.jar --self-test
```

## Teaching bit-cost model

This application deliberately exposes its small encoding model:

```text
cost(F) = opcode bits + width(left operand) + width(right operand)
opcode bits = 2
width(n) = max(1, floor(log2(n)) + 1), for n >= 0
```

Therefore:

```text
F(0): ADD 2 2   = 2 + 2 + 2 =  6 bits  ← selected
F(1): SUB 14 10 = 2 + 4 + 4 = 10 bits
F(2): SUB 8 4   = 2 + 4 + 3 =  9 bits
```

This is an illustrative classroom encoding, not a RedTail-X wire format or an
active MRT-X storage rule.

## Build once with the local Kotlin compiler

```sh
cd VQTD
chmod +x build.sh
./build.sh
java -jar build/vqtd.jar
```

The app uses Kotlin and Java Swing only. No dependency download is required for
the `build.sh` path.

## Classroom views

- **Hunt Map:** seven selectable animated slides: F(i) formula exercise,
  Vandermonde generator, BRMR Merkle owner-map root, RedTail-X variable
  tail repair, **File Scope**, **Reed–Solomon**, and **Layer Bridge**, which places the formulas beside the
  matrix in both 2D and 3D. The bridge first follows RedTail-X's actual
  storage hunt: locate the stripe from the manifest, reject two erasures by
  share-hash checks, then retain four verified survivors. The F(i) classroom
  exercise follows that hunt. All three formulas evaluate to 4;
  the selected numerical result is used as one illustrative input byte in
  `d[t]=[04,00,00,00]ᵀ`, producing `c[t]=Gd[t]`. The formulas are not matrix
  coefficients or an accepted storage format. Each bridge view has its own
  stage and layer position.
  Every slide has **2D View** and **3D View**. Reset, Next, Play,
  and Pause work on the current slide and mode. Drag rotates a 3D scene, the
  wheel zooms, and clicking a node reveals its meaning. The
  F(i) exercise includes the later concepts
  of manifest coordinates, Merkle proof, and matrix repair. The latter stages
  are conceptual connections, not a claim that the papers implement a Merkle
  commitment to F(i). The matrix's 3D scene is a rotatable 6×4 generator
  lattice repeated across 40 byte positions. Play scans its slices; Next
  advances one slice. Its depth represents byte positions, not an extra
  mathematical dimension of the matrix. The other 3D scenes use depth for
  routes, tree tiers, and the storage/repair pipeline; those axes are visual
  explanations, not new equations. The displayed matrix
  GF(2⁸) coefficients are a classroom construction using evaluation points
  1–6 and polynomial `0x11d`, not a claim about the exact crate coefficients.
  File Scope shows two illustrative stripes, six coded shares per stripe,
  share and metadata hash leaves, stripe roots, and a file root. This Merkle
  layer is a **proposed teaching composition** over RedTail-X records. The
  published RedTail-X uses share and stripe hashes; BRMR's published Merkle
  root commits an owner map. A Merkle node binds descendant content to a
  digest; it does not hold enough bytes to reconstruct a lost file. The
  Reed–Solomon slide traces `G = V · Vtop⁻¹`, six coded output rows, two
  erasures, selection of four surviving rows, inversion, and hash verification.
  The numbered λ₁…λ₄ proofs mentioned in conversation are pending their
  definitions; the supplied PDFs do not define those four proof statements.
- **Math Foundations (second row of Hunt Map buttons):** three slides that
  teach the arithmetic the paper slides rely on. Every value is computed at
  runtime from the same `Gf256` code, and each slide has **2D View** and
  **3D View**.
  - **GF(2⁸) by Hand:** why `52·04 = 55`. A byte is a polynomial; ×04 is two
    left shifts; the second shift overflows bit 8, and XOR with the field
    polynomial `0x11d` folds it back. `F7·04` overflows twice → `F3` → `FB`,
    reproducing the Layer Bridge parity bytes. The 3D view stacks one tier of
    bit nodes per step.
  - **Row Reduction:** Gauss–Jordan on `[A | I]` where A is generator rows
    0, 2, 3, 5 (shares 1 and 4 lost). It shows the row swaps a zero pivot
    forces, scaling by a field inverse, and XOR elimination, then
    `d = A⁻¹·[04,00,00,FB] = [04,00,00,00]`. The 3D view puts one Gauss–Jordan
    column per depth plane; older planes fade.
  - **Entropy H₈:** three 8 KiB samples (constant 0.00, English text ≈ 4.19,
    pseudo-random ≈ 7.98 bits/byte), the RedTail-X 6.4 gate, and the link
    `S = (k_B ln 2)·H` to Gibbs/Boltzmann entropy. It closes on the boundary
    that gives the app its "quasi": same mathematical form, but bytes have no
    temperature, and H₈ never decides recovery.
- **Learning Lab:** six paper-backed predict, reveal, explain, and revisit
  cards. It records a confidence choice before feedback, compares correctness
  with confidence, and preserves written explanations during this app session.
  It has no timer, telemetry, or automated grading of prose.
- **Paper Math:** searchable, page-cited formula catalog across RedTail-X,
  DarkRock CAD, and BRMR, with a plain explanation, code association, and
  status such as reported, measured, simulated, or excluded.
- **Binary Lens:** shows the invented 2-bit opcode exercise and its bit costs.
  **3D View** lays each candidate's bits out in depth (opcode, left operand,
  right operand), then the totals and the arg min.
- **Glossary & Symbols:** formal meaning, everyday explanation, and code link.
- **Lecture Board:** three-paper course outline, worked example, class
  questions, and evidence boundaries.
- **Code Bridge:** maps paper operations to implementation roles and includes
  a runnable **GF(2⁸) calculator**: multiply (every shift, overflow, and XOR
  printed), add (XOR), and inverse (`a²⁵⁴`), next to the multiply source code.
- **Paper Notes:** source map and open questions.

The top bar offers **Beige**, **Dark Green**, and a separate **High Contrast**
toggle. High contrast works with either theme.

## Professor glossary

The **Glossary & Symbols** tab is searchable and gives every entry five linked
views:

1. Mathematical symbol
2. Professor-level term
3. Formal meaning
4. Plain-language bridge
5. Kotlin/C++ code association

It includes `F(i)`, `arg min`, encoded length `ℓ`, quantifiers, ceiling,
`GF(2⁸)`, XOR, transpose, BRMR abstention `⊥`, Vandermonde and inverse matrices,
systematic generators, hashes, concatenation, Merkle leaves and proofs,
coordinates, invariants, manifests, domain separation, `ℵ`, `∞`, and flagged
`X`. Paper-specific symbols without a confirmed definition are labeled as
unresolved rather than assigned a guessed meaning.

The **Lecture Board** tab presents learning objectives, a worked derivation,
established mathematics, current project interpretations, unresolved notation,
and class discussion questions.

The three paper roles are:

```text
RedTail-X: 4+2 arrays recover exact stored bytes using the variable tail.
DarkRock CAD: candidate geometry keys may choose a delta base; exact restore wins.
BRMR: unique reverse references repair owner fields in a separate view.
```

## Paper and teaching boundary

See [PAPERS.md](PAPERS.md) for printed page locations and the equations behind
the lessons. The paper PDFs are source material and remain unchanged. `ℵ` and
an infinity-flagged `X` are not defined by these PDFs. BRMR uses `X` for the
sealed original file and `⊥` for abstention.

See [PSYCHOLOGY.md](PSYCHOLOGY.md) for the Learning Lab's rationale, research
sources, and the possible later bridge to neuromorphic work.

For standalone preview PNGs of the rendered diagrams, run:

```sh
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-matrix
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-slides
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-3d-slides
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-layer-bridge
java -Djava.awt.headless=true -jar build/vqtd.jar --check-layer-bridge
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-lab
java -Djava.awt.headless=true -jar build/vqtd.jar --check-learning-lab
java -Djava.awt.headless=true -jar build/vqtd.jar --preview-math
java -Djava.awt.headless=true -jar build/vqtd.jar --check-math
java -Djava.awt.headless=true -jar build/vqtd.jar --self-test
java -jar build/vqtd.jar --preview-app   # needs a display; clicks through the real window
```

`--check-math` verifies 52·04=55 and F7·04=FB, all 65,536 traced products
against `Gf256.multiply`, all 255 inverses, the row reduction against
`Gf256.invert4`, all 15 survivor sets, the entropy sample values, and that
every 3D scene has one note per stage and no dangling edges. `--self-test`
runs it together with the Layer Bridge and Learning Lab checks.

## Code layout

- `Main.kt`: window, tabs, theme, Hunt Map slide registry (`buildSlides()`),
  `Gf256`, and the preview/check entry points. Adding a Hunt Map slide is one
  `Slide` entry: a 2D player, a 3D player, and their step/reset/tick hooks.
- `MathLessons.kt`: traced GF(2⁸) arithmetic, row reduction, entropy samples,
  their 2D slides, and the 3D scenes for the math foundations and Binary Lens.
- `Spatial3D.kt`: the shared projected-3D canvas and the paper scenes.
- `LayerBridge.kt`, `Lessons.kt` (Paper Math catalog), `Psychology.kt`
  (Learning Lab).

## License

Code: [Apache License 2.0](LICENSE). Copyright 2026 Michael Cohee. Anyone
redistributing VQTD or a derivative must keep the attribution in
[NOTICE](NOTICE). The PDFs in `Papers/` are not covered by this license; each
keeps the license on its Zenodo record.

## Citing

See [CITATION.cff](CITATION.cff). The three source reports are cited there
by DOI; VQTD is a teaching companion to them, not a substitute.
