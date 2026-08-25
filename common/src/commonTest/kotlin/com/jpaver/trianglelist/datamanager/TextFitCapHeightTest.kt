package com.jpaver.trianglelist.datamanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TextFit が扱う「サイズ」がキャップハイトであることを pin する。
 *
 * 2026-08-25 user 指摘「補正とか掛けてる時点で抽象化が足りてない」の 3 件目。
 *
 * DXF の TEXT height はキャップハイト (group code 40)。TextFit.estimateWidth はその値を
 * 受け取るが、旧実装は全角 1 文字の advance を「= 1.0 × キャップハイト」と置いていた。
 * 実際の全角グリフの advance は 1.0 em で、em はキャップハイトより 3 割ほど大きい
 * (どのフォントでも cap < em、比は 0.7〜0.8 程度)。つまり幅を約 26% 過小に見積もっていた。
 *
 * 実害 (2026-08-25 ブラウザ実測): 表題欄の「図面番号」がセル幅 0.8999 に対し実描画幅
 * 0.9459 で 5% はみ出した。estimateWidth が 0.70 と答えるので TextFit.fitSize は
 * 「収まる」と判断して縮小をかけていなかった。
 *
 * これは font-size を em と cap で取り違えていたのと同じ間違いで、量の取り違えが
 * 「幅」側にも同じ形で入っていた ── 直すのは係数ではなく、どの量で喋るかの取り決め。
 */
class TextFitCapHeightTest {

    private val tol = 1e-4f

    // ---- どの経路 (実測 / fallback) でも成り立つべき不変条件 ----

    @Test
    fun `全角の advance はキャップハイトより広い`() {
        // 全角 1 文字の advance = 1.0 em、em > cap はどのフォントでも成立する。
        // 「1 全角 = 1 キャップハイト」と置いていた旧実装はこれを満たさない。
        // desktop はさわらびゴシック実測経路、他は fallback 経路を通るが、
        // どちらでも満たされなければならない性質なのでここで押さえる。
        val capHeight = 1.0f
        val w = TextFit.estimateWidth("図", capHeight)
        assertTrue(
            w > capHeight * 1.2f,
            "全角 1 文字の幅がキャップハイト相当しかない ($w) ── em と cap を取り違えている"
        )
        assertTrue(w < capHeight * 1.6f, "逆に広すぎる ($w) ── 変換が二重に掛かっていないか")
    }

    @Test
    fun `半角は全角より狭い`() {
        // 実測経路では厳密に 1対2 にはならない (フォント依存) ので、大小関係だけ押さえる
        val capHeight = 1.0f
        val full = TextFit.estimateWidth("図", capHeight)
        val half = TextFit.estimateWidth("A", capHeight)
        assertTrue(half < full, "半角 ($half) が全角 ($full) 以上になっている")
    }

    // ---- 実測できない経路 (DXF: STYLE=Standard で開く側のフォント不定) の fallback 式 ----

    @Test
    fun `fallback 比は cap と em の間の妥当な範囲にある`() {
        // 実測不能時の仮定値。cap < em はどのフォントでも成立するので 1 未満、
        // 現実のフォントの実測値 (sans-serif 0.74 / MS Gothic 0.77) を挟む範囲に置く。
        assertTrue(
            TextFit.ASSUMED_CAP_HEIGHT_RATIO in 0.70f..0.80f,
            "仮定するキャップハイト比が現実的な範囲外: ${TextFit.ASSUMED_CAP_HEIGHT_RATIO}"
        )
    }

    @Test
    fun `幅はキャップハイトに比例する`() {
        // 単位が一貫していれば、サイズを 2 倍にした幅はちょうど 2 倍になる
        val a = TextFit.estimateWidth("図面番号", 0.175f)
        val b = TextFit.estimateWidth("図面番号", 0.350f)
        assertEquals(a * 2f, b, tol)
    }

    @Test
    fun `図面番号 が表題欄のラベルセルに収まるサイズへ縮む`() {
        // 2026-08-25 にブラウザで実測したセル幅とサイズをそのまま再現ケースにする。
        // 旧実装は estimateWidth("図面番号", 0.175) = 0.70 で target 0.765 以下 → 無縮小 →
        // 実描画 0.9459 ではみ出した。単位を直すと縮小が効いて箱に収まる。
        val cellWidth = 0.8999f
        val baseSize = 0.175f
        val fit = TextFit.fitSize("図面番号", cellWidth, baseSize)
        val fittedWidth = TextFit.estimateWidth("図面番号", fit.size)
        assertTrue(
            fittedWidth <= cellWidth,
            "縮小後もセル幅を超えている (fitted=$fittedWidth cell=$cellWidth size=${fit.size})"
        )
        assertTrue(fit.size < baseSize, "この文字列とセル幅なら縮小が効くはず (size=${fit.size})")
    }
}
