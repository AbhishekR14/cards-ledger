# Multiple cards

This fork turns the single-card Live+ Ledger into **Cards Ledger**: a grid of credit cards, each
with its own ledger, merchant mappings and reward plan. The HSBC Live+ card is one tile among
them and still behaves exactly as upstream intends.

## The shape of it

- **Home** is a grid of tiles. A tile shows the card's name, bank, last four digits, what it has
  earned this period against its cap, and a dot when something needs review. Tap a tile to open
  the full ledger for that card; press and hold to rename, recolour, reorder or remove it.
- **A card's screen** is upstream's app — Overview, Activity, Settings — scoped to that card.
- **Settings on the home screen** holds what belongs to the phone rather than to one card:
  automatic capture, the SMS app it watches, the theme.

## What each card keeps to itself

| Per card | Shared by the device |
| --- | --- |
| Transactions, refunds, duplicates | Capture on/off and its Android permission |
| Merchant → category mappings | The SMS messaging app that capture watches |
| Last four digits, statement day, cap | Light or dark theme |
| The reward plan | |
| Backups: export and restore are per card | |

Card 1 uses the file names the single-card app used (`ledger.db`, `settings`), so nothing has to
be migrated inside the app.

## Adding a card

Tap **+** and give the card:

- **Card name** — whatever you call it, e.g. "Axis Ace".
- **Bank name as its messages write it** — the word capture looks for, e.g. `Axis`, `ICICI`,
  `HDFC`. This is how an alert is attributed to the right card, so it matters.
- **Last four digits** — matched against the card number in the alert.
- **Statement closing day** — or 0 for a calendar month.
- **Reward plan** — the closest preset; every rate is editable afterwards.

## Reward plans

A plan is a list of categories, each with a rate, plus the cap they share. Open a card →
**Settings → Reward plan**.

- **A category** earns either a percentage of the spend, or points per block of spend (for
  example 4 points per ₹150).
- **Shares this card's cap** marks a category as accelerated. Uncapped categories with a rate are
  the base rate; a category at 0% is excluded spend.
- **The cap** limits either the rewards earned in a period or the spend that qualifies for the
  accelerated rate.
- **Rounding** applies once to all categories paying the same rate, which is how a statement
  works and why four categories at 10% round as one figure rather than four.
- **Points plans** can state what one point is worth, and the card then also shows an estimated
  rupee value.

Removing a category renumbers the ones after it, and the card's existing transactions and
mappings are renumbered with it; purchases in the removed category go back to needing one.

### Editing a plan as JSON

**Settings → Reward plan → Plan as JSON** copies the plan out or pastes one in. The HSBC Live+
preset reads:

```json
{
  "id": "hsbc-liveplus",
  "name": "HSBC Live+",
  "unit": "cashback",
  "cap": { "basis": "reward", "amountPaise": 120000 },
  "categories": [
    { "name": "Dining / food delivery", "rateBps": 1000, "capped": true,  "rounding": "floor" },
    { "name": "Groceries",              "rateBps": 1000, "capped": true,  "rounding": "floor" },
    { "name": "Shopping",               "rateBps": 1000, "capped": true,  "rounding": "floor" },
    { "name": "Utilities",              "rateBps": 1000, "capped": true,  "rounding": "floor" },
    { "name": "Other eligible",         "rateBps": 150,  "capped": false, "rounding": "round" },
    { "name": "Excluded",               "rateBps": 0,    "capped": false, "rounding": "round" }
  ]
}
```

| Field | Meaning |
| --- | --- |
| `unit` | `cashback` or `points` |
| `pointValuePaise` | What one point is worth, for a points plan |
| `cap.basis` | `reward`, `spend` or `none` |
| `cap.amountPaise` | The cap, in paise (or in points for a points plan) |
| `categories[].rateBps` | Basis points: `1000` is 10%, `150` is 1.5% |
| `categories[].points` / `blockPaise` | Points earned per block of spend, instead of a percentage |
| `categories[].capped` | Whether the category counts towards the cap |
| `categories[].rounding` | `round` to the nearest, or `floor` down |

Category order is the category numbering, and a transaction stores its number. Append new
categories rather than reordering them.

## How an alert finds its card

Capture is one device-wide switch; each card then decides whether its own alerts are added, in
that card's **Settings → Automatic capture**.

When an alert arrives, each capturing card is tried in grid order, and a card takes it only if
**both** hold:

1. the alert names that card's bank, in its text or its sender id (`AD-AXISBK` carries `Axis`);
2. the card number in the alert ends in that card's four digits.

The first card that matches keeps it, so a purchase is never recorded twice. Everything else —
reading the amount, merchant, date and refunds, and rejecting OTPs, declines and repayments — is
upstream's parser, unchanged, so two cards at the same bank with different suffixes are separated
by the digits, and two cards sharing a suffix at different banks are separated by the bank.

Sender ids are an optional extra filter for a bank whose name is too common in other messages.

Merchant suggestions from the built-in catalogue apply to the **HSBC Live+ plan only**, because
that catalogue returns that plan's category numbers. On other cards, categories come from the
card's own merchant mappings until you set one, and anything unknown waits in review.

## Installing a test build

The **Test build APKs** workflow publishes
[Cards-Ledger-SMS.apk](https://github.com/AbhishekR14/cards-ledger/releases/download/test-build/Cards-Ledger-SMS.apk)
as the `test-build` prerelease. It captures purchases from new incoming SMS.

Chrome blocks APK downloads behind a *"This type of file can harm your device"* prompt — tap
**Download anyway** — and release assets often will not download inside the GitHub mobile app, so
open the link in a browser.

Upstream's notification-capture edition is still in the tree and still builds with
`./build.ps1 -Edition notifications`; this fork does not publish it. Install only one of the two
on a phone: they share an app identity.

Every run of that workflow generates its own signing key, so test builds cannot replace each
other: uninstall the previous one first, after exporting a backup from each card. Once you want
builds that upgrade in place, put a keystore in the `ANDROID_KEYSTORE_BASE64` and
`ANDROID_KEYSTORE_PASSWORD` repository secrets and tag a release, which `release.yml` signs with
that retained key.

## Coming from Live+ Ledger

Cards Ledger installs as a separate app (`in.local.cards`), so both can sit on the phone at once
and nothing is overwritten.

1. In Live+ Ledger: **Settings → Export backup (JSON)**.
2. Install Cards Ledger and add your HSBC card, choosing the **HSBC Live+** preset.
3. Choose **Restore a backup** when it offers, or open the card and use
   **Settings → Restore backup**.
4. Check a period's figures against the old app, then turn capture on here and uninstall the old
   app. Leaving both capturing means both will record the same purchase.

Backups are the same format in both apps, one file per card.
