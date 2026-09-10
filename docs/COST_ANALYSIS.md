# SmartDock – Cost Analysis

Costing for the SmartDock prototype described in `ELDROID_NULL POINT_Paper.docx`,
sized for a **4-box demo** with an RFID tag on every piece of equipment and
boxes built as **separate modules that stack**.

**Budget constraint: ~₱600 per member.** §7 is the build that meets it, at
**₱541 each**, and it is the recommended one. §4's MDF enclosure is the upgrade
if the budget ever allows; §7.2 replaces it with bought stackable bins.
The team already owns one ESP32, so that line is ₱0 throughout.

> **Prices are estimates, not quotations**, except where marked ✔. They reflect
> typical Philippine hobby-electronics retail (Shopee / Lazada / Makerlab /
> e-Gizmo) in PHP. Every row is marked ☐ until checked against a real cart.
>
> **Known calibration gap.** The one price verified against an actual purchase —
> the team's ESP32 at **₱372** — came in **49% above** the ₱250 estimated here.
> One data point is not a trend, but it points the same way: these figures were
> taken at the low end of each listing range, and a real cart will land higher.
> The contingency is set at **20%, not 10%**, to absorb that. See §7.4 for what
> gives if prices run higher still.

---

## 0. Open decisions – for the group to weigh in on

Six things in this document are **not settled** and are not mine to settle.
Everything else follows from these.

| # | Decision | Options | Who it affects |
|---|---|---|---|
| 1 | **How equipment is identified** | RFID tag scanned at the reader (the paper, §3.1) vs box position via IR beam (the firmware checklist, §1). This BOM prices both — see §0.1 | Bryce (firmware), whoever owns the paper |
| 2 | **Budget tier** | Bins at ₱590/member, MDF at ₱1,061, acrylic at ₱1,448 (§8) | Everyone paying |
| 3 | **Bin type** | Opaque parts bins (₱480 for four) vs clear drop-front shoe boxes (~₱880) — cheaper vs better-looking at the defense (§7.2) | Bryce, and anyone presenting |
| 4 | **Four boxes or three** | Dropping to three saves ₱215; the checklist's demo script only exercises boxes 1 and 2 (§7.4) | Everyone |
| 5 | **Equipment RFID tags** | ₱80 for four. Dropping them saves the least and contradicts the paper — but it is on the cut list (§7.4) | Whoever owns the paper |
| 6 | **Who verifies the four prices** | Bins, FC-51s, MCP23017, RC522 — ₱960 of the ₱1,965 subtotal (§7.4) | Needs one volunteer |

**The one that blocks the others is #1.** It is not a cost question — the paper
and the firmware checklist currently describe two different machines, and
whichever way it goes, one of those documents needs rewriting before the
defense.

**#6 is the most useful thing anyone can do today.** Every price here except the
ESP32 is an estimate, and the verified one came in 49% high. Four searches move
half this budget from guess to fact.

---

## 0.1 Design assumption behind this BOM

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
| ✔ | Microcontroller | ESP32 DevKit V1 — **the team already owns one**, bought at **₱372** | *(owned)* | 1 | 0 | **0** |
| ☐ | RFID reader | MFRC522 kit — module + 1 white card + 1 keyfob | `RC522 RFID module kit` | 1 | 120 | 120 |
| ☐ | GPIO expander | MCP23017 **breakout module** (not the bare DIP), I2C, addr 0x20 | `MCP23017 I2C module` | 1 | 140 | 140 |
| ☐ | Buzzer | **Active** 5V buzzer module, 3-pin (KY-012). Not passive | `active buzzer module 5V` | 1 | 35 | 35 |
| ☐ | LEDs | 5mm diffused, 1 green + 1 red | `5mm LED assorted` | 2 | 5 | 10 |
| ☐ | Resistors | 220Ω 1/4W carbon film, ±5% (100-pc pack) | `220 ohm resistor 100pcs` | 1 | 50 | 50 |
| ☐ | Breadboard | 830 tie-point MB-102 | `breadboard 830` | 1 | 110 | 110 |
| ☐ | Jumpers | 40-pc ribbon: M-M, M-F, F-F, 20cm — one set each | `dupont jumper wire 40pcs` | 3 | 60 | 180 |
| ☐ | Power | 5V 2A adapter + Micro-USB **data** cable (not charge-only) | `5V 2A adapter micro usb` | 1 | 200 | 200 |
| | **Subtotal** | | | | | **845** |

**Check the board you already have.** The checklist's pinout uses GPIO 5, 18,
19, 22, 23, 25, 26 and 27. Both the 38-pin and 30-pin ESP32 variants break all
eight out, so an existing board is almost certainly fine — confirm those pins
are labelled on yours before building around it.

**The USB cable must carry data.** A charge-only cable powers the board but
never uploads, which looks exactly like a dead board.

**₱372 is the verified board price**, not the ₱250 first estimated. It does not
change the totals — the board is already bought — but it is the number to use
for a spare (§9) or a second station (§10), and it is the reason the contingency
in this document is 20%.

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

