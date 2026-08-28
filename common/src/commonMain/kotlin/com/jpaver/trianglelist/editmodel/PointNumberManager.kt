package com.jpaver.trianglelist.editmodel

// java.lang.Cloneable は JVM 専用のため、wasmJs 追加に伴い PointXY と同じ
// com.example.trilib.Cloneable<T> へ切替 (clone() の型・呼び出し形は不変)
class PointNumberManager( ): com.example.trilib.Cloneable<PointNumberManager> {
    /**
     * isMovedByUser  … user が指でつまんで動かした
     * isAutoAligned  … autoAlign (面積・辺長の閾値) が既定位置を寄せた
     * isEscaped      … 自動退避 (NumberCircleEscape) が寸法値を避けて動かした (2026-08-27)
     *
     * isEscaped を分けている理由: 退避位置は calcPoints の「既定位置へ戻す」対象から
     * 外す必要があるが、それを isMovedByUser で代用すると「user が動かした」という
     * 別の意味 (CSV 永続化・アプリの手動配置判定) と混ざる。
     */
    data class Flags(
        var isMovedByUser: Boolean = false,
        var isAutoAligned: Boolean = false,
        var isEscaped: Boolean = false,
    )
    var flag = Flags()

    public override fun clone(): PointNumberManager {
        val b = PointNumberManager()

        b.flag = flag.copy()
        return b
    }

    // region pointNumber

    fun setPointByUser(to: com.example.trilib.PointXY, triangle: Triangle, is_user:Boolean = true ): com.example.trilib.PointXY {
        val BORDER = 20f * triangle.scaleFactor
        val length = to.lengthTo(triangle.pointcenter)
        if( length > BORDER ) return triangle.pointnumber// あまり遠い時はスルー
        flag.isMovedByUser = is_user
        flag.isAutoAligned = false
        return to
    }

    val WEIGHT = 35f

    fun resetAutoFlag(isAuto: Boolean = false){
        flag.isAutoAligned = isAuto
    }

    val BORDER_AREA = 3f
    val BORDER_LENGTH = 0.5f
    fun autoAlign(triangle: Triangle, outlineList: OutlineList? = null) : com.example.trilib.PointXY {
        if(flag.isMovedByUser || flag.isAutoAligned || flag.isEscaped ) return triangle.pointnumber

        val length = arrayOf(triangle.lengthAforce, triangle.lengthBforce, triangle.lengthCforce)
        // lengthのどれかがBORDERよりも少ない場合にtrueを返す
        val isAnyLengthLessThanBorder = length.any { it < BORDER_LENGTH }

        if (triangle.getArea() <= BORDER_AREA && isAnyLengthLessThanBorder  ){
            flag.isAutoAligned = true

            return pointUnconnectedSide( triangle, outlineList )
        }

        return incenter(triangle)
    }

    /**
     * 番号サークルの既定位置 = 内心 (2026-08-28 user「最初にすべきは三角形の重心比重と
     * 面積に沿って番号サークルを寄せる処理」)。
     *
     * 内心は 3 辺からの最短距離が最大になる点 = **その三角形に描ける最大の円の中心**。
     * 「番号という円を置くのに一番余裕がある所」という要求と定義が一対一で対応する。
     * 位置は辺長比重で、収まるかどうかは [inradius] (面積 / 半周長) で出る ── 位置と可否が
     * 同じ 1 つの幾何から出るのが、従来の [weightedMidpoint] (角度 + magic number 35) との差。
     *
     * 頂点と対辺の対応: pointAB は辺 C の対、pointBC は辺 A の対、pointCA は辺 B の対。
     *
     * 実害の出どころは歩道の巻き込み (samples/makikomi_r3.csv)。半径 3m の扇を細い三角形で
     * 割った形では番号が内寄りに留まり、寸法値が旗揚げに追い込まれる。旗揚げは辺の延長線上へ
     * 出るので細長い三角形では引出線が図形の数倍に伸び、図面が引出線の束に埋もれる
     * (2026-08-28 実測: 10 分割版で衝突が 1 → 3 と悪化)。番号が先に一番広い所へ退けば、
     * 寸法値は図形の中に収まる。
     *
     * 退化 (周長 0) は重心を返す ── NaN 座標を図面に撒かないため。
     */
    fun incenter(triangle: Triangle): com.example.trilib.PointXY {
        val wApex = triangle.lengthC
        val wBC = triangle.lengthA
        val wCA = triangle.lengthB
        val total = wApex + wBC + wCA
        if (total <= 0f || !total.isFinite()) return triangle.pointcenter
        val x = (triangle.pointAB.x * wApex + triangle.pointBC.x * wBC + triangle.pointCA.x * wCA) / total
        val y = (triangle.pointAB.y * wApex + triangle.pointBC.y * wBC + triangle.pointCA.y * wCA) / total
        return com.example.trilib.PointXY(x, y)
    }

