# Handoff: Docket app UI (reference-aligned)

## Overview
Docket is an on-device document scanner (Android, `kamidani711/docket`). This handoff covers the full redesigned UI: onboarding, sign in / sign up / password reset, home, scan, review + save sheet, files, document detail + share sheet, premium, and account/settings. The visual language is aligned to the user's reference UI kit (JPGs in `uploads/References/`): Urbanist typography, `#5468F0` primary, flat `#FAFAFA` tiles, a single 24px outline icon set that fills when active.

## About the Design Files
`Docket App.dc.html` in this bundle is a **design reference created in HTML** — a clickable prototype showing intended look and behavior. It is not production code to port. Recreate these screens in the app's existing environment: **Kotlin + Jetpack Compose**, following the repo's established patterns (`ui/screens/*`, `ui/navigation/Destination.kt`, `res/values/strings.xml`, Material 3 theme). Move the values below into the Compose theme (`Color.kt`, `Type.kt`, `Shape.kt`) rather than hardcoding them per screen.

## Fidelity
**High fidelity.** Colors, type, spacing, radii, shadows and icon geometry are final. Recreate pixel-for-pixel using Compose primitives; the prototype's device frame (398×862 px) approximates a 393dp-wide phone, so **read the px values as dp**.

## Design Tokens

### Color
| Token | Value | Use |
| --- | --- | --- |
| primary | `#5468F0` | brand, FABs, active tab, links, progress, selected states |
| primaryPressed | `#4356DB` | pressed/hover on filled primary |
| primaryTint | `#EDEFFE` | secondary button fill, chips, pastel tool circle, selected chip bg |
| coral | `#FF6B67` | premium card, logout, destructive ("Discard") |
| coralTint | `#FFECEB` | pastel tool circle (ID Card, Warranties) |
| amber | `#FCAF4E` | premium star badge, accent sliver behind premium card |
| amberTint | `#FFF3E1` | pastel tool circle (Scan Doc, Export CSV) |
| mint | `#2ED3A5` | success / protect icon |
| mintTint | `#E3F8F1` | pastel tool circle (Protect); `#1AA47F` text on this bg |
| violet | `#6C6BF0` | Import PDF icon; tint `#ECEAFD` |
| taupe | `#A1887F` | Receipt icon; tint `#F5EFEC` |
| ink | `#212121` | primary text, icon stroke |
| ink2 | `#616161` | tertiary labels |
| muted | `#9E9E9E` | secondary text, inactive tab, placeholder |
| line | `#EEEEEE` | dividers, input underline, 1px borders |
| tile | `#FAFAFA` | list rows, search field, cards |
| surface | `#FFFFFF` | screen background |
| dark.bg / dark.tile / dark.line / dark.text | `#181818` / `#232323` / `#2C2C2C` / `#FAFAFA` | dark mode (Home) |
| camera.bg / camera.chip | `#181818` / `#2C2C2C` | scan + review screens |

### Typography — Urbanist (Google Fonts; ship as `urbanist_*.ttf`)
| Role | Size / weight | Extras |
| --- | --- | --- |
| Screen title ("Docket", "Files", "Account", "Premium") | 23 / 800 | letter-spacing −0.01em |
| Onboarding headline | 28 / 800, line-height 1.25 | −0.01em |
| Auth headline | 30 / 800 | −0.01em |
| Premium price | 44 / 800 | −0.02em; "/ once" 17 / 600 |
| Section header ("Recent Files") | 20 / 800 | |
| Sheet title | 21 / 800 | |
| Screen sub-title (detail app bar, "Review (2 pages)") | 17 / 700 | |
| Settings row label, account name | 16 / 700 | |
| Primary button | 16–17 / 700 | |
| List item title | 15 / 700, line-height 1.35 | |
| Body / paragraph | 15 / 500, line-height 1.6, `muted` | |
| Field text | 16 / 600 | |
| Field label | 13 / 700, `muted` | |
| Row meta / caption | 12 / 500, `muted` | |
| Tool label | 12 / 600 | |
| Tab label | 11 / 500 (700 when active) | |
| Status bar clock | 14 / 700 | |

