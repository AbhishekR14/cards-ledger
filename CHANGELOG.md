# Changelog

All notable changes to Cards Ledger are recorded here. The project follows
[semantic versioning](https://semver.org).

## 1.0.1

### Fixed

- HDFC alerts are now captured. Their card number was written as `x1234` or a bare `Card 1234`,
  and only `XX1234`, `**1234` or "ending 1234" were recognised. Credits read the suffix the same way.
- Alerts dated year first, as HDFC does with `2026-11-13:00:30:59`, keep their own date instead of
  the day the message was pasted.

### Changed

- Swiping between a card's tabs follows your finger: let go past a third of the screen, or flick,
  to move on, or let go early to spring back.
- Stepping through statement periods slides, the review filter crossfades, dialogs grow in and
  settle out, and fields that appear in a dialog ease into place.
- The home screen draws straight away from each card's last figures and catches up in the
  background, so returning to it never waits on the cards' ledgers.
- Scrollbars are gone throughout the app.
- The Reward rules & limits section appears only on HSBC Live+ cards, whose rules it describes.

## 1.0.0

The first release.

### Cards

- A home screen of cards, each tile showing the period's net spend and the reward earned against
  its cap, with a marker when something needs review.
- A separate ledger, statement period, reward plan and set of merchant mappings for each card.
- Swipe navigation between a card's Overview, Activity and Settings.

### Capture

- Automatic capture from new incoming SMS, matched to a card by its bank and last four digits.
- Merchant names read from HSBC, ICICI and Axis alert wordings, among others.
- OTPs, declined transactions and statement reminders discarded; possible duplicates flagged.
- Payments and credits recorded alongside spending. A credit can reverse a category, taking its
  reward back; one matching a recent purchase's amount is suggested as a reversal for review.
- Paste alert and manual entry for anything capture misses.

### Rewards

- Reward plans with percentage or points-per-block categories, a shared cap on rewards or on
  qualifying spend, per-category ceilings and statement-style rounding.
- Presets for HSBC Live+, Amazon Pay ICICI, Swiggy HDFC, PhonePe HDFC Ultimo, Axis NEO, IDFC FIRST
  Millennia, Scapia Federal, HDFC Regalia Gold and IndusInd Tiger, plus blank starting plans.
- Plans editable in the app and importable or exportable as JSON.

### Data

- A whole-app backup of every card, transaction, mapping, plan and setting, restored exactly after a
  reinstall; per-card backups too.
- No internet permission, analytics or advertising.

### Appearance

- Material You colours from the wallpaper on Android 12 and newer, light and dark themes following
  the system, rounded dialogs and filled text fields, and motion throughout that respects Android's
  Remove animations setting.
- An adaptive C+ launcher icon that fits every launcher's shape, with a themed variant for
  Android 13 and newer.
