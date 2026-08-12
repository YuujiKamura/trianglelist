package com.jpaver.trianglelist.datamanager

/**
 * 図面枠テキスト (TextRole) のサイズを一元管理する。
 *
 * 2026-08-12 user確定 (Web版着手前の挙動まで遡って確認): Web版着手直前 (b8ff1e13、2026-06-10、
 * ここまで5年間) は TopTitle/BottomTitleFrame とも寸法値・三角形本体と同じ entityTextSize
 * (= 呼び出し元の textscale_、ADR 0001 の TriangleList.getPrintTextScale/TextScaleCalculator
 * 由来、drawingScale × fileType の実測テーブル値) をそのまま使っていた ── 専用の固定 mm 定数は
 * 存在しなかった。「タイトルだけ紙面固定 mm にする」という発想自体が Web 期 (2026-06-19 の
 * TOP_TITLE_SCALE 導入以降) の逸脱で、そこから TOP_TITLE_MM=7.0 等の独立定数が育ち、CAD で
 * 開くと dimension text と無関係に肥大するに至った (呼び出し元は今も textscale_ を正しく渡して
 * いるのに、writeTopTitle/writeDrawingFrame の中でその引数を握りつぶし定数に差し替えていた)。
 *
 * このため全 role は entityTextSize (呼び出し元の textscale_) から一律に決まる、という Web 以前の
 * 規約に戻す。role を型で通す意味は「独自の固定定数への回帰を物理的に塞ぐ」こと ── 将来
 * 「Web表示のために大きくしたい」と思っても、ここを触らない限り独立定数を足す場所が無い。
 */
object TextSizePolicy {
    fun resolve(role: TextRole, entityTextSize: Float, scale: Float = 1f): Float = when (role) {
        TextRole.TopTitle, TextRole.BottomTitleFrame, TextRole.BottomCredit -> entityTextSize * scale
    }
}
