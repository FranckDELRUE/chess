# Downloads the Stockfish NNUE network used by :core:engine (name must match StockfishEngine.NETWORK_FILE).
$name = "nn-1a298aa575a0.nnue"
$dir = Join-Path $PSScriptRoot "..\core\engine\src\main\assets"
New-Item -ItemType Directory -Force $dir | Out-Null
$target = Join-Path $dir $name
Invoke-WebRequest "https://tests.stockfishchess.org/api/nn/$name" -OutFile $target
$hash = (Get-FileHash $target -Algorithm SHA256).Hash.ToLower()
if (-not $hash.StartsWith("1a298aa575a0")) { Remove-Item $target; throw "Checksum mismatch for $name" }
Write-Host "OK: $target"
