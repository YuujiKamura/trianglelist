package com.jpaver.trianglelist.util;

import android.view.MotionEvent;

public class RotateGestureDetector {
    public interface OnRotateListener {
        boolean onRotate(float degrees, float focusX, float focusY);
    }

    public static class SimpleOnRotateGestureDetector implements OnRotateListener {
        @Override
        public boolean onRotate(float degrees, float focusX, float focusY) {
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
        // 2本の指がタッチパネルに触れているかどうかを確認
        if (event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            boolean result = true;
            // 2本の指のX座標の差を計算
            float x = event.getX(1) - event.getX(0);
            // 2本の指のY座標の差を計算
            float y = event.getY(1) - event.getY(0);
            // 2本の指の中心点のX座標を計算
            float focusX = (event.getX(1) + event.getX(0)) * 0.5f;
            // 2本の指の中心点のY座標を計算
            float focusY = (event.getY(1) + event.getY(0)) * 0.5f;
            // 2本の指が形成する線とX軸との間の角度を計算（アークタンジェントを使用）
            float tan = (float) Math.atan2(y, x);

            // 前回のタッチイベントからの回転があるかどうかを確認
            if (prevX != 0.0f && prevY != 0.0f) {
                // 前回の角度からの差分を計算し、リスナーを通じて回転イベントを処理
                result = listener.onRotate((tan - prevTan) * RADIAN_TO_DEGREES, focusX, focusY);
            }

            // 現在の値を「前回の値」として保存
            prevX = x;
            prevY = y;
            prevTan = tan;
            return result;
        } else {
            // 2本の指のタッチがない場合は、前回の値をリセット
            prevX = prevY = prevTan = 0.0f;
            return true;
        }
    }

}