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
}
