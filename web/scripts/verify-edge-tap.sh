#!/usr/bin/env bash
# CP越し辺タップ検証 — 4 動線 (三/三, 三/台, 台/三, 台/台) を curl だけで回す。
# 出力: $OUT 配下に各ステップの state JSON と canvas PNG。expect 失敗で exit 1。
# 起動前提: web/ で `npm run dev` が走り http://localhost:5183 に /__tlcp/* が応答すること。

set -euo pipefail
export PYTHONIOENCODING=utf-8

BASE="${BASE:-http://localhost:5183}"
OUT="${OUT:-$HOME/.agents/scratch/trianglelist/out/edge-tap}"
mkdir -p "$OUT"
# Python on Windows は MSYS /c/... 形式を解さないので cygpath -m で C:/... に翻訳して渡す。
OUT_PY="$(cygpath -m "$OUT" 2>/dev/null || echo "$OUT")"

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
# Rectangle 列順: num, length, widthA, widthB, parent, side, align, parentKind
read -r -d '' CSV_TRI_TRAP <<'EOF' || true
koujiname,
rosenname,
gyousyaname,
zumennum,
1,3.00,3.00,3.00,-1,-1
ListAngle, 0
Rectangle, 1, 5, 3.00, 5, 1, 1, 2, 0
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

