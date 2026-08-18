---
name: docket-design
description: Visual design system and UI standards for the Docket scanner app. Consult before building or modifying any screen, component, or visual element in this project.
---

# Docket Design System

Read this before writing any UI code in this project. It defines the visual language, the constraints, and — importantly — the things that are deliberately different from a default Material app.

## The premise

Docket competes against apps that look cluttered, cheap, and stuffed with upsell banners. Our visual position is the opposite: **restraint reads as trustworthiness.** A user handing this app a photo of their bank statement should feel like they handed it to a well-made tool, not a free app that will sell their data.

Two failure modes to avoid, in both directions:
- **Generic:** default Material 3 with no opinion. Looks like a tutorial project.
- **Overdesigned:** gradients, glassmorphism, decorative flourishes. Looks untrustworthy for a document app.

Aim for: quiet, precise, confident. Think professional instrument, not consumer social app.

## Colour

**Two environments, two treatments.**

Capture surfaces (camera, review) are **dark** — near-black, so the paper being scanned is the brightest thing on screen and the user's eye goes where it should. This also performs better outdoors and on OLED.

Content surfaces (library, detail, settings) are **light by default** — documents are white, and the app should feel like paper. Full dark mode supported.

- One accent colour only, used exclusively for primary actions and active states. Never decorative.
- Neutrals do the heavy lifting: a proper 9-step grey ramp, not three arbitrary greys.
- Semantic colours for warranty urgency (safe / approaching / imminent / expired) must be distinguishable by icon or label as well as hue — colourblind users and bright sunlight both defeat colour-only signalling.
- Never place text on a photograph without a scrim behind it.

## Typography

- **One distinctive display typeface for headings and screen titles.** This is the single highest-leverage change for perceived quality. The platform default everywhere is the strongest signal of an unfinished app.
- System font for body and UI labels — better multilingual coverage, better performance, better rendering at small sizes.
- Body text one step larger than Material default. Users are often older, outdoors, or reading in poor light.
- Maximum three type sizes visible on any single screen. If you need a fourth, the screen is doing too much.
- Never centre body text. Centre only short titles in empty states.

## Spacing and layout

- Scale: 4 / 8 / 12 / 16 / 24 / 32 / 48. Nothing off-scale.
- Screen edge padding: 20dp. Not 16 — the extra breathing room is a meaningful part of the "professional" read.
- Related items 8dp apart, unrelated groups 24dp apart. Grouping by proximity does more work than dividers. Prefer whitespace to lines.
- One primary action per screen, unmistakable. Everything else is visually secondary.

## Motion

Motion is where cheap apps and good apps separate most visibly, and it is cheap to add.

- Nothing longer than 300ms. Most things 150–200ms.
- Standard easing for entry, decelerate for elements arriving, accelerate for elements leaving.
- **Shared element transitions** from document thumbnail into document detail. This one transition alone significantly raises perceived quality.
- Staggered entry for list items — 30ms offset each, first eight items only.
- One moment of delight: the scan-complete animation. Earn it, don't overdo it.
- Haptics on capture, on successful save, on destructive confirmation. Nowhere else.

## Component rules

**Buttons** — 56dp height for primary, 48dp for secondary. Full-width primary at the bottom of a flow. Loading state replaces the label with a spinner, never disables silently.

**Cards** — documents render as white cards with soft elevation and a subtle edge shadow, so they read as sheets of paper. Rounded corners, consistent radius across the app. Pick one radius value and never deviate.

**Camera overlay** — animated corner brackets that snap to detected edges, not a static rectangle. Bracket colour shifts to the accent when a stable lock is achieved. Auto-capture shows a progress ring around the shutter.

**Filter selection** — always live thumbnail previews of the actual page, never text labels. The user should never have to imagine what "Enhance" does.

**Empty states** — a real illustration, a one-line explanation, and a single action. Never a centred grey sentence.

**Bottom sheets** for contextual actions. Dialogs only for destructive confirmation.

## Non-negotiable constraints

These override any aesthetic decision.

1. **RTL correctness.** Arabic and Urdu are core markets. Every screen must be tested mirrored. Directional icons must flip; brand marks must not.
2. **Touch targets** 48dp minimum, 56dp for primary actions.
3. **Font scaling to 200%** without layout breakage. Test it.
4. **WCAG AA contrast** on all text.
5. **Content descriptions** on every interactive element.
6. **No new dependencies** for visual effects without asking the user first.
7. **Nothing that implies network activity.** No cloud icons, no sync indicators, no "uploading" language anywhere in the UI. The offline claim is the product and the interface must never contradict it.

## Screen priority

If effort has to be rationed, spend it in this order:

1. **Camera capture** — the hero screen, the first screenshot, and where perceived quality is set
2. **Library** — the screen users see most often
3. **Review / edit** — where the user judges whether the app is any good
4. **Document detail**
5. Everything else

## Before you finish any screen

- Does it work in RTL?
- Does it work at 200% font scale?
- Does it work in dark mode?
- Is there exactly one obvious primary action?
- Would a stranger know what to do without instructions?
- Does anything on screen imply data is leaving the device?
