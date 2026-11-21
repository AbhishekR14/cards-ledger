# Device testing

The test suites cover parsing, routing and reward maths. This checklist covers what only a phone
can: permissions, capture, layout and storage. Work through it for every release.

Use synthetic alerts for anything you paste, and never make a purchase only to test the app.

## Install and first run

- [ ] The APK installs on Android 8.0 or newer, or updates an earlier release in place with its data
      intact.
- [ ] The home screen opens in the phone's light or dark theme, without a flash of the other.
- [ ] On Android 12 or newer, colours follow the wallpaper.

## Cards

- [ ] Adding a card from a preset shows its tile; adding a second card with the same name is refused.
- [ ] Pressing and holding a tile renames, recolours, reorders and removes it.
- [ ] Swiping left and right moves between Overview, Activity and Settings; swiping right on Overview
      returns to the grid; vertical scrolling never changes tab.

## Capture

- [ ] Turning on capture asks for the SMS permission, and the home screen then reads *Capture is on*.
- [ ] A real purchase alert appears once, on the right card, with the right amount, merchant and date.
- [ ] An OTP, a declined transaction and a statement reminder are not recorded.
- [ ] A bill payment alert is recorded as *Paid to the card* and does not change the reward.
- [ ] With two cards at one bank, each alert lands on the card whose digits it names.

## Ledger

- [ ] Paste alert previews a synthetic purchase before adding it.
- [ ] Changing a purchase's category updates the Overview and the tile.
- [ ] A credit set to reverse a category reduces that category's reward and the card's net spend.
- [ ] Needs review lists unknown categories, suggestions and possible duplicates, and clears as they
      are resolved.
- [ ] The period arrows match the card's statement closing day.

## Backups

- [ ] **Back up everything** writes a file; after uninstalling and reinstalling, **Restore
      everything** brings back every card, transaction, mapping and plan, and the theme.
- [ ] A one-card backup restores into an empty card.

## Accessibility

- [ ] With **Remove animations** turned on in Android's accessibility settings, screens appear
      without motion.
- [ ] Text stays readable at the largest font size, and every control can be reached with TalkBack.