# figureKind を desired ('triangle' | 'rectangle') に合わせる。
# cp_load は figureKind をリセットしない (web/src/main.ts の loadCsv は parseCsvToState のみ)
# ので前テストの toggle が残る。fabFigureKind は単純 toggle なので必要なら 1 回押す。
ensure_figure_kind() {
  local desired="$1"
  cp_state > "$OUT/_tmp.json"
  local cur
  cur=$(python -c "
import json
d = json.load(open(r'$OUT_PY/_tmp.json', encoding='utf-8'))
print(d['figureKind'])
")
  if [ "$cur" != "$desired" ]; then
    cp_click fabFigureKind
    settle
  fi
}

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
d = json.load(open(r'$OUT_PY/_tmp.json', encoding='utf-8'))
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

# 三角形 1 の辺中点 (モデル座標) を "MX MY" で返す。layer='tri' の中で
# tri=1 (= 主三角形 #1) かつ side=指定 (0=A, 1=B, 2=C) に絞る。
# layer は三角形/台形/RectChild すべて 'tri' 共通 (web/src/main.ts)、tri/side 識別子で絞らないと
# 台形辺と混ざる。引数は (layer, side) — layer は互換のため受けるが 'tri' を期待。
mid_line() {
  local layer="$1" side="$2"
  cp_state > "$OUT/_tmp.json"
  python -c "
import json
d = json.load(open(r'$OUT_PY/_tmp.json', encoding='utf-8'))
hits = [p for p in d['prims']
        if p.get('type') == 'line'
        and p.get('layer') == '$layer'
        and p.get('tri') == 1
        and p.get('side') == $side]
if not hits:
    raise SystemExit(f'no edge: layer=$layer tri=1 side=$side')
L = hits[0]
print(f\"{(L['x1']+L['x2'])/2} {(L['y1']+L['y2'])/2}\")
"
}

# 台形辺 (side 番号で指定) の中点。
# 実観察: web 版は 三角形/台形/RectChild すべて layer='tri' で発行する (web/src/main.ts
# 2876-2929、nearestTrapEdge は l.tri 範囲で台形を切り出す)。
# 台形の global tri 番号 = triCount() + trap_idx。renderRectangle 順 0=底辺/1=右脚/2=上辺/3=左脚。
mid_trap_side() {
  local side="$1"
  cp_state > "$OUT/_tmp.json"
  python -c "
import json
d = json.load(open(r'$OUT_PY/_tmp.json', encoding='utf-8'))
tc = sum(1 for r in d['rows'] if r.get('kind') == 'triangle')
trap_count = sum(1 for r in d['rows'] if r.get('kind') == 'rectangle')
trap_min = tc + 1
trap_max = tc + trap_count
# tri == trap_min (= 最初の台形) かつ side == 指定 side で 1 本に絞る。
hits = [p for p in d['prims']
        if p.get('type') == 'line'
        and p.get('layer') == 'tri'
        and p.get('tri') is not None
        and trap_min <= p['tri'] <= trap_max
        and p.get('side') == $side]
if not hits:
    raise SystemExit(f'no trap edge: trap_min={trap_min} trap_max={trap_max} side=$side')
L = hits[0]
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
# 連続実行で前回 run の figureKind が rectangle のまま残っている可能性。
# cp_load は figureKind を保つので明示的に triangle へ正規化する。
ensure_figure_kind triangle
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
# tap で selectEdge → buildShadow が即 3 辺 (三角形親辺長から推定) を立てる。
# input 後も三角形のままなので shadowLen は 3 のまま。
expect_json "10" "d['shadowLen']" "3"

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
ensure_figure_kind rectangle
snap "21-b-figkind-trap"
expect_json "21" "d['figureKind']" "rectangle"

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
expect_json "24" "d['rows'][1]['kind']" "rectangle"
expect_json "24" "d['rows'][1]['parentKind']" "0"
expect_json "24" "str(d['rows'][1]['parent'])" "1"
expect_json "24" "str(d['rows'][1]['conn'])" "1"
expect_json "24" "d['shadowLen']" "0"

# ============================================================
# 30 (c) 台→三: 台形親に三角形を乗せる
# ============================================================
echo "--- 30 (c) 台→三 ---"
cp_load "$CSV_TRI_TRAP"
# cp_load は figureKind を保つので前 (b) の rectangle が残る → triangle に正規化。
ensure_figure_kind triangle
snap "30-c-reset"
expect_json "30" "d['figureKind']" "triangle"
expect_json "30" "len(d['rows'])" "2"

read -r mx my <<<"$(mid_trap_side 2)"
echo "  tap trap.side2 (上辺) @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "31-c-tap-trap-top"
expect_json "31" "d['pendingTrapParent']" "{'trap': 1, 'side': 2}"

# CSV_TRI_TRAP の widthB=5 (台形上辺) → 子三角形は A=5, B/C は三角不等式 B+C>5 が必要。
# brief の 2,2 では 2+2<5 で renderCsvToPrimitivesWithOverrides が 0 prim 返し
# pickShadowLines が空 → shadowPrims=null。 4,4 にして valid triangle にする。
cp_key newB 4
cp_key newC 4
snap "32-c-input"
expect_json "32" "d['shadowLen']" "3"

cp_click fabReplace
snap "33-c-added"
expect_json "33" "len(d['rows'])" "3"
# 台形親に三角形を乗せた行は内部表現 kind='recttri' (台形子三角形)。 web/src/main.ts addTriOnTrap (3009) で
# 三角形扱いだが RectChild (CSV "RectChild" 行) として持つため kind は 'recttri'。
expect_json "33" "d['rows'][-1]['kind']" "recttri"
expect_json "33" "str(d['rows'][-1]['parent'])" "1"
expect_json "33" "str(d['rows'][-1]['conn'])" "2"
expect_json "33" "d['pendingTrapParent']" "None"

# ============================================================
# 40 (d) 台→台: 台形親に台形を乗せる (parentKind=1)
# ============================================================
echo "--- 40 (d) 台→台 ---"
cp_load "$CSV_TRI_TRAP"
snap "40-d-reset"
# 前 (c) の triangle を引きずる前提で 1 回 toggle → rectangle。
# ensure_figure_kind で念のため正規化 (前テスト状態に依存しない)。
ensure_figure_kind rectangle
snap "41-d-figkind-trap"
expect_json "41" "d['figureKind']" "rectangle"

read -r mx my <<<"$(mid_trap_side 2)"
echo "  tap trap.side2 (上辺) @ ($mx, $my)"
cp_tap "$mx" "$my"
snap "42-d-tap-trap-top"
expect_json "42" "d['pendingTrapParent']" "{'trap': 1, 'side': 2}"
expect_json "42" "d['figureKind']" "rectangle"

cp_key newB 2
cp_key newC 2
snap "43-d-input"
expect_json "43" "d['shadowLen']" "4"

cp_click fabReplace
snap "44-d-added"
expect_json "44" "len(d['rows'])" "3"
expect_json "44" "d['rows'][-1]['kind']" "rectangle"
expect_json "44" "d['rows'][-1]['parentKind']" "1"
# 台形の親列は **混在通し番号** (triCount + 台形群index)。 三角形 1 個 + 台形 1 個の状態で
# 新たに台形を乗せる場合、 親=台形群 1 → 混在通し番号 2 (rows[1] = 親台形)。
# brief は "1" を期待していたが、 addRectangle が per-group のまま CSV を吐くと common が
# 「親=三角形 1」と解釈して trap-on-trap が崩れる (44-d-added.png で確認)。
expect_json "44" "str(d['rows'][-1]['parent'])" "2"
expect_json "44" "str(d['rows'][-1]['conn'])" "2"
expect_json "44" "d['pendingTrapParent']" "None"

echo
echo "=== ALL PASS ==="