**Marginal cost per additional box:** ₱330 on the MDF build (₱105 electronics
+ ~₱225 for the module's own shell, §4), or **₱215 on the budget build** (₱95
electronics + ₱120 for one more bin). The checklist lists six boxes
(`box-01`…`box-06`); completing all six adds **₱430** at budget prices.

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

## 7. Budget build – target ₱600 per member

The MDF build lands at ₱972 each. This version reaches **₱541** without giving
up stacking, four boxes, or an RFID tag per item. Every substitution and what
it costs you:

### 7.1 Where the money comes off

| Line | Standard | Budget | Saved | What you give up |
|---|---|---|---|---|
| ESP32 | 250 | **0** | 250 | Nothing — already owned |
| Enclosure | 1,710 | **640** | 1,070 | Bought bins instead of a built carcass |
| Power supply | 200 | **80** | 120 | Reuse a phone charger; buy the data cable only |
| Jumper sets | 180 | **120** | 60 | 2 sets, not 3 — buy M-F and M-M |
| Box connectors | 50 | **0** | 50 | Dupont from the jumper sets instead of JST |
| Box wiring | 100 | **80** | 20 | One 10 m roll beats four 1 m cuts |
| Borrower cards | 180 | **100** | 80 | 5-pack, not 10 |
| Consumables | 350 | **180** | 170 | No perfboard, no wood glue/screws/sanding |
| | | | **1,820** | |

### 7.2 The enclosure: bought bins, not built boxes

Four bins, plus one for the controller head. The category matters more than the
price — most things sold as "storage bins" will not work here.

| ☐ | Item | Spec | ₱ |
|---|---|---|---|
| ☐ | Storage bin ×4 | Open-front **stacking** parts bin, PP, ≥300 × 200 × 130mm interior | 480 |
| ☐ | Controller housing | One more bin, or a plastic project box | 100 |
| ☐ | Mounting | Adhesive pads / velcro for sensors and boards | 60 |
| | **Subtotal** | | **640** |

#### What to buy

**Louvre / parts bins** — the open-fronted plastic bins used on workshop and
hardware-store walls. Moulded PP, slanted open front, a lip on the rim that a
second bin's base sits into.

> Search: `stackable parts bin`, `bin box organizer`, `tool storage bin`
> Size: a "large" or "#4/#5" bin, roughly 350 × 200 × 150mm outside
> Price: ₱80–200 each

They are the cheapest thing that is genuinely purpose-built for what this needs:
stacking, and reaching in from the front.

**Transparent drop-front shoe boxes** are the better-looking alternative — clear
plastic, hinged front door, made to stack. The audience can see the item without
opening anything, which reads well at a defense.

> Search: `shoe box organizer stackable drop front`, `transparent shoe box`
> Size: ~330 × 230 × 190mm
> Price: ₱150–250 each — about ₱400 more across four

Remove or tape back the doors for the demo; an extra motion between tap and
removal muddies the timing you are trying to show.

#### Three ways to buy the wrong thing

| Trap | Why it fails |
|---|---|
| **Nestable, not stackable** | Many cheap bins nest — they collapse into each other to save shelf space, which is the exact opposite of holding a stack. Some do both, stacking only when rotated 180°. The listing photo must show them **stacked while full**, not nested |
| **Top-opening crate or basket** | Unreachable the moment a box stacks on it. Already ruled out in §4, and it is the most common thing returned by a "stackable storage" search |
| **Drawer units** | The drawer has to be pulled out to reach the item, so the sensor sees a moving drawer wall rather than the item, and every borrow gains a step |

#### Size check against the actual items

| Item | Needs (mm) |
|---|---|
| Wireless microphone | 230 long — **this sets the minimum** |
| HDMI cable, coiled | 100 ⌀ × 40 |
| Presentation remote | 130 × 35 × 25 |
| 65W charger | 70 × 70 × 30 |

Absolute minimum interior is **250 × 120 × 80mm**. Buy to **300 × 200 × 130mm**
so the boxes look substantial on the table and the microphone is not a
millimetre exercise.

#### Mounting the sensor — check the plastic first

| Plastic | Typical of | How to make the hole |
|---|---|---|
| **PP** (opaque, flexible, slightly waxy) | Parts bins | Step bit, slow. PP is tough and will not crack |
| **PS / PET** (clear, rigid, brittle) | Shoe boxes | Cracks under a spade bit. Back the spot with masking tape and drill very slowly, or melt it with a soldering iron — ventilate, melting plastic fumes |

**With clear bins, try no hole at all first.** Near-IR passes through clear
PS and PET, so tape the FC-51 to the *outside* of the back wall and test it
against each item. If it reads reliably, you have skipped the drilling and kept
the bins undamaged. Watch for the wall itself reflecting back at the sensor and
reading a permanent "present" — if that happens, drill.

Whichever you buy, the bins are not purely a compromise. They are **designed**
to stack and interlock, so registration, load and squareness come free — the
corner blocks, dowels, glued shear panel and panel-saw cuts in §4 exist only
because MDF does not stack on its own. You also skip the build entirely.

What you lose is the custom-fabricated look, which may count at a defense, and
a chosen interior size in place of whatever the bin happens to be.

