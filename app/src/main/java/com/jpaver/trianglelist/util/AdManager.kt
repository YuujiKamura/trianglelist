package com.jpaver.trianglelist.util

import android.view.View
import com.google.android.gms.ads.AdView

class AdManager {

    fun disableAd( adView: AdView ){
        adView.visibility = View.INVISIBLE
    }
}