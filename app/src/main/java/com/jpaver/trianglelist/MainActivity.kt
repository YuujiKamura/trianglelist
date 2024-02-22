package com.jpaver.trianglelist

import AdInitializerImpl
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.jpaver.trianglelist.databinding.ActivityMainBinding
import com.jpaver.trianglelist.filemanager.XlsxWriter
import com.jpaver.trianglelist.fragment.MyDialogFragment
import com.jpaver.trianglelist.util.AdInitializer
import com.jpaver.trianglelist.util.AdManager
import com.jpaver.trianglelist.util.EditTextViewLine
import com.jpaver.trianglelist.util.EditorTable
import com.jpaver.trianglelist.util.Params
import com.jpaver.trianglelist.util.TitleParamStr
import com.jpaver.trianglelist.util.TitleParams
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import kotlin.math.roundToInt


data class ResStr(
        // 文字列リソースをstringに変換して保持する。
        var tTitle_: String = "",
        var eRName_: String = "",
        var tCname_: String = "",
        var tDtype_: String = "",
        var tDname_: String = "",
        var tScale_: String = "",
        var tNum_: String = "",
        var tDateHeader_: String = "",
        var tDate_: String = "",
        var tAname_: String = "",
        var menseki_: String = "",
        var mTitle_: String = "",
        var mCname_: String = "",
        var mSyoukei_: String = "",
        var mGoukei_: String = "",
        var tCredit_: String = ""
)

//region TextWatcher
private class MyTextWatcher(
    val mELine: EditTextViewLine,
    var lastParams: Params
) : TextWatcher {
    //private val afterTextChanged_: TextView = findViewById<TextView>(R.id.afterTextChanged)
    //private val beforeTextChanged_: TextView = findViewById<TextView>(R.id.beforeTextChanged)
    //private val onTextChanged_: TextView = findViewById<TextView>(R.id.onTextChanged)
    override fun afterTextChanged(s: Editable) {
        val input = mELine.name.text.toString()
//            myEditor.LineRewrite(Params(input,"",myDeductionList.length()+1,dP.a, dP.b, dP.c, dP.pn, i, PointXY(0f,0f)), myELFirst)

        if(input == "仕切弁" || input == "ソフト弁" || input == "ドレーン") {
            mELine.a.setText( 0.23f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "消火栓" || input == "空気弁") {
            mELine.a.setText( 0.55f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "下水") {
            mELine.a.setText( 0.72f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "汚水") {
            mELine.a.setText( 0.67f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "雨水枡" || input == "電柱"){
            mELine.a.setText( 0.40f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "電気" || input == "NTT"){
            mELine.a.setText("1.0")
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "基準点"){
            mELine.a.setText("0.3")
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "消火栓B") {
            mELine.a.setText( 0.35f.toString() )
            mELine.b.setText( 0.45f.toString() )
            mELine.pl.setSelection(1)
        }
        if(input == "基礎") {
            mELine.a.setText( 0.50f.toString() )
            mELine.b.setText( 0.50f.toString() )
            mELine.pl.setSelection(1)
        }
        if(input == "集水桝") {
            mELine.a.setText( 0.70f.toString() )
            mELine.b.setText( 0.70f.toString() )
            mELine.pl.setSelection(1)
        }

        // 記憶した控除パラメータの復元
        if(input == lastParams.name){
            mELine.a.setText(lastParams.a.toString())
            mELine.b.setText(lastParams.b.toString())
            mELine.pl.setSelection(lastParams.pl)
        }
    }

    override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) {
        ("start=" + start
                + ", count=" + count
                + ", after=" + after
                + ", s=" + s.toString())
        //beforeTextChanged_.text = input
    }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
        ("start=" + start
                + ", before=" + before
                + ", count=" + count
                + ", s=" + s.toString())
        //onTextChanged_.text = input
    }
}

interface CustomTextWatcher: TextWatcher{
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
}
//endregion

class MainActivity : AppCompatActivity(),
        MyDialogFragment.NoticeDialogListener {

    //region viewbinding init
    private lateinit var prefSetting: SharedPreferences

    private lateinit var bindingMain: ActivityMainBinding

    private lateinit var my_view: MyView

    lateinit var fab_replace: FloatingActionButton
    lateinit var fab_flag: FloatingActionButton
    lateinit var fab_dimsidew: FloatingActionButton
    lateinit var fab_dimsideh: FloatingActionButton
    lateinit var fab_nijyuualign: FloatingActionButton
    lateinit var fab_minus: FloatingActionButton
    lateinit var fab_undo: FloatingActionButton
    lateinit var fab_fillcolor: FloatingActionButton
    lateinit var fab_texplus: FloatingActionButton
    lateinit var fab_texminus: FloatingActionButton
    lateinit var fab_setB: FloatingActionButton
    lateinit var fab_setC: FloatingActionButton
    lateinit var fab_rot_l: FloatingActionButton
    lateinit var fab_rot_r: FloatingActionButton
    lateinit var fab_deduction: FloatingActionButton
    lateinit var fab_resetView: FloatingActionButton
    lateinit var fab_up: FloatingActionButton
    lateinit var fab_down: FloatingActionButton
    lateinit var fab_debug: FloatingActionButton
    lateinit var fab_testbasic: FloatingActionButton
    lateinit var fab_pdf: FloatingActionButton
    lateinit var fab_share: FloatingActionButton
    lateinit var fab_mail: FloatingActionButton
    lateinit var fab_numreverse: FloatingActionButton
    lateinit var fab_xlsx: FloatingActionButton

    lateinit var ela1 : EditText
    lateinit var elb1 : EditText
    lateinit var elc1 : EditText
    lateinit var ela2 : EditText
    lateinit var elb2 : EditText
    lateinit var elc2 : EditText
    lateinit var elsa1 : String
    lateinit var elsb1 : String
    lateinit var elsc1 : String
    lateinit var elsa2 : String
    lateinit var elsb2 : String
    lateinit var elsc2 : String
//endregion


//region Parameters
    var isViewAttached = false

    var isCSVsavedToPrivate = false

    private lateinit var myELFirst: EditTextViewLine
    private lateinit var myELSecond: EditTextViewLine
    private lateinit var myELThird: EditTextViewLine

    private val sNumberList = listOf(
            "No.0", "No.1", "No.2", "No.3", "No.4", "No.5", "No.6", "No.7", "No.8", "No.9",
            "No.10", "No.11", "No.12", "No.13", "No.14", "No.15", "No.16", "No.17", "No.18", "No.19",
            "No.20", "No.21", "No.22", "No.23", "No.24", "No.25", "No.26", "No.27", "No.28", "No.29"
    )
    private val dedNameListC = listOf(
            "仕切弁",
            "ソフト弁",
            "ドレーン",
            "汚水",
            "下水",
            "雨水枡",
            "電柱",
            "基準点",
            "空気弁",
            "消火栓",
            "NTT",
            "電気"
    )

    // val dedmap = MapOf(dedNameList to )

    private var myEditor: EditorTable = EditorTable()
    private var dParams: Params = Params("", "", 0, 0f, 0f, 0f, 0, 0,
        PointXY(0f, 0f)
    )
    private var lastParams: Params = dParams

    // タイトルパラメータ、stringリソースから構成する

    private lateinit var rStr : ResStr
    private lateinit var titleTri: TitleParams//
    private lateinit var titleDed: TitleParams
    private lateinit var titleTriStr : TitleParamStr
    private lateinit var titleDedStr : TitleParamStr

    private lateinit var myTriangleList: TriangleList //= TriangleList(Triangle(0f,0f,0f,PointXY(0f,0f),180f))
    private lateinit var myDeductionList: DeductionList

    private var trilistUndo: TriangleList = TriangleList()


    var deductionMode: Boolean = false
    private var mIsCreateNew: Boolean = false
    private val onetohandred = 11.9f
    private val experience = 4f
    private val mScale = onetohandred*experience

    private var koujiname = ""
    private var rosenname = ""
    private var gyousyaname = ""
    private var zumennum = "1/1"
    private var drawingStartNumber = 1
    private var isNumberReverse = false

    private var colorindex = 4
    private val resColors = arrayOf(
            R.color.colorPink,   //0
            R.color.colorOrange, //1
            R.color.colorYellow, //2
            R.color.colorLime,   //3
            R.color.colorSky     //4
    )


    private var TAG = "MainActivity"
    //private val isAdTEST_ = true
    //private val TestAdID_ = "ca-app-pub-3940256099942544/6300978111"
    //private val UnitAdID_ = "ca-app-pub-6982449551349060/2369695624"



    private var fileType: String = "notyet"
    private var filename = "notyet"
    private lateinit var sendMailLauncher: ActivityResultLauncher<Intent>
    private lateinit var shareFilesLauncher: ActivityResultLauncher<Intent>
    private lateinit var loadContent: ActivityResultLauncher<Intent>

    val shareFiles: MutableList<String> = mutableListOf(strDateRosenname(".csv"), strDateRosenname(".xlsx"), strDateRosenname(".dxf"))
    val shareUris: MutableList<Uri> = mutableListOf()

    //endregion

    //region File ActivityResultLauncher
    private fun handleSendMailResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            // メールアプリが正常に終了した後の処理
            deletePrivateFile(strDateRosenname(".xlsx"))
            deletePrivateFile(strDateRosenname(".dxf"))
            // その他の必要な処理
        }
    }

    private fun handleShareFilesResult(result: ActivityResult, fileNamesToDelete: List<String>, fileUris: List<Uri>) {
        if (result.resultCode == Activity.RESULT_OK) {
            // 正常に終了した後、指定された内部ファイルを削除
            deletePrivateFiles(fileNamesToDelete)
            // 共有操作が成功した後、ファイルに対する永続的なアクセス権を取得
            fileUris.forEach { uri ->
                try {
                    // 永続的なアクセス権を付与
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    // アクセス権の付与に失敗した場合の処理
                    e.printStackTrace()
                }
            }
        }
    }

    private fun handleLoadContentResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.data?.let { uri ->
                try {
                    //showEncodingSelectionDialog(uri)
                     loadFileWithEncoding(uri) { reader ->
                        // ここでreaderを使用したファイルの読み込み処理を行う
                        // 例: CSVファイルの読み込みとパース
                        parseCSV(reader)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                setTitles()
            }
        }
    }

    private fun loadFileWithEncoding(uri: Uri, encoding: String = "Shift-JIS", onFileLoaded: (BufferedReader) -> Unit) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, encoding))
                onFileLoaded(reader)  // 選択されたエンコーディングでファイルを読み込んだ後に渡された関数を実行
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // エラーハンドリング
        }
    }


    // endregion


//region adMob loading
    private lateinit var mAdView : AdView
    val adManager = AdManager()
    private var mInterstitialAd: InterstitialAd? = null
    val adInitializer: AdInitializer = AdInitializerImpl()
    private fun adMobInit() {

        adInitializer.initialize()

    if ( BuildConfig.FLAVOR != "free" ){
            Log.d("AdMob", "adMobInit() cancelled.")
            return
        }

        // AdView の参照を取得
        mAdView = findViewById(R.id.adView)

        // Mobile Ads SDK の初期化
        MobileAds.initialize(this)

        // デバッグモードの場合、テストデバイスを設定
        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf("TEST_EMULATOR") // エミュレーターをテストデバイスとして使用
            val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)
        }

        // 広告リクエストの作成
        val adRequest = AdRequest.Builder().build()

        // AdListener をセットして広告のロード状態を監視
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // 広告が正常にロードされたときの処理
                Log.d("AdMob", "Ad loaded successfully.")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // 広告のロードに失敗したときの処理
                Log.d("AdMob", "Failed to load ad: ${adError.message}")
            }

            override fun onAdOpened() {
                // 広告が画面いっぱいに表示されたときの処理（インタースティシャル広告など）
                Log.d("AdMob", "Ad opened.")
            }

            override fun onAdClicked() {
                // 広告がクリックされたときの処理
                Log.d("AdMob", "Ad clicked.")
            }

            override fun onAdClosed() {
                // 広告が閉じられたときの処理（インタースティシャル広告が閉じられたときなど）
                Log.d("AdMob", "Ad closed.")
            }
        }

        // 広告のロードを開始
        mAdView.loadAd(adRequest)
    }

    private fun adMobDisable() {
        adInitializer.disableBannerAd()

        if ( BuildConfig.FLAVOR != "free" ){
            Log.d("AdMob", "adMobDisable() cancelled.")
            return
        }
        // 広告の非表示
        //if( BuildConfig.FLAVOR == "free" ){

        adManager.disableAd(mAdView)
        //findViewById<EditText>(R.id.editLengthC1).requestFocus()
        //mAdView.visibility = VISIBLE
        //}
    }

    private fun adShowInterStitial() {
        adInitializer.showInterstitialAd()

        if ( BuildConfig.FLAVOR != "free" ){
            Log.d("AdMob", "adShowInterStitial() cancelled.")
            return
        }
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,"ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("AdMob", "InterAd was loaded.")
                    mInterstitialAd = interstitialAd
                    // 広告がロードされたら、ここで広告を表示
                    mInterstitialAd?.show(this@MainActivity)
                }
            }
        )
    }
