# SmartDock – Cost Analysis

Costing for the SmartDock prototype described in `ELDROID_NULL POINT_Paper.docx`,
sized for a **4-box demo** with an RFID tag on every piece of equipment and
boxes built as **separate modules that stack**.

> **Prices are estimates, not quotations.** They reflect typical Philippine
> hobby-electronics retail (Shopee / Lazada / Makerlab / e-Gizmo) in PHP and
> must be confirmed against live listings before this table goes in the paper.
> Every row is marked ☐ until someone has checked it against a real cart.

---

## 0. Design assumption behind this BOM

The paper (§3.1) identifies equipment by an **RFID tag scanned at the reader**.
`ESP32_MVP_CHECKLIST.md` §1 identifies it by **box position**, using one IR
break-beam sensor per box. These are different machines and they cost different
amounts.

This BOM prices the **hybrid**, which is what the demo needs:

- **One MFRC522 reader** – reads the borrower's card at tap-in.
- **One IR break-beam per box** – detects the item leaving or returning, which
  is the only way to satisfy **FR-04 (unauthorized removal)**; a tag-scan-only
  design cannot tell that an item walked off.
- **One MIFARE tag per equipment item** – keeps the paper's per-equipment
  identity and gives an audit trail independent of which box it sat in.

Dropping the tags saves ~₱80 on a 4-box build but contradicts the paper.
Dropping the beams saves ~₱220 but forfeits FR-04.

---

## 1. Core electronics – shared, one-time

Search terms are what actually returns the right part on Shopee/Lazada PH.

| ☐ | Part | Exact spec to buy | Search term | Qty | Unit ₱ | ₱ |
|---|---|---|---|---|---|---|
| ☐ | Microcontroller | ESP32 DevKit V1, ESP-WROOM-32, **38-pin**, CP2102 or CH340 USB | `ESP32 DevKit V1 38 pin` | 1 | 250 | 250 |
| ☐ | RFID reader | MFRC522 kit — module + 1 white card + 1 keyfob | `RC522 RFID module kit` | 1 | 120 | 120 |
| ☐ | GPIO expander | MCP23017 **breakout module** (not the bare DIP), I2C, addr 0x20 | `MCP23017 I2C module` | 1 | 140 | 140 |
| ☐ | Buzzer | **Active** 5V buzzer module, 3-pin (KY-012). Not passive | `active buzzer module 5V` | 1 | 35 | 35 |
| ☐ | LEDs | 5mm diffused, 1 green + 1 red | `5mm LED assorted` | 2 | 5 | 10 |
| ☐ | Resistors | 220Ω 1/4W carbon film, ±5% (100-pc pack) | `220 ohm resistor 100pcs` | 1 | 50 | 50 |
| ☐ | Breadboard | 830 tie-point MB-102 | `breadboard 830` | 1 | 110 | 110 |
| ☐ | Jumpers | 40-pc ribbon: M-M, M-F, F-F, 20cm — one set each | `dupont jumper wire 40pcs` | 3 | 60 | 180 |
| ☐ | Power | 5V 2A adapter + Micro-USB **data** cable (not charge-only) | `5V 2A adapter micro usb` | 1 | 200 | 200 |
| | **Subtotal** | | | | | **1,095** |

**Two traps to avoid.** Get the **38-pin** ESP32, not the 30-pin — the 30-pin
board omits pins the checklist's pinout uses. And the USB cable must carry
data; a charge-only cable powers the board but never uploads, which looks
exactly like a dead board.

The MCP23017 exposes 16 pins, so it carries the 4 demo boxes and scales to 16
with no second expander. That is what makes per-box sensing affordable — the
ESP32's own free GPIOs run out once the RC522 takes its six.

## 2. Per-box hardware – ×4

| ☐ | Part | Exact spec | Search term | Qty | Unit ₱ | ₱ |
|---|---|---|---|---|---|---|
| ☐ | Presence sensor | **FC-51** IR obstacle module, LM393, 3-pin, pot-adjustable 2–30cm | `FC-51 IR obstacle sensor` | 4 | 55 | 220 |
| ☐ | Equipment tag | MIFARE Classic **1K**, 13.56MHz, 25mm white round sticker | `RFID sticker 13.56mhz mifare 1k` | 4 | 20 | 80 |
| ☐ | Box wiring | 3-core stranded 24AWG, 1 m per box | `3 core wire 24awg` | 4 | 25 | 100 |
| ☐ | Inter-box connector | JST-XH 3-pin plug + socket pair (4 boxes + 1 spare) | `JST XH 3 pin connector` | 5 | 10 | 50 |
| | **Subtotal** | | | | | **450** |

