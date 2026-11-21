# Privacy policy

Cards Ledger is an offline app. It has no servers, no accounts and no internet permission, so
nothing you enter or capture can leave your phone through it.

## What the app accesses

| Access | Why | Limits |
| --- | --- | --- |
| **Receive SMS** (`RECEIVE_SMS`) | To record purchases from your banks' transaction alerts | New messages only, from the moment capture is turned on. The app never reads your inbox (`READ_SMS` is not requested) and cannot send messages. |

Capture is optional. Everything else in the app works without any permission.

Android delivers every new SMS to an app holding this permission. Cards Ledger checks each message
on the phone and discards it unless it names the bank of one of your cards and ends in that card's
last four digits. OTPs, declined transactions and promotional messages are discarded too.

## What the app stores

From a matching message, only the transaction is kept: the amount, the merchant, the date, the card
it belongs to, and a one-way fingerprint used to avoid recording the same message twice. The message
text itself is not stored.

Alongside your transactions, the app stores what you set up: each card's name, bank, last four
digits, statement day, reward plan and merchant mappings, and your theme. The app never asks for,
and has no field for, a full card number, CVV, PIN, OTP or bank login.

All of it lives in the app's private storage, which other apps cannot read.

## Backups

A backup is written only when you ask for one, to a location you choose. Backup files are plain
JSON and contain your transactions and card details, so store them privately. Android's own cloud
backup is turned off for this app.

## Deleting your data

- **One card:** its **Settings → Erase this card's ledger**, or remove the card from the home screen
  by pressing and holding its tile.
- **Everything:** uninstall the app, or clear its storage in Android's app settings.

## Third parties

There are none. The app contains no analytics, advertising, crash reporting or tracking libraries.

## Contact

Questions about this policy can be raised as an
[issue on GitHub](https://github.com/AbhishekR14/cards-ledger/issues).
