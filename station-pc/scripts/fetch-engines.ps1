# Downloads the PC engines into build/station/ (not versioned):
#   Stockfish 19 for Windows x86-64, universal build (picks AVX2 / AVX-512 at run time), GPL v3
#   lc0 v0.32.1 with the CUDA 12 backend and its NVIDIA runtime DLLs (no CUDA toolkit needed), GPL v3
# The Maia networks come from scripts/fetch-maia-nets.ps1 (build/maia/).
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"  # Invoke-WebRequest is very slow with the progress bar
$dir = Join-Path $PSScriptRoot "..\..\build\station"
New-Item -ItemType Directory -Force $dir | Out-Null

# SHA-256 as published by GitHub for each release asset.
$files = [ordered]@{
    "stockfish.zip" = @(
        "https://github.com/official-stockfish/Stockfish/releases/download/sf_19/stockfish-windows-x86-64-universal.zip",
        "3c8bf1f9ea66a09350a40df4f632288285ac206d99f33ab5842c408fc30b48a7")
    "lc0-cuda12.zip" = @(
        "https://github.com/LeelaChessZero/lc0/releases/download/v0.32.1/lc0-v0.32.1-windows-gpu-nvidia-cuda12.zip",
        "8d0ce17676eb15e303bea9e790742d31c94ce5d24107f6187adc58e820f6d2f7")
}

foreach ($name in $files.Keys) {
    $url, $expected = $files[$name]
    $target = Join-Path $dir $name
    if (-not (Test-Path $target)) { Invoke-WebRequest $url -OutFile $target }
    $hash = (Get-FileHash $target -Algorithm SHA256).Hash.ToLower()
    if ($hash -ne $expected) { Remove-Item $target; throw "Checksum mismatch for $name" }
    $out = Join-Path $dir ([IO.Path]::GetFileNameWithoutExtension($name))
    if (-not (Test-Path $out)) { Expand-Archive $target -DestinationPath $out -Force }
    Write-Host "OK: $name -> $out"
}
