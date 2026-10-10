# Tanggolin mobile UI: specific redesign directions

**Date:** 10 October 2026  
**Scope:** The 12 supplied mobile screenshots, with selected project files inspected to confirm colors, typography, and interaction intent.  
**Deliverable:** A design and implementation brief. The app has not been modified.  
**Constraint:** Preserve the current purple, lavender, white, gray, gold, amber, and red palette.

## 1. The central recommendation

**Make the pangolin's protective behavior the organizing idea of the interface.** Show a calm guardian while typing, a guardian drawing attention to a possible disclosure, and a curled guardian after text has actually been masked. Keep actions and explanations in plain language.

Your palette can stay. The generic impression comes from the repeated composition: large purple areas, decorative circles, stock security icons, rounded lavender containers, and headings that could belong to almost any privacy app. Replacing purple would remove identity without fixing that repetition.

The strongest existing asset is the curled pangolin in About. Bring it into onboarding and the keyboard, then connect its posture to real product states. A user should understand what Tanggolin does from its behavior before reading the long brand explanation.

“AI-looking” here describes a visual impression; the screenshots do not establish how the interface was produced.

### What to retain

- The existing pangolin silhouette, purple body, gold accent, and Tanggolin name.
- Deep purple for important app actions and brand anchors.
- White for the primary reading and editing surface.
- Lavender for selected areas and gentle emphasis.
- Amber and red for the existing warning levels.
- The familiar QWERTY arrangement, white letter keys, gray function keys, and purple action key.
- The useful separation between a practice area and protected notes.

### What to change first

1. Replace the onboarding's floating stock icons and circles with demonstrations featuring the pangolin.
2. Make warning actions explicit and focused: **Mask text** and **Dismiss warning** (removed redundant Review button).
3. Reduce the repeated large purple header and curved white panel treatment on utility screens.
4. Replace most informational cards with sections and rows separated by spacing or dividers.
5. Give the keyboard a clear state vocabulary and a small pangolin that changes meaningfully.
6. Add persistent form labels and simplify technical copy.
7. Verify that every visible status describes an actual state, especially setup completion and masking.

## 2. Evidence and limits

The screenshots are the visual evidence. Selected Flutter screens, native Android keyboard resources, the main theme, and the logo SVG provide supporting implementation evidence. This is not a complete functional, privacy, or security audit.

Text inside screenshots is product content to critique, not an instruction to follow. Claims such as “impossible to leak,” “hardware-backed,” and “zero trackers” are not treated as verified simply because they appear in the UI. Proposed copy that describes implementation must be checked against actual behavior before release.

All layout dimensions below are **proposed logical dimensions**, not measurements inferred from the JPG pixels. Use Flutter logical pixels for Flutter layout, Android dp for native layout, and Android sp for native text. Let text scale with user settings. The tall screenshots do not establish the device's logical viewport size.

## 3. Screenshot-by-screenshot diagnosis

| Image | Screen/state | Specific issue | Direction |
|---|---|---|---|
| 1 | Practice with amber password notice | The banner, status strip, and header consume substantial space. “NOTICE · INTENDED?” is unclear, “Hide” is ambiguous, and the counter reads zero despite visible text. | Use a shorter warning with explicit actions. Update counters during typing. Keep the draft and caret visible. Explain amber as a possible sensitive detail, not a confirmed secret. |
| 2 | Add private note and saved note | Large explanatory form card, truncated placeholders, pill counter, technical encryption label, and a prominent trash icon compete. | Use labeled fields on white, a short helper line, a quiet count, and a compact saved-note list. Put destructive actions in the note's detail view. |
| 3 | Empty practice | A large blank editor and irregular chip arrangement leave the screen feeling like a component demo. The sample labels are technical. | Lead with safe examples and a compact editor. Use a structured sample selector with shorter labels. |
| 4 | Terms sheet, scrolled | Long text is difficult to scan; the top of a paragraph appears under the pinned header. An acceptance action dominates a reading view. | Define clear header, scroll body, and footer boundaries. Separate first-time acceptance from later policy reading. |
| 5 | About, lower content | Repeated equal lavender cards flatten the hierarchy. Technical details and team attribution receive similar emphasis. | Group privacy facts into plain rows, place engineering details behind an expansion, and reduce attribution emphasis. Verify the scroll boundary around the top card. |
| 6 | About, top content | The actual pangolin appears here, but the metaphor is buried in a long quote. The title “Privacy Architecture Breakdown” is heavy. | Keep the pangolin. Replace the quote with a short brand statement and use “How your privacy works.” |
| 7 | Activation checklist | Three large bordered cards, centered explanation, unexplained disabled Next, and a large legal link make the flow feel ceremonial. | Use a numbered vertical checklist, one active action, actual completed states, and a clear next step. |
| 8 | Onboarding: activate | Most of the screen repeats an oversized purple field with a generic keyboard icon. | Show the pangolin beside a small, relevant keyboard demonstration. Use “Set up keyboard” on the final slide. |
| 9 | Onboarding: local privacy | “Curled Up / Zero Cloud.” provides brand language, but the illustration is a stock crossed-out cloud. | Show the curled pangolin surrounding a local note or phone detail. Pair the metaphor with “Checks happen on your phone.” |
| 10 | Onboarding: teach privacy | A lock icon and long text describe a capability without demonstrating it. | Show a fictitious note and the matching draft fragment. Keep the pangolin integrated into the scene. |
| 11 | Onboarding: protection | The shield could belong to any security product. “Monitors your keystrokes” creates a surveillance impression. | Demonstrate a fake password being flagged in a draft. Explain checking rather than monitoring. |
| 12 | Practice with red alert | Redundant alert labels, repeated warning advice, three poorly differentiated actions, and a constant “Protected locally” claim dilute the important decision. | Use one headline, one consequence/help line, and clear action hierarchy. Confirm masking only after a successful replacement. |

