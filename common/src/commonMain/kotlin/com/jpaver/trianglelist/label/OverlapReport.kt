package com.jpaver.trianglelist.label

/**
 * 重なり集計の共通データ型 (dxf/model 両方の OverlapAnalyzer が共用)。
 * DxfOverlapAnalyzer (DXF ファイル起点) と ModelOverlapAnalyzer (編集モデル起点) は
 * 入力の素性が違うだけで、CollisionField への登録・クエリ・報告の形は同一のため
 * ここに切り出す (2026-08-25 user 指摘: 判定ロジックは dxf と関係のないモデル層に置く)。
 */

/**
 * 重なりペア 1 件。textId は必ず TEXT 側、otherId は相手 (TEXT は LABEL、LINE は EDGE、
 * CIRCLE は CIRCLE)。TEXT 同士のペアは A-B / B-A を正規化して 1 件にする。
 * depthMm は重なり深さ (model mm): 0 = 境界接触 (contact、寸法値が自分の辺に
 * 寄り添う正常配置もここに落ちる)、> 0 = めり込み (intrusion)。
 */
data class OverlapPair(
    val textId: String,
    val otherId: String,
    val otherKind: ObstacleKind,
    val depthMm: Double,
)

/**
 * 番号サークルと認識された TEXT↔CIRCLE のペア (rev6)。番号サークルは円が当たり判定の
 * 主体で、内部の番号 TEXT は円に内包されて一緒に動くだけ ── 判定の世界に入れない。
 */
data class CircledNumber(val textId: String, val circleId: String)

/** 図面 (または編集モデル) 1 枚分の重なり集計。事実の数値のみで、良し悪しの判定や閾値は持たない。 */
data class OverlapReport(
    /** 判定対象の TEXT 数 (サークル番号としてペアリングされた TEXT を除いた後)。 */
    val totalTexts: Int,
    val overlappingTexts: Int,
    val pairs: List<OverlapPair>,
    /** ペアリング結果 (観測の透明性のため、どの TEXT がどの円の番号と認識されたか)。 */
    val circledNumbers: List<CircledNumber> = emptyList(),
) {
    /**
     * 寸法値が自分の辺に寄り添う正常配置 (depthMm ≤ EPS)。問題ではない ── ここだけを
     * 見て「重なっている」と誤報しないための分離 (desktop CP `overlaps` コマンドの
     * partition ロジックと同じ基準、閾値を各呼び出し側で重複定義しないようここに集約する)。
     */
    val contacts: List<OverlapPair> get() = pairs.filter { it.depthMm <= LabelBox.EPS }

    /**
     * 実際のめり込み (depthMm > EPS)。
     * ただし、テキスト同士 (LABEL vs LABEL) の場合は、境界接触 (depth=0) であっても
     * 視覚的に「重なって読めない」問題となるため、無条件で intrusion 扱いとする。
     */
    val intrusions: List<OverlapPair> get() = pairs.filter { 
        it.depthMm > LabelBox.EPS || it.otherKind == ObstacleKind.LABEL
    }

    /**
     * TEXT id → 「その寸法テキストが何と衝突して読めなくなっているか」。
     * ビューワーが判定 box の枠を**色分けする唯一の根拠** (desktop CAD Viewer の
     * box overlay / web エディタの寸法枠が共用する、2026-08-27 user 指示
     * 「衝突してる寸法テキストを正しく色分けした枠付けて表示」)。
     *
     * 衝突していない TEXT は入らない (= 枠は無彩色のまま)。決め方は 3 つ:
     *
     * 1. **辺 (EDGE) は見ない**。寸法値の判定 box は縦アライメントのパディング分
     *    (ascent/descent の余白) だけ実際のグリフより大きく、辺に沿って置かれる
     *    寸法値はほぼ必ず辺に当たる ── 深さがあっても「読めない」の証拠にならない。
     *    それを塗ると図面が埋まって本当に読めない箇所が隠れる (user 2026-08-27
     *    「縦アライメントのパディングの都合で線接触判定になってる、本来は文字同士の
     *    接触だけをみてくれればいい」「円との接触も問題になる」)。見るのは
     *    文字どうし (LABEL) と番号サークル (CIRCLE)。
     *    ※ 本筋は box のパディングを正しくスペーサーとして持つこと (user 同日
     *    「パディングをただしくスペーサー入れるのが良いだろうけど」)。それが入るまでの
     *    暫定ではなく、「何を衝突と呼ぶか」の定義としてここに置く。
     * 2. **ペアの両側に入れる**。衝突は 2 つの box の *間* に起きる事実なのに、
     *    OverlapPair は A-B / B-A を 1 件に正規化して textId 側しか持たない。
     *    素朴に textId だけ見ると寸法値が団子になった所で**片側だけ色が付く**。
     *    相手が TEXT (= LABEL) の時だけ otherId 側も当事者として入れる
     *    (CIRCLE の otherId は TEXT ではないので入れない)。
     * 3. **1 box = 1 色**。複数と衝突していたら「一番読めなくなる相手」を代表に取る
     *    (LABEL > CIRCLE)。深さではなく種別の重さで決める。
     */
    val collisionKindByText: Map<String, ObstacleKind>
        get() {
            val worst = mutableMapOf<String, ObstacleKind>()
            fun mark(id: String, kind: ObstacleKind) {
                val cur = worst[id]
                if (cur == null || kind.legibilityCost > cur.legibilityCost) worst[id] = kind
            }
            for (hit in pairs) {
                if (hit.otherKind == ObstacleKind.EDGE) continue
                mark(hit.textId, hit.otherKind)
                if (hit.otherKind == ObstacleKind.LABEL) mark(hit.otherId, ObstacleKind.LABEL)
            }
            return worst
        }
}

/** 「読めなくなる度」= 色分けの優先度。1 box が複数と衝突した時の代表色を決める。 */
val ObstacleKind.legibilityCost: Int
    get() = when (this) {
        ObstacleKind.LABEL -> 3 // 寸法値どうしが重なる = 数字が判読不能、最悪
        ObstacleKind.CIRCLE -> 2 // 番号サークルに食い込む
        ObstacleKind.EDGE -> 0 // 辺: 色分けしない (パディング由来の接触が支配的)
    }
