# Cards Ledger

An offline Android app for tracking spending and estimated rewards across **several Indian credit cards** — one tile per card, each with its own ledger, merchant mappings and reward rules.

Cards Ledger grew out of [Live+ Ledger](https://github.com/chetanatole/hsbc-liveplus-ledger) by Chetan Atole, a single-card tracker for the HSBC Live+ card. Its alert parser, merchant catalogue and HSBC cashback rules carry on here as the HSBC Live+ card's preset.

**Android 8.0+ · Native Java · On-device storage · No internet permission**

Independent project, not affiliated with any bank. Merchant classifications, posting dates and card terms decide the rewards actually paid.

## Download

**[Cards-Ledger-SMS.apk](https://github.com/AbhishekR14/cards-ledger/releases/download/test-build/Cards-Ledger-SMS.apk)** — around 91 KB ([release page](https://github.com/AbhishekR14/cards-ledger/releases/tag/test-build)).

That link always serves the newest build: each run of the workflow replaces the `test-build` release, so the address never changes. The release page names the commit it was built from, and `SHA256SUMS.txt` beside the APK carries its checksum.

It records purchases from **new incoming SMS**: it never reads your inbox, never sends messages, and asks for no notification access.

If the download will not start, it is the browser rather than the link. Chrome blocks APK files with *"This type of file can harm your device"* — tap **Download anyway**, then open the file from **Downloads** and allow installs from that app when Android asks. Release assets often fail to download inside the GitHub mobile app; open the link in a browser instead.

**This is a test build, not a release.** Every build signs with a newly generated key, so the next one will not install over it — uninstall the previous build first, exporting a backup from each card if you want to keep its ledger. Proper releases go through `release.yml`, which needs a retained signing key in the repository secrets.

## What it does

- **A grid of cards.** Each tile shows that card's rewards this period, its progress against the cap, and anything waiting for review. Tap a tile to open the card; press and hold to rename, recolour, reorder or remove it.
- **A ledger per card.** Transactions, refunds, duplicates, merchant mappings, statement period and backups stay separate.
- **Reward rules you enter yourself.** Each category earns a percentage or points per block of spend; mark the ones that share a cap; cap either the rewards earned or the qualifying spend. Presets for HSBC Live+, accelerated cashback, flat cashback and reward points, and plans import and export as JSON.
- **Capture routed by card.** One switch for the phone and a toggle per card. An alert goes to the card whose bank *and* last four digits both match, so a purchase is never counted twice.
- **Payments and credits too.** A bill payment is recorded and left out of rewards. A credit that reverses a purchase takes back that category's cashback or points — matched to the purchase automatically when the amount lines up, or chosen by you.
- **Review before it counts.** Suggested categories are provisional; unknown categories and possible duplicates stay out of the estimate until you confirm them, and refunds need linking to their purchase.
- **One backup for the whole app.** Settings → Back up everything writes a single file holding every card, its entries, its merchant mappings, its plan and its settings. Restore it after an uninstall and the app is as it was.
- Manual entry, pasted alerts, per-card backups, and colours that follow your wallpaper on Android 12 and newer.

[Using several cards](docs/MULTI-CARD.md) covers the reward-plan format and the routing rules in full.

## Setting it up

1. Install the APK on Android 8.0 or newer and open it.
2. Tap **+** and give the card a name, the bank as its messages write it (`HSBC`, `Axis`, `ICICI`), its last four digits, the statement closing day, and the closest reward preset.
3. **Settings** on the home screen → turn capture on and approve Android's incoming-SMS permission.
4. Open the card → **Settings → Reward plan** to set its rates exactly.

Capture is optional: adding a purchase by hand or pasting an alert works without granting anything. See [installation and troubleshooting](docs/INSTALLATION.md) for sideloading, Play Protect and permission prompts.

### Coming from Live+ Ledger

Cards Ledger installs as `in.local.cards`, beside the old app rather than over it. Export a backup there, add the card here with the **HSBC Live+** preset, then restore into it — the add-card flow offers this directly. [The full steps](docs/MULTI-CARD.md#coming-from-live-ledger).

## Privacy

Everything stays in the app's private storage on the phone: no internet permission, no analytics, no advertising SDKs, no inbox access. The app does not request `READ_SMS`; it sees only messages delivered after you turn capture on, filters them on the device, and keeps nothing but the transaction details. OTPs, declines and repayments are discarded. RCS messages are not delivered to an SMS receiver, so they are not captured. [PRIVACY.md](PRIVACY.md) has the detail.

## Build and test

The parser, reward and routing suites need only a JDK and run anywhere:

```sh
./tools/test.sh            # or: pwsh ./tools/test.ps1
```

APKs need Windows PowerShell, JDK 17, Android SDK Platform 35 and Build-Tools 35.0.0:

```powershell
./build.ps1 -Edition sms `
  -JavaHome 'C:/path/to/jdk-17' `
  -AndroidJar 'C:/path/to/Android/Sdk/platforms/android-35/android.jar' `
  -BuildTools 'C:/path/to/Android/Sdk/build-tools/35.0.0'
```

That writes `build/Cards-Ledger-SMS.apk`. A notification-capture edition, for phones where you would rather not grant SMS access, still builds with `-Edition notifications`; it is not published. No Gradle, Android Studio or emulator is involved.

In CI, **Actions → Test build APKs** builds and publishes the APK, and a push to the development branch does the same.

Further reading: [development](docs/DEVELOPMENT.md), [device testing](DEVICE-CHECK.md), [releases](docs/RELEASING.md), [reward assumptions for the HSBC card](docs/REWARDS.md), [merchant catalogue](docs/MERCHANTS.md), [capture editions](docs/EDITIONS.md).

## Contributing and license

[CONTRIBUTING.md](CONTRIBUTING.md) · [MIT License](LICENSE), which keeps Live+ Ledger's original copyright notice alongside this app's.
