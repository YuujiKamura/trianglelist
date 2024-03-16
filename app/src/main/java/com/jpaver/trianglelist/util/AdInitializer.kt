package com.jpaver.trianglelist.util

import android.util.Log
import com.jpaver.trianglelist.BuildConfig

interface AdInitializer {
    fun initialize()
    fun showBannerAd()
    fun disableBannerAd()
    fun showInterstitialAd()
}

object AdInitializerFactory {
    fun create(): AdInitializer {
        return if (BuildConfig.FLAVOR == "free") {
            FreeAdInitializer()
        } else {
            FullAdInitializer()
        }
    }
}

class FreeAdInitializer: AdInitializer {
    override fun initialize() {
        // AdMobの初期化コード
        Log.d("AdMob", "AdMob initialized for free version.")
    }

    override fun showBannerAd() {

    }

    override fun disableBannerAd() {

    }

    override fun showInterstitialAd() {

    }
}

class FullAdInitializer: AdInitializer {
    override fun initialize() {
        // AdMobの初期化コード
        Log.d("AdMob", "AdMob initialized for free version.")
    }

    override fun showBannerAd() {

    }

    override fun disableBannerAd() {

    }

    override fun showInterstitialAd() {

    }
}