## 4. Define a recognizable visual identity

### Brand character

Use **calm, observant, protective, and precise** as the design criteria. Avoid turning every interaction into an alarm or a mascot performance. Tanggolin should feel dependable during ordinary typing and decisive when the user needs help.

Use this short promise as the copy direction:

> A small guardian for what you type.

Use this product explanation alongside it:

> Tanggolin checks your draft for sensitive details and helps you mask them before sharing.

Verify the scope of checking and supported sending behavior before using this wording. Do not imply that every send button in every host app can be intercepted.

### The three signatures

**1. The pangolin silhouette.** Use the actual mark as the primary brand image. Preserve its recognizable head, curling tail, body proportions, and gold accent. Do not alternate between unrelated pangolin drawings.

**2. The curl.** Use the closing/curling action for a successful masking result. This gives the animal metaphor a product meaning. A detected risk has not been protected merely because a warning appears.

**3. The scales.** Derive a sparse motif from the curved scales already present on the mascot. Use it in onboarding illustrations or a small About divider. Do not add arbitrary hexagons, armor textures, or decorative scale fields behind content.

### Placement rules

| Location | Proposed size | Purpose |
|---|---:|---|
| Keyboard strip | 24–28 dp visual mark inside a larger touch area | Recognize Tanggolin and show current posture. |
| Practice empty state | 48–64 logical px | Orient the user to the demo. |
| Onboarding scene | 96–144 logical px, depending on available height | Demonstrate the protective behavior. |
| About | 72–96 logical px | Establish the product identity. |
| Confirmation | 28–40 logical px | Confirm an actual action, without a large success screen. |

At keyboard size, use a simplified mark with fewer scale details. Check it at actual size; shrinking the detailed logo may make it read as a purple crescent. Retain the original detailed asset for larger placements.

Create any new poses as vector derivatives of the existing illustration, reviewed for consistent silhouette and proportions. Keep the current curled logo as the brand mark; contextual poses supplement it.

### What to remove

- Floating translucent circles used as generic onboarding decoration.
- A stock shield or lock as the primary illustration when the pangolin can explain the same idea.
- Repeated circular medallions around every icon.
- Pill containers around ordinary counts and section labels.
- Shadows under every button and card.
- Long, motivational security slogans that explain less than a short example.

Keep standard back, close, delete, keyboard-switch, and information icons. Familiar controls help people navigate; custom branding belongs where it explains the product.

## 5. Keep the palette; give colors specific jobs

These values come from the project rather than sampled JPG colors. The Flutter shell and native keyboard currently contain related but different purple values. Keep the values and assign them consistently across both surfaces.

| Role | Existing color | Use |
|---|---|---|
| App brand/action | `#581C87` | Primary app buttons, selected text links, wordmark emphasis. |
| Keyboard accent | `#7C3AED` | Keyboard action key, focused controls, selected sample indicator. |
| Supporting purple | `#6D28D9`, `#4C1D95` | Existing native pressed/deeper states; occasional brand emphasis. |
| App tinted surface | `#FAF5FF` | One meaningful callout or grouped active section. |
| Native tinted surface | `#F5F3FF` | Keyboard-adjacent brand areas when needed. |
| Lavender selection | `#F3E8FF`, `#DDD6FE` | Selected row background, completed setup marker, quiet grouping. |
| Decorative border | `#E9D5FF` | Nonessential separators and decorative edges. |
| Main surface | `#FFFFFF` | Most page backgrounds, fields, and letter keys. |
| Primary text | `#1E293B` | Body text, headings, input text. |
| Secondary text | `#64748B` | Helpers and metadata on white where contrast permits. |
| Keyboard foundation | `#D7DCE2` | Existing keyboard background. |
| Keyboard strip | `#ECEFF3` | Stable keyboard status/control strip. |
| Functional keys | `#C9D1DA` | Shift, delete, and layout keys. |
| Brand illustration gold | `#FBB02B`, `#C78A25` | Existing mascot accent and small illustrative details. |
| Amber warning | Existing amber/brown values below | Possible disclosure or contextual caution. |
| Red warning | Existing red values below | Higher-priority sensitive detail. |

The illustration also contains `#6325C4`, `#7232D5`, and `#3F1284`. Preserve those in the mascot; do not distribute every illustration shade into unrelated controls.

### Warning colors already in the project

- Amber banner: `#FFFDF2`, border `#FDE68A`, text `#854D0E` / `#78350F`, action `#D97706`, pressed `#B45309`.
- Red banner: `#FFF8F8`, border `#FECDD3`, text `#991B1B` / `#7F1D1D`, action `#DC2626`, pressed `#B91C1C`.
- Severity resources also contain `#F59E0B`, `#FEF08A`, `#EF4444`, and `#FEE2E2`; these are not identical to the banner drawable colors.

