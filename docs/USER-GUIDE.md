# User guide

Cards Ledger keeps a separate ledger for each of your credit cards and estimates the cashback or
points each one earns, from the transaction alerts your banks already send.

- [The app at a glance](#the-app-at-a-glance)
- [Adding a card](#adding-a-card)
- [How an alert finds its card](#how-an-alert-finds-its-card)
- [Categories and merchants](#categories-and-merchants)
- [Reward plans](#reward-plans)
- [Card presets](#card-presets)
- [Payments and credits](#payments-and-credits)
- [Refunds, duplicates and review](#refunds-duplicates-and-review)
- [Backups](#backups)

## The app at a glance

**Home** is a grid of your cards. Each tile shows what the card spent this statement period, net of
credits, and the reward it earned against its cap. A dot means something is waiting for review.
Tap a tile to open the card; press and hold to rename, recolour, reorder or remove it.

**A card** has three tabs:

| Tab | What it holds |
| --- | --- |
| **Overview** | The period's estimated reward, progress against the cap, spend by reward tier, credits and net spend |
| **Activity** | Every purchase, refund and credit in the period, with the controls to add, paste and review them |
| **Settings** | The card's digits, statement day, cap, reward plan, capture toggle, merchant mappings and backup |

Swipe left and right to move between tabs. Swiping right from Overview returns to the grid. Use the
arrows beside the dates to step through statement periods.

**Settings** on the home screen holds what belongs to the phone rather than to one card: automatic
capture and its permission, the theme, and the whole-app backup.

| Each card keeps | The phone keeps |
| --- | --- |
| Transactions, refunds and credits | Capture on or off, and its SMS permission |
| Merchant mappings | The theme |
| Last four digits, statement day and cap | |
| Its reward plan | |

## Adding a card

Tap **+** on the home screen and fill in:

| Field | What to enter |
| --- | --- |
| Card name | Whatever you call the card, for example *Axis Ace*. Names must be unique. |
| Bank name | The word the bank's alerts use, for example `HSBC`, `Axis`, `ICICI`, `HDFC` or `Federal`. Check one real SMS: this is how an alert is matched to the card. |
| Last four digits | The end of the card number as alerts print it. |
| Statement closing day | The day your statement closes, 1–31, or 0 to follow calendar months. Use the closing day, not the payment due date. |
| Reward plan | The closest preset. Every rate can be changed afterwards. |

Then turn on capture once, from **Settings** on the home screen, and approve Android's SMS permission.

## How an alert finds its card

Capture listens for **new** SMS only: it never reads your inbox or messages that arrived before you
turned it on. Each card can opt out in its own **Settings → Automatic capture**.

When an alert arrives, each capturing card is tried in grid order. A card takes the alert only when
**both** are true:

1. the alert names that card's bank, in its text or in the sender id (`AD-AXISBK` carries `Axis`);
2. the card number in the alert ends in that card's four digits.

The first card that matches keeps it, so a purchase is never recorded twice. Two cards at the same
bank are told apart by their digits, and two cards with the same digits at different banks are told
apart by the bank. For a bank whose name appears in unrelated messages, add its **sender ids**
(comma separated) in the card's settings to narrow matching further.

OTPs, declined transactions and statement reminders are discarded. Only the amount, merchant, date
and card are kept from a message; the message itself is not stored.

An alert that names no merchant is recorded as *Unknown merchant* and waits for review. Merchant
names are read from the wordings banks commonly use: "at SWIGGY", "towards AIRTEL", ICICI's
"on 21-Nov-26 on AMAZON PAY IN R", and Axis's merchant line between the time and the available limit.

**Paste alert** in Activity reads one copied message through the same rules, for anything capture
missed.

## Categories and merchants

Every purchase sits in one of its card's reward categories. A category is chosen, in order, from:

1. **Your mapping** for that merchant on that card, saved with *Remember this merchant's category*
   while editing a purchase, or from **Settings → Merchant categories**;
2. **The plan's merchant list**, for presets that know their own merchants — Swiggy on the Swiggy
   HDFC plan, Amazon on the Amazon Pay ICICI plan;
3. **The built-in catalogue** of about 120 merchant names, for the HSBC Live+ plan;
4. otherwise **Needs category**, which waits in review and is left out of estimates.

Suggestions from steps 2 and 3 count toward the estimate straight away but are marked *suggested*
until you confirm them. Mappings match whole words and ignore capitalisation and spacing.

Choose a category by the bank's merchant category, not the item bought: a phone bought at a
supermarket earns at the supermarket's rate.

## Reward plans

Open a card, then **Settings → Reward plan**.

- **A category** earns a percentage of spend, or a number of points per block of spend (for example
  4 points per ₹150).
- **Shares this card's cap** marks the categories that count toward the plan's cap. A category at 0%
  is excluded spend.
- **The cap** limits either the reward earned in a period or the spend that qualifies, whichever the
  card's terms describe.
- **A category ceiling** caps one tier on its own, for cards that cap each tier separately.
- **Rounding** happens once per rate, the way a statement totals it, rather than once per category.
- **Points plans** can state the value of a point, and the card then shows an estimated rupee value.
- **Merchants that count here** lists names that land in the category automatically.

Removing a category moves the ones after it up a place, and existing purchases and mappings follow
them. Purchases in the removed category return to *Needs category*.

### Plans as JSON

**Settings → Reward plan → Plan as JSON** copies a plan out or pastes one in. The HSBC Live+ preset:

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
| `pointValuePaise` | The value of one point, for a points plan |
| `cap.basis` | `reward`, `spend` or `none` |
| `cap.amountPaise` | The cap, in paise, or in points for a points plan |
| `categories[].rateBps` | Basis points: `1000` is 10%, `150` is 1.5% |
| `categories[].points`, `blockPaise` | Points earned per block of spend, instead of a percentage |
| `categories[].capped` | Whether the category counts toward the plan's shared cap |
| `categories[].ceilingPaise` | A ceiling for this category alone |
| `categories[].merchants` | Merchant names that fall in this category by default |
| `categories[].rounding` | `round` to the nearest, or `floor` down |

Category order is the category numbering, and each purchase stores its number: add new categories at
the end rather than reordering them.

## Card presets

Rates were read from the issuers' own pages in **November 2026**. Card terms change often, so treat a
preset as a starting point and check it against your statement.

| Preset | What it assumes | Notes |
| --- | --- | --- |
| **HSBC Live+** | 10% on dining, groceries, shopping and utilities, sharing a ₹1,200 monthly cashback cap; 1.5% on other eligible spend | Amazon, Flipkart and Myntra earn the 1.5% base rate. Fuel, international spend and several merchant codes are excluded. |
| **Amazon Pay ICICI** | 5% on Amazon, 2% at Amazon Pay partners, 1% elsewhere, no cap | The 5% rate is for Prime members; change it to 3% otherwise. Fuel, rent, EMIs and wallet loads are excluded. |
| **Swiggy HDFC** | 10% on the Swiggy app, capped at ₹1,500 a month; 5% online, capped at ₹1,500; 1% elsewhere | This is the BLCK variant. The Ornge variant pays 5% on Swiggy. Swiggy transactions need ₹249 or more for 10%. |
| **PhonePe HDFC Ultimo** | 10% on PhonePe bills and recharges, capped at ₹1,000; 5% at select online brands, capped at ₹500; 1% on UPI scan and pay, capped at ₹500 | Check the per-tier caps against your card's current terms. |
| **Axis NEO** | 1 point per ₹200, worth ₹0.20; fuel, rent and utilities earn nothing | Most of this card's value comes from checkout discounts, which never appear as rewards. |
| **IDFC FIRST Millennia** | 3 points per ₹200; 10 per ₹200 above ₹20,000 of monthly spend and on your birthday; 1 per ₹200 on FASTag and railways; a point is worth ₹0.25 | The ₹20,000 threshold is not applied automatically: move those purchases into the accelerated category yourself. |
| **Scapia Federal** | 20 coins per ₹100 on Scapia travel bookings, 10 on everyday spend, 5 on UPI; 5 coins make ₹1 | Only the RuPay variant can be linked to UPI. Utilities and insurance earn nothing. |
| **HDFC Regalia Gold** | 4 points per ₹150; 20 per ₹150 at Nykaa, Myntra, Reliance Digital and Marks & Spencer; a point is valued at ₹0.20 | Points are worth about ₹0.50 through SmartBuy flights and hotels: set the value to the way you redeem. |
| **IndusInd Tiger** | 1 point per ₹100, worth ₹0.40 | The rate rises with annual spend, up to 6 points per ₹100 above ₹5 lakh. Raise it in the plan as you cross a slab. |
| **Accelerated cashback**, **Flat cashback**, **Reward points** | Blank starting shapes | For any other card. |

Some things no plan can work out on its own:

- **A threshold that depends on a running total**, such as IDFC's ₹20,000 a month or IndusInd's annual
  slabs. Categorise those purchases by hand, or change the rate when a slab changes.
- **A discount at checkout** rather than a reward, such as Axis NEO's coupons. A ledger never sees it.
- **A point whose value depends on how you redeem it.** Set the point value to the route you use.

## Payments and credits

Money paid **to** a card is recorded alongside its spending. A credit is one of two things, and you
choose which:

- **A bill payment** — the default. It changes neither spend nor rewards and shows on the Overview's
  *Paid to the card* line.
- **A reversal** — money given back for spending in a category. Choose that category on the credit
  and it comes off that category's spend in the period it posts, taking that much cashback or points
  back with it. The Overview shows it as *Reversed by credits*, and the card's net spend and tile
  drop by it. A period that reverses more than it spent in a tier shows that tier's reward below
  zero, as a statement's clawback does.

When a credit arrives for exactly the amount of a purchase made on the same card in the previous 60
days, it takes that purchase's category as a **suggestion** and is flagged for review. Confirm it if
it was a refund; set it back to *Not a reversal* if it was a bill payment that happened to match.

To record a credit by hand, use **Add purchase** and tick *Money paid to the card, not spent*.

## Refunds, duplicates and review

**Needs review** in Activity filters the period down to what wants a look:

- **Unknown categories**, which are left out of estimates until you choose one;
- **Suggested categories**, which count already but are not yet confirmed;
- **Possible duplicates** — the same amount at the same merchant within two minutes — which are
  left out until you keep or delete one;
- **Refunds** whose message names a refund, which reduce spend once linked to the purchase they
  reverse; a refund posted in a later period corrects the original period;
- **Suggested reversals**, described above.

Estimates do not model bank rounding to the paise, delayed posting, fee waivers or welcome offers.
Changing a card's period, cap or rates recalculates its whole history.

## Backups

| Backup | Where | What it holds |
| --- | --- | --- |
| **Everything** | Home → **Settings → Back up everything** | Every card with its transactions, refunds, credits, merchant mappings, reward plan and settings, and the theme |
| **One card** | Card → **Settings → Export backup** | That card's transactions and settings |

**Restore everything** replaces what is on the phone with the file's contents, so after reinstalling
the app is exactly as it was. Capture stays off after a restore until you turn it on again. A
one-card backup restores into an empty card.

Backups are plain JSON and contain financial data. Keep them somewhere private.