**Confirm the tag chip is MIFARE Classic 1K.** Sellers substitute NTAG213 or
125kHz tags at the same price and neither reads on an RC522. Finding that out
during demo week is the avoidable failure here.

Every box terminates in a **JST-XH plug, not soldered-through wire**. Modules
that stack have to come apart to be carried, and a stack wired as one harness
cannot. Ten pesos a box buys that.

**Power the FC-51 from the ESP32's 3.3V rail, not 5V.** Its digital output
swings to whatever it is powered from, and the MCP23017 in this build runs at
3.3V. Feeding it 5V puts 5V on an expander input.

**Marginal cost per additional box: ₱330** — ₱105 of electronics above plus
~₱225 for the module's own shell (§4). The checklist lists six boxes
(`box-01`…`box-06`); completing all six adds **₱660**.

## 3. Borrower cards

| ☐ | Part | Exact spec | Search term | Qty | ₱ |
|---|---|---|---|---|---|
| ☐ | Borrower card | MIFARE Classic **1K**, 13.56MHz, blank white CR80 (85.6 × 54mm), 10-pack | `RFID card 13.56mhz mifare 1k blank` | 1 | 180 |
| | **Subtotal** | | | | **180** |

One card ships with the RC522 kit. A 10-pack covers all four group members plus
spares for volunteer testers during the demo, and gives you an unregistered card
to trigger the rejection path (NFR-07) on stage.

## 4. Docking station enclosure – the 4 boxes

### 4.1 Which item goes in which box

Box IDs and items are already fixed by `ESP32_MVP_CHECKLIST.md` §0. The demo
uses the first four:

| Box ID | # | Item | Category | Approx. item size (mm) |
|---|---|---|---|---|
| `box-01` | 1 | Wireless Microphone | Audio | 230 L × 50 ⌀ |
| `box-02` | 2 | HDMI Cable (2 m) | Cables | ~100 ⌀ coiled, 40 tall |
| `box-03` | 3 | Presentation Remote | Presentation | 130 × 35 × 25 |
| `box-04` | 4 | 65W USB-C Charger | Power | 70 × 70 × 30 |

The microphone at 230mm sets the depth of every compartment; the rest fit
inside that envelope with room to spare.

### 4.2 Construction: separate stacking modules

Each box is its **own closed module**, not a compartment in a shared carcass.
Four boxes stack two-high in two columns, capped by a controller head that
holds the ESP32 and the reader.

| | mm |
|---|---|
| Compartment interior (each) | **200 W × 130 H × 289 D** |
| Box module, outer | **224 W × 154 H × 292 D** |
| Controller head, outer | **448 W × 100 H × 292 D** |
| Assembled stack (2 × 2 + head) | **448 W × 408 H × 292 D** |
| Material | 12mm MDF, 3mm hardboard backs |
| Weight, one box | ≈ 1.5 kg |

**Why 12mm and not the 9mm of a single carcass.** A shared carcass carries its
load through panels that never move. Modules get lifted, separated, carried and
restacked, and the load path runs through the joints every time. 9mm MDF splits
when you screw into its edge grain; 12mm takes a screw reliably.

The reader ends up at **408mm** — a natural tap height for someone standing at
a table, which is the reason the head goes on top rather than behind.

### 4.3 How the modules stack

Five details, each solving a specific failure:

| Detail | Spec | Prevents |
|---|---|---|
| **Load path** | Top and bottom panels full-width; side panels **between** them | Load runs top panel → side walls → bottom panel → box below, in compression through the panel faces rather than the joints |
| **Corner blocks** | 20 × 20mm stripwood, 4 per box, glued and screwed into the vertical corners | Butt joints working loose after repeated handling; also gives the screws real material to bite |
| **Back panel** | 3mm hardboard, glued **and** pinned on all four edges | Racking — a box with an open back folds into a parallelogram under a side push. This one panel is the single largest contributor to stack stiffness |
| **Registration** | 2 × 8mm dowels per box, protruding 8mm from the top panel, into 8.5mm holes in the box above (front-left, back-right) | The stack sliding apart; also forces correct alignment so nobody stacks a box crooked |
| **Feet** | 4 rubber feet, **bottom module only** | The stack walking across a table during a demo |

