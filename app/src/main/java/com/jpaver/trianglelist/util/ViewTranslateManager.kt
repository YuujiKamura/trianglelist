package com.jpaver.trianglelist.util

import android.graphics.Canvas

class ViewTranslateManager() {

    var baseInView = com.example.trilib.PointXY(0f, 0f)
    var centerInModel = com.example.trilib.PointXY(0f, 0f)
    var pressedInModel = com.example.trilib.PointXY(0f, 0f)

    var zoomSize: Float = 1.0f

    fun screenTranslate( canvas: Canvas ){
        canvas.translate(baseInView.x, baseInView.y) // baseInViewはview座標系の中央を標準としていて、そこからスクロールによって移動した数値になる。
        canvas.scale(zoomSize, zoomSize )//, mFocusX, mFocusY )//, scaleCenter.x, scaleCenter.y )//この位置に来ることでscaleの中心がbaseInViewに依存する。
        //canvas.translate(-pressedInModel.x, pressedInModel.y)//どこで更新されているのか追跡
        canvas.translate(-centerInModel.x, centerInModel.y)
    }

    fun setParameters(_baseInView: com.example.trilib.PointXY, _zoomSize: Float, _centerInModel: com.example.trilib.PointXY, _pressedInModel: com.example.trilib.PointXY){
        baseInView = _baseInView
        zoomSize = _zoomSize
        centerInModel = _centerInModel
        pressedInModel = _pressedInModel
    }

    fun pressedInViewToModel(pressedInView: com.example.trilib.PointXY): com.example.trilib.PointXY {
        return pressedInView.translateAndScale(
            baseInView,
            centerInModel,
            zoomSize, //ズームレベルによってなぜか位置が動いている
        )
    }

}

class SynchronizedPoint(){
    var pModel = com.example.trilib.PointXY(0f, 0f)
    var pView  = com.example.trilib.PointXY(0f, 0f)
    var viewSize: Float = 1.0f

}