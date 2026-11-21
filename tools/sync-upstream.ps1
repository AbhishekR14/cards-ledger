param(
 [string]$Upstream = 'upstream',
 [string]$UpstreamUrl = 'https://github.com/chetanatole/hsbc-liveplus-ledger',
 [string]$Mirror = 'main',
 [string]$Fork = 'claude/hsbc-multi-card-ledger-vqq6ku',
 [switch]$Push
)
# Pulls upstream into the mirror branch, then merges the mirror into the fork branch.
# Stops at the first conflict so it can be resolved by hand. See docs/FORK-SYNC.md.
$ErrorActionPreference = 'Stop'
function Run([string[]]$Arguments) {
 & git @Arguments
 if ($LASTEXITCODE -ne 0) { throw "git $($Arguments -join ' ') failed with exit code $LASTEXITCODE" }
}
$repo = Split-Path -Parent $PSScriptRoot
Push-Location $repo
try {
 if (@(& git status --porcelain).Count -gt 0) { throw 'Commit or stash local changes before syncing.' }
 $remotes = @(& git remote)
 if ($remotes -notcontains $Upstream) { Run @('remote','add',$Upstream,$UpstreamUrl) }
 Run @('fetch',$Upstream,'--tags')
 Run @('checkout',$Mirror)
 Run @('merge','--ff-only',"$Upstream/$Mirror")
 if ($Push) { Run @('push','origin',$Mirror) }
 Run @('checkout',$Fork)
 & git merge --no-edit $Mirror
 if ($LASTEXITCODE -ne 0) {
  Write-Warning 'Merge stopped with conflicts. Resolve them, run ./tools/test.sh (or tools/test.ps1), then commit.'
  Write-Warning 'docs/FORK-SYNC.md lists which side to prefer per file.'
  exit 1
 }
 $testScript = Join-Path $PSScriptRoot 'test.ps1'
 & $testScript
 if ($LASTEXITCODE -ne 0) { throw 'Tests failed after the merge. See docs/FORK-SYNC.md.' }
 if ($Push) { Run @('push','origin',$Fork) }
 Write-Output "Synced $Fork with $Upstream/$Mirror."
} finally {
 Pop-Location
}
