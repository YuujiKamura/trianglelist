# CAD Viewer の開発ループ 1 コマンド版 (2026-08-27)。
#
#   pwsh -File desktop/scripts/cad-dev.ps1 samples/8.25_bad.csv -Boxes -Capture out.png
#
# やること: 差分コンパイル → 旧 viewer を落とす → java 直起動 → CP が応答するまで待つ
#           → (任意) boxes on / capture。
#
# なぜ `./gradlew :desktop:run` ではないか: gradle は起動のたびに configuration +
# task graph で 20-30 秒かかる。java 直起動なら数秒。しかも「起動できたか」を
# CP のポート応答で判定するので、盲目の sleep が要らない (以前は sleep 50 を毎回入れていた)。
param(
    [Parameter(Position = 0)][string]$File = "",       # .csv / .dxf / .sfc (相対可)
    [switch]$Boxes,                                     # 起動後に「boxes on」を送る
    [string]$Capture = "",                              # 起動後にスクショを撮る出力先
    [string]$View = "",                                 # 「<scale> <ox> <oy>」で視点指定
    [switch]$NoBuild,                                   # コンパイルを飛ばす (実行中の再起動だけ)
    [int]$CpPort = 9876,
    [int]$TimeoutSec = 90
)
$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "../..")
$cpFile = Join-Path $repo "desktop/build/viewer-classpath.txt"

if (-not $NoBuild) {
    Push-Location $repo
    & ./gradlew :desktop:compileKotlin :desktop:dumpRuntimeClasspath -q
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "compile failed" }
    Pop-Location
}
if (-not (Test-Path $cpFile)) { throw "classpath がない: ./gradlew :desktop:dumpRuntimeClasspath" }

# 旧 viewer を落とす (CP ポートを掴んだままだと新しい方が listen できない)
Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowTitle -eq 'CAD Viewer' } |
    ForEach-Object { Stop-Process -Id $_.Id -Force }

$argList = @("@$cpFile", "-Dcompose.swing.render.on.graphics=true", "MainKt")
if ($File) {
    $target = if ([System.IO.Path]::IsPathRooted($File)) { $File } else { Join-Path $repo $File }
    $argList += (Resolve-Path -LiteralPath $target).Path
}
# PATH の java は古いことがある (実測: class file 60.0 までしか読めない JRE)。
# gradle と同じ JDK を使う - JAVA_HOME 優先、無ければ PATH の java
$javaExe = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin/java.exe"))) {
    Join-Path $env:JAVA_HOME "bin/java.exe"
} else { "java" }
Start-Process -FilePath $javaExe -ArgumentList $argList -WorkingDirectory (Join-Path $repo "desktop") -WindowStyle Normal | Out-Null

# CP が応答したら起動完了 (盲目 sleep をやめる根拠: 「立ったか」を実際に確かめている)
$sw = [Diagnostics.Stopwatch]::StartNew()
$ready = $false
while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $c.Connect("127.0.0.1", $CpPort)
        $c.Close()
        $ready = $true
        break
    } catch { Start-Sleep -Milliseconds 300 }
}
if (-not $ready) { throw "viewer の CP が $TimeoutSec 秒で応答しない" }
Write-Host ("[cad-dev] ready in {0:N1}s" -f $sw.Elapsed.TotalSeconds)

function Send-Cp([string]$msg) {
    $c = New-Object System.Net.Sockets.TcpClient
    $c.Connect("127.0.0.1", $CpPort)
    $s = $c.GetStream()
    $w = New-Object System.IO.StreamWriter $s
    $r = New-Object System.IO.StreamReader $s
    $w.WriteLine($msg); $w.Flush()
    Write-Host ("[cad-dev] {0} -> {1}" -f $msg, $r.ReadLine())
    $c.Close()
}

if ($Boxes) { Send-Cp "boxes on" }
if ($View) { Send-Cp "view $View" }
if ($Capture) {
    Start-Sleep -Milliseconds 500   # 直前の状態変更の再描画待ち (CP 応答後の 1 フレーム分)
    $abs = if ([System.IO.Path]::IsPathRooted($Capture)) { $Capture }
           else { [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Capture)) }
    Send-Cp "capture $abs"
}
