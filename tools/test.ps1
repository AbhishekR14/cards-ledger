param(
  [string]$JavaHome = $env:JAVA_HOME,
  [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (!$OutputDirectory) { $OutputDirectory = Join-Path $repoRoot 'build/test' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$suffix = if ([IO.Path]::DirectorySeparatorChar -eq '\') { '.exe' } else { '' }
if ($JavaHome) {
  $javac = Join-Path $JavaHome "bin/javac$suffix"
  $java = Join-Path $JavaHome "bin/java$suffix"
} else {
  $javac = (Get-Command "javac$suffix" -ErrorAction Stop).Source
  $java = (Get-Command "java$suffix" -ErrorAction Stop).Source
}
$sources = @(
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Engine.java'),
  (Join-Path $repoRoot 'tests/EngineTest.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/SmsCapture.java'),
  (Join-Path $repoRoot 'tests/SmsCaptureTest.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/SettingsValues.java'),
  (Join-Path $repoRoot 'tests/SettingsValuesTest.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Json.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/RewardPlan.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Rewards.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/CardBook.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Alerts.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Rows.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Credits.java'),
  (Join-Path $repoRoot 'app/src/main/java/in/local/cards/Vault.java'),
  (Join-Path $repoRoot 'tests/JsonTest.java'),
  (Join-Path $repoRoot 'tests/RewardPlanTest.java'),
  (Join-Path $repoRoot 'tests/RewardsTest.java'),
  (Join-Path $repoRoot 'tests/CardBookTest.java'),
  (Join-Path $repoRoot 'tests/AlertsTest.java'),
  (Join-Path $repoRoot 'tests/CreditsTest.java'),
  (Join-Path $repoRoot 'tests/VaultTest.java'),
  (Join-Path $repoRoot 'tests/MultiCardTest.java')
)
& $javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d $OutputDirectory @sources
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed: $LASTEXITCODE" }
& $java -cp $OutputDirectory EngineTest
if ($LASTEXITCODE -ne 0) { throw "Tests failed: $LASTEXITCODE" }
& $java -cp $OutputDirectory MultiCardTest
if ($LASTEXITCODE -ne 0) { throw "Multi-card tests failed: $LASTEXITCODE" }
