# Contributing

Thanks for helping improve Cards Ledger. The app's scope is offline tracking of credit card spending
and rewards; changes that add network access, accounts or analytics will not be accepted.

## Reporting a problem

Open an issue describing what you expected and what happened, with your Android version and the app
version from **Settings → About**.

**Never post real financial data.** If an alert was misread, rewrite it with made-up details: a
synthetic amount, a merchant name, and the digits `1234` in place of your card's. Do not include
OTPs, balances, reference numbers, backups or screenshots showing card details.

## Changing the code

1. Read [Development](docs/DEVELOPMENT.md) for the project layout.
2. For a parser, reward or capture change, add a test that reproduces the case first. Tests live in
   `tests/`; `MultiCardTest` runs the reward, routing, credit and backup suites, and `EngineTest`
   runs the parser and capture suites.
3. Run the tests with `./tools/test.sh` or `pwsh ./tools/test.ps1`.
4. For a change to the interface, build the APK and check it on a phone using the
   [device testing checklist](docs/DEVICE-TESTING.md).
5. Describe what changed and how you verified it in the pull request.

Before committing, check `git status`: keystores, APKs, backups and databases must never be added.
`.gitignore` excludes the common ones.

By contributing, you agree that your contribution is licensed under the project's
[MIT License](LICENSE).
