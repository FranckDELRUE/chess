# Downloads the Maia-1 networks (CSSLab, GPL v3) and lc0 for Windows (GPL v3) into build/maia/.
# Then run: python tools/maia2onnx/maia2onnx.py  (converts to ONNX into core/engine/src/main/assets).
$ErrorActionPreference = "Stop"
$dir = Join-Path $PSScriptRoot "..\build\maia"
New-Item -ItemType Directory -Force $dir | Out-Null

$files = [ordered]@{
    "maia-1100.pb.gz" = "https://github.com/CSSLab/maia-chess/releases/download/v1.0/maia-1100.pb.gz"
    "maia-1500.pb.gz" = "https://github.com/CSSLab/maia-chess/releases/download/v1.0/maia-1500.pb.gz"
    "maia-1900.pb.gz" = "https://github.com/CSSLab/maia-chess/releases/download/v1.0/maia-1900.pb.gz"
    "lc0.zip" = "https://github.com/LeelaChessZero/lc0/releases/download/v0.32.1/lc0-v0.32.1-windows-cpu-openblas.zip"
}
$sha256 = @{
    "maia-1100.pb.gz" = "e1cf1cd0c96b8a4fa6a275f4b9fd54ed1ffebf9fe44641b9fceded310e9619c4"
    "maia-1500.pb.gz" = "35ab6f20421d59e1df3b17c5a5016947af4c6761368ef84044a9a9c7619a9a00"
    "maia-1900.pb.gz" = "e2f565f42d7cd9f122557e6dc4eb84e5bbaedceda1d404dc485d3611c7c97a12"
    "lc0.zip" = "b2caa8443f0e0cb15cf76c335c53985f2973cd6438e77d3e2366cd21d2effa38"
}

foreach ($name in $files.Keys) {
    $target = Join-Path $dir $name
    if (-not (Test-Path $target)) { Invoke-WebRequest $files[$name] -OutFile $target }
    $hash = (Get-FileHash $target -Algorithm SHA256).Hash.ToLower()
    if ($sha256.ContainsKey($name) -and $sha256[$name] -ne $hash) { Remove-Item $target; throw "Checksum mismatch for $name" }
    Write-Host "OK: $name $hash"
}

$lc0 = Join-Path $dir "lc0"
if (-not (Test-Path (Join-Path $lc0 "lc0.exe"))) { Expand-Archive (Join-Path $dir "lc0.zip") -DestinationPath $lc0 -Force }
Write-Host "OK: $lc0\lc0.exe"