### Spacing, radius, elevation
- Screen horizontal padding: **24** (list containers 16 so rows read as 24 inside their 12 padding).
- Section rhythm: header 16 top / 18 bottom; divider `1px line` with 18 below.
- Radius: phone frame 38, big cards 26, sheets `28 28 0 0`, tiles/rows 16, folder icon 12, chips & buttons full pill (999), OTP box 16, doc thumbnail 6, tool circle 50%.
- Elevation: **flat by default** — tiles use fill, not shadow. Colored glow only on brand surfaces:
  - primary button `0 10px 24px rgba(84,104,240,.35)`
  - FAB `0 10px 26px rgba(84,104,240,.45)`
  - premium banner `0 12px 28px rgba(84,104,240,.35)`
  - coral premium card `0 16px 34px rgba(255,107,103,.32)`
  - paper preview `0 10px 30px rgba(33,33,33,.08)`; camera paper `0 18px 40px rgba(0,0,0,.45)`
  - capture ring `4px` halo `rgba(84,104,240,.22)`
- Toggle: 48×28 track (`#E0E0E0` off / primary on), 22 knob, 3 inset, 180ms ease.

### Icons
One outline set, 24px grid, **stroke 1.8, round cap + join**, `currentColor` = `ink` (or `ui.text` in dark mode, `#FFFFFF` on camera screens, tone color inside pastel circles). Active bottom-tab icons are the **filled** variant in primary. Members used: home, folder, star, user, search, camera, image, doc, shield(+check), gear, eye, logout, info, sort, folder-plus, arrow-right, arrow-left, chevron-right, check, crop, rotate, retake, frame, spark, flash, receipt, id-card, pdf, csv, grid, share, dots (vertical), dots-in-circle. Nearest off-the-shelf match: **Iconsax / Feather-style linear**, 1.8dp stroke.

## Screens / Views

### 1. Onboarding (3 slides)
White. Illustration block 270×290: `#EDEFFE` 200 circle, primary dot 28 at top-left (18% opacity), amber dot 18 bottom-right (50%), and a 146×222 white document card (radius 18, shadow `0 12px 34px rgba(33,33,33,.14)`) containing a 6×50 primary bar, a 3-col grid of 26-high tinted chips (radius 9), then two 32-high `#FAFAFA` blocks. Headline 28/800, body 15/500 muted max-width 300, then pager dots (8 circle, active 24×8 pill primary, inactive `#E0E0E0`, gap 6). Footer row, padding `0 24 36`: "Skip" pill (`primaryTint` bg, primary text, flex 1) + "Next" pill (primary, white, flex 1.4, glow) — Next advances slide, then goes to Sign in.
Copy, verbatim: 1 — "Scan anything, keep it on your phone" / "Receipts, warranty cards, leases. Docket captures pages and files them without sending anything to a server." 2 — "Every word is searchable" / "Text is read on device the moment a scan is saved, so you can find a document by what it says, not where you filed it." 3 — "Receipts and warranties, tracked" / "Docket pulls totals off receipts and reminds you before a warranty runs out."

### 2. Sign in
Back arrow 24. Headline "Welcome back". Body: "Docket keeps everything on this device. Sign in only if you want backup and sync." Fields are **underline only** (1.5px `line`; focused field 1.5px primary), label 13/700 muted above, value 16/600, 11 vertical padding, 22 gap. Checkbox 22 square radius 6, 1.5px primary border, filled primary + white check when on, label "Remember me" 15/600. Centered "Forgot password" link 15/700 primary. Divider row: 1px lines either side of "or continue with" (13/500 muted). Social row: 3 × 56-high boxes, 1px `line`, radius 16, glyph 17/700 `#616161`. Bottom: full-width primary pill "Sign in" (glow) + "Don't have an account? **Sign up**" 14/500 muted with primary bold link.

