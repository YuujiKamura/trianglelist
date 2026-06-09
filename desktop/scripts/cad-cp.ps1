# CAD viewer の control plane (127.0.0.1:9876) に 1 行コマンドを送る汎用クライアント。
#
# プロトコル (Main.kt LaunchedEffect 内):
#   open  <abs-path>            ── DXF を再起動なしで差し替え
#   zoom  <factor>              ── 現 scale に factor 倍率を乗算
#   pan   <dx> <dy>             ── current offset に (dx, dy) 加算
#   view  <scale> <ox> <oy>     ── 絶対値で scale + offset を set
#   fit                         ── initial scale / offset を null にして全体フィット再計算
#   state                       ── 現在の scale + offset を返す
#
# 使い方:
#   pwsh -File desktop/scripts/cad-cp.ps1 "fit"
#   pwsh -File desktop/scripts/cad-cp.ps1 "zoom 2.0"
#   pwsh -File desktop/scripts/cad-cp.ps1 "pan 100 -50"
#   pwsh -File desktop/scripts/cad-cp.ps1 "view 0.05 100 800"
#   pwsh -File desktop/scripts/cad-cp.ps1 "state"
#   pwsh -File desktop/scripts/cad-cp.ps1 "open C:/Users/yuuji/StudioProjects/trianglelist/app/build/test-output/test-scale-large.dxf"

param(
    [Parameter(Mandatory=$true, Position=0, ValueFromRemainingArguments=$true)][string[]]$Args,
    [string]$CpHost = "127.0.0.1",
    [int]$CpPort = 9876
)

$ErrorActionPreference = "Stop"
$message = ($Args -join " ").Trim()

$client = New-Object System.Net.Sockets.TcpClient
$client.Connect($CpHost, $CpPort)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter $stream
$reader = New-Object System.IO.StreamReader $stream
$writer.WriteLine($message)
$writer.Flush()
$response = $reader.ReadLine()
Write-Host "[cad-cp] $response"
$client.Close()
