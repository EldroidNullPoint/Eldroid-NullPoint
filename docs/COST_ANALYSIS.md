# SmartDock – Cost Analysis

Costing for the SmartDock prototype described in `ELDROID_NULL POINT_Paper.docx`,
sized for a **4-box demo** with an RFID tag on every piece of equipment.

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
| | **Subtotal** | | | | | **400** |

**Confirm the tag chip is MIFARE Classic 1K.** Sellers substitute NTAG213 or
125kHz tags at the same price and neither reads on an RC522. Finding that out
during demo week is the avoidable failure here.

**Power the FC-51 from the ESP32's 3.3V rail, not 5V.** Its digital output
swings to whatever it is powered from, and the MCP23017 in this build runs at
3.3V. Feeding it 5V puts 5V on an expander input.

**Marginal cost per additional box: ₱100.** The checklist lists six boxes
(`box-01`…`box-06`); completing all six adds **₱200**.

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

### 4.2 Box dimensions

One carcass, **2×2 grid of four identical compartments**. Identical beats
bespoke here — one cut size repeated, one sensor mounting jig, and any item can
move to any box without rebuilding.

| | mm |
|---|---|
| Compartment interior (each) | **200 W × 130 H × 280 D** |
| Overall carcass | **427 W × 287 H × 280 D** |
| Material | 9mm MDF carcass, 3mm hardboard back |
| Finished weight | ≈ 4 kg |

130mm of height is deliberate: tall enough for the 50mm microphone, short
enough that an empty compartment reads clearly different from a full one.

### 4.3 Cut list – 9mm MDF

| Piece | Qty | Size (mm) |
|---|---|---|
| Side panel | 2 | 287 × 280 |
| Top / bottom | 2 | 409 × 280 |
| Vertical divider | 1 | 269 × 280 |
| Horizontal divider | 2 | 200 × 280 |
| Back panel *(3mm hardboard)* | 1 | 427 × 287 |

Total 9mm area ≈ 0.58 m². **One 2ft × 4ft (610 × 1220mm) sheet covers it** with
room for a mis-cut. Sides go outside the top and bottom; the two horizontal
dividers sit either side of the vertical one, so no notching or joinery is
needed — butt joints, glue, and panel pins.

### 4.4 Sensor mounting – the part that decides whether this works

Mount each FC-51 on the **back wall of its compartment, facing forward** toward
the opening, centred, 40mm above the compartment floor. Drill a 10mm hole for
the LED pair; the board sits behind the panel with its pot reachable.

Back-facing, not ceiling-facing, and the margin is why:

| | Sensor-to-item |
|---|---|
| Item in place, resting against the back wall | 0–60 mm |
| Compartment empty — nothing until the front edge | 280 mm |

Set each pot to trigger at ~80mm. A ceiling mount would have had to separate
"item present" at 105mm from "empty" at 130mm — a 25mm window per box, drifting
with every item swap. Back-mounting turns that into a 220mm gap.

Two follow-ons:

- **Stick each item's white RFID tag on the face that meets the sensor.** The
  tag is the most reflective surface on the item, which is exactly what an IR
  reflective sensor wants. The black HDMI cable is the one that needs this.
- **A hand reaching in will trip the sensor briefly.** The checklist's 200ms
  debounce (§ Sensor hygiene) already absorbs that — do not skip it.

If a compartment still reads unreliably, the fallback is a through-beam pair
across the opening at 20mm above the floor, which is material-independent but
needs two aligned holes per box and a comparator.

### 4.5 Enclosure materials

| ☐ | Item | Spec | ₱ |
|---|---|---|---|
| ☐ | MDF sheet | 9mm, 2ft × 4ft | 400 |
| ☐ | Hardboard back | 3mm lawanit, 2ft × 4ft | 150 |
| ☐ | Wood glue | 250 mL PVA | 60 |
| ☐ | Panel pins / screws | 1" × 100 | 50 |
| ☐ | Sandpaper | #120 and #220 | 40 |
| | **Subtotal** | | **700** |

Cutting: any hardware store with a panel saw will cut the sheet to the list
above for ₱50–150, and their cuts will be squarer than a handsaw's. Square
matters — the dividers only sit flush if the panels are true.

**Cheaper and dearer alternatives**, same dimensions throughout:

| Option | ₱ | Trade-off |
|---|---|---|
| 5mm foam board + glue | 250 | Light and fast, will not survive the trip to campus |
| **9mm MDF (above)** | **700** | Recommended — sturdy, cuttable anywhere |
| 3mm laser-cut acrylic | 1,500 | Best-looking; needs a shop and lead time |

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

| Build | Core | Boxes | Cards | Enclosure | Misc | **Subtotal** | +10% contingency | **Total ₱** |
|---|---|---|---|---|---|---|---|---|
| Budget (foam board) | 1,095 | 400 | 180 | 250 | 350 | 2,275 | 228 | **2,503** |
| **Standard (MDF)** | 1,095 | 400 | 180 | 700 | 350 | 2,725 | 273 | **2,998** |
| Premium (acrylic) | 1,095 | 400 | 180 | 1,500 | 350 | 3,525 | 353 | **3,878** |

**Recommended: the standard MDF build at ≈₱3,000**, split four ways is **₱750
per member**. It survives being carried to and from campus, which the foam-board
build may not, and it does not depend on a laser-cutting shop's schedule.

The 10% contingency is not padding — expect at least one dead sensor module and
one mis-cut panel on a first build.

---

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
| One more box on the existing station | 100 |
| Boxes 5 and 6 (completing the checklist's six) | 200 |
| Filling the MCP23017 to 16 boxes | 1,200 |
| A second full station (new ESP32, reader, expander) | ~1,100 + boxes + enclosure |
| One more registered borrower | 18 (one card) |

Per-borrower cost is the headline number for a school pitch: **₱18 per student**
once a station exists, with no recurring cloud fee.
