#!/usr/bin/env bash
# CP越し辺タップ検証 — 4 動線 (三/三, 三/台, 台/三, 台/台) を curl だけで回す。
# 出力: $OUT 配下に各ステップの state JSON と canvas PNG。expect 失敗で exit 1。
# 起動前提: web/ で `npm run dev` が走り http://localhost:5183 に /__tlcp/* が応答すること。

set -euo pipefail
export PYTHONIOENCODING=utf-8

BASE="${BASE:-http://localhost:5183}"
OUT="${OUT:-$HOME/.agents/scratch/trianglelist/out/edge-tap}"
mkdir -p "$OUT"

# --- 死活 ---
curl -sf -m 5 "$BASE/__tlcp/state" > /dev/null \
  || { echo "dev server not responding at $BASE — run 'npm run dev' in web/ first" >&2; exit 1; }

# --- リセット用 CSV (heredoc) ---
# 三角形 1 個 (3,3,3、独立)。CSV ヘッダ 4 行 + 図形 1 行 + ListAngle。
read -r -d '' CSV_TRI_ONLY <<'EOF' || true
koujiname,
rosenname,
gyousyaname,
zumennum,
1,3.00,3.00,3.00,-1,-1
ListAngle, 0
EOF

# 三角形 1 個 + 台形 T1 (親=三角形1 の B 辺、parentKind=0=三角形親)。
# Trapezoid 列順: num, length, widthA, widthB, parent, side, align, parentKind
read -r -d '' CSV_TRI_TRAP <<'EOF' || true
koujiname,
rosenname,
gyousyaname,
zumennum,
1,3.00,3.00,3.00,-1,-1
ListAngle, 0
Trapezoid, 1, 5, 3.00, 5, 1, 1, 2, 0
EOF

# --- ヘルパ ---
cp_state() { curl -sf "$BASE/__tlcp/state"; }
cp_tap()   { curl -sf "$BASE/__tlcp/tap?x=$1&y=$2" > /dev/null; }
cp_click() { curl -sf "$BASE/__tlcp/click?target=$1" > /dev/null; }
cp_key()   { curl -sf "$BASE/__tlcp/key?target=$1&value=$2" > /dev/null; }
cp_load()  { curl -sf -X POST -H 'Content-Type: text/plain' --data-binary "$1" "$BASE/__tlcp/load" > /dev/null; }
cp_capture() { curl -sf "$BASE/__tlcp/capture" -o "$OUT/$1.png"; }

# stale read 防止: load/tap/click は次の draw まで非同期、軽い sleep を噛ます
settle() { sleep 0.15; }

snap() {
  settle
  cp_state > "$OUT/$1.json"
  cp_capture "$1"
  echo "  snap $1.{json,png}"
}

# state JSON に対する python 式評価 (cp932 問題回避のため中間ファイル経由)。
# 失敗時は stderr に式と現在値、 exit 1。
expect_json() {
  local label="$1" expr="$2" expected="$3"
  cp_state > "$OUT/_tmp.json"
  local val
  val="$(python -c "
import json
d = json.load(open(r'$OUT/_tmp.json', encoding='utf-8'))
v = $expr
print(v)
")"
  if [ "$val" = "$expected" ]; then
    echo "  PASS $label: $expr = $val"
  else
    echo "  FAIL $label: $expr = $val (expected $expected)" >&2
    echo "  state dump: $OUT/_tmp.json" >&2
    exit 1
  fi
}