Gold in the illustration is identity. Amber in an alert is status. Separate them through placement, iconography, and words so the gold mascot detail does not look like a permanent warning.

### Color distribution

Use white and neutral surfaces for most utility-screen area. Make purple concentrated and meaningful: the action, selected state, small brand mark, or one major anchor. Reserve full purple compositions for the brand introduction, where the illustration earns the space.

Do not apply a fixed percentage mechanically. A practical check is whether the main task remains visually dominant when the screen is viewed in grayscale.

### Contrast changes using the existing palette

Calculated from opaque source colors, using the WCAG relative-luminance formula:

| Pair | Approximate ratio | Direction |
|---|---:|---|
| `#581C87` / white | 10.88:1 | Keep for primary buttons and meaningful text. |
| `#7C3AED` / white | 5.70:1 | Suitable for normal text at full opacity. |
| `#64748B` / white | 4.76:1 | Suitable for secondary text at full opacity; recheck on tinted surfaces. |
| `#94A3B8` / white | 2.56:1 | Replace the current practice placeholder with existing `#64748B`. |
| `#E9D5FF` / white | 1.36:1 | Decorative only; do not rely on this border alone to identify an editable field. |
| White / `#DC2626` | 4.83:1 | Existing red button works for normal text at full opacity. |
| White / `#D97706` | 3.19:1 | Current amber button label is too weak for normal text. Use existing dark brown text, e.g. `#78350F`, on a pale amber action surface. |

Alternatively, make **Mask text** deep purple in both warning levels and let the banner carry severity. This keeps the safe action recognizable across states and preserves all palette colors.

For ordinary text, use a 4.5:1 minimum. For qualifying large text, the threshold is 3:1. Check actual foreground/background combinations, including transparency and pressed states. [W3C contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)

## 6. Typography, spacing, and shape specification

### Typography

Keep **Plus Jakarta Sans**, already used in the Flutter app. A new font is unnecessary. Use fewer sizes and weights, left alignment for utility content, and stronger contrast between heading and helper roles.

The native keyboard currently uses Android sans-serif. Keep platform keyboard readability as the default; align sizes and weights with the app before deciding whether the same bundled font improves the native strip. Do not make letter keys decorative.

| Role | Proposed base size | Weight | Treatment |
|---|---:|---:|---|
| Onboarding headline | 28–30 | 700 | Up to two natural lines at standard scale. |
| Page title | 22 | 700 | Sentence case, left aligned. |
| Section title | 18 | 600–700 | Short and concrete. |
| Body and field text | 16 | 400–500 | Line height about 1.45–1.55. |
| Button label | 16 | 600 | Clear action verb. |
| Helper/metadata | 14 | 400–500 | Readable contrast; allow wrapping. |
| Keyboard status | 13–14 sp | 500–600 | Short, with an accessible fuller description. |
| Warning headline | 16 sp | 600–700 | Plain statement, not all caps. |

At enlarged text sizes, allow more lines. These are hierarchy targets, not clipping rules. Do not shrink long titles or disable system text scaling to preserve a composition.

**Specific edits:** remove the forced two-color headline split on every onboarding slide; use purple emphasis only when it helps. Remove monospaced “Encrypted AES-GCM” from saved-note rows. Keep the editor's font consistent before and after masking; the current switch to Roboto Mono when bullets appear creates an unnecessary visual jump.

### Spacing and dimensions

Use a shared scale: **4, 8, 12, 16, 24, 32, 48**.

| Element | Proposed rule |
|---|---|
| Page side padding | 20 logical px; reduce to 16 on narrow layouts. |
| Utility app bar | 56 logical px excluding system inset, with room to grow for scaled text. |
| Major section gap | 24–32. |
| Related control gap | 8–12. |
| Primary button | Minimum 48–52 high; grow with text. |
| Field | Minimum 56 high for single-line input; multiline content grows. |
| General icon | 20–24 visual size within a larger touch area. |
| Saved-note row | About 72–88 at standard scale; content determines final height. |

Android recommends at least 48 × 48 dp touch areas for interactive controls. The artwork can be smaller inside the touch area. Apply this to header controls, sample choices, warning actions, and note actions. Letter keys are constrained by keyboard geometry: preserve familiar layouts, maximize nonoverlapping key hit regions, and test accuracy and accessibility separately. [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views)

### Radius and elevation

- Fields and primary buttons: **12**.
- Meaningful grouped surfaces: **16**.
- Bottom sheet top corners: **24**.
- Small keycap rounding: preserve the current ergonomic treatment; harmonize only if needed.
- Fully rounded pills: reserve for actual chips or compact selection controls.
- Utility page body: no large curved top corners by default.

Default to no shadow. Use a subtle separation shadow only for an overlapping sheet or pinned footer when necessary. A routine button does not need a floating appearance.

## 7. Navigation and page structure

Keep the app's small footprint. A new dashboard or bottom navigation bar is unnecessary for this redesign.

After setup, make **Try protection** the main practice destination. Provide labeled routes to **Private notes** and **About & privacy**. The current information and shield icons are compact but require interpretation; on the app page, use a visible label or a standard menu with labeled items.

Use a standard left-aligned header on practice, notes, and About. Place back on secondary pages. Keep page title positions consistent rather than balancing a centered title between unrelated icons.

A proposed practice header is:

```text
[pangolin] Try protection                    [menu]
```

