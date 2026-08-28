package com.jpaver.trianglelist.label

/**
 * 寸法ラベルの自動配置を効かせるかどうか (2026-08-27 user「寸法自動配置は、設定でトグルして
 * オフに出来たほうが良い」)。
 *
 * 自動配置は「衝突している所だけを避難させる」もので完全ではない ── 図形の繋がり方に
 * よってはむしろ重なる向きへ出すことがある (user 指摘)。気に入らない時に切れる口を
 * 持たせておく。
 *
 * OFF にすると **既定配置のまま**になるだけで、既に確定した配置が巻き戻ることはない
 * (自動が触るのは「今から決める分」だけ)。user が手で動かした配置は ON/OFF に関わらず不可侵。
 *
 * アプリ側の設定 (SharedPreferences) からここに反映する。プロセス全体で 1 つの設定なので
 * 状態はここに置く ── 呼び出し側 (画面 / 3 つの書き出し経路) が個別に判定を持つと、
 * どれか 1 つ書き換え忘れて「画面だけ効いている」ような食い違いが生まれる。
 */
object LabelArrangePolicy {
    var enabled: Boolean = true
}

/**
 * 自動配置を「毎回、既定から決め直す」ための巻き戻し (2026-08-28)。
 *
 * これが無いと配置が**前回の結果に依存する**。自動配置の結果は pointnumber / dimHorizontal
 * という CSV に載るフィールドに入るので、保存 → 開き直しの後は「既に退避済みの状態」から
 * 探索が始まり、別の解に落ちる ── 「保存して開き直すたびに寸法の位置が変わる」。
 * 実際 AutoArrangeRoundTripTest が往復後の番号位置のずれとして検出した。
 *
 * 巻き戻すのは**自動処理が置いた分だけ**。user が手で動かした配置 (isMovedByUser) は
 * 不可侵なので触らない。これで配置は (図形 + user の手動配置 + 文字サイズ) の純関数になり、
 * 冪等性と往復不変が構造的に保証される (探索側の努力ではなく前提として)。
 *
 * 巻き戻し先:
 *   - dimHorizontal → 0 (中央)。horizontal を自動で入れるのは退避だけ (Dims.controlHorizontal は
 *     user 操作で isMovedByUser を立てる、autoDimHorizontalByAngle は封印済み)
 *   - 番号 → isEscaped を落として既定位置 (autoAlign = 内心) を計算し直す
 */
object LabelArrangeReset {

    fun reset(list: com.jpaver.trianglelist.editmodel.EditList<out com.jpaver.trianglelist.editmodel.CycleShape>) {
        list.forEachItem { shape ->
            val tri = shape as? com.jpaver.trianglelist.editmodel.Triangle ?: return@forEachItem
            if (!tri.dim.flag.getOrNull(0)?.isMovedByUser.let { it == true }) tri.dimHorizontal.a = 0
            if (!tri.dim.flag.getOrNull(1)?.isMovedByUser.let { it == true }) tri.dimHorizontal.b = 0
            if (!tri.dim.flag.getOrNull(2)?.isMovedByUser.let { it == true }) tri.dimHorizontal.c = 0
            if (!tri.dim.flagS.isMovedByUser) tri.dimHorizontal.s = 0
            if (!tri.pointNumber.flag.isMovedByUser && tri.pointNumber.flag.isEscaped) {
                tri.pointNumber.flag.isEscaped = false
                tri.pointNumber.flag.isAutoAligned = false
                tri.pointnumber = tri.pointNumber.autoAlign(tri)
            }
        }
    }
}


/**
 * 自動配置の入口 (2026-08-28)。**手順を 1 箇所に集約する**。
 *
 * これまでアプリ経路 (TriangleList.arrangeLabelsWithoutCollision) と書き出し経路
 * (WebDrawingExport.buildDxfText) に同じ順番が別々に書かれていた。片方だけ直すと
 * 「画面と図面で配置が違う」が起きる ── 2026-08-27 に user が「基本アプリと図面が
 * 食い違うのはバグだと思っていい。期待値と違うわけだから」と言った類の事故が、
 * 手順の写し間違いという形でいつでも入り得る状態だった。
 *
 * 順番: 巻き戻し → 番号 (自由に動ける方が先) → 寸法 (辺に紐づいた離散スロット)。
 * 設定 (LabelArrangePolicy.enabled) の判定もここに置く ── 書き出し側が policy を見て
 * いなかったので、アプリでトグルを切っても書き出しには効いていなかった。
 */
object LabelArrange {

    fun run(
        list: com.jpaver.trianglelist.editmodel.EditList<out com.jpaver.trianglelist.editmodel.CycleShape>,
        textSize: Float,
    ) {
        if (textSize <= 0f) return
        if (!LabelArrangePolicy.enabled) return
        LabelArrangeReset.reset(list)
        NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, textSize))
        DimensionTextEscape.apply(list, DimensionTextEscape.solve(list, textSize))
    }
}
