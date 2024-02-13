package com.jpaver.trianglelist.util

import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class AdManager {

    fun disableAd( adView: AdView ){
        adView.visibility = View.INVISIBLE
    }

    fun initializeAdView(adView: AdView) {
        // ここでAdViewの初期化や設定を行う
        // 例:
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}