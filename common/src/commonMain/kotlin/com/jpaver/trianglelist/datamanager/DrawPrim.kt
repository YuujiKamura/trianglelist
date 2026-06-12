package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY

/**
 * 作図プリミティブ (ADR 0010 段A、frontend/backend 分離の foundation)。
 *
 * 「何を・何処に描くか」を**形式非依存のデータ**として表す。レイアウト側 (writeDrawingFrame
 * 等の frontend) はこの DrawPrim のリストだけを組み、各 writer (DXF/SFC/PDF/Web = backend) は
 * `DrawingFileWriter.drawScene` 経由で自分のプリミティブ実装に翻訳して出力する。
 *
 * 座標は実寸 (モデル座標)。単位変換 (×unitscale) と縮尺の流儀は各 backend のプリミティブ実装
 * (writeLine/writeTextHV 等) が吸収するので、ここには現れない。これにより「同じ scene を全形式が
 * 食う」= 形式間の位置/サイズのズレが構造上起きない (ezdxf の frontend/backend と同型、ADR 0010)。
 *
 * フィールドは既存プリミティブ (DrawingFileWriter.writeLine/writeRect/writeCircle/writeTextHV) の
 * 引数をそのまま写したもの。drawScene が 1:1 で対応プリミティブを呼ぶため、リスト化しても
 * 出力はインライン呼び出し時とバイト単位で同一になる (段A のバイト不変性の根拠)。
 */
sealed interface DrawPrim {
    data class Line(
        val p1: PointXY,
        val p2: PointXY,
        val color: Int,
        val scale: Float = 1f,
    ) : DrawPrim

    data class Rect(
        val center: PointXY,
        val sizeX: Float,
        val sizeY: Float,
        val color: Int,
        val scale: Float = 1f,
    ) : DrawPrim

    data class Circle(
        val center: PointXY,
        val size: Float,
        val color: Int,
        val scale: Float = 1f,
    ) : DrawPrim

    data class Text(
        val text: String,
        val pos: PointXY,
        val color: Int,
        val size: Float,
        val alignH: Int,
        val alignV: Int = 0,
        val angle: Double = 0.0,
        val scale: Float = 1f,
    ) : DrawPrim
}
