package com.jpaver.trianglelist

class SfcWriter {

    external fun hello(): String

    companion object {
        init {
            System.loadLibrary("sample-native")
            System.loadLibrary("sample-main")
        }
    }
}