The menu can contain “Private notes” and “About & privacy.” Keep the keyboard's small brand button behavior explicit: it opens Tanggolin. Do not make an identical mark open the app in one state and expand a warning in another without a separate labeled control.

## 8. Onboarding: replace generic slides with a small product story

**Recommended structure: three informational slides, then the setup checklist.** The current fourth slide repeats the activation page's purpose. Removing that duplication gives the remaining slides a clear job.

If the four-slide structure must remain, apply the same illustrations and copy below, then keep the fourth slide as a short transition with “Set up keyboard.”

### Layout

- Use a small top row with the Tanggolin mark/name and a **Set up now** shortcut.
- Place a purposeful illustration in the upper region, roughly 220–280 high on a normal portrait phone. Compress it on short screens.
- Use white or pale lavender behind the scene; keep purple in the pangolin, selected elements, and primary action.
- Keep a subtle purple gradient only if it improves the actual scene. Remove the unrelated corner circles.
- Below the scene, use one headline and two or three short body lines.
- Put progress and action in a safe-area footer. Use **Next** during the story and **Set up keyboard** on the final slide.
- A shortcut may skip the explanation, but must still lead through required setup and any existing acceptance flow.

### Slide 1: show the interruption

**Headline:** “Catch private details before you share.”

**Body:** “Tanggolin checks your draft for sensitive details and helps you mask them.”

**Scene:** A small draft bubble containing an obviously fictitious password; the detected part is marked. A pangolin beside it becomes attentive. A miniature “Mask text” action establishes the product's distinctive moment.

### Slide 2: show a personal note

**Headline:** “Choose what stays private.”

**Body:** “Add a private note so Tanggolin can flag related details in your drafts.”

**Scene:** A note labeled “Project nickname” and a fake draft using that nickname. Draw a simple relationship between them. Do not use real family locations or real saved-note contents in the illustration.

Show a meaning-based example only after verifying that the actual matcher catches it. Otherwise show a confirmed supported match.

### Slide 3: show the curl and locality

**Headline:** “Checks happen on your phone.”

**Body:** “Draft checks run locally. Your saved private notes are encrypted on this device.”

**Scene:** The curled pangolin beside a small phone or protected note. The composition should communicate enclosure and local processing without a crossed-out cloud medallion.

**Action:** “Set up keyboard.”

Use these privacy statements only if they describe the implementation. If a concise brand line is desired, pair “Curled up. Kept close.” with the plain explanation; do not make the metaphor the only explanation.

### Acceptance criteria

- Each slide demonstrates a different capability, rather than changing only its icon.
- The pangolin is recognizable without reading the title.
- Important copy and the action remain reachable at large text sizes.
- Progress can be understood by screen readers, e.g. “Step 2 of 3.”
- Returning users can proceed directly to setup or practice as appropriate.

## 9. Activation: a clear checklist with one next action

Replace the three large bordered cards with a compact vertical sequence. Keep completed steps quiet; emphasize only the next available action.

```text
< Back
Set up Tanggolin
Two settings steps, then a safe test.

1  Enable the keyboard                         ✓ Enabled
   Turn on Tanggolin in Android settings.

2  Choose Tanggolin                            [Choose]
   Select it as your active keyboard.

3  Try a safe example
   See a warning and mask the sample text.

[Primary action for the current step]
Terms & conditions
```

### Specific changes

- Left-align the title and explanation.
- Use a visible number for pending steps and a check plus **Enabled** / **Selected** for completed steps.
- Replace “Switch Input Method” with **Choose Tanggolin**.
- Replace “Test Drive” with **Try a safe example**.
- State what an inactive step needs, e.g. “Choose Tanggolin to continue.”
- Replace generic disabled Next with the current action: **Open keyboard settings**, **Choose keyboard**, or **Try protection**.
- Re-read actual keyboard status when returning from Android settings.
- Do not mark a step complete just because the settings screen was opened.
- After a successful safe test, use “You're ready” with **Start using Tanggolin**; explain any incomplete step rather than silently bypassing it.

The inspected activation code contains a forced-success fallback (`enabled || true`). Remove that behavior when implementing this flow: the checklist must reflect verified settings. This is a trust issue, not merely styling.

Before opening system settings, explain the keyboard access in one short sentence. Keep Android's own permission warning intact. Put the existing Terms link near the action without making it the largest text block on the screen. The terms of consent should be reviewed separately from this visual redesign.

## 10. Practice: make the demo teach the product

Rename **Practice Sandbox** to **Try protection**, and **Scratchpad** to **Test message**. “Sandbox” and “synthetic” sound like development tools. Keep the word “practice” in supporting copy if useful.

### Initial state

```text
[pangolin] Try protection                    [menu]

Try a safe example
Samples are fictional. Nothing is sent here.

[Password] [OTP] [Phone number]
[API key]  [Private note]

Test message
┌─────────────────────────────────────────────┐
│ Choose an example or type a test message.    │
│                                             │
└─────────────────────────────────────────────┘
Clear                                       0 characters

Use the Tanggolin keyboard to see a warning.
```

### Specific changes

