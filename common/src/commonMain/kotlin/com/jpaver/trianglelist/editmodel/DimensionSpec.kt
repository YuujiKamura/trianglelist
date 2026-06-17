package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.label.DimensionPlacement

/**
 * SoT 一本化 段3 寸法多態 (2026-06-15): 図形種別 (Triangle / Rectangle) に依らず
 * 寸法 1 本を表す純データ。EditObject.emitDimensionSpecs() が返し、WebPrimitiveRenderer は
 * これを JSON prim 化するだけ — 図形ごとの「寸法をどう計算するか」 (DimensionLayout 呼び・
 * 接続辺の重複防止判定・B 延長の垂線根選び等) はモデル側に閉じ、view layer はもう
 * 図形種別を知らない (=「描画側の場合分けを基底へ吸収」 user 指針 2026-06-14)。
 *
 * フィールドは既存 WebPrimitiveRenderer.dimText の引数集合に対応:
 *   side       : 物理 side (0=A, 1=B, 2=C, Rectangle のみ 3=D)
 *   text       : 寸法値文字列 ("12.34" 等、既存 Triangle.strLengthX / Float.formattedString(2) と同形式)
 *   place      : DimensionLayout.layout の結果 (dimpoint + pointA/B + verticalDxf)
 *   angle      : テキストの回転角 (start.calcDimAngle(end))
 *   h, v       : DimAligns 値 (web 側 W/H cycle のため、prim にそのまま乗せる)
 *   emitFlag   : 旗揚げ線 (place.pointA → place.pointB) を引くか (= h > 2)
 */
data class DimensionSpec(
    val side: Int,
    val text: String,
    val place: DimensionPlacement,
    val angle: Double,
    val h: Int,
    val v: Int,
    val emitFlag: Boolean,
)