    /**
     * 内接円半径 r = 面積 / 半周長。番号サークルの半径がこれを超える三角形には、
     * どこに置いても番号が図形内に収まらない ── 自動退避が図形内を探しても解けないことを
     * 探索の前に判定できる (NumberCircleEscape の「図形外へ出すか」の分岐材料)。
     */
    fun inradius(triangle: Triangle): Float {
        val s = (triangle.lengthA + triangle.lengthB + triangle.lengthC) / 2f
        if (s <= 0f || !s.isFinite()) return 0f
        return triangle.getArea() / s
    }

    //pointNumberだけ使う
    fun pointUnconnectedSide(triangle: Triangle, outlineList: OutlineList?=null ): com.example.trilib.PointXY {
        //外側に出すと実行時エラーになる
        val KEISUU = 0.7
        val FLAG_LENGTH_B = triangle.lengthB*KEISUU
        val FLAG_LENGTH_C = triangle.lengthA*KEISUU
        val angle_ = arrayOf( triangle.angleCA, triangle.angleAB, triangle.angleBC )
        val point_ = arrayOf( triangle.pointCA, triangle.pointAB, triangle.pointBC )

        if (triangle.nodeB == null){
            val pointB = getPointByOuterAngle( triangle, angle_[1], angle_[2], point_[1], point_[2], outlineList )
            val resultB = triangle.pointcenter.offset( pointB, FLAG_LENGTH_C )
            return resultB
        }
        if (triangle.nodeC == null){
            val pointC = getPointByOuterAngle( triangle,  angle_[2], angle_[0], point_[2], point_[0], outlineList )
            val resultC = triangle.pointcenter.offset( pointC, FLAG_LENGTH_B )
            return resultC
        }

        return incenter(triangle)
    }

    fun getPointByOuterAngle(triangle: Triangle, angle1:Float, angle2:Float, point1: com.example.trilib.PointXY, point2: com.example.trilib.PointXY, outlineList: OutlineList? ): com.example.trilib.PointXY {
        //val number = triangle.mynumber

        println("getPointByOuterAngle triangle${triangle.mynumber} $angle1 $angle2 $point1 $point2")

        if(outlineList==null) return getPointByAngle(angle1,angle2,point1, point2)

        return outlineList.compare(point1,point2)
    }

    fun getPointByAngle(angle1:Float, angle2:Float, point1: com.example.trilib.PointXY, point2: com.example.trilib.PointXY): com.example.trilib.PointXY {
        if( angle1 > angle2 ) return point1
        return point2
    }

    fun weightedMidpoint(triangle: Triangle, bias: Float): com.example.trilib.PointXY {

        // 角度が大きいほど重みを大きくするための調整
        var weight1 = triangle.angleAB + bias // 角度が大きいほど重みが大きくなる
        var weight2 = triangle.angleBC + bias
        var weight3 = triangle.angleCA + bias

        // 重みの合計で正規化
        val totalWeight = weight1 + weight2 + weight3
        weight1 /= totalWeight
        weight2 /= totalWeight
        weight3 /= totalWeight
        val p1 = triangle.pointAB
        val p2 = triangle.pointBC
        val p3 = triangle.point[0]

        // 重み付き座標の計算
        val weightedX = p1.x * weight1 + p2.x * weight2 + p3.x * weight3
        val weightedY = p1.y * weight1 + p2.y * weight2 + p3.y * weight3
        return com.example.trilib.PointXY(weightedX, weightedY)
    }

    //endregion pointNumber

}