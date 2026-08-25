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

    /** 図面枠の項目内容 (工事名・路線名等) の paper mm 標準。
     *  2026-08-12 に desktop/sample/sample.dxf の実測値 (2.5mm、JIS ラダー最小段) へ一旦
     *  合わせたが、DIMENSION_PAPER_MM (3.5mm) より表題欄の方が小さいという逆転を生み、
     *  web 画面でタイトル系の文字が判読できないレベルまで縮む結果になった (2026-08-25 user
     *  指摘)。表題欄・タイトルは寸法値より格下ではないはずなので、DIMENSION_PAPER_MM と
     *  同格の 3.5mm (JIS ラダー次の段) に引き上げる。 */
    const val FRAME_LABEL_PAPER_MM: Float = 3.5f

    /** 図面タイトル (面積展開図など) の paper mm 標準。user 指示「上部タイトルは
     *  表題欄に対して2倍」: FRAME_LABEL_PAPER_MM (3.5mm) の 2 倍 = 7.0mm (JIS ラダー次の段)。 */
    const val TITLE_PAPER_MM: Float = FRAME_LABEL_PAPER_MM * 2f

    /** paper mm を model mm に換算。drawingScaleDenominator は 1/50 図面なら 50f。 */
    fun paperToModel(paperMm: Float, drawingScaleDenominator: Float): Float =
        paperMm * drawingScaleDenominator

    /** model mm を paper mm に逆算。既存 DXF の検査用 (Inspector で使う)。 */
    fun modelToPaper(modelMm: Float, drawingScaleDenominator: Float): Float =
        if (drawingScaleDenominator == 0f) 0f else modelMm / drawingScaleDenominator
}