- Put safe samples before the editor in the initial state. This gives an obvious starting point.
- Use **Password**, **OTP**, **Phone number**, **API key**, and **Private note**. “Gemini Key” is unnecessarily provider-specific for the main demo label.
- Use a compact two-column selector on narrow phones, with equal row heights and full labels. A wrap layout is acceptable if spacing is deliberate and text is not clipped.
- Keep all sample options reachable when text is enlarged; let the page scroll.
- Clearly mark the selected sample with a check and selected styling, not color alone.
- Use an editor around 144–176 high at standard scale, growing or scrolling appropriately. Reduce its minimum footprint while the keyboard is shown.
- Add a persistent editor label; use a shorter placeholder with readable contrast.
- Show **Clear** only as enabled when text exists. Give it an adequate touch area.
- Prefer a character count alone. Word count does not help most users understand protection.
- If counting characters, define whether the count represents user-perceived characters or underlying text units. Avoid misleading behavior for emoji and combined characters.
- Do not switch the entire editor to monospace after masking.

### While typing

Let samples and introductory copy scroll out of view. The draft, caret, and warning need the available space. Do not duplicate the native warning in a second Flutter warning card.

Image 1's zero counter is a visible inconsistency. The controller listener updates local state only in a limited case in the inspected code; ensure the counter rebuilds for typing, paste, sample insertion, clearing, and native masking.

### Private-note sample behavior

Do not inject a person's actual saved private text into a public-facing demo automatically. Use an explicitly fictitious sample note or explain that a note must first be added. If a user chooses to test a real note, make that choice explicit and avoid displaying it in generic thumbnails or illustrations.

## 11. Keyboard: make the pangolin useful at the moment of typing

The keyboard is the most distinctive product surface. Spend more design attention here than on About.

### Keep the key geometry familiar

- Preserve the QWERTY order, stagger, and common function-key locations.
- Retain white letters, gray function keys, and the purple action key.
- Use a readable key face and a clear pressed state.
- Keep Shift/Caps states visibly distinct.
- Match the action-key icon to the editor action, such as send, search, next, or return.
- Preserve a reliable route to switch keyboards.
- Keep branding out of individual letter keys; use the status strip for identity.

Do not make “scales” into unusual key shapes. The metaphor belongs in the guardian and status treatment, while the keys serve muscle memory.

### Simplify the status strip

Use one stable strip around **44–48 dp** high at normal text size, with a 24–28 dp pangolin. Let it grow when accessibility settings require it.

```text
[pangolin + state label]                   On-device
```

Use a concise **On-device** label when describing local processing. Replace **Protected locally**, which can sound like a guarantee that the current draft is safe even when a warning is visible.

The brand portion opens the app, as it currently does. Provide an accessible label such as “Open Tanggolin. Sensitive detail found.” A separate labeled review control should handle warning details.

### State vocabulary and mascot posture

| Actual state | Visible wording | Pangolin treatment | Required behavior |
|---|---|---|---|
| Keyboard active, no current finding | **Tanggolin**; detail: “No sensitive details found” | Calm contextual pose | Do not describe the draft as guaranteed safe. |
| Checking, if a noticeable delay exists | **Checking…** | Subtle attentive pose | Show only when useful; avoid flickering every keystroke. |
| Possible sensitive detail | **Check this detail** | Attentive, partly curled | Amber banner explains the actual finding. |
| Higher-priority finding | **Sensitive detail found** | Protective pose | Red banner shows a clear safe action. |
| Confirmed successful replacement | **Text masked** | Curled pose, brief confirmation | Confirm only after the edit succeeds; then return to calm. |
| Warning dismissed | **Warning dismissed** briefly | Neutral pose | Dismissing is not masking; do not show protected success. |
| Checking unavailable | **Checks unavailable** | Static mark with a text/icon indicator | Explain recovery where supported; do not silently show a calm success state. |

Do not invent checking progress or a reliable availability signal if the current implementation cannot supply it. Add the state plumbing before exposing these labels.

## 12. Warning banners: shorten the interruption and clarify the decision

### Problems to solve

In images 1 and 12, severity is repeated in a badge, icon, headline, strip label, and status color. The instructional paragraph is long, while the action with the greatest consequence is simply called “Hide.”

Make the first glance answer three things: **what was found, what the user can do, and whether the text has changed**.

### Proposed compact warning

```text
[attentive pangolin] This may be a password.
Mask it before sharing.

Dismiss       [Mask text]
```

For the red variant:

```text
[protective pangolin] Password detected.
Mask it before sharing.

Dismiss       [Mask text]
```

The shorter headline must still match the engine's certainty. Do not upgrade a possible finding into a definite statement simply to make the copy stronger.

### Layout specification

- Aim for roughly **112–144 dp** height at standard text scale, as a prototype target.
- Use **12–16 dp** padding, a **10–12 dp** radius, and the existing red or amber surface.
- Give the main action at least 48 dp height.
- Use a separate action row when inline actions would crowd the headline.
- Let the banner grow for long labels or large text. Never force the height by clipping advice or shrinking type.
- Keep ordinary key sizes stable. Optimize the banner before reducing key usability.
- Keep the button bar clean and focused on two actions: Dismiss and Mask text.

### Action semantics

| Current | Proposed | What it must do |
|---|---|---|
| Hide | **Mask text** | Replace the detected sensitive content in the actual draft, not merely obscure the UI. |
| Review | *Removed* | Evaluated and removed. It was a redundant no-op / context-breaking app switcher that crowded mobile keyboards. |
| Dismiss | **Dismiss** | Remove the interruption while leaving the draft unchanged. |
| Send anyway, during supported interception | **Send anyway** | Proceed only after the user's explicit choice in that specific intercepted flow. |

