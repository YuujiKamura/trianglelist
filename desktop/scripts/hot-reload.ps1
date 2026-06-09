# Compose Desktop viewer hot reload
#
# desktop/src と common/src を監視し、Kotlin source が変わったら
# :desktop:run を kill して再ビルド + 再起動する。
#
# 状態保持型の hot reload (JetBrains Compose Hot Reload) は
# Kotlin 2.1.20+ / Compose Multiplatform 1.8.2+ が必要で、
# 現プロジェクト (Kotlin 2.0.0 / Compose 1.7.0) は要件未達。
# view state は ViewStateManager で永続化されているため、
# 再起動式でも zoom/pan/last file は復元される。
#
# 使い方:
#   pwsh -File desktop/scripts/hot-reload.ps1
#   pwsh -File desktop/scripts/hot-reload.ps1 -DxfPath sample/sample.dxf
#   pwsh -File desktop/scripts/hot-reload.ps1 -DxfPath C:\path\to\file.dxf

param(
    [string]$DxfPath = "sample/sample.dxf",
    [int]$DebounceMs = 800
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repoRoot

$watchRoots = @(
    (Join-Path $repoRoot "desktop\src\main\kotlin"),
    (Join-Path $repoRoot "common\src")
)

$script:viewerProc = $null
$script:pendingRestart = $false
$script:lastRestartTick = 0

function Stop-Viewer {
    if ($script:viewerProc -and -not $script:viewerProc.HasExited) {
        Write-Host "[hot-reload] stopping viewer (PID $($script:viewerProc.Id))..."
        try {
            # 子プロセス (java) も一緒に殺す
            taskkill /PID $script:viewerProc.Id /T /F 2>$null | Out-Null
        } catch {
            Write-Warning "stop failed: $_"
        }
    }
    $script:viewerProc = $null
}

function Start-Viewer {
    Stop-Viewer
    Write-Host "[hot-reload] launching viewer with DxfPath=$DxfPath ..."
    $gradleArgs = @(":desktop:run", "--args=$DxfPath", "--console=plain")
    $script:viewerProc = Start-Process -PassThru -NoNewWindow `
        -FilePath (Join-Path $repoRoot "gradlew.bat") `
        -ArgumentList $gradleArgs
    Write-Host "[hot-reload] viewer started (PID $($script:viewerProc.Id))"
}

function Schedule-Restart {
    $script:pendingRestart = $true
    $script:lastRestartTick = [Environment]::TickCount
}

# initial launch
Start-Viewer

# watchers
$watchers = @()
foreach ($p in $watchRoots) {
    if (-not (Test-Path $p)) { continue }
    $w = New-Object System.IO.FileSystemWatcher
    $w.Path = $p
    $w.Filter = "*.kt"
    $w.IncludeSubdirectories = $true
    $w.EnableRaisingEvents = $true
    Register-ObjectEvent $w "Changed" -SourceIdentifier "Changed-$p" -Action { Schedule-Restart } | Out-Null
    Register-ObjectEvent $w "Created" -SourceIdentifier "Created-$p" -Action { Schedule-Restart } | Out-Null
    Register-ObjectEvent $w "Deleted" -SourceIdentifier "Deleted-$p" -Action { Schedule-Restart } | Out-Null
    Register-ObjectEvent $w "Renamed" -SourceIdentifier "Renamed-$p" -Action { Schedule-Restart } | Out-Null
    $watchers += $w
    Write-Host "[hot-reload] watching $p"
}

Write-Host "[hot-reload] Ctrl+C to quit"

# debounce loop
try {
    while ($true) {
        Start-Sleep -Milliseconds 200
        if ($script:pendingRestart) {
            $elapsed = [Environment]::TickCount - $script:lastRestartTick
            if ($elapsed -ge $DebounceMs) {
                $script:pendingRestart = $false
                Write-Host "[hot-reload] change detected, restarting..."
                Start-Viewer
            }
        }
    }
} finally {
    foreach ($w in $watchers) {
        $w.EnableRaisingEvents = $false
        $w.Dispose()
    }
    Get-EventSubscriber | Unregister-Event -ErrorAction SilentlyContinue
    Stop-Viewer
}
