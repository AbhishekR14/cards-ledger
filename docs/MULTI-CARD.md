# Multiple cards

**Cards Ledger** is a grid of credit cards, each with its own ledger, merchant mappings and reward
plan. An HSBC Live+ card on its preset behaves exactly as the original single-card Live+ Ledger
did.

## The shape of it

- **Home** is a grid of tiles. A tile shows the card's name, bank, last four digits, what it has
  earned this period against its cap, and a dot when something needs review. Tap a tile to open
  the full ledger for that card; press and hold to rename, recolour, reorder or remove it.
- **A card's screen** has three tabs — Overview, Activity, Settings. Swipe left and right to move
  between them; swiping right from Overview goes back to the grid.
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
| `categories[].capped` | Whether the category counts towards the plan's shared cap |
| `categories[].ceilingPaise` | A ceiling for this tier alone, when the card caps each tier separately |
| `categories[].merchants` | Merchant names that fall in this category by default |
| `categories[].rounding` | `round` to the nearest, or `floor` down |

A card can cap each tier on its own (`ceilingPaise`) as well as share one cap across tiers
(`cap`), because Indian cards do both. The Swiggy HDFC preset uses per-tier ceilings; HSBC Live+
uses a shared cap.

`merchants` is what lets a preset categorise on its own: a purchase from a listed merchant lands
in that category without any setup. The longest listed name wins, so "Amazon Pay" in one category
beats a bare "Amazon" in another. A mapping you save on the card always wins over the plan's list,
and the built-in merchant catalogue applies only to a plan whose categories are still HSBC Live+'s.

Category order is the category numbering, and a transaction stores its number. Append new
categories rather than reordering them.

## Payments and credits

The purchase parser reads spending and turns a repayment away on purpose. The app keeps those
too: a bill payment, a reversed charge or a cashback credit is stored in the same ledger, marked
as money paid **to** the card.

A credit is one of two things, and you choose which:

- **A bill payment** — the default. It changes neither spend nor rewards, never needs review, and
  shows on the Overview's **Paid to the card** line.
- **A reversal** — a credit that gives back money spent in a category. Pick that category on the
  credit and it comes off that category's spend in the period it posts, taking that much
  cashback or points back with it. The Overview shows it as **Reversed by credits**, and the
  card's net spend and its tile both drop by it. If a period reverses more in a category than it
  spent, the reward for that tier goes below zero, as a statement's clawback does.

When a credit arrives for exactly the amount of an earlier purchase on the same card — within 60
days before it — the app takes that purchase's category as a **suggestion**. The reward comes
off straight away, as any suggested category counts, but the credit is flagged for review: a bill
payment that happens to equal one purchase is the case to correct. With no matching purchase, a
credit stays a payment.

Activity shows credits with a minus in green and the note the alert used ("Payment received",
"Cashback credited"); tap one to change what it reverses.
- A refund against a purchase is *not* a credit: the app records those separately and links them to
  the purchase they reverse, which reduces that purchase's eligible spend.
- Add one by hand with **Add purchase**, ticking "Money paid to the card, not spent".

## Backing up everything

A card's own Settings screen backs up that card alone. **Settings → Back
up everything**, on the home screen, writes one file with:

- every card, with its number so its ledger is found again, its name, bank, sender filters,
  colour and capture toggle;
- every transaction, refund and credit, with its category, source and replay id;
- every merchant mapping;
- every reward plan, and each card's last four digits, statement day and cap;
- the theme.

**Restore everything** replaces what is on the phone with the file's contents, so the app comes
back exactly as it was after an uninstall. Capture stays off until you turn it on again, as a
restore should not start writing to ledgers on its own. The file is plain JSON holding financial
data: keep it somewhere private.

## Card presets

Rates below were read from the issuers' own pages on **21 November 2026**. Indian card terms move
constantly and several of these were mid-change, so treat a preset as a starting point: check it
against your statement and edit it in the card's **Settings → Reward plan**.

