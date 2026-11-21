<p align="center">
  <img src="docs/assets/icon.png" width="96" height="96" alt="Cards Ledger">
</p>

<h1 align="center">Cards Ledger</h1>

<p align="center"><strong>Track spending and rewards across all your credit cards, offline and on your phone.</strong></p>

<p align="center">
  <a href="https://github.com/AbhishekR14/cards-ledger/releases/latest"><img src="https://img.shields.io/github/v/release/AbhishekR14/cards-ledger?label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&amp;logoColor=white" alt="Android 8.0+">
  <a href="https://github.com/AbhishekR14/cards-ledger/actions/workflows/tests.yml"><img src="https://github.com/AbhishekR14/cards-ledger/actions/workflows/tests.yml/badge.svg" alt="Tests"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="License: MIT"></a>
</p>

Cards Ledger reads the transaction alerts your banks already send, files each purchase under the
right card, and estimates the cashback or points it earns under that card's own rules. There is no
bank login, nothing beyond each card's last four digits, and no internet permission: everything
stays on your phone.

## Features

- **Every card in one place.** A home screen of cards, each showing this period's net spend and
  the reward earned against its cap.
- **Automatic capture.** New transaction SMS are matched to the right card by bank and last four
  digits, so two cards at one bank, or two cards with the same digits, never get mixed up.
- **Reward rules per card.** Percentages or points per block of spend, shared caps and per-tier
  ceilings, category by category. Presets cover HSBC Live+, Amazon Pay ICICI, Swiggy HDFC, PhonePe
  HDFC Ultimo, Axis NEO, IDFC FIRST Millennia, Scapia Federal, HDFC Regalia Gold and IndusInd Tiger.
- **Payments and reversals.** Bill payments are recorded without touching rewards; a credit that
  reverses a purchase takes that category's cashback or points back with it.
- **A review queue.** Unknown merchants, suggested categories, possible duplicates and unlinked
  refunds wait for a look instead of quietly skewing the estimate.
- **One-file backup.** Every card, transaction, merchant mapping and reward plan, restored exactly
  after a reinstall.
- **Made for Android today.** Material You colours from your wallpaper, light and dark themes that
  follow the system, and swipe navigation.

## Download

**[Cards-Ledger-SMS.apk](https://github.com/AbhishekR14/cards-ledger/releases/latest/download/Cards-Ledger-SMS.apk)**
from the [latest release](https://github.com/AbhishekR14/cards-ledger/releases/latest). Requires
Android 8.0 or newer.

The app is installed from the APK rather than a store. If Chrome warns that the file may be harmful,
choose **Download anyway**. [Installation](docs/INSTALLATION.md) covers Play Protect, Android's
permission prompts and troubleshooting.

## Getting started

1. Open Cards Ledger and tap **+** to add a card: its name, the bank as its alerts name it, its last
   four digits, its statement closing day and the closest reward preset.
2. Open **Settings** on the home screen, turn on automatic capture and allow Android's SMS
   permission.
3. Use the card normally. Each purchase alert appears in the card's **Activity**, and its
   **Overview** shows the period's spend and estimated reward.
4. Adjust a card's rates under its **Settings → Reward plan** if your terms differ from the preset.

Capture is optional: purchases can be added by hand, or pasted from a copied alert.

The [user guide](docs/USER-GUIDE.md) explains matching, categories, reward plans, credits, review
and backups in full.

## Privacy

Cards Ledger has no internet permission, no analytics and no advertising. It asks only for
permission to receive new SMS, never reads your existing messages, and keeps nothing from a message
beyond the transaction it describes. All data lives in the app's private storage on your phone.
See the [privacy policy](PRIVACY.md).

## Documentation

| Document | Contents |
| --- | --- |
| [User guide](docs/USER-GUIDE.md) | Using the app, reward plans and presets |
| [Installation](docs/INSTALLATION.md) | Installing, permissions and troubleshooting |
| [Privacy](PRIVACY.md) | What the app accesses, stores and never does |
| [Development](docs/DEVELOPMENT.md) | Project layout, building and testing |
| [Releasing](docs/RELEASING.md) | Versioning, signing and publishing |
| [Device testing](docs/DEVICE-TESTING.md) | The checklist for verifying a build on a phone |
| [Changelog](CHANGELOG.md) | What changed in each release |

## Building from source

The parser and reward tests need only JDK 17:

```sh
./tools/test.sh          # or: pwsh ./tools/test.ps1
```

The APK builds on Windows with JDK 17 and Android SDK Platform 35 and Build-Tools 35.0.0, with no
Gradle or Android Studio required:

```powershell
./build.ps1 -Edition sms
```

See [Development](docs/DEVELOPMENT.md) for the details.

## Contributing

Issues and pull requests are welcome; please read [CONTRIBUTING.md](CONTRIBUTING.md) first. Never
include real card numbers, OTPs, balances or backups in an issue.

## License

Released under the [MIT License](LICENSE).

Cards Ledger is an independent project and is not affiliated with any bank or card issuer. Its
figures are estimates: your statement is the authority on what you earned.
