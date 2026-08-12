package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.scale.TextSizePolicy as PaperModelPolicy

/**
 * 図面枠テキスト (TextRole) のサイズを一元管理する。
 *
 * 2026-08-12 経緯 (2 回の誤りを経て確定):
 * 1回目: 「Web以前は textscale_ をそのまま使っていた」を根拠に全 role を entityTextSize に統一
 *   → 寸法値と同じ極小サイズになりタイトルが最も目立たない文字になった (画像で確認)。
 * 2回目: scale/TextSizePolicy.kt (JIS 準拠、TITLE_PAPER_MM=7.0 等) を発見し
 *   paperToModel(paperMm, drawingScaleDenominator) で model 空間に変換 → 桁違いに巨大化して
 *   画面全体が文字で埋まる大惨事になった (画像で確認)。paperToModel は「寸法値のように
 *   drawingScale に応じて実寸が変わる model 空間」向けの変換であって、writeTopTitle /
 *   writeDrawingFrame は ADR 0001 が明文化した通り「unitscale_ *= printscale_ で
 *   drawingScale を打ち消した後の、常に paper 固定 cm 空間」で動いている ── model 空間には
 *   一度も属していない。paperToModel を挟んだこと自体が誤り。
 *
 * 正しい変換は mm → cm の単位変換 (÷10) のみ。paper mm 定数 (scale.TextSizePolicy 由来、
 * JIS Z 8313 準拠の 7.0/5.0/3.5 階段) を、この関数群が実際に使っている paper-cm 座標系
 * (paperWcm/paperHcm 等、mm/10 の cm 単位) に合わせて ÷10 するだけで良い。旧バグはまさに
 * この ÷10 が抜けていたこと (TOP_TITLE_MM=7.0 を cm 空間にそのまま渡し 10 倍巨大化) だった。
 */
object TextSizePolicy {
    /** @param scale DrawPrim 側の追加スケール (通常 1f)。drawingScale とは無関係 (paper 固定のため)。 */
    fun resolve(role: TextRole, scale: Float = 1f): Float {
        val paperMm = when (role) {
            TextRole.TopTitle -> PaperModelPolicy.TITLE_PAPER_MM
            TextRole.BottomTitleFrame -> PaperModelPolicy.FRAME_LABEL_PAPER_MM
            TextRole.BottomCredit -> PaperModelPolicy.DIMENSION_PAPER_MM
        }
        return (paperMm / 10f) * scale
    }
}
