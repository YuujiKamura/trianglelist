package com.jpaver.trianglelist.util;

import android.content.Context;
import android.view.MotionEvent;

public class RotateGestureDetector {
    public interface OnRotateListener {
        boolean onRotate(float degrees, float focusX, float focusY);
        boolean onRotateEnd();
    }

    public static class SimpleOnRotateGestureDetector implements OnRotateListener {
        @Override
        public boolean onRotate(float degrees, float focusX, float focusY) {
            return false;
        }
        @Override
        public boolean onRotateEnd() {
            return false;
        }
    }

    private static final float RADIAN_TO_DEGREES = (float) (180.0 / Math.PI);
    private final OnRotateListener listener;
    private float prevX = 0.0f;
    private float prevY = 0.0f;
    private float prevTan;

    public RotateGestureDetector(OnRotateListener listener) {
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            boolean result = true;
            float x = event.getX(1) - event.getX(0);
            float y = event.getY(1) - event.getY(0);
            float focusX = (event.getX(1) + event.getX(0)) * 0.5f;
            float focusY = (event.getY(1) + event.getY(0)) * 0.5f;
            float tan = (float) Math.atan2(y, x);

            if (prevX != 0.0f && prevY != 0.0f) {
                result = listener.onRotate((tan - prevTan) * RADIAN_TO_DEGREES, focusX, focusY);
            }

            prevX = x;
            prevY = y;
            prevTan = tan;
            return result;
        } else {
            prevX = prevY = prevTan = 0.0f;
            return true;
        }
    }
}