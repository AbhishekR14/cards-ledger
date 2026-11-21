# Changelog

## 1.4.0

- Swipe left and right to move between a card's Overview, Activity and Settings; swiping right
  from Overview goes back to the grid. A swipe has to be clearly sideways, so scrolling never
  changes tab by accident.
- Motion throughout: tabs slide in from the side you moved toward, a card's screen slides in from
  the grid and back out again, tiles and cards rise in one after another, the cap bar fills and
  the cashback figure counts up. All of it is skipped when Android's "Remove animations" is on.
- Tiles and the cashback card are soft gradients of the card's colour, lift slightly with a
  shadow, ripple and shrink a little when pressed.
- The theme follows the system by default, including the screen Android shows while the app
  starts, and a whole-app backup keeps that choice instead of restoring it as light.
- The Java package is now in.local.cards, matching the app. Installed data is unaffected.

## 1.3.0

- A credit can name the category it reverses, taking that category's cashback or points back. A
  credit for exactly the amount of a recent purchase takes its category as a suggestion and asks
  for review. A credit with no category is a bill payment and changes nothing.
- The Overview shows what credits reversed, what was paid to the card and net spend; a tile leads
  with net spend.
- A tier with one category is named after it, rather than "Other eligible spend".
- Buttons have room between them and the header sits lower.

## 1.2.0

- Payments and credits to a card are recorded instead of discarded.
- Settings → Back up everything writes one file with every card, entry, merchant mapping, plan and
  setting; Restore everything puts the app back as it was after a reinstall.
- Colours follow the wallpaper on Android 12 and newer.

## 1.1.0 — multi-card fork

First release of the **Cards Ledger** fork of Live+ Ledger. Tracks several credit cards, one tile
per card; the HSBC Live+ card keeps upstream's parser, categories and cashback maths.

- Home screen grid of cards, with each card's rewards this period, cap progress and review count.
- Each card keeps its own ledger, merchant mappings, statement period, cap and reward plan.
- Reward plans are editable per card: a percentage or points per block of spend for each
  category, a cap on rewards or on qualifying spend, and rounding to match the bank. Presets for
  HSBC Live+, accelerated cashback, flat cashback and reward points; plans import and export as
  JSON.
- Automatic capture is one device-wide switch; each card decides whether its own alerts are
  added. An alert goes to the card whose bank and last four digits both match, so a purchase is
  never counted twice.
- Device settings (capture, SMS app, theme) moved to the home screen; card settings stayed with
  the card.
- Installs as `in.local.cards` beside Live+ Ledger. Move data across with a JSON backup.

## Unreleased

- Refresh the Overview and Activity screenshots and keep the download section focused on APK selection.
- Show only the release version in Settings, with Android’s version code retained internally.

## 1.0.3 — 2026-11-07

- Provide separate Notifications and SMS APK downloads with the same Live+ Ledger name, app identity and retained signing key. Neither reads existing SMS or scans the inbox.
- Add opt-in new incoming-SMS capture using only `RECEIVE_SMS`, with consent, permission-denial handling, multipart parsing and duplicate protection. This APK has no notification listener or `READ_SMS` permission.
- Limit notification capture to one installed SMS messaging app. Explain Android's OTP-related content restriction in both Overview and Settings, including clearing the conversation notification before later alerts and pasting an already missed purchase.
- Improve extraction of readable grouped purchases around OTP entries, and prevent replay of known history through empty/hidden notification updates. Baseline visible history when capture starts and retain event identity when retrying a failed save.
- Apply valid settings edits automatically, with inline validation, protected card changes and explicit permission actions for capture access.
- Show spend and estimated cashback for the 10% and 1.5% groups, respect the accelerated cap and linked refunds, and show excluded 0% spend without a cashback sub-row. Match the total layout to the category breakdown.
- Preserve local ledger/settings/merchant mappings during compatible edition switches, with capture off until the new access method is enabled.
- Publish both APKs with individual checksums and enforce packaged permission/component isolation. Update installation, privacy, edition comparison and release documentation with direct downloads.

## 1.0.2 — 2026-11-03

- Expand built-in suggestions to 119 names/keywords, including business-name aliases, shopping and utilities.
- Match merchant-name boundaries and punctuation, prioritize grocery services over broader dining names, and leave ambiguous payment-platform names for review.
- Add a Settings manager showing only remembered and user-added merchant mappings, with add/edit/rename/delete controls.
- Apply saved merchant mappings consistently to captured alerts and pasted purchases without recategorizing existing transactions.

## 1.0.1 — 2026-11-03

- Add saved Light and Dark themes, with Light as the default and Appearance below card/capture settings.
- Make navigation tabs and the review filter distinct with outlines, selected states and touch/focus feedback.
- Keep Add purchase, Paste alert and Needs review controls visible above the Activity list; highlight the current period's review count.
- Restore scroll positions when returning to the app or recreating the screen, with separate positions for tabs, periods and review filters.
- Select purchase dates with a calendar picker and show 10%, 1.5% or 0% in reward-category labels.
- Reflect remembered merchant categories in the editor; unchecking and saving removes the merchant rule without changing other transactions.
- Simplify Overview, Settings, empty-state and transaction-source labels; hide scrollbars on Overview and Settings.
- Refresh Overview and Activity screenshots.
- Recheck official HSBC reward terms on 3 November 2026; retain the 10%/₹1,200 cap and 1.5% base rate, and mark the expired Myntra offer as historical.

## 1.0 — 2026-10-31

- Offline spending and estimated cashback dashboard.
- Configurable reward periods and accelerated cashback cap.
- Transaction-notification capture, category suggestions and merchant rules.
- Duplicate review and full/partial refund linking.
- Manual purchase entry, pasted-alert preview and JSON backup/restore.
