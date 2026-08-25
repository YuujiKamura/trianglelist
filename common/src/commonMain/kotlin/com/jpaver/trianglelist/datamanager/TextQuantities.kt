package com.jpaver.trianglelist.datamanager

// commonMain では kotlin.jvm.* の default import が効かないので明示 import が要る
// (JVM 専用コードだと自動で入るため書き忘れやすい)。value class は全ターゲットで有効。
import kotlin.jvm.JvmInline

/**
 * 文字の「縦の大きさ」を表す 2 つの量を型で分ける。
 *
 * 2026-08-25 user 指摘「補正とか掛けてる時点で抽象化が足りてない」への構造側の答え。
 *
 * この日 1 日で同じ取り違えが 4 箇所見つかった:
 *   1. WebFrame          … paper mm と paper cm を取り違え、枠テキストが 1/10 (cc21adfb)
 *   2. web の ctx.font   … キャップハイトを em として渡し、全テキストが 0.74 倍
 *   3. TextFit           … 全角 advance を「1 キャップハイト」と置き、幅を 26% 過小
 *   4. PlatformTextMetrics.desktop … キャップハイトを AWT の font size (em) として渡し 25% 過小
 *
 * 4 件とも「裸の Float が `size` / `fs` という名前だけで境界を渡り、受け手が意味を思い出して
 * 手で変換する」構造から出ている。片方は思い出し損ねて壊れ (1)、片方は思い出して「補正」と
 * 呼ばれていた (2〜4) だけで、同じ穴。
 *
 * ここで型にするのは **意味 (キャップハイトか em か)** であって、座標空間 (paper cm / model)
 * ではない。空間の方は DrawPrim.Text.size が寸法系 (model) と枠系 (paper cm) の両方を運ぶ
 * 設計 (ADR 0001 の 2 path 構造) になっていて、型で割ると設計と喧嘩する ── そちらは
 * TextSizeBackendParityTest (DXF と web の実寸突き合わせ) で押さえる。
 * 意味の方はどの空間でも共通なので、型が素直に乗る。
 *
 * value class なので実行時のコストはゼロ (JVM では Float にインライン展開される)。
 *
 * 参考: ezdxf の make_font(font_name, cap_height, width_factor) -> AbstractFont も、
 * キャップハイトを渡してフォントを作らせることで呼び出し側に割り算をさせない形をとる。
 */

/** 大文字の高さ。DXF TEXT の group code 40 と同じ量。呼び出し側の座標系に乗る。 */
@JvmInline
value class CapHeight(val value: Float) {
    operator fun times(k: Float): CapHeight = CapHeight(value * k)
    operator fun div(k: Float): CapHeight = CapHeight(value / k)
    operator fun compareTo(other: CapHeight): Int = value.compareTo(other.value)

    /**
     * em に直す。キャップハイト / em 比はフォント固有の性質なので、必ず実測値を渡すこと
     * (実測できない経路だけ TextFit.ASSUMED_CAP_HEIGHT_RATIO を使う)。
     * この関数を通らずに CapHeight を font size として使えない、というのが型の目的。
     */
    fun toEm(capHeightRatio: Float): Em = Em(value / capHeightRatio)
}

/** フォントの em サイズ。TTF の font-size / AWT の Font size / CSS の px はこちら。 */
@JvmInline
value class Em(val value: Float) {
    operator fun times(k: Float): Em = Em(value * k)
}
