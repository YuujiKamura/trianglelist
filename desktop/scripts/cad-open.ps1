# CAD viewer の control plane (127.0.0.1:9876) に "open <path>" を送り、
# 再起動なしで DXF を差し替えさせる。
#
# 使い方:
#   pwsh -File desktop/scripts/cad-open.ps1 <dxf-path>
#   pwsh -File desktop/scripts/cad-open.ps1 app/build/test-output/test-scale-large.dxf

param(
    [Parameter(Mandatory=$true)][string]$DxfPath,
    [string]$Host = "127.0.0.1",
    [int]$Port = 9876
)

$ErrorActionPreference = "Stop"

# 絶対パス化 (viewer 側の cwd は desktop/ なので相対だと解決できない)
$abs = (Resolve-Path -LiteralPath $DxfPath -ErrorAction Stop).Path

$client = New-Object System.Net.Sockets.TcpClient
$client.Connect($Host, $Port)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter $stream
$reader = New-Object System.IO.StreamReader $stream
$writer.WriteLine("open $abs")
$writer.Flush()
$response = $reader.ReadLine()
Write-Host "[cad-open] $response"
$client.Close()