### 7.3 Two cuts that carry real risk

Both are worth taking at this budget, but know what you are accepting:

- **No perfboard — the circuit stays on the breadboard.** Loose breadboard
  contacts are the most common cause of a demo failing on stage. Mitigate:
  zip-tie the harness, mount the breadboard down with adhesive pads, and do not
  move the wiring once it works. Add the ₱100 perfboard later if you can.
- **Dupont instead of JST connectors.** Dupont pulls apart more easily. Put a
  zip-tie strain relief behind each box's connector so the pull lands on the
  tie, not the pins.

Everything else on that list is free money: the ESP32 you own, a charger you
own, a smaller card pack, and bins that do a job MDF needed ₱1,070 of material
to do.

---

### 7.4 If prices come in higher than estimated

The ESP32 ran 49% over estimate. If the rest of the BOM does the same, the
budget build lands at roughly **₱805 per member**, not ₱541 — over the ceiling.
Two things to do about that:

**Verify these four lines first.** They are ₱960 of the ₱1,965 subtotal, so
pinning them down settles half the budget in four searches:

| ☐ | Line | Estimated ₱ | Actual ₱ |
|---|---|---|---|
| ☐ | Storage bins ×4 | 480 | |
| ☐ | FC-51 sensors ×4 | 220 | |
| ☐ | MCP23017 module | 140 | |
| ☐ | MFRC522 kit | 120 | |

**Then cut in this order if you still need room.** Cheapest damage first:

| Cut | Saves ₱ | Cost to the project |
|---|---|---|
| Borrow a breadboard and jumper sets | 230 | None, if someone in the class has them |
| 3-pack of borrower cards, not 5 | 40 | Fewer spare cards on demo day |
| Demo 3 boxes instead of 4 | 215 | Still enough for the checklist's demo script — it only exercises boxes 1 and 2 |
| Drop the per-equipment RFID tags | 80 | Contradicts the paper's §3.1 and loses per-item identity |
| Skip the controller housing, mount the board behind the stack | 100 | Looks unfinished; reader ends up at an awkward height |

Do **not** cut the FC-51 sensors, the RC522 or the MCP23017 to save money.
Those three are the system; everything else is packaging.

## 8. Totals

Contingency raised to 20% — see the calibration note at the top.

| Build | Core | Boxes | Cards | Enclosure | Misc | **Subtotal** | +20% | **Total ₱** | **Per member** |
|---|---|---|---|---|---|---|---|---|---|
| **Budget (bins)** | 665 | 380 | 100 | 640 | 180 | 1,965 | 393 | **2,358** | **590** |
| Standard (12mm MDF) | 845 | 450 | 180 | 1,710 | 350 | 3,535 | 707 | **4,242** | 1,061 |
| Premium (acrylic) | 845 | 450 | 180 | 3,000 | 350 | 4,825 | 965 | **5,790** | 1,448 |

**The budget build is ₱590 per member — still under ₱600, but the headroom is
now ₱10 instead of ₱59.** The doubled contingency ate it. That is the honest
position: the target is met on paper, and a single line running 50% over will
break it.

This is why §7.4 lists the four prices to verify. Four searches convert most of
this table from estimate to fact, and until they happen, ₱590 is a projection
rather than a number to quote in the paper.

## 9. Procurement notes

- **Buy the beam sensors as a 5-pack, not 4 singles.** They are the most
  failure-prone part in the BOM and the per-unit price drops in a pack.
- **Order tags and cards early.** MIFARE Classic 1K is standard, but a seller
  substituting a different chip type will not read on the RC522, and finding
  that out the week of the demo is avoidable.
- **AliExpress is roughly 40–50% cheaper** on every electronic line item, but
  ships in 2–4 weeks. Viable only if ordered well ahead of the deadline; local
  sellers are the safe choice this close to submission.
- **A spare ESP32 costs ₱372** — the verified price. At this budget it is
  almost certainly out of reach, so treat the board you own as irreplaceable:
  double-check the 3.3V rail before powering anything, and never rewire it
  live. A board killed the night before the demo is the one failure nothing
  else in this BOM can substitute for.

## 10. Cost per additional unit (for the paper's scalability section)

If SmartDock were deployed beyond the prototype:

| Scenario | Budget build ₱ | MDF build ₱ |
|---|---|---|
| One more box (bin/shell, sensor, tag, wiring) | 215 | 330 |
| Boxes 5 and 6 — completing the checklist's six | 430 | 660 |
| Filling the MCP23017 to 16 boxes (12 more) | 2,580 | 3,960 |
| A second full station (ESP32, reader, expander, head) | ~920 + boxes | ~1,520 + boxes |
| One more registered borrower | 20 | 18 |

Each new box brings its own shell rather than sharing walls with its neighbours
— **₱215 at budget prices**. That is the standing price of modularity, and it
is the good version of this number for the paper: the system scales one box at
a time, with no rebuild and no rewiring of what is already there, at a known
and constant unit cost.

Per-borrower cost is the headline number for a school pitch: **₱20 per student**
once a station exists, with no recurring cloud fee.