**On load:** four boxes plus contents put roughly **5 kg** on the bottom module.
That is far below what 12mm MDF side walls carry in compression — the panels are
not the limit here. What actually fails on a stacked student build is joint
racking and edge-grain screw pull-out under handling, which is what the corner
blocks and the glued back panel address. Build those two properly and the stack
is sound; skip them and the material thickness will not save it.

**Cable routing:** a 12mm hole in the back-left corner of every module, aligned
so they line up when stacked. Sensor wires drop down the back of the stack to
the head, each box unplugging at its JST connector.

### 4.4 Cut list – 12mm MDF

Per box module, ×4:

| Piece | Qty | Size (mm) |
|---|---|---|
| Top / bottom | 2 | 224 × 292 |
| Side panel | 2 | 130 × 292 |
| Corner block *(20×20 stripwood)* | 4 | 130 long |
| Back panel *(3mm hardboard)* | 1 | 224 × 154 |

Controller head, ×1:

| Piece | Qty | Size (mm) |
|---|---|---|
| Top / bottom | 2 | 448 × 292 |
| Side panel | 2 | 76 × 292 |
| Back panel *(3mm hardboard)* | 1 | 448 × 100 |
| Front panel *(3mm hardboard, RC522 behind it)* | 1 | 448 × 100 |

Total 12mm area ≈ **1.13 m²** → **two 2ft × 4ft sheets** (1.49 m²), leaving ~24%
for mis-cuts. The head costs nothing extra in material; it comes out of the
offcuts of the same two sheets.

The RC522 reads through the 3mm hardboard front panel — at 13.56MHz with a ~5cm
range, 3mm of board is not an obstacle. Do not put the reader behind 12mm.

### 4.5 Sensor mounting – the part that decides whether this works

Mount each FC-51 on the **back wall of its module, facing forward** toward the
opening, centred, 40mm above the floor. Drill a 10mm hole for the LED pair; the
board sits behind the panel with its pot reachable through the back.

Back-facing, not ceiling-facing, and the margin is why:

| | Sensor-to-item |
|---|---|
| Item in place, resting against the back wall | 0–60 mm |
| Compartment empty — nothing until the front edge | 289 mm |

Set each pot to trigger at ~80mm. A ceiling mount would have had to separate
"item present" at 105mm from "empty" at 130mm — a 25mm window per box, drifting
with every item swap. Back-mounting turns that into a 229mm gap.

Two follow-ons:

- **Stick each item's white RFID tag on the face that meets the sensor.** The
  tag is the most reflective surface on the item, which is exactly what an IR
  reflective sensor wants. The black HDMI cable is the one that needs this.
- **A hand reaching in will trip the sensor briefly.** The checklist's 200ms
  debounce (§ Sensor hygiene) already absorbs that — do not skip it.

If a module still reads unreliably, the fallback is a through-beam pair across
the opening at 20mm above the floor: material-independent, but two aligned
holes per box and a comparator.

### 4.6 Enclosure materials

| ☐ | Item | Spec | ₱ |
|---|---|---|---|
| ☐ | MDF sheet | 12mm, 2ft × 4ft, ×2 | 1,000 |
| ☐ | Hardboard | 3mm lawanit, 2ft × 4ft | 150 |
| ☐ | Stripwood | 20 × 20mm, 3 m (corner blocks) | 150 |
| ☐ | Dowel rod | 8mm × 1 m (registration pins) | 40 |
| ☐ | Wood glue | 250 mL PVA | 60 |
| ☐ | Wood screws | 1¼", 100 pcs | 80 |
| ☐ | Rubber feet | self-adhesive, 4 pcs | 40 |
| ☐ | Sandpaper | #120 and #220 | 40 |
| ☐ | Panel cutting | hardware-store panel saw, both sheets | 150 |
| | **Subtotal** | | **1,710** |

Pay for the panel-saw cuts. Modules that stack have to be square or the dowels
will not line up, and a handsaw will not hold square across twenty panels.

**Alternative material**, same dimensions:

| Option | ₱ | Trade-off |
|---|---|---|
| **12mm MDF (above)** | **1,710** | Recommended — sturdy, stackable, cuttable anywhere |
| 5mm laser-cut acrylic | ~3,000 | Best-looking and stacks well; needs a shop and lead time |

