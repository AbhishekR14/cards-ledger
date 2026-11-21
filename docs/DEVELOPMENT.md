# Development

Cards Ledger is a native Android app in plain Java, with no third-party libraries and no Gradle. The
build is a short PowerShell script around the Android SDK's own tools, and the logic that matters
most — parsing, routing and reward maths — is plain Java with its own test suites.

## Project layout

```
app/src/main/java/in/local/cards/    shared code
app/src/sms/java/in/local/cards/     the SMS capture edition (published)
app/src/notifications/...            the notification capture edition (optional build)
app/src/main/res/                    themes, transitions, drawables
tests/                               plain-Java test suites
tools/                               test runners and APK verification
build.ps1                            builds, tests, signs and verifies one APK
```

| Class | Responsibility |
| --- | --- |
| `Engine` | Purchase parsing, merchant catalogue, statement periods, HSBC Live+ maths |
| `Alerts` | Routes an alert to its card; merchant fallbacks for other banks' wording |
| `Credits` | Reads payments and credits to a card |
| `SmsCapture`, `NotificationCapture` | Assemble messages and fingerprint them against replays |
| `RewardPlan`, `Rewards` | A card's reward scheme and the period totals it produces |
| `Rows` | Ledger rows, credits, reversal matching and review counts |
| `CardBook`, `Cards` | The card registry and each card's storage |
| `Store` | One SQLite ledger per card, with merchant mappings |
| `Vault` | The whole-app backup format |
| `Json` | A small strict JSON reader and writer, so the above stay testable |
| `HomeActivity`, `MainActivity`, `PlanEditor` | The interface |
| `Palette`, `Motion` | Colour, including Material You, and animation |

Everything above `HomeActivity` in the table is free of Android dependencies, which is what lets the
test suites run on a plain JDK.

## Running the tests

```sh
./tools/test.sh            # macOS, Linux
pwsh ./tools/test.ps1      # Windows, or anywhere with PowerShell
```

This runs `EngineTest` (parser, periods, notification and SMS capture, settings validation) and
`MultiCardTest` (JSON, reward plans, reward totals, the card registry, alert routing, credits and
backups) — about five thousand checks. `RewardsTest` includes a randomised comparison of the generic
reward calculator against `Engine.totals` for the HSBC Live+ preset.

The **Tests** workflow runs the same suites on every push to `main` and on pull requests.

## Building the APK

Requirements: Windows PowerShell, JDK 17, Android SDK Platform 35 and Build-Tools 35.0.0.

```powershell
./build.ps1 -Edition sms `
  -JavaHome 'C:/path/to/jdk-17' `
  -AndroidJar 'C:/path/to/Android/Sdk/platforms/android-35/android.jar' `
  -BuildTools 'C:/path/to/Android/Sdk/build-tools/35.0.0'
```

With `JAVA_HOME` and `ANDROID_SDK_ROOT` set, `./build.ps1 -Edition sms` is enough. The script runs
the tests, compiles resources and code, signs the APK and checks that it requests only the
permissions its edition should, writing `build/Cards-Ledger-SMS.apk`.

On the first build it creates a personal signing key under `.signing/`, which `.gitignore` excludes.
Keep that folder: an update must be signed with the same key to install over an earlier build.

### Editions

The same code builds two editions that differ only in how they receive alerts:

| Edition | Receives alerts from | Permission |
| --- | --- | --- |
| `sms` (published) | New incoming SMS | `RECEIVE_SMS` |
| `notifications` | Notifications posted by one chosen SMS app | Notification access |

`./tools/build-all.ps1` builds both. `tools/verify-apk.ps1` fails a build whose manifest or code
contains the other edition's capture component.

## Preview builds

The **Test build APKs** workflow builds the SMS edition on every push to `main` that touches the app,
and publishes it as the `test-build` prerelease. It can also be started by hand from the Actions tab.
Each run signs with a newly generated key, so a preview build will not install over a release or over
another preview build.

## Conventions

- Amounts are integer paise throughout; never use floating point for money except where
  `Engine.totals` already does, which `Rewards` mirrors on purpose.
- Category numbers are stored on each transaction. A plan's category order is its numbering, so new
  categories are appended.
- Test data is synthetic. Use the digits `1234` and invented merchants, never real alerts.