The code confirms that Hide calls the masking engine and replaces draft text. Its label is now **Mask text**. The former "Review" button was removed to eliminate keyboard banner clutter and prevent jarring app switches while typing.

Do not disguise **Send anyway** as Dismiss. Do not add a hold gesture or another confirmation purely as visual polish; retain established behavior unless a separately approved interaction change is needed.

### Expanded review

Show the category and useful explanation first. Keep any sensitive preview masked by default. If the user needs to inspect actual text, make reveal explicit and avoid copying raw secrets into a second persistent screen. Return focus to the original draft when the review closes.

Put the longer “If you already shared this password…” advice here rather than in every first-level warning. In practice mode, prefer demonstration instructions over post-leak advice.

### After masking

1. Confirm the replacement worked in the host editor.
2. Show **Text masked** with the curled pangolin briefly.
3. Recheck the updated draft, including other findings.
4. Return the strip to the appropriate actual state.

If replacement fails, retain the finding and show “Could not mask this text. Review it manually.” Do not curl into a success state after merely attempting an edit.

An Undo action is optional future work, not part of the baseline redesign: offer it only if restoring sensitive content is reliable, explicit, and compatible with the app's data-handling model.

### Host-app boundary

The warning lives in a native input-method surface. Tanggolin does not control every chat editor or app send button. Verify draft access, replacement, cursor behavior, and supported interception in representative host apps before making broad “before you hit send” promises. Android defines the input view and interaction through the IME and input connection. [Android input-method documentation](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)

## 13. Private notes: a practical tool with a branded moment

Rename the page to **Private notes**. Keep “Teach Tanggolin what to watch for” as a short supporting line, rather than a long page title.

### Proposed structure

```text
< Private notes
Teach Tanggolin what to watch for.

Note label
[Project nickname                          ]
Choose a label you can recognize later.

Private details
[The phrase or details to flag              ]
[                                          ]
Saved on this device, encrypted.

[Save private note]

Saved notes                                1 of 5
Project nickname                         [chevron]
Stored on this device
─────────────────────────────────────────────────
```

### Specific changes

- Remove the large enclosing lavender form card. The fields and button already establish structure.
- Use permanent labels **Note label** and **Private details**. Placeholders should be short examples, not the only labels.
- Use a multiline details field; a note may be longer than one sentence.
- Make clear that labels are visible in the note list, so users should choose discreet labels.
- Replace “Save and Protect Secret” with **Save private note**. Saving configures detection; it does not guarantee no future disclosure.
- Use one short encryption helper. Put algorithm names in technical details.
- Use a quiet **1 of 5 saved** count. Avoid “Secrets Protected,” which sounds like a success metric for future safety.
- If five is the actual product limit, explain it plainly when reached: “You have 5 notes. Delete one to add another.” Do not invent a paid tier.
- Keep raw note content out of the saved list. Avoid variable bullet strings that unnecessarily expose length or look like broken text.
- Open note details from the row; put delete there, with the existing confirmation flow adapted to explain that deletion stops watching for that note.
- Offer editing only if persistence and detection support it; do not add a nonfunctional edit affordance.

### States to design

| State | Specific treatment |
|---|---|
| Empty | Small pangolin with a blank note; “No private notes yet. Add a phrase or detail you want Tanggolin to watch for.” |
| Missing label/details | Inline error under the relevant field; retain what the user entered. |
| Saving | “Saving…”; prevent duplicate submissions. |
| Saved | “Private note saved” with a brief branded confirmation; update count and list. |
| Save failed | Clear error with retry; keep entered text in the current form. |
| Limit reached | Visible explanation and a route to manage existing notes. |
| Delete | “Delete this note? Tanggolin will stop watching for its details.” |

When the keyboard is open, scroll the focused field and save action into view. Avoid a large pinned button that covers the details field.

## 14. About & privacy: readable trust information

Keep the existing pangolin, but reduce the introductory stack. Replace the long quotation with two sentences:

> A pangolin curls up to protect itself. Tanggolin helps you protect private details in your drafts.

Avoid “impenetrable” for the animal or the app; the interface gains trust through precise behavior and evidence.

### Proposed hierarchy

1. Pangolin mark, **Tanggolin**, and **Privacy keyboard**.
2. Short brand statement.
3. **How your privacy works**: plain, separated rows.
4. **Technical details**: collapsed by default.
5. **Terms & conditions**: clear navigation row.
6. **Team & credits** and version: quieter footer content.

### Replace equal cards with fact rows

| User-facing heading | Short explanation direction |
|---|---|
| Checks run on this phone | Explain local draft checking, if verified. |
| Notes are encrypted | Explain protection of saved notes, if verified. |
| Typed text is not saved | Describe the actual logging and retention behavior, if verified. |

Each fact should have a short heading and one or two supporting lines, with a small standard icon if useful. A divider is enough; each fact does not need its own tinted box.

Put model names, model sizes, ONNX Runtime, AES-GCM, and Android Keystore in **Technical details**. Those details are useful for interested readers but do not help most users decide what to tap.

Replace “Impossible to leak data to the cloud” with a bounded statement such as “Tanggolin runs draft checks on this device.” The inspected main manifest has no INTERNET permission, but that alone does not prove all claims about data flows, backups, dependencies, or every build variant.