# tri layer の line index N の中点 (モデル座標) を "MX MY" で返す。
# layer 名は実装に依存 ── 未知の場合は実画面の state を観て直す。
mid_line() {
  local layer="$1" idx="$2"
  cp_state > "$OUT/_tmp.json"
  python -c "
import json
d = json.load(open(r'$OUT/_tmp.json', encoding='utf-8'))
lines = [p for p in d['prims'] if p.get('type') == 'line' and p.get('layer') == '$layer']
L = lines[$idx]
print(f\"{(L['x1']+L['x2'])/2} {(L['y1']+L['y2'])/2}\")
"
}

# 台形辺 (side 番号で指定) の中点。台形描画は底辺=0/左斜=1/上辺=2/右斜=3 の並び想定。
# layer 名は実観察で確定 ── "trap" と "trap_e" など揺れがあるので、 まず trap で試して
# 失敗したら別関数で fallback。
mid_trap_side() {
  local side="$1"
  cp_state > "$OUT/_tmp.json"
  python -c "
import json
d = json.load(open(r'$OUT/_tmp.json', encoding='utf-8'))
# 台形 layer 候補を広めに掃く (実観察で確定)
candidates = [p for p in d['prims'] if p.get('type') == 'line' and (p.get('layer','').startswith('trap') or 'trap' in p.get('layer',''))]
# side=0 (底辺), 1 (左斜), 2 (上辺), 3 (右斜) と仮定して index で取る
L = candidates[$side]
print(f\"{(L['x1']+L['x2'])/2} {(L['y1']+L['y2'])/2}\")
"
}

echo "=== verify-edge-tap.sh starting ==="
echo "BASE=$BASE"
echo "OUT=$OUT"
echo

# ============================================================
# 00 init: 三角形 1 個 (3,3,3) をロードして基準点
# ============================================================
echo "--- 00 init ---"
cp_load "$CSV_TRI_ONLY"
snap "00-init"
expect_json "00" "d['figureKind']" "triangle"
expect_json "00" "len(d['rows'])" "1"
expect_json "00" "d['edgeSel']" "None"
expect_json "00" "d['pendingTrapParent']" "None"
expect_json "00" "d['shadowLen']" "0"

# ============================================================
# 10 (a) 三→三: 既存動作の pin
# ============================================================
echo "--- 10 (a) 三→三 ---"
read -r mx my <<<"$(mid_line tri 2)"
echo "  tap tri.C @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "10-a-tap-tri-c"
expect_json "10" "d['edgeSel']" "{'tri': 1, 'side': 2}"
expect_json "10" "d['shadowLen']" "0"

cp_key newB 4
cp_key newC 4
snap "11-a-input"
expect_json "11" "d['shadowLen']" "3"

cp_click fabReplace
snap "12-a-added"
expect_json "12" "len(d['rows'])" "2"
expect_json "12" "d['rows'][1]['kind']" "triangle"
expect_json "12" "d['edgeSel']" "None"
expect_json "12" "d['shadowLen']" "0"

# ============================================================
# 20 (b) 三→台: 三角形親に台形を乗せる
# ============================================================
echo "--- 20 (b) 三→台 ---"
cp_load "$CSV_TRI_ONLY"
snap "20-b-reset"
cp_click fabFigureKind
snap "21-b-figkind-trap"
expect_json "21" "d['figureKind']" "trapezoid"

read -r mx my <<<"$(mid_line tri 1)"
echo "  tap tri.B @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "22-b-tap-tri-b"
expect_json "22" "d['edgeSel']" "{'tri': 1, 'side': 1}"

cp_key newB 2
cp_key newC 2
snap "23-b-input"
expect_json "23" "d['shadowLen']" "4"

cp_click fabReplace
snap "24-b-added"
expect_json "24" "len(d['rows'])" "2"
expect_json "24" "d['rows'][1]['kind']" "trapezoid"
expect_json "24" "d['rows'][1]['parentKind']" "0"
expect_json "24" "str(d['rows'][1]['parent'])" "1"
expect_json "24" "str(d['rows'][1]['conn'])" "1"
expect_json "24" "d['shadowLen']" "0"

# ============================================================
# 30 (c) 台→三: 台形親に三角形を乗せる
# ============================================================
echo "--- 30 (c) 台→三 ---"
cp_load "$CSV_TRI_TRAP"
snap "30-c-reset"
expect_json "30" "d['figureKind']" "triangle"
expect_json "30" "len(d['rows'])" "2"

read -r mx my <<<"$(mid_trap_side 2)"
echo "  tap trap.side2 (上辺) @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "31-c-tap-trap-top"
expect_json "31" "d['pendingTrapParent']" "{'trap': 1, 'side': 2}"

cp_key newB 2
cp_key newC 2
snap "32-c-input"
expect_json "32" "d['shadowLen']" "3"

cp_click fabReplace
snap "33-c-added"
expect_json "33" "len(d['rows'])" "3"
expect_json "33" "d['rows'][-1]['kind']" "triangle"
expect_json "33" "str(d['rows'][-1]['parent'])" "1"
expect_json "33" "str(d['rows'][-1]['conn'])" "2"
expect_json "33" "d['pendingTrapParent']" "None"

# ============================================================
# 40 (d) 台→台: 台形親に台形を乗せる (parentKind=1)
# ============================================================
echo "--- 40 (d) 台→台 ---"
cp_load "$CSV_TRI_TRAP"
snap "40-d-reset"
cp_click fabFigureKind
snap "41-d-figkind-trap"
expect_json "41" "d['figureKind']" "trapezoid"

read -r mx my <<<"$(mid_trap_side 2)"
echo "  tap trap.side2 (上辺) @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "42-d-tap-trap-top"
expect_json "42" "d['pendingTrapParent']" "{'trap': 1, 'side': 2}"
expect_json "42" "d['figureKind']" "trapezoid"

cp_key newB 2
cp_key newC 2
snap "43-d-input"
expect_json "43" "d['shadowLen']" "4"

cp_click fabReplace
snap "44-d-added"
expect_json "44" "len(d['rows'])" "3"
expect_json "44" "d['rows'][-1]['kind']" "trapezoid"
expect_json "44" "d['rows'][-1]['parentKind']" "1"
expect_json "44" "str(d['rows'][-1]['parent'])" "1"
expect_json "44" "str(d['rows'][-1]['conn'])" "2"
expect_json "44" "d['pendingTrapParent']" "None"

echo
echo "=== ALL PASS ==="
