package com.jpaver.trianglelist.util

import android.view.ScaleGestureDetector

interface ScaleGestureCallback {
    fun onZoomChange(zoomStep: Float, focusX: Float, focusY: Float)
    fun onScaleBegin() {
        // デフォルト実装は空で、必要に応じてオーバーライドできます。
    }
    fun onScaleEnd() {
        // デフォルト実装は空で、必要に応じてオーバーライドできます。
    }
}

class MyScaleGestureListener(private val callback: ScaleGestureCallback) : ScaleGestureDetector.OnScaleGestureListener {
    private var timeStart: Long = 0
    private var distanceStart: Float = 0f

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val focusX = detector.focusX
        val focusY = detector.focusY

        val timeCurrent = detector.eventTime
        val timeElapsed = timeCurrent - timeStart

        if (timeElapsed >= 10) {  // 単位がミリ秒なので、0.5秒は500ミリ秒になります
            val distanceCurrent = detector.currentSpan

            if (distanceStart == 0f) {
                distanceStart = distanceCurrent
            }

            val zoomStep = if (distanceCurrent - distanceStart > 5) {
                (distanceCurrent - distanceStart) * 0.005f
            } else if (distanceStart - distanceCurrent > 5) {
                -(distanceStart - distanceCurrent) * 0.005f
            } else {
                0f
            }

            if (zoomStep != 0f) {
                callback.onZoomChange(zoomStep, focusX, focusY)
                timeStart = timeCurrent
                distanceStart = distanceCurrent
            }
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        callback.onScaleBegin()
        distanceStart = 0f
        timeStart = detector.eventTime
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        callback.onScaleEnd()
    }
}