### 3. Sign up
Same field system. Headline "Create account"; body "Free forever. Premium is a single purchase, never a subscription." Fields: Full name (Andrew Ainsley), Email (andrew.ainsley@yourdomain.com), Password (masked). Consent row = same checkbox + 14/500 muted text "I agree to the Terms of Service and the Privacy Policy. Scans stay on this device." Bottom primary pill "Sign up" + link to Sign in.

### 4. Password reset
Headline "Check your mail"; body names the address. Four OTP boxes: flex 1, height 66, radius 16, 1.5px `line`, filled boxes `#FAFAFA` with 26/700 digit ("4","6","7", empty). Then "Didn't receive the email?" and "You can resend the code in **55 s**" (primary bold), both 14/500 muted centered. Primary pill "Confirm". A decorative numeric keypad sits at the bottom: `#FAFAFA` radius 18, 3-col grid, 46-high keys 21/600.

### 5. Home (dark-mode aware)
Header row (padding `16 24 18`): 28 primary circle logo with a 9 white inner dot + "Docket" 23/800; trailing search icon button (no background).
Tool grid: 4 columns, row gap 18, column gap 4, padding `0 16 20`. Each item = 54 circle in its pastel tint + 24 icon in the tone color, label 12/600 muted, gap 9. Items and routes: Scan Doc (frame, amber) → Scan; Receipt (receipt, taupe) → Scan; ID Card (id-card, coral) → Scan; Import PDF (pdf, violet) → Review; Warranties (shield-check, coral) → Files; Protect (shield, mint) → Files; Export CSV (csv, amber) → Files; All Tools (grid, primary, filled) → Files.
1px divider inset 24. Section row "Recent Files" 20/800 + primary arrow-right button → Files.
File rows (padding `0 16`, row = 12 padding, radius 16, `tile` bg, gap 14, 10 bottom margin): 46×62 thumbnail (radius 6, 1px `line`, striped fill `repeating-linear-gradient(180deg,#D4D4D4 0 2px,#F5F5F5 2px 6px)`), title 15/700, meta 12/500 muted (`12/03/2026   09:41`), trailing share + vertical-dots icons (20, gap 10). Rows: Whirlpool fridge receipt 12/03/2026 09:41 · Apartment lease 09/03/2026 10:20 · Dell XPS warranty card 04/03/2026 14:56 · Pharmacy receipt 28/02/2026 09:37. Tapping a row → Detail.
Two 56 FABs bottom-right (right 22, bottom 104, gap 12), primary with glow: camera → Scan, image → Review.
Bottom nav: borderless, padding `8 0 24`, 4 equal items, 23 icon + 11 label, gap 6; active = filled icon + primary + weight 700, inactive = outline + muted.
Dark mode swaps bg/tile/line/text/muted and the thumbnail stripes, and **the header search + row share/dots icons must follow the themed text color** (they are near-black in light mode).

### 6. Scan (camera)
`#181818`. App bar: white back arrow; trailing frame, spark, flash, dots-in-circle icons (22, gap 20). Viewfinder: flex-fill, margin `0 16`, radius 20, striped placeholder `repeating-linear-gradient(160deg,#2C2C2C 0 14px,#242424 14px 28px)`; detected page = inset `56 38`, 2px primary border, radius 8, fill `rgba(84,104,240,.12)`; four 16 white corner brackets (3px) inset 48/30; top center hint chip `rgba(24,24,24,.8)` pill, 14/700 white — text varies by mode: Document "Page detected — tap to capture", Receipt "Hold steady over the receipt", ID Card "Fit the card inside the frame", Book "Both pages inside the frame".
Mode strip: Document / Receipt / ID Card / Book, 14/700, active white with 2px primary underline, inactive `#BDBDBD`, gap 24.
Shutter row (padding `16 36 32`): 46 `#2C2C2C` radius-14 gallery button (folder icon) → Files; 76 white capture button with 5px primary ring + halo; 46×56 last-shot thumbnail, 2px primary border, radius 8, striped fill, with a primary "2" count pill offset −7/−9. Capture and thumbnail → Review.

