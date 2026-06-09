package com.jpaver.trianglelist.scale

/**
 * 紙面と model 空間の換算 policy。
 *
 * AutoCAD 業界の paper space / model space 概念を借用する:
 *   - paper: 印刷後の紙の上のミリ。3.5 mm の文字は印刷したら 3.5 mm。
 *   - model: CAD の model space に書くミリ。縮尺分母を掛けた後の絶対値。
 *   - drawingScaleDenominator: 縮尺の分母。1/50 図面なら 50f、1/600 なら 600f。
 *
 * 換算: modelMm = paperMm * drawingScaleDenominator
 *
 * JIS Z 8313-0:1998 / 国土交通省 CAD 製図基準で標準とされる文字呼び寸法階段
 *   2.5, 3.5, 5, 7, 10, 14, 20 mm (paper 上)。
 *
 * 変数名規約: 値の単位を必ず suffix で持たせる (paperMm / modelMm)。
 * 無印 Float の掛け算で渡り歩かない ── 意図と違う変換が入り込まないため。
 */
object TextSizePolicy {

    /** 寸法値・注記の paper mm 標準 (JIS Z 8313 / CAD 製図基準の主流)。 */
    const val DIMENSION_PAPER_MM: Float = 3.5f

    /** 図面枠の項目内容 (工事名・路線名等) の paper mm 標準。 */
    const val FRAME_LABEL_PAPER_MM: Float = 5.0f

    /** 図面タイトル (面積展開図など) の paper mm 標準。 */
    const val TITLE_PAPER_MM: Float = 7.0f

    /** paper mm を model mm に換算。drawingScaleDenominator は 1/50 図面なら 50f。 */
    fun paperToModel(paperMm: Float, drawingScaleDenominator: Float): Float =
        paperMm * drawingScaleDenominator

    /** model mm を paper mm に逆算。既存 DXF の検査用 (Inspector で使う)。 */
    fun modelToPaper(modelMm: Float, drawingScaleDenominator: Float): Float =
        if (drawingScaleDenominator == 0f) 0f else modelMm / drawingScaleDenominator
}