//endregion

    //region ActivityLifeCycle

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivityLifeCycle", "onCreate")

        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_NoActionBar) //Note that this should be called before any views are instantiated in the Context (for example before calling Activity.setContentView(View) or LayoutInflater.inflate(int, ViewGroup)).

        bindingMain = ActivityMainBinding.inflate(layoutInflater)

        val view = bindingMain.root

        setSupportActionBar(bindingMain.toolbar)
        setContentView(view)
        Log.d("MainActivityLifeCycle", "setContentView")

        sendMailLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::handleSendMailResult)

        shareFilesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // ラムダ式内で handleShareFilesResult を呼び出し、追加のパラメータを渡す
            handleShareFilesResult(result, shareFiles, shareUris )
        }

        loadContent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::handleLoadContentResult)

        val tArray = resources.getStringArray(R.array.ParentList)
        initSpinner(tArray)
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // This example applies an immediate update. To apply a flexible update
                // instead, pass in AppUpdateType.FLEXIBLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    1 )
            }
            else Log.d( "AppUpdate", "Update is not Available.")
        }

        adMobInit()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        myDeductionList = DeductionList()
        //Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()

        initFabs()
        fabController()

    }
    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        //bMyView = FragmentFirstBinding.bind( findViewById(R.id.my_view) )//inflate(layoutInflater)
        my_view = findViewById(R.id.my_view)//bMyView.myView
        Log.d("myView", "Instance check in MainActivity: " + my_view )
        Log.d("MainActivityLifeCyce", "onAttachedToWindow")

        rStr = ResStr(
            getString(R.string.tenkai_title),
            getString(R.string.rosen1),
            getString(R.string.tenkai_koujimei),
            getString(
                R.string.tenkai_zumenmei
            ),
            getString(R.string.tenkai_rosenmei),
            getString(R.string.tenkai_syukusyaku),
            getString(R.string.tenkai_zuban),
            getString(
                R.string.tenkai_sakuseibi
            ),
            getString(R.string.tenkai_nengappi),
            getString(R.string.tenkai_sekousya),
            getString(R.string.menseki),
            getString(
                R.string.menseki_title
            ),
            getString(R.string.menseki_koujimei),
            getString(R.string.menseki_syoukei),
            getString(R.string.menseki_goukei),
            getString(
                R.string.credit
            )
        )
        titleTri = TitleParams(
            R.string.menseki,
            R.string.editor_number,
            R.string.editor_sokuten,
            R.string.editor_lA,
            R.string.editor_lB,
            R.string.editor_lC,
            R.string.editor_parent,
            R.string.editor_setuzoku
        )
        titleDed = TitleParams(
            R.string.menseki,
            R.string.editor_number,
            R.string.editor_name,
            R.string.editor_lA,
            R.string.editor_lB,
            R.string.editor_lC,
            R.string.editor_syozoku,
            R.string.editor_form
        )
        titleTriStr = TitleParamStr(
            getString(titleTri.type), getString(titleTri.n), getString(
                titleTri.name
            ), getString(titleTri.a), getString(titleTri.b), getString(titleTri.c), getString(
                titleTri.pn
            ), getString(titleTri.pl)
        )
        titleDedStr = TitleParamStr(
            getString(titleDed.type), getString(titleDed.n), getString(
                titleDed.name
            ), getString(titleDed.a), getString(titleDed.b), getString(titleDed.c), getString(
                titleDed.pn
            ), getString(titleDed.pl)
        )

        val filepath = this.filesDir.absolutePath + "/" + "privateTriList.csv"
        val file = File(filepath)
        if(file.exists()) {
            resumeCSV()

        }
        else createNew()
        loadEditTable()
        colorMovementFabs()
        //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
        fab_replace.backgroundTintList = getColorStateList(R.color.colorLime)
        setEditNameAdapter(sNumberList)

        checkPermission()

        ela1 = findViewById<EditText>(R.id.editLengthA1)
        elb1 = findViewById<EditText>(R.id.editLengthB1)
        elc1 = findViewById<EditText>(R.id.editLengthC1)
        ela2 = findViewById<EditText>(R.id.editLengthA2)
        elb2 = findViewById<EditText>(R.id.editLengthB2)
        elc2 = findViewById<EditText>(R.id.editLengthC2)

        // EditTextの入力値の変化を追跡するリスナーを登録
        elb1.addTextChangedListener(object : CustomTextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                my_view.watchedB1_ = p0.toString()
                my_view.invalidate()
            }
            override fun afterTextChanged(p0: Editable?) {
                my_view.watchedB1_ = p0.toString()
                my_view.invalidate()
            }
        })

        elc1.addTextChangedListener(object : CustomTextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                my_view.watchedC1_ = p0.toString()
                my_view.invalidate()
            }
            override fun afterTextChanged(p0: Editable?) {
                my_view.watchedC1_ = p0.toString()
                my_view.invalidate()
            }
        })

        ela2.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                my_view.watchedA2_ = p0.toString()
                my_view.invalidate()
            }
        })


        elb2.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                my_view.watchedB2_ = p0.toString()
                my_view.invalidate()
            }
        })

        elc2.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                my_view.watchedC2_ = p0.toString()
                my_view.invalidate()
            }
        })

        isViewAttached = true
        Log.d("MainActivity", "OnAttachedToWindow Process Done.")

        adShowInterStitial()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isViewAttached = false
    }

    override fun onPause(){
        super.onPause()
    }

    override fun onStop(){
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        isViewAttached = true
        Log.d("MainActivityLifeCycle", "OnResume")
        adMobDisable()

        //my_view.setScreenSize() //スクリーンサイズの更新
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d("MainActivityLifeCycle", "onCreateOptionsMenu")
        // 上部のOptionsMenuの表示　Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
//endregion

    //region File Intent
    fun playMedia(file: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = file
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment?) {
        Log.d("MainActivityLifeCycle", "onDialogPositiveClick")
        mIsCreateNew = true
        fileType = "CSV"
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.type = "text/csv"
        i.putExtra(Intent.EXTRA_TITLE, strDateRosenname(".csv"))
        saveContent.launch( Pair( "text/csv", strDateRosenname(".csv") ) )
        setResult(RESULT_OK, i)

    }

    override fun onDialogNegativeClick(dialog: DialogFragment?) {
        editorResetBy(getList(deductionMode))
        createNew()
    }

    private fun launchIntentBasedOnFileType(fileType: String, mimeType: String, fileExtension: String) {
        when (fileType) {
            "CSV", "XLSX" -> {
                launchCreateDocumentIntent(fileType, mimeType, fileExtension)
            }
            "DXF", "SFC" -> {
                showExportDialog(fileExtension, "Export $fileType", fileType, mimeType)
            }
            "PDF" -> {
                showDialogInputZumenTitles("Save PDF") { launchIntentToSavePdf() }
            }
            // 他のファイルタイプに関する処理を追加する場合は、ここに記述
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()


        when (item.itemId) {
            R.id.action_new -> {
                MyDialogFragment().show(supportFragmentManager, "dialog.basic")
            }
            R.id.action_save_csv -> {
                launchIntentBasedOnFileType("CSV", "text/csv", ".csv")
            }
            R.id.action_load_csv -> {
                openDocumentPicker()  // CSVファイルの読み込み
            }
            R.id.action_save_dxf -> {
                launchIntentBasedOnFileType("DXF", "*/*", ".dxf")
            }
            R.id.action_save_sfc -> {
                launchIntentBasedOnFileType("SFC", "*/*", ".sfc")
            }
            R.id.action_save_xlsx -> {
                launchIntentBasedOnFileType("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx")
            }
            R.id.action_save_pdf -> {
                launchIntentBasedOnFileType("PDF", "", "")
            }
            R.id.action_send_mail -> {
                sendMail()
            }
            R.id.action_usage, R.id.action_privacy -> {
                val url = if (item.itemId == R.id.action_usage) "https://trianglelist.home.blog" else "https://drive.google.com/file/d/1C7xlXZGvabeQoNEjmVpOCAxQGrFCXS60/view?usp=sharing"
                playMedia(Uri.parse(url))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        isCSVsavedToPrivate = false
        isCSVsavedToPrivate = saveCSVtoPrivate()

        return true
    }


    private fun launchCreateDocumentIntent(fileType: String, mimeType: String, fileExtension: String) {
        this.fileType = fileType
        saveContent.launch(Pair(mimeType, strDateRosenname(fileExtension)))
    }


    private fun showDialogInputZumenTitles( title: String, onComplete: () -> Unit ) {
        // 入力に関する情報をリストで管理
        val inputs = listOf(
            Pair(getString(R.string.inputcname), koujiname),
            Pair(getString(R.string.inputdname), rosenname),
            Pair(getString(R.string.inputaname), gyousyaname),
            Pair(getString(R.string.inputdnum), zumennum)
        )

        // 再帰的にダイアログを表示する関数
        fun showInputDialogRecursively(index: Int = 0) {
            if (index >= inputs.size) {
                onComplete() // 全ての入力が完了したらonCompleteを実行
                return
            }

            val (message, prefillText) = inputs[index]
            showInputDialog(
                title,
                message = message,
                prefillText = prefillText,
                onInputReceived = { input ->
                    // 入力された値を適切な変数に格納
                    when (index) {
                        0 -> koujiname = input
                        1 -> rosenname = input
                        2 -> gyousyaname = input
                        3 -> zumennum = input
                    }
                    // 次の入力ダイアログを表示
                    showInputDialogRecursively(index + 1)
                }
            )
        }

        // 最初のダイアログを表示
        showInputDialogRecursively()
    }

    private fun showInputDialog(title: String, message: String, prefillText: String?, onInputReceived: (String) -> Unit) {
        val inputForm = EditText(this).apply {
            hint = message
            filters = arrayOf(InputFilter.LengthFilter(50))
            setText(prefillText)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(inputForm)
            .setPositiveButton("OK") { _, _ ->
                onInputReceived(inputForm.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("NotUse"){ _, _ ->
                onInputReceived("")
            }
            .show()
    }

    private fun launchIntentToSavePdf() {
        fileType =
            "PDF"
        Intent(
            Intent.ACTION_CREATE_DOCUMENT
        ).apply {
            addCategory(
                Intent.CATEGORY_OPENABLE
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            type =
                "application/pdf"
            putExtra(
                Intent.EXTRA_TITLE,
                strDateRosenname(".pdf")
            )

        }
        saveContent.launch( Pair( "application/pdf", strDateRosenname(".pdf") ) )
    }

    //endregion



    //region editorTable Controll

    private fun printDebugConsole(){
        /*val tvd: TextView = findViewById(R.id.debugconsole)
        //面積(控除なし): ${myTriangleList.getArea()}㎡　(控除あり):${myTriangleList.getArea()-myDeductionList.getArea()}㎡
        tvd.text = """ myView.Center: ${my_view.myTriangleList.center.x} ${my_view.myTriangleList.center.y}
                        |TriCurrent: ${my_view.getTriangleList().getCurrent()} T1.color ${
            my_view.getTriangleList().get(
                1
            ).color_
        } ${myTriangleList.get(1).color_}
                        |TapTL: ${my_view.tapTL_} , lastTapNum: ${my_view.getTriangleList().lastTapNumber_}, lastTapSide: ${my_view.getTriangleList().lastTapSide_}
                        |viewX: ${my_view.getViewSize().x}, viewY ${my_view.getViewSize().y}, zoomsize: ${my_view.zoomSize}
                        |mtsX: ${
            myTriangleList.measureMostLongLine().x
        } , mtsY: ${
            myTriangleList.measureMostLongLine().y
        }  mtcX: ${myTriangleList.center.x} , mtcY: ${
            myTriangleList.center.y
        }
                        |mtscl: ${myTriangleList.scale} , mtc: ${myTriangleList.getCurrent()}  mdl: ${myDeductionList.size()} , mdc: ${myDeductionList.getCurrent()}
                        |currentname: ${
            myTriangleList.get(myTriangleList.size()).getMyName_()
        }  cur-1name: ${
            myTriangleList.get(
                myTriangleList.size() - 1
            ).getMyName_()
        }
                        |myAngle: ${myTriangleList.myAngle}
            """.trimMargin()*/
    }


    private fun setEditNameAdapter(namelist: List<String>){
        val textView =
            findViewById<View>(R.id.editName1) as AutoCompleteTextView
        val textView2 =
            findViewById<View>(R.id.editName2) as AutoCompleteTextView
        val textView3 =
            findViewById<View>(R.id.editName3) as AutoCompleteTextView

        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, namelist)
        textView.setAdapter(adapter)
        textView2.setAdapter(adapter)
        textView3.setAdapter(adapter)
        textView.threshold = 1
        textView2.threshold = 1
        textView3.threshold = 1
        textView.addTextChangedListener(MyTextWatcher(myELFirst, lastParams))

    }
    private fun setTitles(){
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()
        //findViewById<EditText>(R.id.rosenname).setText(rosenname)

        val dedArea = myDeductionList.getArea()
        val triArea = myTriangleList.getArea()
        val totalArea = roundByUnderTwo(triArea - dedArea).formattedString(2)
        title = rStr.menseki_ + ": ${ totalArea } m^2"

        /*if( myTriangleList.lastTapNumber_ > 0 ){
            val coloredArea = myTriangleList.getAreaC( myTriangleList.lastTapNumber_ )
            val colorStr = arrayOf( "red: ", "orange: ", "yellow: ", "green: ", "blue: " )
            val tapped = myTriangleList.get( myTriangleList.lastTapNumber_ )
            title = rStr.menseki_ + ": ${ totalArea } m^2" + " ( ${ colorStr[tapped.color_]+coloredArea } m^2 )"
        }*/

    }

    private fun roundByUnderTwo(fp: Float) :Float {
        val ip: Int = ( fp * 100f ).roundToInt()
        return ip * 0.01f
    }
    private fun turnToBlankTrilistUndo(){
        trilistUndo = TriangleList() // reset
        bindingMain.fabUndo.backgroundTintList = getColorStateList(R.color.colorPrimary)
    }

    private fun typeToInt(type: String) :Int{
        var pl = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }

    private fun parentBCtoCParam(pbc: Int, lenA: Float, cp: ConnParam) : ConnParam {
        when(pbc){
            1 -> return ConnParam(1, 0, 2, 0f)//B
            2 -> return ConnParam(2, 0, 2, 0f)//C
            3 -> return ConnParam(1, 1, 2, lenA)//BR
            4 -> return ConnParam(1, 1, 0, lenA)//BL
            5 -> return ConnParam(2, 1, 2, lenA)//CR
            6 -> return ConnParam(2, 1, 0, lenA)//CL
            7 -> return ConnParam(1, 1, 1, lenA)//BC
            8 -> return ConnParam(2, 1, 1, lenA)//CC
            9 -> return ConnParam(1, 2, cp.lcr, lenA)//BF
            10 -> return ConnParam(2, 2, cp.lcr, lenA)//CF
        }

        return ConnParam(0, 0, 0, 0f)
    }

    private fun initSpinner(tArray: Array<out String>) {
        //val spinnerItemBinding = SpinnerItemBinding.inflate(layoutInflater)

        val R = android.R.layout.simple_spinner_item
        //val R2 = spinnerItemBinding.hashCode()

        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this, R, tArray
        )
        setSpinnerAdapter(spinnerArrayAdapter)
    }

    private fun setSpinnerAdapter(spinnerArrayAdapter: ArrayAdapter<String>) {
        findViewById<Spinner>(R.id.editParentConnect1).adapter = spinnerArrayAdapter
        findViewById<Spinner>(R.id.editParentConnect2).adapter = spinnerArrayAdapter
        findViewById<Spinner>(R.id.editParentConnect3).adapter = spinnerArrayAdapter
    }


    private fun flipDeductionMode() {
        myDeductionList.current = myDeductionList.size()
        myTriangleList.changeSelectedNumber(myTriangleList.size())
        //printDebugConsole()
        colorMovementFabs()

        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        if(!deductionMode) {
            deductionMode = true
            my_view.deductionMode = true
            Toast.makeText(this, "Edit Mode : Area Deductions", Toast.LENGTH_LONG).show()

            // 入力テーブルの見かけの変更、タイトル行の文字列とカラー
            myEditor.setHeaderTable(
                findViewById(R.id.TV_NUM),
                findViewById(R.id.TV_Name),
                findViewById(R.id.TV_A),
                findViewById(R.id.TV_B),
                findViewById(R.id.TV_C),
                findViewById(R.id.TV_PN),
                findViewById(R.id.TV_PL),
                titleDed
            )
            findViewById<TableRow>(R.id.LL1).setBackgroundColor(Color.rgb(255, 165, 155))

            //　fab群の見かけの変更
            //fab.setBackgroundTintList(getColorStateList(R.color.colorTT2))
            fab_replace.backgroundTintList = getColorStateList(R.color.colorTT2)
            fab_resetView.backgroundTintList = getColorStateList(R.color.colorTT2)
            fab_up.backgroundTintList = getColorStateList(R.color.colorTT2)
            fab_down.backgroundTintList = getColorStateList(R.color.colorTT2)
            fab_numreverse.backgroundTintList = getColorStateList(R.color.colorTT2)
            fab_deduction.backgroundTintList = getColorStateList(R.color.colorWhite)
            fab_flag.backgroundTintList = getColorStateList(R.color.colorWhite)
            val iconB: Icon = Icon.createWithResource(this, R.drawable.box)
            val iconC: Icon = Icon.createWithResource(this, R.drawable.circle)
            val iconF: Icon = Icon.createWithResource(this, R.drawable.flag)
            val iconRL: Icon = Icon.createWithResource(this, R.drawable.rot_dl)
            val iconRR: Icon = Icon.createWithResource(this, R.drawable.rot_dr)
            fab_setB.setImageIcon(iconB)
            fab_setC.setImageIcon(iconC)
            fab_flag.setImageIcon(iconF)
            fab_rot_l.setImageIcon(iconRL)
            fab_rot_r.setImageIcon(iconRR)

            //　入力テーブルのオートコンプリート候補の変更、名前入力列にフォーカス、ソフトキーボード表示
            val dArray = resources.getStringArray(R.array.DeductionFormList)
            initSpinner(dArray)
            findViewById<EditText>(R.id.editName1).requestFocus()
            inputMethodManager.showSoftInput(findViewById(R.id.editName1), 0)
            setEditNameAdapter(dedNameListC)

            //クロスヘアラインを画面中央に描画
//            my_view.drawCrossHairLine()

        } else {
            deductionMode = false
            my_view.deductionMode = false
            Toast.makeText(this, "Edit Mode : Triangles", Toast.LENGTH_LONG).show()
            // 入力テーブルの見かけの変更、タイトル行の文字列とカラー
            myEditor.setHeaderTable(
                findViewById(R.id.TV_NUM),
                findViewById(R.id.TV_Name),
                findViewById(R.id.TV_A),
                findViewById(R.id.TV_B),
                findViewById(R.id.TV_C),
                findViewById(R.id.TV_PN),
                findViewById(R.id.TV_PL),
                titleTri
            )
            findViewById<TableRow>(R.id.LL1).setBackgroundColor(Color.rgb(185, 255, 185))

            //　fab群の見かけの変更
            //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
            fab_replace.backgroundTintList = getColorStateList(R.color.colorLime)
            fab_resetView.backgroundTintList = getColorStateList(R.color.colorSky)
            fab_up.backgroundTintList = getColorStateList(R.color.colorSky)
            fab_down.backgroundTintList = getColorStateList(R.color.colorSky)
            fab_numreverse.backgroundTintList = getColorStateList(R.color.colorAccent)
            fab_deduction.backgroundTintList = getColorStateList(R.color.colorAccent)
            fab_flag.backgroundTintList = getColorStateList(R.color.colorAccent)
            val iconB: Icon = Icon.createWithResource(this, R.drawable.set_b)
            val iconC: Icon = Icon.createWithResource(this, R.drawable.set_c)
            val iconF: Icon = Icon.createWithResource(this, R.drawable.flag_b)
            val iconRL: Icon = Icon.createWithResource(this, R.drawable.rot_l)
            val iconRR: Icon = Icon.createWithResource(this, R.drawable.rot_r)
            fab_setB.setImageIcon(iconB)
            fab_setC.setImageIcon(iconC)
            fab_flag.setImageIcon(iconF)
            fab_rot_l.setImageIcon(iconRL)
            fab_rot_r.setImageIcon(iconRR)

            //　入力テーブルのオートコンプリート候補の変更、名前入力列にフォーカス、ソフトキーボードは出さない
            val tArray = resources.getStringArray(R.array.ParentList)
            initSpinner(tArray)
            findViewById<EditText>(R.id.editLengthA2).requestFocus()
            setEditNameAdapter(sNumberList)
        }
        editorResetBy(getList(deductionMode))
        //inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0)

    }

    private fun editorResetBy(elist: EditList){
        val currentNum = elist.retrieveCurrent()
        val eo = elist.get(currentNum)
        val eob = elist.get(currentNum - 1)

        loadEditTable()
        my_view.setParentSide(elist.size(), 0)
        myEditor.lineRewrite(
                Params(
                        "",
                        "",
                        elist.size() + 1,
                        0f,
                        0f,
                        0f,
                        elist.size(),
                        0,
                    PointXY(0f, 0f)
                ), myELFirst
        )
        myEditor.lineRewrite(eo.getParams(), myELSecond)
        if(currentNum > 1) myEditor.lineRewrite(eob.getParams(), myELThird)
        if(currentNum == 1) myEditor.lineRewrite(
                Params(
                        "",
                        "",
                        0,
                        0f,
                        0f,
                        0f,
                        0,
                        0,
                    PointXY(0f, 0f)
                ), myELThird
        )
    }

    fun isValid(dp: Params) : Boolean{
        if (dp.a <= 0.0f || dp.b <= 0.0f || dp.c <= 0.0f) return false
        if (dp.a + dp.b <= dp.c ){
            Toast.makeText(this, "Invalid!! : C > A + B", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.b + dp.c <= dp.a ){
            Toast.makeText(this, "Invalid!! : A > B + C", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.c + dp.a <= dp.b ){
            Toast.makeText(this, "Invalid!! : B > C + A", Toast.LENGTH_LONG).show()
            return false
        }

        if ( dp.pn > myTriangleList.size() || ( dp.pn < 1 && dp.n != 1 )) {
            Toast.makeText(this, "Invalid!! : number of parent", Toast.LENGTH_LONG).show()
            return false
        }
        if (  dp.pl < 1 && dp.n != 1  ) {
            Toast.makeText(this, "Invalid!! : connection in parent", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun loadEditTable(){
        myELFirst =
            EditTextViewLine(
                    findViewById(R.id.editNumber1),
                    findViewById(R.id.editName1),
                    findViewById(R.id.editLengthA1),
                    findViewById(R.id.editLengthB1),
                    findViewById(R.id.editLengthC1),
                    findViewById(R.id.editParentNumber1),
                    findViewById(R.id.editParentConnect1)
            )

        myELSecond =
            EditTextViewLine(
                    findViewById(R.id.editNumber2),
                    findViewById(R.id.editName2),
                    findViewById(R.id.editLengthA2),
                    findViewById(R.id.editLengthB2),
                    findViewById(R.id.editLengthC2),
                    findViewById(R.id.editParentNumber2),
                    findViewById(R.id.editParentConnect2)
            )

        myELThird =
            EditTextViewLine(
                    findViewById(R.id.editNumber3),
                    findViewById(R.id.editName3),
                    findViewById(R.id.editLengthA3),
                    findViewById(R.id.editLengthB3),
                    findViewById(R.id.editLengthC3),
                    findViewById(R.id.editParentNumber3),
                    findViewById(R.id.editParentConnect3)
            )
        Log.d("EditorTable", "Load Success.")


    }

    private fun validDeduction(dp: Params): Boolean {
        return isValidName(dp.name) && isValidDimensions(dp)
    }

    private fun isValidName(name: String): Boolean {
        return name.isNotEmpty()
    }

    private fun isValidDimensions(dp: Params): Boolean {
        if (dp.a < 0.1f) return false
        if (dp.type == "Box" && dp.b < 0.1f) return false
        return true
    }

    private fun getList(dMode: Boolean) : EditList {
        return if(dMode) myDeductionList
        else myTriangleList
    }

    fun toString( editText: EditText ) : String{
        return editText.text.toString()
    }

    private fun updateElStrings(){
        elsa1 = toString( ela1 )
        elsb1 = toString( elb1 )
        elsc1 = toString( elc1 )
        elsa2 = toString( ela2 )
        elsb2 = toString( elb2 )
        elsc2 = toString( elc2 )
    }


    //endregion

    //region FabController
    private fun initFabs(){
        fab_replace =   bindingMain.fabReplace
        fab_flag =      bindingMain.fabFlag
        fab_dimsidew =  bindingMain.fabDimsidew
        fab_dimsideh =  bindingMain.fabDimsideh
        fab_nijyuualign = bindingMain.fabNijyuualign
        fab_minus =     bindingMain.fabMinus
        fab_undo =      bindingMain.fabUndo
        fab_fillcolor = bindingMain.fabFillcolor
        fab_texplus =   bindingMain.fabTexplus
        fab_texminus =  bindingMain.fabTexminus
        fab_setB =      bindingMain.fabSetB
        fab_setC =      bindingMain.fabSetC
        fab_rot_l =     bindingMain.fabRotL
        fab_rot_r =     bindingMain.fabRotR
        fab_deduction = bindingMain.fabDeduction
        fab_resetView = bindingMain.fabResetView
        fab_up =        bindingMain.fabUp
        fab_down =      bindingMain.fabDown
        fab_debug =     bindingMain.fabDebug
        fab_testbasic = bindingMain.fabTestbasic
        fab_pdf =       bindingMain.fabPdf
        fab_share =     bindingMain.fabShare
        fab_mail =      bindingMain.fabMail
        fab_numreverse = bindingMain.fabNumreverse
        fab_xlsx = bindingMain.fabXlsx
    }

    private fun saveAndPerform(action: () -> Unit) {
        action()
        isCSVsavedToPrivate = false
        isCSVsavedToPrivate = saveCSVtoPrivate()
    }

    private fun setCommonFabListener(fab: FloatingActionButton, action: () -> Unit) {
        fab.setOnClickListener {
            saveAndPerform(action)
        }
    }

    private fun fabController(){

        val mainViewModel = MainViewModel()

        setCommonFabListener(fab_replace) {
            fabReplace(dParams, false)
        }

        setCommonFabListener(fab_flag) {

            fabFlag()
        }


        setCommonFabListener(fab_dimsidew) {

            mainViewModel.setMember( deductionMode, myTriangleList, myDeductionList )
            mainViewModel.fabDimSide("W", { setListAndResetView( { my_view.invalidate() }, false ) } )

        }

        setCommonFabListener(fab_dimsideh) {
            mainViewModel.setMember( deductionMode, myTriangleList, myDeductionList )
            mainViewModel.fabDimSide("H", { setListAndResetView( { my_view.invalidate() }, false ) }  )

        }

        setCommonFabListener(fab_nijyuualign) {
            if(!deductionMode && myTriangleList.lastTapNumber_ > 1 ){
                myTriangleList.rotateCurrentTriLCR()
                //myTriangleList.resetTriConnection(myTriangleList.lastTapNum_, );
                my_view.setTriangleList(myTriangleList, mScale, false )
                my_view.resetView(my_view.toLastTapTriangle())
                editorResetBy(getList(deductionMode))

            }
        }

        var deleteWarning = 0
        setCommonFabListener(fab_minus) {
            val listLength = getList(deductionMode).size()

            if(listLength > 0 && deleteWarning == 0) {
                deleteWarning = 1
                bindingMain.fabMinus.backgroundTintList = getColorStateList(R.color.colorTT2)

            }
            else {
                if (listLength > 0) {
                    trilistUndo = myTriangleList.clone()

                    var eraseNum = listLength
                    if(!deductionMode) eraseNum = myTriangleList.lastTapNumber_

                    getList(deductionMode).remove(eraseNum)

                    //my_view.removeTriangle()
                    my_view.setDeductionList(myDeductionList, mScale)
                    my_view.setTriangleList(myTriangleList, mScale)

                    editorResetBy(getList(deductionMode))
                }
                deleteWarning = 0
                bindingMain.fabMinus.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            printDebugConsole()
            colorMovementFabs()
            my_view.resetViewToLastTapTriangle()
            setTitles()

        }

        setCommonFabListener(fab_undo){
            if( trilistUndo.size() > 0 ){
                myTriangleList = trilistUndo.clone()
                //my_view.undo()
                my_view.setTriangleList(trilistUndo, mScale)
                my_view.resetViewToLastTapTriangle()

                trilistUndo.trilist_.clear()

                bindingMain.fabUndo.backgroundTintList = getColorStateList(R.color.colorPrimary)
                editorResetBy(getList(deductionMode))
                setTitles()
            }
        }

        setCommonFabListener(fab_fillcolor) {
            if(!deductionMode){
                myTriangleList.get(my_view.myTriangleList.selectedNumber)


                colorindex ++
                if(colorindex == resColors.size) colorindex = 0
                bindingMain.fabFillcolor.backgroundTintList = getColorStateList(resColors[colorindex])

                //dParams_ = myEditor.ReadLine(dParams_, myELSecond)
                myTriangleList.get(my_view.myTriangleList.selectedNumber).color_ = colorindex

                my_view.setFillColor(colorindex, myTriangleList.selectedNumber)
            }
        }

        setCommonFabListener(fab_texplus) {
            my_view.textSize += 5f
            my_view.setAllTextSize(my_view.textSize)

//            my_view.paintTexS.textSize = my_view.ts_
            my_view.invalidate()
        }

        setCommonFabListener(fab_texminus) {
            my_view.textSize -= 5f
            my_view.setAllTextSize(my_view.textSize)

            my_view.invalidate()
        }


        setCommonFabListener(fab_rot_l) {
            fabRotate(5f, true )
        }

        setCommonFabListener(fab_rot_r) {
            fabRotate(-5f, true )
        }

        setCommonFabListener(fab_numreverse){
            trilistUndo = myTriangleList.clone()
            mainViewModel.setMember( deductionMode, myTriangleList, myDeductionList )

            whenTriDed({
                if( BuildConfig.DEBUG ){
                    myTriangleList = mainViewModel.fabReverse() as TriangleList
                    setListAndResetView( { my_view.toLastTapTriangle() } )
                }
            }, {
                myDeductionList = mainViewModel.fabReverse() as DeductionList
                setListAndResetView( { my_view.invalidate() } )
            })

        }

        //ここからはビュー操作用とファイルシェア用のFAB、図形を書き換えないのでオートセーブの対象外
        fab_setB.setOnClickListener {
            autoConnection(1)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        fab_setC.setOnClickListener {
            autoConnection(2)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }


        fab_deduction.setOnClickListener {
            deleteWarning = 0
            fab_minus.backgroundTintList = getColorStateList(R.color.colorAccent)
            flipDeductionMode()
            colorMovementFabs()
        }

        fab_resetView.setOnClickListener {

            try{
                if(!deductionMode) my_view.resetViewToLastTapTriangle()
                else if( myDeductionList.size() > 0 ){
                    val currentIndex = my_view.myDeductionList.current
                    my_view.resetView(
                        my_view.myDeductionList.get(currentIndex).point.scale(
                            PointXY(
                                1f,
                                -1f
                            )
                        )
                    )
                }
            } catch ( e: NullPointerException ){
                Toast.makeText(this, "List is NUll.", Toast.LENGTH_LONG).show()
            }
        }

        fab_up.setOnClickListener {
            myEditor.scroll(-1, getList(deductionMode), myELSecond, myELThird)

            if(!deductionMode) moveTrilist()
            else if( myDeductionList.size() > 0 ){
                my_view.myDeductionList.current = myDeductionList.current
                val currentIndex = my_view.myDeductionList.current
                my_view.resetView(
                    my_view.myDeductionList.get(currentIndex).point.scale(
                        PointXY(
                            1f,
                            -1f
                        )
                    )
                )
            }

            colorMovementFabs()
            printDebugConsole()
            setTitles()
        }

        fab_down.setOnClickListener {
            myEditor.scroll(1, getList(deductionMode), myELSecond, myELThird)

            if(!deductionMode) moveTrilist()
            else if( myDeductionList.size() > 0 ){
                my_view.myDeductionList.current = myDeductionList.current
                val currentIndex = my_view.myDeductionList.current
                my_view.resetView(
                    my_view.myDeductionList.get(currentIndex).point.scale(
                        PointXY(
                            1f,
                            -1f
                        )
                    )
                )
            }

            colorMovementFabs()
            printDebugConsole()
            setTitles()

        }

        fab_debug.setOnClickListener {
            //my_view.isDebug_ = !my_view.isDebug_

            if(!my_view.isAreaOff_){
                my_view.isAreaOff_ = true
                fab_debug.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            else{
                my_view.isAreaOff_ = false
                fab_debug.backgroundTintList = getColorStateList(R.color.colorLime)
            }

            my_view.invalidate()

            // オートセーブpdf, dxf
            //if( BuildConfig.BUILD_TYPE == "debug" ) {
            //    AutoSavePDF()
            //AutoSaveDXF()
            //}

        }

        fab_testbasic.setOnClickListener {
            //CreateNew()

            findViewById<TextView>(R.id.editLengthB1).text = "" // reset
            fabReplace(Params("", "", 1, 7f, 7f, 7f, 0, 0), true)
            findViewById<TextView>(R.id.editLengthB1).text = 0.6f.toString()//"6f" // add
            fabReplace(Params("", "", 2, 7f, 6f, 6f, 1, 2), true)

            findViewById<TextView>(R.id.editLengthA1).text = 0.23f.toString()//"0.23f" // add
            deductionMode = true
            fabReplace(
                Params(
                    "仕切弁", "Circle", 1, 0.23f, 0f, 0f, 1, 0,
                    PointXY(1f, 0f),
                    PointXY(
                        0f,
                        0f
                    )
                ), true
            )
            deductionMode = false


            Toast.makeText(this, "Basic Senario Test Done.", Toast.LENGTH_SHORT).show()
        }

        fab_pdf.setOnClickListener {
            viewPdf( getAppLocalFile(this, "privateTrilist.pdf") )
        }

        fab_xlsx.setOnClickListener{
            viewXlsx( getAppLocalFile(this, "privateTrilist.xlsx") )
        }

        fab_share.setOnClickListener {
            sendFiles()
        }

        fab_mail.setOnClickListener {
            //findViewById<ProgressBar>(R.id.indeterminateBar).visibility = View.VISIBLE
            //progressBar.visibility = View.VISIBLE

            sendMail()

            //    progressBar.visibility = View.INVISIBLE

        }


    }

    private fun whenTriDed(methodFalse:() -> Unit, methodTrue:() -> Unit ){
        when( deductionMode ){
            false -> methodFalse()
            true -> methodTrue()
        }
    }
    private fun setListByDedMode( moveCenter: Boolean = true ){
        whenTriDed({
            my_view.setTriangleList( myTriangleList, mScale, moveCenter )
        },{
            my_view.setDeductionList(myDeductionList, mScale)
        })
    }
    private fun setListAndResetView(resetViewMethod:() -> Unit, moveCenter: Boolean = true ) {
        editorResetBy(getList(deductionMode))
        setListByDedMode( moveCenter )
        resetViewMethod()
        printDebugConsole()
        //saveCSVtoPrivate()
    }

    fun fabFlag(){
        dParams = myEditor.readLineTo(dParams, myELSecond)// 200703 // if式の中に入っていると当然ながら更新されない時があるので注意

        if(deductionMode){
            val d = myDeductionList.get(dParams.n)
            dParams.ptF = my_view.getTapPoint()
            dParams.pt = d.point
            //var ded = myDeductionList.get(dParams_.n)
            my_view.getTapPoint().scale(
                PointXY(
                    0f,
                    0f
                ), 1 / mScale, -1 / mScale)
            if(validDeduction(dParams)) {// あまり遠い時はスルー
                myDeductionList.replace(dParams.n, dParams)
//                    EditorReset(getList(myDeductionMode),getList(myDeductionMode).length())
                my_view.setDeductionList(myDeductionList, mScale)
            }
        }
        else{
            val tri = myTriangleList.get(dParams.n)
            val tp = my_view.getTapPoint().scale(
                PointXY(
                    0f,
                    0f
                ), 1 / mScale, -1 / mScale)
            if( tp.lengthTo(tri.pointCenter_) < 10f ){ // あまり遠い時はスルー
                tri.pointNumber_ = tp
                tri.isPointNumberMovedByUser_ = true
                my_view.setTriangleList(myTriangleList, mScale, false)
            }
        }

        my_view.invalidate()
    }

    fun fabRotate(degrees: Float, bSeparateFreeMode: Boolean){
        if(!deductionMode) {
            myTriangleList.rotate(PointXY(0f, 0f), degrees, myTriangleList.lastTapNumber_, bSeparateFreeMode )
            myDeductionList.rotate(PointXY(0f, 0f), -degrees )
            my_view.setTriangleList(myTriangleList, mScale)
            my_view.setDeductionList(myDeductionList, mScale)
            my_view.invalidate()//resetViewToLSTP()
        }
        // ded rotate
        else {
            val vdltip = my_view.myDeductionList.lastTapIndex_+1
            myDeductionList.get(vdltip).rotateShape(myDeductionList.get(vdltip).point, -degrees )
            my_view.setDeductionList(myDeductionList, mScale)
            my_view.invalidate()
        }
    }

    fun fabReplace(params: Params = dParams, useit: Boolean = false ){
        //val editor = myEditor
        trilistSaving(myTriangleList)
        val dedMode = deductionMode

        var readedFirst  = Params()
        var readedSecond = Params()
        myEditor.readLineTo(readedFirst, myELFirst)
        myEditor.readLineTo(readedSecond, myELSecond)
        if(useit){
            readedFirst = params
            readedSecond = params
        }
        val strTopA = findViewById<TextView>(R.id.editLengthA1).text.toString()
        val strTopB = findViewById<TextView>(R.id.editLengthB1).text.toString()
        val strTopC = findViewById<TextView>(R.id.editLengthC1).text.toString()

        if (!dedMode) {
            if (strTopB == "") {
                resetTrianglesBy(readedSecond)
            } else if (strTopC == "" && !useit) {
                return
            } else {
                addTriangleBy(readedFirst)
            }
        } else { // if in deduction mode
            if (strTopA == "") {
                resetDeductionsBy(readedSecond)
                my_view.myDeductionList[readedSecond.n].point
            } else {
                addDeductionBy(readedFirst)
                my_view.myDeductionList[readedFirst.n].point
            }
            findViewById<EditText>(R.id.editName1).requestFocus()
        }

        my_view.setTriangleList(myTriangleList, mScale)
        setListAndResetView( { whenTriDed( {my_view.resetView(my_view.toLastTapTriangle())}, {my_view.invalidate()} ) } )
        setTitles()


        my_view.myTriangleList.isDoubleTap_ = false
        my_view.myTriangleList.lastTapSide_ = 0

        logListCurrent()
        /*if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(
                this,
                isSucceed.toString(),
                Toast.LENGTH_SHORT
        ).show()*/
    }


    private fun moveTrilist(){
        my_view.getTriangleList().changeSelectedNumber(myTriangleList.retrieveCurrent())
        my_view.myTriangleList.lastTapNumber_ = myTriangleList.retrieveCurrent()
        myTriangleList.lastTapNumber_ = myTriangleList.retrieveCurrent()
        my_view.resetViewToLastTapTriangle()
    }

    private fun colorMovementFabs() : Int{
    val max: Int = getList(deductionMode).size()
    val current: Int = getList(deductionMode).retrieveCurrent()
    val min = 1
    var movable = 0
    //fab_zoomin.setBackgroundTintList(getColorStateList(R.color.colorSky))
    //fab_zoomout.setBackgroundTintList(getColorStateList(R.color.colorSky))
    fab_resetView.backgroundTintList = getColorStateList(R.color.colorSky)
    //色
    fab_fillcolor.backgroundTintList = getColorStateList(resColors[colorindex])

    fab_share.backgroundTintList = getColorStateList(R.color.colorLime)


    if(max > current) {
        fab_down.backgroundTintList = getColorStateList(R.color.colorSky)
        movable++
    }
    else fab_down.backgroundTintList = getColorStateList(R.color.colorAccent)

    if(min < current){
        fab_up.backgroundTintList = getColorStateList(R.color.colorSky)
        movable += 2
    }
    else fab_up.backgroundTintList = getColorStateList(R.color.colorAccent)

    return movable
}
    // Params インスタンスの中身をログに出力する関数
    private fun logParams(params: Params, tag: String = "ParamsLog") {
        val paramsContents = with(params) {
            """
        |name: $name
        |type: $type
        |n: $n
        |a: $a
        |b: $b
        |c: $c
        |pn: $pn
        |pl: $pl
        |pt: (${pt.x}, ${pt.y})
        |pts: (${ptF.x}, ${ptF.y})
        """.trimMargin()
        }
        Log.d(tag, paramsContents)
    }

    private fun addDeductionBy(params: Params) : Boolean {
        if (!validDeduction(params)) {
            Log.d( "Deduction", "invalid parameters" )
            logParams(params, "add Dedution")
            return false
        }

        // 所属する三角形の判定処理
        myDeductionList.add( processDeduction(params) )
        my_view.setDeductionList(myDeductionList, mScale)
        lastParams = params

        logFabController()
        return true

    }


    private fun processDeduction(params: Params): Params {
        params.pt = my_view.getTapPoint()
        params.ptF = params.pt //PointXY(0f, 0f)

        //形状の自動判定
        if( params.b > 0f ) params.type = "Box"
        else params.type = "Circle"

        // 所属する三角形の判定処理
        if (params.pt != PointXY(0f, 0f)) {
            params.pn = my_view.myTriangleList.isCollide(
                params.pt.scale(
                    PointXY(
                        1f,
                        -1f
                    )
                )
            )

            if (params.pn != 0) {
                my_view.myTriangleList.dedmapping(myDeductionList, -1)
                Log.d(
                    "Deduction",
                    "ptri dedcount" + my_view.myTriangleList.get(params.pn).dedcount
                )

                val trilistinview = my_view.myTriangleList
                val parent = trilistinview.get(params.pn)
                Log.d("Deduction", "parent:" + parent.toString() )
                //ビュー空間からモデル空間にする際にY軸を反転する。そこからビュー空間に戻すためにさらにもう一度Y軸反転をかけている。
                //これいらないのでは・・Deductionの管理をビュー空間ベースからモデル空間にすれば
                params.ptF = parent.pointUnconnectedSide(
                    params.pt.scale(1f,-1f),
                    1f,
                    -1f,
                    PointXY(0f, 0f)
                )
                Log.d("Deduction", "params.point:  " + params.pt.x + ", " + params.pt.y)
                Log.d("Deduction", "params.pointF: " + params.ptF.x + ", " + params.ptF.y)
            }


        }
        return params
    }

    private fun resetDeductionsBy(params: Params) : Boolean {
        if (!validDeduction(params)) {
            Log.d( "Deduction", "invalid parameters" )
            logParams(params, "reset Dedution")
            return false
        }

        //myTriangleList.current = params.pn

        // 所属する三角形の判定処理
        myDeductionList.replace(params.n, processDeduction(params) )
        logFabController()
        return true
    }

    private fun setFabColor(fab: FloatingActionButton, colorIndex: Int ){
        fab.backgroundTintList = getColorStateList( colorIndex )
    }

    private fun trilistSaving( from: TriangleList ){
        trilistUndo = from.clone()
    }
    private fun createNewTriangle( params: Params, parentTri: Triangle ): Triangle{
        val newTri = Triangle(
            parentTri,
            params
        )
        newTri.myNumber_ = params.n
        return newTri
    }

    private fun trilistAdd(params: Params, triList: TriangleList ){
        val newTri = createNewTriangle( params, triList.getMemberByIndex(params.pn) )
        triList.add(newTri, true)
        triList.lastTapNumber_ = triList.size()
    }
    private fun setUI(){
        setFabColor( fab_undo, R.color.colorLime )
        findViewById<EditText>(R.id.editLengthA1).requestFocus()
    }

    private fun addTriangleBy(params: Params) : Boolean {
        if ( isValid( params ) ) {

            trilistSaving( myTriangleList )
            trilistAdd( params, myTriangleList )
            setUI()

            return true
        }
        return false
    }

    private fun resetTrianglesBy(params: Params) : Boolean {

        return if (isValid(params)){
            trilistUndo = myTriangleList.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            myTriangleList.resetFromParam(params)
        } // if valid triangle
        else false
    }


//endregion


    //region TapEvent
    private fun autoConnection(i: Int){

        my_view.myTriangleList.lastTapSide_ = i
        dParams = myEditor.readLineTo(dParams, myELFirst) //keep them

        var focusTo = elb1

        if( my_view.myTriangleList.isDoubleTap_ == true ){
            if(i == 1) focusTo = elb2
            if(i == 2) focusTo = elc2
        }

        if(!deductionMode) {

            val t: Triangle = myTriangleList.get(dParams.pn)
            myEditor.lineRewrite(
                    Params(
                            dParams.name,
                            "",
                            myTriangleList.size() + 1,
                            t.getLengthByIndex(i),
                            dParams.b,
                            dParams.c,
                            t.myNumber_,
                            i,
                        PointXY(0f, 0f),
                        PointXY(0f, 0f)
                    ), myELFirst
            )

            my_view.setParentSide(t.getMyNumber_(), i)

            if(my_view.myTriangleList.lastTapSide_ != -1){
                my_view.myTriangleList.isDoubleTap_ = true

                focusTo.requestFocus()
                focusTo.setSelection(focusTo.text.length)
                val inputMethodManager: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(focusTo, 0)
            }
        }
        else{
            setFabSetBC(i)
            myEditor.lineRewrite(
                    Params(
                            dParams.name,
                            "",
                            myDeductionList.size() + 1,
                            dParams.a,
                            dParams.b,
                            dParams.c,
                            dParams.pn,
                            i,
                        PointXY(
                            0f,
                            0f
                        )
                    ), myELFirst
            )
        }
    }

    private fun setFabSetBC(i: Int){
        if(i == 1) {
            fab_setB.backgroundTintList = getColorStateList(R.color.colorAccent)

        }
    }

    fun setTargetEditText(zoomsize: Float)
    {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        val trilistV = my_view.myTriangleList

        if(deductionMode){

            my_view.myDeductionList.setScale(my_view.myScale)
            val lasttap = my_view.myDeductionList.getTapIndex(my_view.pressedInModel)
            Log.d("Deduction", "lasttap:"+lasttap )

            if ( my_view.myDeductionList.lastTapIndex_ > -1 ) {
                val tapIndex = my_view.myDeductionList.lastTapIndex_+1
                if( -1 < tapIndex ) {
                    //Toast.makeText(this, "deduction tap", Toast.LENGTH_SHORT).show()
                    myEditor.scroll(
                            tapIndex - myDeductionList.current,
                            getList(deductionMode), myELSecond, myELThird
                    )
                    //my_view.resetView( my_view.myDeductionList.get( tapIndex ).point.scale( PointXY(1f,-1f ) ) )
                }
            }

            // 三角形番号が押されたときはセンタリング
            trilistV.getTap(
                my_view.pressedInModel.scale(
                    PointXY(
                        0f,
                        0f
                    ), 1f, -1f),
                0.4f / zoomsize
            )
            if ( trilistV.lastTapNumber_ != 0 ){
                handleTriangleTap(trilistV, myEditor, myTriangleList)
                if( trilistV.lastTapSide_ == 3 ) my_view.resetViewToLastTapTriangle()
            }


        }
        else { // edit triangle
            updateElStrings()
            my_view.setWatchedStrings(elsa1,elsb1,elsc1,elsa2,elsb2,elsc2)

            val lpp = my_view.pressedInModel.scale(PointXY(0f, 0f), 1f, -1f)
            val slpp = my_view.shadowTri_.getTapLength(lpp, 0.8f / zoomsize)

            when (slpp) {
                1 -> {
                    findViewById<EditText>(R.id.editLengthB1).requestFocus() // EditTextB1にフォーカスを設定
                    // my_view.myTriangleList.lastTapSide_ = 1
                    return
                }
                2 -> {

                    findViewById<EditText>(R.id.editLengthC1).requestFocus() // EditTextC1にフォーカスを設定
                    // my_view.myTriangleList.lastTapSide_ = 2
                    return
                }
            }


            // view　の　trilistのlastTapとcurrentをずらして editorTableを移動させる
            trilistV.getTap(lpp, my_view.textSize * 0.02f / zoomsize )

            // タップされた三角形がある場合の処理を行う
            if (trilistV.lastTapNumber_ != 0) {
                handleTriangleTap(trilistV, myEditor, myTriangleList, true ) // タップされた三角形の基本処理を行う
                setTriangleDetails(myTriangleList) // タップされた三角形の詳細設定を行う
                setEditTextContent(myTriangleList) // EditTextに三角形情報をセットする

                // タップされた辺に応じた処理を行う
                when (my_view.myTriangleList.lastTapSide_) {
                    0 -> handleSideZero(inputMethodManager) // 辺0がタップされたときの処理
                    1, 2 -> autoConnection(my_view.myTriangleList.lastTapSide_) // 辺1または2がタップされたときの自動接続処理
                    3 -> my_view.resetViewToLastTapTriangle() // 辺3がタップされたときのビューをリセットする処理
                }
            }

        }

        Log.d("SetTarget", "Tap Triangle is : " + my_view.myTriangleList.lastTapNumber_ + ", side is :" + my_view.myTriangleList.lastTapSide_ )
        logListCurrent()
    }

    // タップされた三角形に関連する基本処理を行う関数
    fun handleTriangleTap(trilistV: TriangleList, myEditor: EditorTable, myTriangleList: TriangleList, isEditorScroll: Boolean = false ) {
        if( isEditorScroll ) myEditor.scroll(trilistV.lastTapNumber_ - trilistV.selectedNumber, myTriangleList, myELSecond, myELThird) // スクロールしてタップされた三角形を表示
        trilistV.selectedNumber = trilistV.lastTapNumber_ // 現在の三角形を更新
        myTriangleList.changeSelectedNumber(my_view.myTriangleList.lastTapNumber_) // myTriangleListの現在の三角形を更新
        myTriangleList.lastTapNumber_ = my_view.myTriangleList.lastTapNumber_ // 最後にタップされた三角形の番号を更新
        myTriangleList.lastTapSide_ = my_view.myTriangleList.lastTapSide_ // 最後にタップされた三角形の辺を更新
    }

    // タップされた三角形の詳細設定を行う関数
    fun setTriangleDetails(myTriangleList: TriangleList) {
        colorindex = myTriangleList.get(myTriangleList.lastTapNumber_).color_ // タップされた三角形の色を取得
        colorMovementFabs() // 色の設定を更新
        printDebugConsole() // デバッグコンソールに情報を出力
        setTitles() // タイトルを設定
    }

    // EditTextに三角形情報をセットする関数
    fun setEditTextContent(myTriangleList: TriangleList) {
        findViewById<EditText>(R.id.editParentNumber1).setText(myTriangleList.lastTapNumber_.toString()) // 最後にタップされた三角形の番号を設定
        findViewById<EditText>(R.id.editNumber1).setText(myTriangleList.size().toString()) // 三角形リストのサイズを設定
    }

    // 辺0がタップされたときの処理を行う関数
    fun handleSideZero(inputMethodManager: InputMethodManager) {
        val editLengthA2 = findViewById<EditText>(R.id.editLengthA2) // 辺Aの長さを編集するEditTextを取得
        editLengthA2.requestFocus() // フォーカスを設定
        editLengthA2.setSelection(editLengthA2.text.length) // EditTextのテキストを選択状態にする
        inputMethodManager.showSoftInput(editLengthA2, 0) // ソフトキーボードを表示
        my_view.setParentSide(my_view.getTriangleList().lastTapNumber_, 3) // 親となる辺を設定
    }
//endregion





    //region File Saving
    class CreateDocumentWithType : ActivityResultContract<Pair<String, String>, Uri?>() {
        override fun createIntent(context: Context, input: Pair<String, String>): Intent {
            // input ペアから MIME タイプとファイル名を取得
            val (mimeType, fileName) = input
            return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType  // 動的に指定された MIME タイプを設定
                putExtra(Intent.EXTRA_TITLE, fileName)  // ファイル名を設定
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode == Activity.RESULT_OK) {
                return intent?.data
            }
            return null
        }
    }


    // ActivityResultLauncher の定義
    private val saveContent = registerForActivityResult(CreateDocumentWithType()) { uri: Uri? ->
        uri?.let {
            try {
                // 永続的なアクセス権を取得
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                // Uri 保存
                prefSetting.edit {
                    putString("uri", uri.toString())
                }

                // 保存処理
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    when (fileType) {
                        "DXF" -> saveDXF(BufferedWriter(OutputStreamWriter(outputStream, "Shift-JIS")))
                        "CSV" -> saveCSV(BufferedWriter(OutputStreamWriter(outputStream, "Shift-JIS")))
                        "PDF" -> savePDF(outputStream)
                        "SFC" -> saveSFC(BufferedOutputStream(outputStream))
                        "XLSX" -> {
                            val xlsxWriter = XlsxWriter()
                            xlsxWriter.write(outputStream, myTriangleList, myDeductionList, rosenname)
                        }
                        else -> {}
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
                // 権限取得失敗時の処理
            }
        }
        setTitles()
    }

    private fun saveToFile(filename: String, writeData: (OutputStream) -> Unit) {
        try {
            // プライベート領域にファイルを開く
            openFileOutput(filename, Context.MODE_PRIVATE).use { fileOutputStream ->
                // データの書き込みを委譲
                writeData(fileOutputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()  // エラーハンドリング
        }
    }

    private fun saveDxfToPrivate(filename: String) {
        saveToFile(filename) { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, "Shift-JIS")).use { bufferedWriter ->
                saveDXF(bufferedWriter)
            }
        }
    }

    private fun saveSfcToPrivate(filename: String) {
        saveToFile(filename) { outputStream ->
            BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                saveSFC(bufferedOutputStream)
            }
        }
    }

    //endregion




    //region File Permission
    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.MANAGE_DOCUMENTS,
            Manifest.permission.INTERNET
        )
        private const val REQUESTPERMISSION = 1000
    }

    private fun checkPermission() {
        if (!isGranted()) {
            requestPermissions(PERMISSIONS, REQUESTPERMISSION)
        }
    }

    private fun isGranted(): Boolean {
        for (i in PERMISSIONS.indices) {
            //初回はPERMISSION_DENIEDが返る
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                //一度リクエストが拒絶された場合にtrueを返す．初回，または「今後表示しない」が選択された場合，falseを返す．
                if (shouldShowRequestPermissionRationale(PERMISSIONS[i])) {
                    Toast.makeText(this, "アプリを実行するためには許可が必要です", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTPERMISSION) {
            //checkPermission()
        }
        Log.d("MainActivityLifeCycle", "onRequestPermissionsResult")
    }

    //endregion




    //region File Share
    fun showExportDialog(filePrefix: String, title: String, fileType: String, intentType: String): Boolean {
        val editText = createNumberInputEditText()
        filename = createFileName(filePrefix)

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.hintInputTriNumber)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                processExport(title, editText, fileType, intentType, filePrefix, reverseNumber = false)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("NumReverse") { _, _ ->
                processExport(title, editText, fileType, intentType, filePrefix, reverseNumber = true)
            }

        dialogBuilder.show()
        return true
    }

    private fun createNumberInputEditText(): EditText {
        return EditText(this).apply {
            hint = getString(R.string.hintInputTriNumber)
            filters = arrayOf(InputFilter.LengthFilter(3))
            setText(drawingStartNumber.toString())
        }
    }

    private fun createFileName(filePrefix: String): String {
        return "$rosenname ${LocalDate.now()} $filePrefix"
    }

    private fun processExport( title:String, editText: EditText, fileType: String, intentType: String, filePrefix: String, reverseNumber: Boolean) {
        showDialogInputZumenTitles(title) {
            launchIntentToSaveDXF(editText, fileType, intentType, filePrefix, reverseNumber)
        }
    }

    private fun strDateRosenname(fileprefix: String): String {
        return LocalDate.now().monthValue.toString() + "." + LocalDate.now().dayOfMonth.toString() + " " + rosenname + fileprefix
    }

    private fun launchIntentToSaveDXF(
        editText5: EditText,
        filetype: String,
        intentType: String,
        fileprefix: String,
        isNumReverse: Boolean = false
    ) {
        drawingStartNumber = editText5.text.toString().toInt()
        isNumberReverse = isNumReverse

        fileType = filetype
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.type = intentType
        i.apply{
            addCategory( Intent.CATEGORY_OPENABLE )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        i.putExtra(
            Intent.EXTRA_TITLE,
            strDateRosenname(fileprefix)
        )


        //i.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION// or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED//flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        saveContent.launch( Pair( intentType, strDateRosenname(fileprefix) ) )
    }


    private fun viewPdf(contentUri: Uri){
        savePDFinPrivate()

        if ( contentUri != Uri.EMPTY ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION// or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED//flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            //intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/pdf")
            //intent.setPackage("com.adobe.reader")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.d("viewPDF", "PDF viewer is not installed." )
                Toast.makeText(this, "PDF viewer is not installed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewXlsx(contentUri: Uri){
        // ファイルを生成。
        saveXlsxInPrivate()
        setTitles()

        if ( contentUri != Uri.EMPTY ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            //intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.d("viewXlsx", ".xlsx viewer is not installed." )
                Toast.makeText(this, ".xlsx viewer is not installed.", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun deletePrivateFile(fileName: String) {

        try {
            // 内部ストレージのアプリプライベート領域(filesDir)からファイルを取得
            val file = File(filesDir, fileName)

            // ファイルが存在する場合は削除
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("deletePrivateFile", "$fileName was successfully deleted.")
                } else {
                    Log.d("deletePrivateFile", "Failed to delete $fileName.")
                }
            } else {
                Log.d("deletePrivateFile", "$fileName does not exist.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deletePrivateFiles(fileNames: List<String>) {
        fileNames.forEach { fileName ->
            deletePrivateFile(fileName)
        }
    }


    private fun saveFileAndGetUri(filename: String): Uri {
        // ファイルをプライベート領域に保存
        saveToPrivate(filename)

        // プライベート領域からファイルのContent URIを取得
        return getAppLocalFile(this, filename)
    }

    private fun sendMail(){
        Log.d("SendMail", "begin" )

        showDialogInputZumenTitles("Send Mail", {
            showFileTypeSelectionDialog( {
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.putExtra(Intent.EXTRA_STREAM, makeShareUris(shareFiles) )
                Log.d("SendMail", "contentUrl add succeed." )
                intent.type = "*/*" // MIMEタイプを指定（汎用）

                //intent.type = "message/rfc822"
                intent.setPackage("com.google.android.gm")
                Log.d("SendMail", "intent setPackage succeed." )

                try {
                    sendMailLauncher.launch(intent) //startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Gmailアプリがインストールされていない場合の処理
                    Log.d("SendMail", "Gmail is not installed." )
                    Toast.makeText(this, "Gmail is not installed.", Toast.LENGTH_LONG).show()

                }

                Log.d("SendMail", "process done." )
            } )
        } )
        return
    }

    private fun getAppLocalFile(context: Context, filename: String) :Uri {
        val newFile = File(context.filesDir, filename)
        return if (newFile.exists())
            getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)
        else Uri.EMPTY
    }

    private fun showFileTypeSelectionDialog(onComplete: () -> Unit, checkedItems: BooleanArray = booleanArrayOf(true, true, true, false, false)) {
        val fileTypes = arrayOf(".csv", ".xlsx", ".dxf", ".sfc", ".pdf")
        val selectedFileTypes = ArrayList<String>()

        AlertDialog.Builder(this)
            .setTitle("Select File Types")
            .setMultiChoiceItems(fileTypes, checkedItems) { _, which, isChecked ->
                // チェック状態が変更されたらcheckedItemsを更新
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                // OKボタンが押された時、checkedItemsに基づいてselectedFileTypesを更新
                selectedFileTypes.clear()
                fileTypes.forEachIndexed { index, fileType ->
                    if (checkedItems[index]) {
                        selectedFileTypes.add(fileType)
                    }
                }
                // 選択されたファイルタイプに対する処理
                handleSelectedFileTypes(selectedFileTypes, onComplete)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleSelectedFileTypes(selectedFileTypes: List<String>, onComplete: () -> Unit ) {
        // 選択されたファイルタイプに対する処理をここに記述
        // 例: 選択されたファイルタイプをログに出力
        shareFiles.clear()
        var i = 0
        selectedFileTypes.forEach { fileType ->
            shareFiles.add( strDateRosenname(fileType) )
            Log.d("SelectedFileType", fileType)
            Log.d("SelectedFileType", shareFiles[i] )
            i++
        }


        // 必要に応じて他の処理を追加
        onComplete()
    }


    private fun sendFiles(){

        //先に路線名を決めてもらう（削除する為のファイル名が変わる）
        showDialogInputZumenTitles("Share Files", {
            showFileTypeSelectionDialog( { shareFiles(shareFiles) } )
        } )

    }

    private fun shareFiles(fileNames: List<String>) {
        Log.d("ShareFiles", "begin")

        val uris = makeShareUris(fileNames)
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*" // MIMEタイプを指定（汎用）
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) // 添付ファイルのURIリストを追加
        }

        // ユーザーにアプリを選択させる
        val chooser = Intent.createChooser(intent, "Choose an app to share")

        try {
            shareFilesLauncher.launch(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No suitable app installed.", Toast.LENGTH_LONG).show()
        }

        Log.d("ShareFiles", "process done.")
    }

    private fun makeShareUris(fileNames: List<String>): ArrayList<Uri> {
        // URIのリストを作成
        val uris = ArrayList<Uri>()

        shareUris.clear()
        // ファイル名のリストをループして各ファイルのURIを取得
        for (fileName in fileNames) {
            val uri = saveFileAndGetUri(fileName)
            shareUris.add(uri)
            uris.add(uri)
            Log.d("ShareFiles", "$fileName saved and URI obtained.")
        }
        return uris
    }

    //endregion

    //region File saving
    private fun saveDXF(bWriter: BufferedWriter) :BufferedWriter{

        val writer = DxfFileWriter(
                myTriangleList
        )
        writer.rStr_ = rStr
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr
        writer.textscale_ = my_view.textSize * 0.016f //25*0.014f=0.35f, 25/0.02f=0.5f

        writer.writer = bWriter
        writer.drawingLength = myTriangleList.measureMostLongLine()
        writer.dedlist_ = myDeductionList
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.isDebug = my_view.isDebug_

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = isNumberReverse

        writer.save()
        bWriter.close()

        return bWriter
    }

    private fun saveSFC(out: BufferedOutputStream) {

        val writer = SfcWriter(myTriangleList, myDeductionList, out, filename, drawingStartNumber)
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.rStr_ = rStr
        writer.textscale_ = my_view.textSize * 20f //25*14f=350f, 25/20f=500f
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = isNumberReverse

        writer.save()
        out.close()

    }

    private fun savePDF(out: OutputStream){
        val writer = PdfWriter(
                myTriangleList.getPrintScale(1f),
                myTriangleList
        )
        writer.out_ = out
        writer.deductionList_ = myDeductionList

        writer.textscale_ = my_view.textSize * 0.5f / writer.printScale_ //25*0.4f=10f, 25/0.3f=7.5f
        writer.initPaints()
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr
        writer.rStr_ = rStr
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)

        //my_view.isAreaOff_ = false
        writer.isRulerOff_ = true

        writer.startNewPage(
                writer.sizeX_.toInt(),
                writer.sizeY_.toInt(),
                writer.currentPageIndex_
        )
        writer.translateCenter()

        val viewPointer =
        my_view.drawPDF(
                writer,
                writer.currentCanvas_,
                writer.paintTri_,
                writer.paintTexS_,
                writer.paintRed_,
                writer.textscale_,//my_view.myTriangleList.getPrintTextScale(my_view.myScale, "pdf"),
                experience
        )

        // translate back by view pointer
        writer.translate(writer.currentCanvas_, -viewPointer.x, -viewPointer.y)
        writer.writeTitleFrame(writer.currentCanvas_)
        writer.closeCurrentPage()

        //writer.writeAllCalcSheets()

        writer.closeDocAndStream()
    }

    //endregion

    //region File Private Save and Load
    private fun createNew(){
        val tri = Triangle(5f, 5f, 5f,
            PointXY(0f, 0f), 0f)
        tri.autoSetDimAlign()
        val trilist = TriangleList(tri)
        myTriangleList = trilist
        myDeductionList.clear()

        // メニューバーのタイトル
        koujiname = ""
        rosenname = "新規路線"//rStr.eRName_
        gyousyaname =""
        findViewById<EditText>(R.id.rosenname).setText(rosenname)
        setTitles()

        my_view.setTriangleList(trilist, mScale)
        my_view.setDeductionList(myDeductionList, mScale)
        my_view.myTriangleList.lastTapNumber_ = my_view.myTriangleList.size()
        my_view.resetViewToLastTapTriangle()

        Log.d("FileLoader", "createNew: " + my_view.myTriangleList.size() )

        fab_fillcolor.backgroundTintList = getColorStateList(resColors[colorindex])

        printDebugConsole()
        editorResetBy(getList(deductionMode))

    }

    private fun savePDFinPrivate(filename: String = "privateTrilist.pdf"){
        try {
            savePDF(openFileOutput(filename, MODE_PRIVATE))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveXlsxInPrivate(filename: String = "privateTrilist.xlsx"){
        try {
            XlsxWriter().write( openFileOutput(filename, MODE_PRIVATE ), myTriangleList, myDeductionList, rosenname )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveToPrivate(filename: String) {
        // ファイル名から拡張子を取得
        val extension = filename.substringAfterLast('.', "")

        when (extension) {
            "xlsx" -> XlsxWriter().write( openFileOutput(filename, MODE_PRIVATE ), myTriangleList, myDeductionList, rosenname )
            "pdf" -> savePDFinPrivate(filename)
            "dxf" -> saveDxfToPrivate(filename)
            "sfc" -> saveSfcToPrivate(filename)
            "csv" -> saveCSVtoPrivate(filename)
            // その他のファイル形式に対応する処理を追加
            else -> throw IllegalArgumentException("Unsupported file format: $extension")
        }
    }

    //endregion

    //region File CSV Save and Load

    private fun saveCSV(writer: BufferedWriter): Boolean {
        return try {
            // 入力データの取得
            rosenname = findViewById<EditText>(R.id.rosenname).text.toString()

            // CSVファイルへのヘッダー情報の書き込み
            writer.apply {
                write("koujiname, $koujiname")
                newLine()
                write("rosenname, $rosenname")
                newLine()
                write("gyousyaname, $gyousyaname")
                newLine()
                write("zumennum, $zumennum")
                newLine()
            }

            // 三角形リストのデータをCSVファイルに書き込み
            for (index in 1..myTriangleList.size()) {
                val mt: Triangle = myTriangleList.getMemberByIndex(index)
                val pt: PointXY = mt.pointNumber_
                val cp = parentBCtoCParam(mt.parentBC, mt.lengthNotSized[0], mt.cParam_)

                writer.write("${mt.getMyNumber_()},${mt.getLengthA()},${mt.getLengthB()},${mt.getLengthC()},${mt.parentNumber},${mt.parentBC},${mt.getMyName_()},${pt.x},${pt.y},${mt.isPointNumberMovedByUser_},${mt.color_},${mt.dimSideAlignA_},${mt.dimSideAlignB_},${mt.dimSideAlignC_},${mt.myDimAlignA_},${mt.myDimAlignB_},${mt.myDimAlignC_},${cp.side},${cp.type},${cp.lcr},${mt.isChangeDimAlignB_},${mt.isChangeDimAlignC_},${mt.angleInGlobal_},${mt.pointCA_.x},${mt.pointCA_.y},${mt.angleInLocal_}")
                writer.newLine()
            }

            // その他の情報をCSVファイルに書き込み
            writer.apply {
                write("ListAngle, ${myTriangleList.angle}")
                newLine()
                write("ListScale, ${myTriangleList.scale}")
                newLine()
                write("TextSize, ${my_view.textSize}")
                newLine()
            }

            // 減算リストのデータをCSVファイルに書き込み
            for (index in 1..myDeductionList.size()) {
                val dd: Deduction = myDeductionList.get(index)
                val pointAtRealscale = dd.point.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
                val pointFlagAtRealscale = dd.pointFlag.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)

                writer.write("Deduction,${dd.num},${dd.name},${dd.lengthX},${dd.lengthY},${dd.parentNum},${dd.type},${dd.angle},${pointAtRealscale.x},${pointAtRealscale.y},${pointFlagAtRealscale.x},${pointFlagAtRealscale.y},${dd.shapeAngle}")
                writer.newLine()
            }

            // すべての書き込みが成功したらtrueを返す
            true
        } catch (e: Exception) {
            // 例外が発生した場合はエラーをログに記録し、falseを返す
            e.printStackTrace()
            false
        } finally {
            // BufferedWriterを安全にクローズ
            try {
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    val PrivateCSVFileName = "privateTrilist.csv"

    private fun saveCSVtoPrivate(filename: String = PrivateCSVFileName): Boolean{
        if( isCSVsavedToPrivate ) return true //既に保存済み
        try {
            setTitles()

            val writer = BufferedWriter(
                OutputStreamWriter(openFileOutput(filename, MODE_PRIVATE), "Shift-JIS"))

            saveCSV(writer)
            // 結果をログに出力
            logFilePreview(PrivateCSVFileName, "saveCSVtoPrivate")


            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        // 広告の再表示
        //if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = VISIBLE

    }


    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            /**
             * It's possible to limit the types of files by mime-type. Since this
             * app displays pages from a PDF file, we'll specify `application/pdf`
             * in `type`.
             * See [Intent.setType] for more details.
             */
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "text/*"

            /**
             * Because we'll want to use [ContentResolver.openFileDescriptor] to read
             * the data of whatever file is picked, we set [Intent.CATEGORY_OPENABLE]
             * to ensure this will succeed.
             */
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        loadContent.launch( intent )
    }

    fun openFileAsBufferedReader( fileName: String, encoding: String = "Shift-JIS", context: Context = this ): BufferedReader? {
        return try {
            val inputStream = context.openFileInput(fileName)  // 'context'は現在のContextオブジェクト
            BufferedReader(InputStreamReader(inputStream, encoding))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun parseCSV(reader: BufferedReader) :Boolean{
//        myDeductionMode = true
//        setDeductionMode(myDeductionMode)

        val str: StringBuilder = StringBuilder()
        var line: String? = reader.readLine()
        if(line == null) return false
        var chunks: List<String?> = line.split(",").map { it.trim() }

        if(chunks[0]!! != "koujiname"){
            Toast.makeText(this, "It's not supported file.", Toast.LENGTH_LONG).show()
            return false
        }

        if(chunks[0]!! == "koujiname") {
            koujiname= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "rosenname") {
            rosenname = chunks[1]!!.toString()
            findViewById<EditText>(R.id.rosenname).setText(rosenname)
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "gyousyaname") {
            gyousyaname= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "zumennum") {
            zumennum= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }

        val trilist = TriangleList()

        val pointfirst = PointXY(0f, 0f)
        val anglefirst = 180f
        //if( chunks.size > 22 ) {
        //if( chunks[22]!!.toFloat() != 180f ){
        //pointfirst = PointXY( -chunks[23]!!.toFloat(), -chunks[24]!!.toFloat() )
        //anglefirst = chunks[22]!!.toFloat() - 180f
        //}
        //}

        trilist.add(
            Triangle(
                chunks.getOrNull(1)?.toFloatOrNull() ?: 0f,
                chunks.getOrNull(2)?.toFloatOrNull() ?: 0f,
                chunks.getOrNull(3)?.toFloatOrNull() ?: 0f,
                pointfirst,
                anglefirst
            ),
            true
        )
        /*
        trilist.add(
            Triangle(
                    chunks[1]!!.toFloat(),
                    chunks[2]!!.toFloat(),
                    chunks[3]!!.toFloat(),
                    pointfirst,
                    anglefirst
            ),
            true
        )*/

        val mt = trilist.getMemberByIndex(trilist.size())


        mt.setMyName_(chunks[6]!!.toString())
        //pointNumber
        if(chunks[9]!! == "true") mt.setPointNumberMovedByUser_(
            PointXY(
                chunks[7]!!.toFloat(),
                chunks[8]!!.toFloat()
            )
        )
        // 色
        if( chunks.size > 10 ) mt.setColor(chunks[10]!!.toInt())

        // dimaligns
        if( chunks.size > 11 ) {
            mt.setDimAligns(
                chunks[11]!!.toInt(), chunks[12]!!.toInt(), chunks[13]!!.toInt(),
                chunks[14]!!.toInt(), chunks[15]!!.toInt(), chunks[16]!!.toInt()
            )
        }
        if( chunks.size > 20 ) {
            mt.isChangeDimAlignB_ = chunks[20]!!.toBoolean()
            mt.isChangeDimAlignC_ = chunks[21]!!.toBoolean()
        }

        if( chunks.size > 17 ) {
            mt.cParam_ = ConnParam(
                chunks[17]!!.toInt(),
                chunks[18]!!.toInt(),
                chunks[19]!!.toInt(),
                chunks[1]!!.toFloat()
            )
        }


        val dedlist = DeductionList()

        while (line != null){
            line = reader.readLine()
            if(line == null) break
            chunks = line.split(",").map { it.trim() }
            if(chunks[0] == "ListAngle") {
                trilist.angle = chunks[1].toFloat()
                continue
            }
            if(chunks[0] == "ListScale") {
                trilist.setScale(PointXY(0f, 0f), chunks[1].toFloat())
                continue
            }
            if(chunks[0] == "TextSize") {
                my_view.setAllTextSize(chunks[1].toFloat())
                continue
            }

            if(chunks[0] == "Deduction"){
//                dedlist.add(Params(chunks[2]!!.toString(),chunks[6]!!.toString(), chunks[1]!!.toInt(),
                //                  chunks[3]!!.toFloat(),chunks[4]!!.toFloat(),0f,
                //                chunks[5]!!.toInt(),typeToInt(chunks[6]!!.toString()),
                //              PointXY(chunks[8]!!.toFloat(),-chunks[9]!!.toFloat()).scale(mScale),
                //            PointXY(chunks[10]!!.toFloat(),-chunks[11]!!.toFloat()).scale(mScale)))
                dedlist.add(
                    Deduction(
                        Params(
                            chunks[2], chunks[6], chunks[1].toInt(),
                            chunks[3].toFloat(), chunks[4].toFloat(), 0f,
                            chunks[5].toInt(), typeToInt(chunks[6]),
                            PointXY(
                                chunks[8].toFloat(),
                                -chunks[9].toFloat()
                            ).scale(mScale),
                            PointXY(
                                chunks[10].toFloat(),
                                -chunks[11].toFloat()
                            ).scale(mScale)
                        )
                    )
                )
                if(chunks[12].isNotEmpty()) dedlist.get(dedlist.size()).shapeAngle = chunks[12].toFloat()
                continue
            }
            //Connection Params
            if( chunks.size > 17 ) {

                if( chunks[5].toInt() == 0 ){
                    val pt = PointXY(0f, 0f)
                    var angle = 0f

                    if( chunks.size > 22 ){
                        pt.set( -chunks[23].toFloat(), -chunks[24].toFloat() )
                        angle = chunks[22].toFloat()
                    }

                    trilist.add(
                        Triangle(
                            chunks[1].toFloat(),
                            chunks[2].toFloat(),
                            chunks[3].toFloat(),
                            pt,
                            angle - 180f
                        ),
                        true
                    )
                }
                else {

                    val ptri = trilist.getMemberByIndex(chunks[4].toInt())
                    val cp = ConnParam(
                        chunks[17].toInt(),
                        chunks[18].toInt(),
                        chunks[19].toInt(),
                        chunks[1].toFloat()
                    )
                    trilist.add(
                        Triangle(
                            ptri, cp,
                            chunks[2].toFloat(),
                            chunks[3].toFloat()
                        ),
                        true
                    )

                }
                trilist.getMemberByIndex(trilist.size()).parentBC_ = chunks[5].toInt()
            }
            else{

                val cp = parentBCtoCParam(
                    chunks[5].toInt(), chunks[1].toFloat(), ConnParam(
                        0,
                        0,
                        0,
                        0f
                    )
                )

                trilist.add(
                    Triangle(
                        trilist.getMemberByIndex(chunks[4].toInt()), ConnParam(
                            cp.side,
                            cp.type,
                            cp.lcr,
                            cp.lenA
                        ),
                        chunks[2].toFloat(),
                        chunks[3].toFloat()
                    ),
                    true
                )

                val mT = trilist.getMemberByIndex(trilist.size())
                mT.parentBC_ = chunks[5].toInt()
                /*
                if( chunks.size > 25 && mT.parentBC_ >= 9 ) mT.rotate(
                    mT.pointCA_,
                    chunks[25].toFloat(),
                    false
                )*/
                // trilist.getTriangle(trilist.size()).setCParamFromParentBC( chunks[5]!!.toInt() )
            }

            val mT = trilist.getMemberByIndex(trilist.size())
            mT.setMyName_(chunks[6])
            if( trilist.size() > 1 ) trilist.get(trilist.size() - 1).childSide_ = chunks[5].toInt()

            // 番号円　pointNumber
            if(chunks[9] == "true") mT.setPointNumberMovedByUser_(
                PointXY(
                    chunks[7].toFloat(),
                    chunks[8].toFloat()
                )
            )

            // 色
            if( chunks.size > 10 ) mT.setColor(chunks[10].toInt())

            // dimaligns
            if( chunks.size > 11 ) {
                mT.setDimAligns(
                    chunks[11].toInt(), chunks[12].toInt(), chunks[13].toInt(),
                    chunks[14].toInt(), chunks[15].toInt(), chunks[16].toInt()
                )
            }

            if( chunks.size > 20 ) {
                mT.isChangeDimAlignB_ = chunks[20].toBoolean()
                mT.isChangeDimAlignC_ = chunks[21].toBoolean()
            }



            str.append(line)
            str.append(System.getProperty("line.separator"))
        }
        //dedlist.scale(PointXY(0f,0f),3f,3f)
        myTriangleList = trilist
        myDeductionList = dedlist
        turnToBlankTrilistUndo()
        //trilist.scale(PointXY(0f,0f), 5f)
        //if( anglefirst != 180f )
        trilist.recoverState(PointXY(0f, 0f))
        //trilist.setChildsToAllParents()
//        myDeductionList.scale(PointXY(0f,0f), 1f, 1f)
        my_view.setDeductionList(dedlist, mScale)
        my_view.setTriangleList(trilist, mScale)
        my_view.resetViewToLastTapTriangle()

        Log.d( "FileLoader", "my_view.setTriangleList: " + my_view.myTriangleList.size() )
        // メニューバーのタイトル
        //setTitles()

        deductionMode = true
        editorResetBy(myTriangleList)
        //colorMovementFabs()
        flipDeductionMode()
        //printDebugConsole()
        return true
    }

    private fun resumeCSV() {
        StringBuilder()
        try {
            val reader = openFileAsBufferedReader(PrivateCSVFileName)

            val parseResult = reader?.let { parseCSV(it) }  // parseCSVの結果を一時変数に格納
            if (parseResult == false) createNew()  // 結果がfalseの場合はcreateNewを呼び出す

            // 結果をログに出力
            logFilePreview(PrivateCSVFileName, "resumeCSV")

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun Context.logFilePreview(filePath: String, callerMethodName: String = "UnknownCaller") {
        val file = File(this.filesDir, filePath)
        if (!file.exists() || !BuildConfig.DEBUG ) {
            Log.d("FileLoader", "$callerMethodName: File not found: $filePath")
            return
        }

        file.bufferedReader( charset("Shift_JIS") ).useLines { lines ->
            val logContent = lines.take(10).joinToString(separator = "\n")
            Log.d("FileLoader", "$callerMethodName: Preview of $filePath:\n$logContent")
        }
    }

    //endregion

    // region logs
    private fun logListCurrent(tag: String="ui", callerName: String = "unknown"){
        Log.d(tag,callerName+" my_view.trilist.current:"+my_view.myTriangleList.selectedNumber)
        Log.d(tag,callerName+" my_view.trilist.lastTapNumber:"+my_view.myTriangleList.lastTapNumber_)
        Log.d(tag,callerName+" mainActivity.trilist.current:"+myTriangleList.selectedNumber)
        Log.d(tag,callerName+" mainActivity.trilist.lastTapNumber:"+myTriangleList.lastTapNumber_)
        Log.d(tag,callerName+" mainActivity.dedlist.current:"+myDeductionList.current)
        Log.d(tag,callerName+" mainActivity.dedlist.lastTapIndex_:"+myDeductionList.lastTapIndex_)
    }

    private fun logFabController(tag: String = "ui fab"){
        Log.d(tag,"mainActivity ${myDeductionList.get(myDeductionList.size())}")
        Log.d(tag,"mainActivity ${my_view.myTriangleList.get(myTriangleList.size())}")
    }

    //endregion

} // end of class