### 7. Review + save sheet
`#181818`. App bar: back → Scan, "Review (2 pages)" 17/700, "Discard" 14/700 coral → Home. Page preview: `aspect 3/4`, padding `22 18`, radius 10, heavy shadow; fill depends on the selected filter (Enhance `#FCFCFD`, B & W `#FFFFFF`, Greyscale `#E8E8E8`, Original `#EFE9DD`); inside, a 9-high `#424242` title bar 60%, a 5-high `#9E9E9E` 40%, then 12 `#BDBDBD` 5-high lines at 100/92/97/88/100/72/95/90/60/98/84/45%.
Page actions: Crop / Rotate / Retake — 44 `#2C2C2C` radius-14 chip + 20 white icon, label 12/600 `#E0E0E0`, gap 34.
Filter row: 4 equal cards, 2px border (`#333333`, selected primary), `#222222` bg, radius 14, 38-high swatch (Original `#8A8A8A`, B & W `#F5F5F5`, Greyscale `#BDBDBD`, Enhance `#EDEFFE`), label 12/600.
Footer: "Add page" (`#2C2C2C`, white, flex 1) + "Save" (primary, flex 1.4, glow) → opens save sheet.
**Save sheet**: scrim `rgba(24,24,24,.6)`, white sheet radius `28 28 0 0`, padding `22 24 32`, 44×4 grab handle `#EEEEEE`. Title "Save document" 21/800. Underlined Name field (value "Whirlpool fridge receipt"). "Folder" label 13/700 muted + chips (No folder / Household / Tax 2026 / + New) — 1.5px border, radius pill, selected primary border + `primaryTint` fill + primary text. "Format" + two flex chips (PDF / Separate images) radius 14, same selected treatment. Actions: "Cancel" (`primaryTint`) + "Save to library" (primary, flex 1.4) → Files.

### 8. Files
Header: logo + "Files" 23/800; trailing sort and folder-plus icons (22, gap 16). Search field: `tile` bg, radius 16, padding `14 16`, 20 muted search icon + placeholder "Search text inside your scans" 15/500. Count line "Total 148 files · 620 MB on device" 14/600 muted. Folder grid: 2 columns, gap 10, `tile` card radius 16 padding 14, 44 primary rounded square (radius 12) with white folder icon, name 14/700, count 11/500 muted — Household 34 files, Tax 2026 21 files. Then the same file rows as Home (ink icons). Bottom nav with Files active.

### 9. Document detail + share sheet
App bar: back → Files, "Whirlpool fridge receipt" 17/700, dots. Paper: `aspect 3/4`, white, 1px `line`, radius 14, soft shadow, inset lines as on Review but `#EEEEEE`. Two info chips (12/700, pill, padding `8 14`): "Receipt · $1,249.00" primary on `primaryTint`; "Warranty to Mar 2029" `#1AA47F` on `mintTint`. Caption "Text layer read on device · 2 pages · 1.4 MB" 13/500 muted. Footer: "Edit" (`tile`, flex 1) → Review + "Share & export" (primary, flex 1.4, glow) → sheet.
**Share sheet**: same shell as save sheet, title "Share & export", 2×2 grid of `tile` cards radius 16 padding 16 — PDF / "With searchable text layer", JPEG pages / "One image per page", CSV / "Receipt totals and dates", Share sheet / "Send to any app" (label 15/700, note 12/500 muted). Full-width primary "Done".

### 10. Premium
Header logo + "Premium". Card stack: an amber sliver (26 wide, radius 20, right 12, inset top 34 / bottom 70) peeking behind a coral card (radius 26, padding `30 24 24`, coral glow). Card content, all white: price "$14.99 / once" centered 44/800 with 17/600 suffix; sub "One purchase, forever. Nothing recurring." 15/600 centered; 1px `rgba(255,255,255,.35)` rule with 22 margin; benefit rows (gap 14) = 22 white rounded square (radius 7) with a 14 coral check + text 15/600: "Batch export, several documents at once", "Searchable text layer in exported PDFs", "Additional OCR languages", "Unlimited folders"; white pill "Unlock Premium" 16/700 ink text. Below the card: text button "Restore purchases" 14/600 muted (hover primary). Bottom nav with Premium active.

