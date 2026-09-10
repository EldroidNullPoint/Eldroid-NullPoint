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

| ☐ | Item | Qty | Unit ₱ | Subtotal ₱ |
|---|---|---|---|---|
| ☐ | ESP32 DevKit V1 (38-pin, CP2102/CH340) | 1 | 250 | 250 |
| ☐ | MFRC522 RFID reader kit (incl. 1 card + 1 keyfob) | 1 | 120 | 120 |
| ☐ | MCP23017 I2C GPIO expander module | 1 | 140 | 140 |
| ☐ | Active buzzer module, 5V | 1 | 35 | 35 |
| ☐ | 5mm LED, green + red | 2 | 5 | 10 |
| ☐ | Resistor pack, 220Ω 1/4W (100 pcs) | 1 | 50 | 50 |
| ☐ | Breadboard, 830 tie-point | 1 | 110 | 110 |
| ☐ | Jumper wire sets (M-M, M-F, F-F) | 3 | 60 | 180 |
| ☐ | 5V 2A adapter + USB cable | 1 | 200 | 200 |
| | **Subtotal** | | | **1,095** |

The MCP23017 exposes 16 pins, so it carries the 4 demo boxes and scales to 16
with no additional expander. That is the component that makes per-box sensing
affordable — the ESP32's own free GPIOs would run out first.

## 2. Per-box hardware – ×4

| ☐ | Item | Qty | Unit ₱ | Subtotal ₱ |
|---|---|---|---|---|
| ☐ | IR break-beam / slotted photointerrupter module | 4 | 55 | 220 |
| ☐ | MIFARE Classic 1K tag/sticker (equipment) | 4 | 20 | 80 |
| ☐ | Hook-up wire to box, ~1 m 3-core | 4 | 25 | 100 |
| | **Subtotal** | | | **400** |

**Marginal cost per additional box: ₱100.** The checklist's equipment table
lists six boxes (`box-01`…`box-06`); going from the 4-box demo to all six adds
**₱200**.

## 3. Borrower cards

| ☐ | Item | Qty | Unit ₱ | Subtotal ₱ |
|---|---|---|---|---|
| ☐ | MIFARE Classic 1K blank cards, 10-pack | 1 | 180 | 180 |
| | **Subtotal** | | | **180** |

One card ships with the RC522 kit. A 10-pack covers all four group members plus
spares for volunteer testers during the demo, and gives you an unregistered card
to trigger the rejection path (NFR-07) on stage.

## 4. Docking station enclosure – 4 boxes

Three viable builds. Pick one; they are alternatives, not additive.

| ☐ | Option | Cost ₱ | Trade-off |
|---|---|---|---|
| ☐ | Cardboard / foam board + cutting | 250 | Cheapest, fine on camera, fragile |
| ☐ | MDF or plywood, cut to size | 700 | Survives handling and transport |
| ☐ | Laser-cut 3mm acrylic | 1,500 | Best-looking; needs a shop and lead time |

Each box needs a mount for its beam sensor and an open face for the item, so
whichever material you pick, budget cutting time as well as material.

## 5. Consumables and assembly

| ☐ | Item | Cost ₱ |
|---|---|---|
| ☐ | Solder, heat-shrink, hot glue, screws, zip ties | 250 |
| ☐ | Perfboard (if the final wiring is soldered off the breadboard) | 100 |
| | **Subtotal** | **350** |

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
| Budget (cardboard) | 1,095 | 400 | 180 | 250 | 350 | 2,275 | 228 | **2,503** |
| **Standard (MDF)** | 1,095 | 400 | 180 | 700 | 350 | 2,725 | 273 | **2,998** |
| Premium (acrylic) | 1,095 | 400 | 180 | 1,500 | 350 | 3,525 | 353 | **3,878** |

**Recommended: the standard MDF build at ≈₱3,000**, split four ways is **₱750
per member**. It survives being carried to and from campus, which the cardboard
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