Foam board and cardboard are **no longer options**. They were viable for a
single static carcass; they will not carry a stack or survive being taken apart
and rebuilt between demos.

## 5. Consumables and assembly

| ☐ | Item | Exact spec | ₱ |
|---|---|---|---|
| ☐ | Solder wire | 60/40 rosin-core, 0.8mm, 50g | 90 |
| ☐ | Heat-shrink tubing | 2mm and 3mm assorted pack | 50 |
| ☐ | Hot glue sticks | 7mm, 10 pcs | 40 |
| ☐ | Zip ties | 100mm, 100 pcs | 40 |
| ☐ | Cable clips / adhesive pads | for running box wiring inside the carcass | 30 |
| ☐ | Perfboard | 5 × 7cm double-sided, if wiring comes off the breadboard | 100 |
| | **Subtotal** | | **350** |

## 6. Cloud and software

| Item | Cost ₱ | Notes |
|---|---|---|
| Firebase (Spark free plan) | 0 | Free tier covers a prototype's reads/writes many times over |
| Android Studio, Arduino IDE, VS Code | 0 | Free |
| Firebase Blaze (pay-as-you-go) | 0 | Only if the free tier is exceeded — it will not be at demo scale |
| **Subtotal** | **0** | |

The entire cloud and software side of SmartDock costs nothing at prototype
scale. This is worth stating explicitly in the paper: the recurring cost of the
system is ₱0, and the whole figure below is one-time capital.

---

## 7. Totals

| Build | Core | Boxes | Cards | Enclosure | Misc | **Subtotal** | +10% | **Total ₱** | Per member |
|---|---|---|---|---|---|---|---|---|---|
| **Standard (12mm MDF)** | 1,095 | 450 | 180 | 1,710 | 350 | 3,785 | 379 | **4,164** | **1,041** |
| Premium (5mm acrylic) | 1,095 | 450 | 180 | 3,000 | 350 | 5,075 | 508 | **5,583** | 1,396 |

**Recommended: the 12mm MDF build at ≈₱4,200**, or **₱1,041 per member**.

**Stacking is what this costs.** A single fixed 2×2 carcass in 9mm MDF came to
₱2,998. Requiring the boxes to stack raises it to ₱4,164 — **+₱1,166, or 39%** —
and all of it lands in the enclosure: thicker material, five separate module
shells instead of one carcass, corner blocks, dowels and connectors. The
electronics do not change at all.

What the money buys is modularity. Boxes come apart for transport, a failed
module swaps out without touching the others, and boxes 5 and 6 bolt on later
without rebuilding anything.

The 10% contingency is not padding — expect at least one dead sensor module and
one mis-cut panel on a first build.

## 8. Procurement notes

- **Buy the beam sensors as a 5-pack, not 4 singles.** They are the most
  failure-prone part in the BOM and the per-unit price drops in a pack.
- **Order tags and cards early.** MIFARE Classic 1K is standard, but a seller
  substituting a different chip type will not read on the RC522, and finding
  that out the week of the demo is avoidable.
- **AliExpress is roughly 40–50% cheaper** on every electronic line item, but
  ships in 2–4 weeks. Viable only if ordered well ahead of the deadline; local
  sellers are the safe choice this close to submission.
- **Buy one spare ESP32** (+₱250) if the budget allows. A board killed by a
  wiring mistake the night before the demo is the single worst failure mode
  here, and it is the one component nothing else can substitute for.

## 9. Cost per additional unit (for the paper's scalability section)

If SmartDock were deployed beyond the prototype:

| Scenario | Added cost ₱ |
|---|---|
| One more box module (shell, sensor, tag, wiring, connector) | 330 |
| Boxes 5 and 6 — completing the checklist's six | 660 |
| Filling the MCP23017 to 16 boxes (12 more) | 3,960 |
| A second full station (head, ESP32, reader, expander) | ~1,400 + boxes |
| One more registered borrower | 18 (one card) |

A stacking module costs **₱330**, against ₱100 for a compartment in a fixed
carcass — each new box now brings its own shell (~₱225 of material) rather than
sharing four walls with its neighbours. That is the standing price of
modularity, and it is worth stating in the paper: the system scales one box at
a time with no rebuild, at a known and constant unit cost.

Per-borrower cost is the headline number for a school pitch: **₱18 per student**
once a station exists, with no recurring cloud fee.