| Preset | What it assumes | How solid |
| --- | --- | --- |
| **HSBC Live+** | 10% on dining, groceries, shopping, utilities sharing a ₹1,200 monthly cashback cap; 1.5% base | The original Live+ Ledger's ruless, parity-tested against them |
| **Amazon Pay ICICI** | 5% on Amazon (Prime), 2% at Amazon Pay partners, 1% elsewhere, no cap | Structure is well documented. **Change 5% to 3% if you are not a Prime member.** ICICI's 2026 fee changes add charges on wallet loads, gaming and large transport spends |
| **Swiggy HDFC** | 10% on Swiggy app capped at ₹1,500 a month, 5% on online spends capped at ₹1,500, 1% elsewhere | The 10% tier and its ₹1,500 cap are confirmed; the 5% tier's cap is not. The card now comes as **BLCK** (10% on Swiggy) and **Ornge** (5%) — this preset is BLCK |
| **PhonePe HDFC Ultimo** | 10% on PhonePe bills and recharges capped at ₹1,000, 5% on select online brands capped at ₹500, 1% on UPI scan and pay capped at ₹500 | Rate structure comes from PhonePe's own materials; **the caps are third-party figures and unverified** |
| **Axis NEO** | 1 point per ₹200, a point worth ₹0.20; fuel, rent and utilities earn nothing | Correct as a reward model, but this card's real value is coupon offers on Zomato, Blinkit and bill payments, which a ledger cannot track |
| **IDFC FIRST Millennia** | 3 points per ₹200 normally, 10 per ₹200 above ₹20,000 of monthly spend and on your birthday, 1 per ₹200 on FASTag and railways, a point worth ₹0.25 | Rates confirmed in the bank's own January 2026 notice. **The ₹20,000 threshold is not applied automatically** — put those purchases in the accelerated category yourself |
| **Scapia Federal** | 20 coins per ₹100 on Scapia app travel bookings, 10 per ₹100 on everyday spends, 5 per ₹100 on UPI, a coin worth ₹0.20 (5 coins = ₹1) | Coin rates and the ₹0.20 value are consistent across sources; the UPI tier comes from Scapia's own app listing. Utilities and insurance stopped earning coins on 27 February 2026. The **UPI tier only applies to the RuPay variant**, since only RuPay credit cards can be linked to UPI |
| **HDFC Regalia Gold** | 4 points per ₹150, 20 per ₹150 at Nykaa, Myntra, Reliance Digital and Marks & Spencer, a point valued at ₹0.20 | Earn rates are on HDFC's own page. **The point value depends on how you redeem**: ₹0.20 as statement cashback, around ₹0.50 through SmartBuy flights and hotels — the preset takes the conservative route. HDFC lists changes effective 15 May 2026 that I could not read; check for a monthly cap on the 5X tier |
| **IndusInd Tiger** | 1 point per ₹100, a point worth ₹0.40 | **The rate is a slab, not a constant**: 1 point per ₹100 up to ₹1 lakh of annual spend, rising to 6 above ₹5 lakh. Raise the rate in the plan as your yearly spend crosses a slab. Cash redemption is capped at 5,000 points a month |
| Accelerated cashback, Flat cashback, Reward points | Blank starting shapes | Yours to fill in |

Three things no plan can work out on its own, so handle them yourself:

- **A threshold that depends on a running total** — IDFC's ₹20,000 a month, IndusInd's annual
  spend slab. Categorise those purchases by hand, or edit the rate when a slab changes.
- **A discount applied at checkout** rather than paid as a reward — Axis NEO's coupons on Zomato
  and Blinkit. A ledger never sees it.
- **A reward whose value depends on how you redeem it** — Regalia points are worth ₹0.20 as
  cashback and around ₹0.50 through SmartBuy. Set the point value to the route you actually use.

### Two cards from the same bank

Nothing special is needed: add a tile for each, give both the same bank name, and let the last
four digits tell them apart. An alert names one suffix, so only one card takes it. Card names
must differ, so call them something like "Scapia RuPay" and "Scapia Visa".

A **Scapia Federal RuPay and Visa pair** can share the preset. Only the RuPay card can be linked
to UPI, so only its tile will ever fill the UPI tier; on the Visa card that category simply stays
empty, or you can remove it from that card's plan. Each card keeps its own plan, so editing one
leaves the other alone.

## How an alert finds its card

Capture is one device-wide switch; each card then decides whether its own alerts are added, in
that card's **Settings → Automatic capture**.

When an alert arrives, each capturing card is tried in grid order, and a card takes it only if
**both** hold:

1. the alert names that card's bank, in its text or its sender id (`AD-AXISBK` carries `Axis`);
2. the card number in the alert ends in that card's four digits.

The first card that matches keeps it, so a purchase is never recorded twice. Everything else —
reading the amount, merchant, date and refunds, and rejecting OTPs, declines and repayments — is
the purchase parser's work, so two cards at the same bank with different suffixes are separated
by the digits, and two cards sharing a suffix at different banks are separated by the bank.

Sender ids are an optional extra filter for a bank whose name is too common in other messages.

### Merchant names other banks write differently

The purchase parser reads the merchant from `at ...`, `towards ...` or `merchant: ...`, which is how HSBC
writes an alert. ICICI writes `on 21-Nov-26 on AMAZON PAY IN R`, and Axis puts the merchant
between the transaction time and the available limit. Those two shapes are read as a fallback,
and only when the parser found no merchant, so its own reading is never overridden.
An alert that still yields nothing is recorded as "Unknown merchant" and waits in review.

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

A notification-capture edition is still in the tree and still builds with
`./build.ps1 -Edition notifications`; it is not published. Install only one of the two
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
