# Downloads the S0-6 on-device LLM candidates (GGUF, Apache 2.0) into build/llm/,
# then pushes them to the tablet with -Push (app external files dir, no permission needed).
#   Qwen3-1.7B (Alibaba Qwen, Apache 2.0), Q4_K_M quantization by unsloth
#   EuroLLM-1.7B-Instruct (UTTER project, Apache 2.0), Q4_K_M quantization by mradermacher
param([switch]$Push)
$ErrorActionPreference = "Stop"
$dir = Join-Path $PSScriptRoot "..\build\llm"
New-Item -ItemType Directory -Force $dir | Out-Null

$files = [ordered]@{
    "qwen3-1.7b-q4_k_m.gguf" = "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf"
    "eurollm-1.7b-instruct-q4_k_m.gguf" = "https://huggingface.co/mradermacher/EuroLLM-1.7B-Instruct-GGUF/resolve/main/EuroLLM-1.7B-Instruct.Q4_K_M.gguf"
}
$sha256 = @{
    "qwen3-1.7b-q4_k_m.gguf" = "b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897"
    "eurollm-1.7b-instruct-q4_k_m.gguf" = "1cade17f491ea46a686dbee51fbd52442e0f001f102380c3b9d66b4a77f84093"
}

$ProgressPreference = "SilentlyContinue"  # Invoke-WebRequest is very slow with the progress bar
foreach ($name in $files.Keys) {
    $target = Join-Path $dir $name
    if (-not (Test-Path $target)) { Invoke-WebRequest $files[$name] -OutFile $target }
    $hash = (Get-FileHash $target -Algorithm SHA256).Hash.ToLower()
    if ($sha256[$name] -ne $hash) { Remove-Item $target; throw "Checksum mismatch for $name" }
    Write-Host "OK: $name $hash"
}

if ($Push) {
    # adb reports progress on stderr, which Windows PowerShell turns into errors: check exit codes instead.
    $ErrorActionPreference = "Continue"
    $remote = "/sdcard/Android/data/com.chesscompanion/files/models"
    adb shell mkdir -p $remote
    foreach ($name in $files.Keys) {
        adb push (Join-Path $dir $name) "$remote/$name" 2>&1 | Write-Host
        if ($LASTEXITCODE -ne 0) { throw "adb push failed for $name" }
    }
}
