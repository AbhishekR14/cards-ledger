#!/bin/sh
# Compiles and runs the dependency-free parser, reward and multi-card suites.
# Mirrors tools/test.ps1 for machines without PowerShell.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
out=${1:-"$root/build/test"}
mkdir -p "$out"
src="$root/app/src/main/java/in/local/liveplus"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d "$out" \
 "$src/Engine.java" \
 "$src/NotificationCapture.java" \
 "$src/SmsCapture.java" \
 "$src/SettingsValues.java" \
 "$src/Json.java" \
 "$src/RewardPlan.java" \
 "$src/Rewards.java" \
 "$src/CardBook.java" \
 "$src/Alerts.java" \
 "$root/tests/EngineTest.java" \
 "$root/tests/NotificationCaptureTest.java" \
 "$root/tests/SmsCaptureTest.java" \
 "$root/tests/SettingsValuesTest.java" \
 "$root/tests/JsonTest.java" \
 "$root/tests/RewardPlanTest.java" \
 "$root/tests/RewardsTest.java" \
 "$root/tests/CardBookTest.java" \
 "$root/tests/AlertsTest.java" \
 "$root/tests/MultiCardTest.java"
java -cp "$out" EngineTest
java -cp "$out" MultiCardTest
