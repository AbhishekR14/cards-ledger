# Releasing

Releases are built and published by the **Build and publish APK release** workflow when a version
tag is pushed.

## Versioning

Versions follow [semantic versioning](https://semver.org). The version lives in
`app/src/main/AndroidManifest.xml`:

- `android:versionName` is the version people see, such as `1.0.0`;
- `android:versionCode` is a whole number that must increase with every release, or Android will
  refuse to install the update.

## Signing

Every release must be signed with the same key, or it will not install over the previous one.
Create the key once and keep it safe:

```sh
keytool -genkeypair -keystore cards-ledger-release.p12 -storetype PKCS12 \
  -alias cards-personal -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=Cards Ledger"
```

Then add two repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The keystore file, base64-encoded (`base64 -w0 cards-ledger-release.p12`) |
| `ANDROID_KEYSTORE_PASSWORD` | The keystore password |

Store the keystore and password somewhere other than the repository. If the key is lost, users will
have to uninstall and restore from a backup to move to a release signed with a new one.

Without these secrets the workflow still publishes, signing with a key generated for that run, and
says so in the release notes.

## Publishing a release

1. Update `versionName` and `versionCode` in the manifest.
2. Add the release's changes to `CHANGELOG.md`, and update `.github/RELEASE_NOTES.md`.
3. Run the tests: `./tools/test.sh`.
4. Commit, then tag the commit and push the tag:

   ```sh
   git tag -a v1.0.0 -m "Cards Ledger 1.0.0"
   git push origin v1.0.0
   ```

The workflow checks that the tag matches `versionName`, builds and verifies the SMS edition, and
publishes a GitHub release with `Cards-Ledger-SMS.apk` and `SHA256SUMS.txt`. A tag with a suffix,
such as `v1.1.0-beta.1`, is published as a prerelease.

## After publishing

Install the release on a phone over the previous version and work through the
[device testing checklist](DEVICE-TESTING.md).
