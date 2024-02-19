package com.jpaver.trianglelist.util

import android.content.Context
import android.util.Log
import android.view.ScaleGestureDetector
import com.jpaver.trianglelist.PointXY

class MyScaleGestureListener(private val context: Context) : ScaleGestureDetector.OnScaleGestureListener {

    private var mFocusX: Float = 0f
    private var mFocusY: Float = 0f
    private var time_start: Long = 0
    private var distance_start: Float = 0f
    private var time_elapsed: Long = 0
    private var time_current: Long = 0
    private var distance_current = 0f
    private var flg_pinch_out: Boolean? = null
    private var flg_pinch_in: Boolean? = null
    private var screen_width = 0
    private var screen_height = 0
    var zoomSize: Float = 1.0f
    var isScale = false
    //画面の対角線の長さ
    private var screen_diagonal = 0

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        mFocusX = detector.focusX
        mFocusY = detector.focusY

        time_current = detector.eventTime
        time_elapsed = time_current - time_start

        if (time_elapsed >= 0.5) {
            distance_current = detector.currentSpan

            if (distance_start == 0f) {
                distance_start = distance_current
            }

            flg_pinch_out = distance_current - distance_start > 5
            flg_pinch_in = distance_start - distance_current > 5

            if (flg_pinch_out!!) {
                zoom((distance_current - distance_start) * 0.005f)

                time_start = time_current
                distance_start = distance_current
            } else if (flg_pinch_in!!) {
                zoom(-(distance_start - distance_current) * 0.005f)

                time_start = time_current
                distance_start = distance_current
            }
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        Log.d("MyViewLifeCycle", "onScaleBegin")
        //resetPointToZero()
        distance_start = detector.eventTime.toFloat()
        if (distance_start > screen_diagonal) {
            distance_start = 0f
        }
        time_start = detector.eventTime

        //resetPressedInModel( PointXY( detector.focusX, detector.focusY ) )
        //(context as MainActivity).setTargetEditText()

        isScale = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {

    }

    fun zoom(zoomstep: Float){
        zoomSize += zoomstep
        if(zoomSize<=0.1f) zoomSize = 0.1f
        if(zoomSize>=5) zoomSize = 5f

    }

}