Use “hardware-backed” only where device capability and the actual key configuration support it. Keep technical statements conditional when hardware support varies. This review does not establish those guarantees.

Check the scrolled screen's content clipping. The screenshot suggests that a card reaches the fixed header boundary, but a still image does not prove the exact cause. Define a clean viewport beneath the app bar and verify scrolling on-device.

## 15. Terms sheet: separate reading from acceptance

Keep the current policy text until it receives its own content review. This section proposes presentation changes, not new legal terms.

- Reduce the decorative shield tile. Use **Terms & conditions** as the main heading and one short subtitle.
- Give the close control a 48 × 48 dp touch area and an accessible name.
- Use a pinned header, a bounded scroll body, and a separate footer.
- Add enough body inset that lines are not hidden behind the pinned header.
- Use clear section headings and a readable 16-sized body with generous line spacing.
- Make numbered markers smaller and quieter; they should support navigation through the text.
- Avoid side-by-side columns or large left indents that compress paragraph width.
- In the required first-time acceptance context, preserve the existing acceptance requirement and provide a clearly labeled action.
- When viewing previously accepted terms from About, use **Close** or **Done** rather than requiring acceptance again.
- Use a full-height sheet or reading page on short screens or at large text sizes.
- Ensure the scrollable area accounts for the footer and bottom system inset.

Do not tie acceptance to an animation finishing or to reaching the bottom unless that is an intentional requirement in the existing consent design. A visual refresh must not silently change consent behavior.

## 16. Copy replacements ready for design layouts

These are proposed product strings, subject to capability verification where noted above.

| Current | Replace with |
|---|---|
| Practice Sandbox | Try protection |
| Scratchpad | Test message |
| Paste or type sensitive text to verify detection and masking… | Choose an example or type a test message. |
| Synthetic quick tests | Safe examples |
| Account Password | Password |
| OTP Code | OTP |
| Gemini Key | API key |
| Clear Text | Clear |
| Teach Me What's Private | Private notes |
| Add Private Note | Add a private note |
| Save and Protect Secret | Save private note |
| 1 / 5 Secrets Protected | 1 of 5 saved |
| Protected Notes | Saved notes |
| Encrypted AES-GCM | Stored on this device |
| Enable in Settings | Enable the keyboard |
| Switch Input Method | Choose Tanggolin |
| Test Drive | Try a safe example |
| Careful! This looks like a password. | This may be a password. / Password detected. |
| NOTICE · INTENDED? | Check this detail |
| Alert! | Sensitive detail found |
| Hide | Mask text |
| Review | *Removed* (redundant no-op) |
| Protected locally | On-device |
| Privacy Architecture Breakdown | How your privacy works |
| Dual On-Device AI Models | Checks run on this phone |

Use sentence case throughout. Avoid calling a user's intent into question with “Intended?”; explain what was detected and offer a choice. Keep the possibility/confirmed distinction in the actual headline.

## 17. Motion and feedback

Use one signature motion: the pangolin curls after successful masking. Build the static state system first; animation is polish after correct state behavior.

- Pose transition: around **180–240 ms**, with gentle easing.
- Confirmation: visible briefly, around **1.5–2 seconds**, then yield to the current detection state.
- Onboarding: simple scene transitions rather than continuous floating or bobbing.
- Warnings: do not pulse indefinitely, shake the whole keyboard, or play repeated animations during typing.
- Respect reduced-motion settings with an immediate posture change and the same textual confirmation.
- Preserve existing user choices for haptic feedback. Do not add a vibration on every detector update.
- Interrupt outdated success animation if another finding appears.

The static posture and text must convey the state even if the user misses the animation.

## 18. Small-screen and accessibility rules

- Prototype at 320, 360, 390, and 412 logical-pixel widths, including a short portrait viewport and landscape.
- Verify at normal text scale and enlarged scales such as 1.3× and 2× where available.
- Keep actions reachable through scrolling; do not rely on the height of these tall screenshots.
- Include status bar, gesture/navigation area, cutouts, and keyboard insets.
- On purple system-bar backgrounds, use readable light system icons where the platform allows. The screenshots show dark status icons with poor visibility.
- Provide accessible names for back, close, delete, menu, app-opening mascot, and keyboard-switch controls.
- Announce a meaningful detection or masking result once; do not announce every keystroke, counter update, or animation frame.
- Keep the caret in the editor during a warning. Opening a review view should manage focus and return it appropriately.
- Use text and icon/posture alongside severity color.
- Never expose secret content in an accessibility label unintentionally.
- Test reading order across pinned headers, forms, review panels, and terms.

Verify with TalkBack and large text rather than relying on visual inspection alone. Flutter's accessibility guidance supports planning for assistive technology and system accessibility settings. [Flutter accessibility documentation](https://docs.flutter.dev/ui/accessibility)

## 19. Implementation map

This appendix maps the design to existing project areas. It is a starting point for implementation, not a completed change.

