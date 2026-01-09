# ワイヤレスADB接続手順

## 環境情報
- ADBパス: `/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe`
- デバイス: Galaxy A55 (R5CX71BJKSM)
- 現在のIPアドレス: 192.168.2.113

## 初回接続手順（USBから無線へ切り替え）

1. **USBケーブルでスマホを接続**

2. **デバイス確認**
```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

3. **スマホのIPアドレス取得**
```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe shell ip addr show wlan0
```
出力から `inet 192.168.x.xxx/24` の部分を確認

4. **TCPモードに切り替え（ポート5555）**
```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe tcpip 5555
```

5. **WiFi経由で接続**
```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe connect 192.168.2.113:5555
```

6. **接続確認**
```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```
`192.168.2.113:5555	device` と表示されればOK

7. **USBケーブルを抜く**

## 再接続手順（2回目以降）

スマホとPCが同じWiFiに接続されている状態で：

```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe connect 192.168.2.113:5555
```

## 接続解除

```bash
/c/Users/yuuji/AppData/Local/Android/Sdk/platform-tools/adb.exe disconnect 192.168.2.113:5555
```

## トラブルシューティング

### IPアドレスが変わった場合
1. スマホの設定 → WiFi → 接続中のネットワーク → IPアドレス確認
2. または上記手順3でIPアドレスを再取得
3. 新しいIPアドレスで接続

### 接続できない場合
1. スマホの開発者オプション → USBデバッグがONか確認
2. WiFiが同じネットワークか確認（2.4GHz/5GHzの違いに注意）
3. Windowsファイアウォールの確認
4. 最初からUSB接続で手順をやり直す

### QRコード方式が使えない理由
- ランダムポートをファイアウォールがブロック
- AP Isolation（ルーターの端末間通信制限）
- 固定ポート5555方式の方が安定

## 注意事項
- スマホのIPアドレスはDHCPで変わる可能性あり
- WiFi再接続時は再度 `adb connect` が必要
- セキュリティ上、公共WiFiでは使用しないこと
