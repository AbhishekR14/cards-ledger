param([Parameter(Mandatory)][string]$Apk,[Parameter(Mandatory)][string]$BuildTools)
# Fails the build unless the APK asks for RECEIVE_SMS and nothing else, registers its SMS receiver
# behind the system's BROADCAST_SMS permission, and contains no code that reads the SMS inbox.
$ErrorActionPreference='Stop'
$tool=Join-Path $BuildTools 'aapt2.exe'
$permissions=@(& $tool dump permissions $Apk)
if($LASTEXITCODE -ne 0){throw 'Could not inspect APK permissions.'}
$xml=(& $tool dump xmltree $Apk --file AndroidManifest.xml) -join "`n"
if($LASTEXITCODE -ne 0){throw 'Could not inspect APK components.'}
$actual=@([regex]::Matches(($permissions -join "`n"),"uses-permission: name='([^']+)'") | ForEach-Object {$_.Groups[1].Value})
if(($actual -join ',') -cne 'android.permission.RECEIVE_SMS'){throw "Unexpected APK permissions: $($actual -join ', ')"}
if($xml -match 'BIND_NOTIFICATION_LISTENER_SERVICE|SMS_DELIVER|E: queries'){throw 'APK contains a notification listener or app queries.'}
if($xml -notmatch 'SmsReceiver' -or $xml -notmatch 'android.permission.BROADCAST_SMS' -or $xml -notmatch 'android.provider.Telephony.SMS_RECEIVED'){throw 'SMS receiver must be protected and registered.'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Apk))
try{
 $entry=$archive.GetEntry('classes.dex');$stream=$entry.Open();$memory=[IO.MemoryStream]::new()
 try{$stream.CopyTo($memory);$dex=[Text.Encoding]::ASCII.GetString($memory.ToArray())}finally{$stream.Dispose();$memory.Dispose()}
 if($dex.Contains('Landroid/provider/Telephony$Sms$Inbox;') -or $dex.Contains('content://sms')){throw 'Inbox access found in APK.'}
}finally{$archive.Dispose()}
Write-Output "PASS: APK permissions, SMS receiver and no inbox access"
