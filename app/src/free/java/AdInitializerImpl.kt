import android.util.Log
import com.jpaver.trianglelist.util.AdInitializer

// src/free/java/AdInitializerImpl.kt
class AdInitializerImpl: AdInitializer {
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