### 11. Account / settings
Header logo + "Account" + dots-in-circle. Profile tile: `tile` radius 20 padding 16, 56 avatar circle (`#EEEEEE`, replace with the real photo), name 17/700 + "Free" badge (11/700 primary, 1px primary border, radius 6, padding `2 8`), "620 MB / 1024 MB" 12/600 muted, then a 6-high `#EEEEEE` track with a 60% primary fill (radius 3).
Premium banner: primary card radius 20 padding 18 with glow → Premium; 46 amber circle + filled white star; title "Go to Premium" 16/800 white; sub "Batch export, OCR languages, text layer" 12/500 at 90%; white pill "Unlock" 13/700 primary.
Settings rows: padding `16 0`, 22 outline icon + label 16/700 + optional value 14/600 muted + 18 chevron `#BDBDBD` — Personal Info (user), Scan preferences (gear, "High quality"), Security (shield-check, "Biometric"), Language (doc, "English (US)"), Backup & restore (info, "Off"), Recently deleted (folder, "6 files"). Then a Dark Mode row with the toggle (no chevron), a 1px divider, and a coral "Logout" row with the logout icon.

## Interactions & Behavior
- Flat navigation between the 11 destinations; the prototype's chip row at the top is a dev-only screen picker — do not ship it.
- Entry points: Onboarding Next → Sign in (after slide 3); Skip → Sign in; Sign in / Sign up submit → Home; Confirm (reset) → Sign in.
- Home: tool tiles route as listed; file row → Detail; camera FAB → Scan; image FAB → Review; search icon → Files; See-all arrow → Files.
- Scan: mode strip changes the hint chip only; shutter and last-shot → Review; gallery → Files.
- Review: filter selection restyles the page preview; Save opens the bottom sheet; "Save to library" closes it and lands on Files; Discard → Home.
- Detail: "Share & export" opens the sheet; "Done" closes it; Edit → Review.
- Account: Dark Mode toggle flips the Home theme (180ms knob slide); Premium banner → Premium.
- States: filled buttons darken to `#4356DB` on press; secondary/tile buttons darken one step (`#E2E6FD` / `#F2F2F2` / `#F4F4F4`); icon buttons drop to 60–70% opacity; sheets use a 50–60% scrim. Ripple/press feedback should use the platform default over these values.
- No loading, empty, or error states are designed yet — the reference kit has a "Not Found" search state (`uploads/References/05.1`) worth designing next.

## State Management
Screen-local UI state only; nothing here needs network. Per the prototype: `currentDestination`, `onboardingSlideIndex` (0–2), `rememberMe`, `darkMode` (persist in settings/DataStore), `captureMode` (Document/Receipt/IdCard/Book), `pageFilter` (Original/BW/Greyscale/Enhance), `saveSheetOpen`, `shareSheetOpen`, `targetFolder`, `exportFormat`. Real data comes from the existing document/folder repository: recent files, folder list, file/size totals, storage usage, receipt total and warranty date on the detail screen.

## Assets
- **Font**: Urbanist (Google Fonts, OFL) — weights 400/500/600/700/800.
- **Icons**: the prototype draws its own 1.8-stroke outline set; substitute an Iconsax/Feather-style linear set with matching geometry, plus filled variants of home/folder/star/user for the active tab.
- **Imagery**: document thumbnails and the profile photo are striped/gray placeholders in the prototype — wire them to real page bitmaps and the user's avatar.
- No image assets ship in this bundle.

## Files
- `Docket App.dc.html` — the full interactive prototype (all 11 screens, dark-mode toggle, both sheets). Open it in a browser; use the chip row at the top to jump between screens.
- Source references (not bundled): `uploads/References/*.JPG` in the design project.
- Repo touchpoints: `app/src/main/java/com/docket/ui/screens/…`, `ui/navigation/Destination.kt`, `res/values/strings.xml`.
