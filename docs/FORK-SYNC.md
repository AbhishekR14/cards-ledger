# Keeping this fork in sync with upstream

This repository is a fork of [chetanatole/hsbc-liveplus-ledger](https://github.com/chetanatole/hsbc-liveplus-ledger)
that turns the single-card app into a multi-card ledger. The HSBC Live+ card is one tile among
several; everything upstream writes still powers that tile.

The fork is organised so that pulling upstream changes stays a routine, low-conflict operation.

## The arrangement

| Branch | Contents | Who writes it |
| --- | --- | --- |
| `main` | An exact mirror of `upstream/main`. Never commit here. | Upstream only |
| `claude/hsbc-multi-card-ledger-vqq6ku` | Mirror plus the multi-card shell. | This fork |

## Syncing

```sh
git fetch upstream
git checkout main
git merge --ff-only upstream/main          # fails if main was ever committed to
git push origin main

git checkout claude/hsbc-multi-card-ledger-vqq6ku
git merge main
# resolve conflicts (see below), then:
./tools/test.sh                            # or: pwsh ./tools/test.ps1
git push origin claude/hsbc-multi-card-ledger-vqq6ku
```

`tools/sync-upstream.ps1` runs the same sequence and stops at the first conflict.

## Why conflicts stay small

The multi-card work lives in **new files**. Upstream files are edited only where unavoidable, and
every such edit carries a `// MULTI-CARD:` comment so a conflict hunk is immediately recognisable.

**Never modified** — upstream owns these outright, so their fixes land in the Live+ tile untouched:

- `app/src/main/java/in/local/liveplus/Engine.java` — parser, merchant hints, category list, HSBC maths
- `app/src/main/java/in/local/liveplus/NotificationCapture.java`
- `app/src/main/java/in/local/liveplus/SmsCapture.java`
- `app/src/main/java/in/local/liveplus/SettingsValues.java`
- `app/src/main/java/in/local/liveplus/SmsApps.java`
- `tests/*.java` (upstream's four suites; this fork adds its own files beside them)
- `tools/Compile.java`, `tools/verify-apk.ps1`

**Modified, in small localised edits** — check these after a merge:

| File | Nature of this fork's edits |
| --- | --- |
| `app/src/main/java/in/local/liveplus/MainActivity.java` | Opens a card by id; labels come from the card's reward plan |
| `app/src/main/java/in/local/liveplus/Store.java` | Per-card database file and preferences file |
| `app/src/main/java/in/local/liveplus/CaptureStore.java` | Saves into the matched card's ledger |
| `app/src/notifications/java/in/local/liveplus/AlertListener.java` | Routes one alert across all cards |
| `app/src/sms/java/in/local/liveplus/SmsReceiver.java` | Same, for incoming SMS |
| `app/src/main/AndroidManifest.xml` | App identity, label, launcher activity |
| `build.ps1`, `tools/test.ps1` | Added test sources and the extra test runner |

## Resolving a conflict

1. **Prefer upstream's version of upstream's logic.** If a conflict is inside parsing, category
   suggestion or HSBC cashback maths, take upstream's side and re-apply the `// MULTI-CARD:` edit
   around it.
2. **Keep the fork's side for plumbing.** Card id, per-card store, plan-driven labels.
3. Run `./tools/test.sh`. `RewardsTest` asserts the HSBC Live+ preset produces numbers identical to
   `Engine.totals`, so a reward-rule change upstream shows up there as a failure rather than as a
   silently wrong tile. When that test fails after a sync, update the preset in `RewardPlan.java` to
   match upstream's new rules.

## When upstream changes HSBC's reward rules

The Live+ tile reads the `hsbc-liveplus` preset in `RewardPlan.java`. Mirror the new rates or cap
there, and the parity test confirms the two agree again. Existing cards keep the plan stored in
their own preferences; only cards still on the preset pick the change up.
