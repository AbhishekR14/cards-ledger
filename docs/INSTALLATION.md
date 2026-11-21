# Installation

## Install the app

1. On an Android 8.0 or newer phone, download
   **[Cards-Ledger-SMS.apk](https://raw.githubusercontent.com/AbhishekR14/cards-ledger/downloads/Cards-Ledger-SMS.apk)**,
   which is always the latest release and works from the GitHub app as well as a browser.
   If Chrome warns that the file may be harmful, choose **Download anyway**. The same file is on
   the [releases page](https://github.com/AbhishekR14/cards-ledger/releases/latest).
2. Open the downloaded file. If Android asks, allow installs from the app you opened it with.
3. Open **Cards Ledger**.

To check a download, compare its SHA-256 with `SHA256SUMS.txt` on the release page.

### If Play Protect blocks the install

Play Protect can show **App blocked to protect your device** for any app installed outside the Play
Store that asks for SMS access. If you trust the APK you downloaded:

1. Open **Play Store → your profile → Play Protect → Settings** and turn off **Scan apps with Play
   Protect**.
2. Install the APK.
3. Turn scanning **back on** straight away, whether or not the install succeeded.

Never do this for a **Harmful app blocked** warning. Menu names vary between phones.

## Turn on automatic capture

1. Add at least one card with **+** on the home screen. Capture needs a card's bank name and last four
   digits to know what to look for.
2. Open **Settings** on the home screen and turn on **Capture new transaction SMS**.
3. Read the explanation, choose **Continue**, and allow the permission when Android asks.

Android grants this permission for incoming SMS from every sender. Cards Ledger keeps only messages
that name one of your cards' banks and end in that card's digits, ignores OTPs and everything else,
and never reads messages that arrived before capture was turned on. It cannot send SMS.

If the permission prompt never appears, or appears greyed out, Android is treating SMS access as a
restricted setting for an app installed from a file. Open **Android Settings → Apps → Cards Ledger →
⋮ → Allow restricted settings**, then turn capture on again.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| A purchase did not appear | That the card's **bank name** matches a word in the alert, and its **last four digits** match the card number printed in it. Then add the purchase with **Paste alert**. |
| Alerts appear on the wrong card | Two cards share a bank name and digits. Give one of them **sender ids** in its settings. |
| Nothing is captured any more | Android may be stopping the app in the background. Set **Android Settings → Apps → Cards Ledger → Battery** to *Unrestricted*. |
| Some messages never arrive | Messages delivered over RCS ("chat") rather than SMS are not visible to SMS capture. Paste them instead. |
| A purchase appears twice | The second is flagged as a possible duplicate in **Needs review**; delete one. |

## Updating

A release installs over an earlier one signed with the same key, and keeps every card and
transaction. If Android refuses the update because the signature differs, back everything up from
**Settings → Back up everything**, uninstall, install the new version and restore the backup.

## Moving to a new phone

Back everything up on the old phone, copy the file across, install Cards Ledger on the new phone and
use **Settings → Restore everything**. Then turn capture on again.