| Project area | Work |
|---|---|
| `Android App/lib/main.dart` | Establish shared semantic colors, type roles, button shape, and input defaults. |
| `Android App/lib/screens/onboarding_screen.dart` | Replace medallion art, refine story and actions, use actual setup completion, simplify checklist. |
| `Android App/lib/screens/practice_screen.dart` | Rename labels, restructure safe examples, improve editor contrast and layout, update counters reliably, keep one editor typeface. |
| `Android App/lib/screens/secrets_screen.dart` | Persistent field labels, plain form, compact saved rows, useful empty/error/limit states. |
| `Android App/lib/screens/about_screen.dart` | Short brand introduction, privacy fact rows, technical expansion, clearer terms presentation. |
| `Android App/assets/icons/tanggolin_icon.svg` and related assets | Preserve mark; derive small-size variant and a small consistent pose set. |
| `Android App/android/app/src/main/res/layout/keyboard_layout.xml` | Compact, readable strip and purposeful control areas. |
| `Android App/android/app/src/main/res/layout/warning_banner.xml` | Shorter hierarchy, explicit actions, accessible targets, scalable layout. |
| `Android App/android/app/src/main/res/values/colors.xml` and warning drawables | Retain values; unify role mapping and fix contrast with existing colors. |
| `Android App/android/app/src/main/kotlin/com/secondlook/WarningBanner.kt` | Clear labels and severity presentation; functional review and dismissal distinctions. |
| `Android App/android/app/src/main/kotlin/com/secondlook/SecondLookIME.kt` | State transitions, actual masking confirmation, host-editor behavior, status semantics. |

Keep Flutter and native Android definitions aligned through a documented token map. They render different parts of the experience; polishing the Flutter screens alone will leave the defining keyboard moment inconsistent.

## 20. Recommended order of work

### Priority 0: make the UI truthful and usable

1. Fix the counter update inconsistency.
2. Replace Hide with Mask text and verify real draft replacement.
3. Streamline warning actions (removed useless Review button, keeping clean Dismiss and Mask text).
4. Remove forced setup-completion fallback and refresh actual status.
5. Correct placeholder and amber action contrast using existing palette values.
6. Replace ambiguous “Protected locally” status with accurate processing/result language.
7. Verify header, footer, and keyboard overlap behavior.

**Review gate:** A fake sensitive detail can be detected, masked, or dismissed without misleading copy or extraneous buttons.

### Priority 1: establish the visible identity

1. Define tokens and consistent utility-page headers.
2. Create small pangolin and consistent contextual poses from the existing asset.
3. Redesign keyboard strip and warning hierarchy.
4. Replace onboarding medallions with three demonstration scenes.
5. Restructure practice and private notes.

**Review gate:** The brand is recognizable in onboarding and the keyboard without reading About; each screen has a distinct practical purpose.

### Priority 2: refine trust and finish

1. Simplify About and hide technical depth behind a clear expansion.
2. Improve terms reading layout while preserving consent requirements.
3. Add the successful curl transition and reduced-motion equivalent.
4. Complete large-text, TalkBack, short-screen, and host-app checks.
5. Capture release-mode screenshots without the DEBUG ribbon.

**Review gate:** The app remains clear and usable under realistic mobile constraints, not only in a tall portrait screenshot.

## 21. Final review checklist

### Visual identity

- [ ] The existing colors and mascot palette remain intact.
- [ ] Onboarding features the pangolin in purposeful scenes.
- [ ] The keyboard uses a legible small pangolin mark.
- [ ] Curling signifies a completed masking action.
- [ ] No arbitrary gradient blobs or duplicated medallion compositions remain.
- [ ] Most explanatory content uses sections or rows rather than equal cards.
- [ ] Font, spacing, corner radius, and alignment rules are consistent.

### Behavior and content

- [ ] Samples are fictitious and clearly identified.
- [ ] Character counts update for typing, paste, selection changes that edit text, clearing, sample insertion, and masking.
- [ ] Warning wording reflects detector certainty and category.
- [ ] Mask text changes the actual draft and preserves unrelated content.
- [ ] Failed edits do not produce a success pose or message.
- [ ] Warning actions are focused and clear (Dismiss and Mask text).
- [ ] Dismiss leaves text unchanged and does not imply protection.
- [ ] Setup states reflect actual Android settings.
- [ ] Every privacy claim has been checked against the implementation.
- [ ] A send-interception statement is limited to supported behavior.
- [ ] Forms cover empty, loading, error, success, and limit states.

### Mobile usability

- [ ] No clipping or unreachable actions at the proposed narrow widths and enlarged text scales.
- [ ] The draft and caret remain usable while warnings are visible.
- [ ] Warning actions and other general controls have suitable touch areas.
- [ ] Letter-key hit regions remain ergonomic and do not overlap.
- [ ] Status icons remain readable against system-bar backgrounds.
- [ ] TalkBack announces controls, findings, and results without leaking raw secrets or repeating constantly.
- [ ] Dismissing a warning banner returns focus sensibly.
- [ ] The terms body is never obscured by its fixed header or footer.
- [ ] Host-app tests cover a normal editor, a chat editor, and relevant special input types.

### Recognition test

Show someone the onboarding and a warning for five seconds, then ask:

1. What does this app help you do?
2. What will Mask text change?
3. What does the curled pangolin mean?

Look for: “It catches private details,” “It replaces them in my draft,” and “The text was masked.” This is a qualitative design check, not proof of protection effectiveness.

## 22. The target result

Tanggolin should be recognizable through a small set of deliberate choices: the familiar purple-and-gold pangolin, a clear protective state change, practical language, and a keyboard that remains easy to use.

Preserve your colors. Make the brand visible where the product acts. Give each screen a clear job, and make every status and action describe what actually happens.
