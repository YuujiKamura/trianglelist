package com.jpaver.trianglelist

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputFilter
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
import com.jpaver.trianglelist.util.AdInitializerFactory
import com.jpaver.trianglelist.util.AdManager
import com.jpaver.trianglelist.util.EditTextViewLine
import com.jpaver.trianglelist.util.EditorTable
import com.jpaver.trianglelist.util.InputParameter
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


data class ZumenInfo(
    // 文字列リソースをstringに変換して保持する。
    // 改めてみると、どれがどこで使われているのか不明、なんでtitleふたつあんの
    var zumentitle: String = "",
    var rosenname: String = "",
    var koujiname: String = "",
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

class MainActivity : AppCompatActivity(),
        MyDialogFragment.NoticeDialogListener {

    //region viewbinding init
    private lateinit var prefSetting: SharedPreferences

    private lateinit var bindingMain: ActivityMainBinding

    private lateinit var myview: MyView

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
    lateinit var fab_pdfview: FloatingActionButton
    lateinit var fab_dxfview: FloatingActionButton
    lateinit var fab_share: FloatingActionButton
    lateinit var fab_mail: FloatingActionButton
    lateinit var fab_numreverse: FloatingActionButton
    lateinit var fab_xlsx: FloatingActionButton

    lateinit var ela1 : EditText
    lateinit var editorline1_lengthB : EditText
    lateinit var elc1 : EditText
    lateinit var ela2 : EditText
    lateinit var editorline2_lengthB : EditText
    lateinit var editorline2_lengthC : EditText
    lateinit var elsa1 : String
    lateinit var elsb1 : String
    lateinit var elsc1 : String
    lateinit var elsa2 : String
    lateinit var elsb2 : String
    lateinit var elsc2 : String
//endregion


//region Parameters
    val PRIVATE_FILENAME_DXF = "privateTrilist.dxf"

    var isViewAttached = false

    var isCSVsavedToPrivate = false

    private lateinit var editorline1: EditTextViewLine
    private lateinit var editorLine2: EditTextViewLine
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
    private var parameter: InputParameter = InputParameter("", "", 0, 0f, 0f, 0f, 0, 0,
        PointXY(0f, 0f)
    )
    private var lastParams: InputParameter = parameter

    // タイトルパラメータ、stringリソースから構成する

    private lateinit var rStr : ZumenInfo
    private lateinit var titleTri: TitleParams//
    private lateinit var titleDed: TitleParams
    private lateinit var titleTriStr : TitleParamStr
    private lateinit var titleDedStr : TitleParamStr

    private lateinit var trianglelist: TriangleList //= TriangleList(Triangle(0f,0f,0f,PointXY(0f,0f),180f))
    private lateinit var myDeductionList: DeductionList

    private var trilistUndo: TriangleList = TriangleList()


    var deductionMode: Boolean = false
    private var mIsCreateNew: Boolean = false
    private val onetohandred = 11.9f
    private val experience = 4f
    private val viewscale = onetohandred*experience

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

    var shareFiles: MutableList<String> = mutableListOf(strDateRosenname(".csv"), strDateRosenname(".xlsx"), strDateRosenname(".dxf"))
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
                // Check the file extension
                val fileName = getFileName(uri)
                if (!fileName.endsWith(".csv", ignoreCase = true)) {
                    Toast.makeText(this, "Selected file is not a CSV file.", Toast.LENGTH_LONG).show()
                    return  // Stop further processing
                }

                try {
                    loadFileWithEncoding(uri) { reader ->
                        // Use reader to perform file reading/parsing
                        parseCSV(reader)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                setTitles()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return if (uri.scheme == "content") {
            getFileNameFromContentUri(uri)
        } else {
            getFileNameFromFilePath(uri)
        }
    }

    private fun getFileNameFromContentUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return ""
    }

    private fun getFileNameFromFilePath(uri: Uri): String {
        return uri.path?.substringAfterLast('/') ?: ""
    }

    private fun loadFileWithEncoding(uri: Uri, encoding: String = "Shift-JIS", onFileLoaded: (BufferedReader) -> Unit) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, encoding))
                onFileLoaded(reader)  // Execute the passed function with the loaded file
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Error handling
        }
    }

    // endregion

//region adMob loading
    private lateinit var mAdView : AdView
    val adManager = AdManager()
    private var mInterstitialAd: InterstitialAd? = null
    val adInitializer: AdInitializer = AdInitializerFactory.create()
    val USEADMOB = false
    private fun adMobInit() {

        adInitializer.initialize()

        if ( !USEADMOB ){
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

        if ( !USEADMOB ){
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

        if ( !USEADMOB ){
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

        initFabs()
        fabController()

    }
    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        //bMyView = FragmentFirstBinding.bind( findViewById(R.id.my_view) )//inflate(layoutInflater)
        myview = findViewById(R.id.my_view)//bMyView.myView
        Log.d("myView", "Instance check in MainActivity: " + myview )
        Log.d("MainActivityLifeCyce", "onAttachedToWindow")

        rStr = ZumenInfo(
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

        val filepath = this.filesDir.absolutePath + "/" + PrivateCSVFileName
        val file = File(filepath)
        if(file.exists()) {
            resumeCSV()

        }
        else{
            createNew()
            showToast("The PrivateCSV file does not exist.")
        }

        loadEditTable()
        colorMovementFabs()
        //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
        fab_replace.backgroundTintList = getColorStateList(R.color.colorLime)
        setEditNameAdapter(sNumberList)

        checkPermission()

        ela1 = findViewById<EditText>(R.id.editLengthA1)
        editorline1_lengthB = findViewById<EditText>(R.id.editLengthB1)
        elc1 = findViewById<EditText>(R.id.editLengthC1)
        ela2 = findViewById<EditText>(R.id.editLengthA2)
        editorline2_lengthB = findViewById<EditText>(R.id.editLengthB2)
        editorline2_lengthC = findViewById<EditText>(R.id.editLengthC2)

        // EditTextの入力値の変化を追跡するリスナーを登録
        editorline1_lengthB.addTextChangedListener(object : CustomTextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                myview.watchedB1_ = p0.toString()
                myview.invalidate()
            }
            override fun afterTextChanged(p0: Editable?) {
                myview.watchedB1_ = p0.toString()
                myview.invalidate()
            }
        })

        elc1.addTextChangedListener(object : CustomTextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                myview.watchedC1_ = p0.toString()
                myview.invalidate()
            }
            override fun afterTextChanged(p0: Editable?) {
                myview.watchedC1_ = p0.toString()
                myview.invalidate()
            }
        })

        ela2.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                myview.watchedA2_ = p0.toString()
                myview.invalidate()
            }
        })


        editorline2_lengthB.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                myview.watchedB2_ = p0.toString()
                myview.invalidate()
            }
        })

        editorline2_lengthC.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                myview.watchedC2_ = p0.toString()
                myview.invalidate()
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
    fun launchViewIntent(file: Uri) {
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
                launchViewIntent(Uri.parse(url))
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
                    if(input == "notuse"){
                        onComplete()
                        return@showInputDialog
                    }
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
                onInputReceived("notuse")
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
        textView.addTextChangedListener(DeductionNameTextWatcher(editorline1, lastParams))

    }
    private fun setTitles(){
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()
        //findViewById<EditText>(R.id.rosenname).setText(rosenname)

        val dedArea = myDeductionList.getArea()
        val triArea = trianglelist.getArea()
        val totalArea = roundByUnderTwo(triArea - dedArea)//.formattedString(2)
        title = rStr.menseki_ + ": $totalArea m^2"

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
        trianglelist.changeSelectedNumber(trianglelist.size())
        //printDebugConsole()
        colorMovementFabs()

        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        if(!deductionMode) {
            deductionMode = true
            myview.deductionMode = true
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
            myview.deductionMode = false
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
        myEditor.lineRewrite(
                InputParameter(
                        "",
                        "",
                        elist.size() + 1,
                        0f,
                        0f,
                        0f,
                        elist.size(),
                        0,
                    PointXY(0f, 0f)
                ), editorline1
        )
        myEditor.lineRewrite(eo.getParams(), editorLine2)
        if(currentNum > 1) myEditor.lineRewrite(eob.getParams(), myELThird)
        if(currentNum == 1) myEditor.lineRewrite(
                InputParameter(
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

    fun isValid(dp: InputParameter) : Boolean{
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

        if ( dp.pn > trianglelist.size() || ( dp.pn < 1 && dp.number != 1 )) {
            Toast.makeText(this, "Invalid!! : number of parent", Toast.LENGTH_LONG).show()
            return false
        }
        if (  dp.pl < 1 && dp.number != 1  ) {
            Toast.makeText(this, "Invalid!! : connection in parent", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun loadEditTable(){
        editorline1 =
            EditTextViewLine(
                    findViewById(R.id.editNumber1),
                    findViewById(R.id.editName1),
                    findViewById(R.id.editLengthA1),
                    findViewById(R.id.editLengthB1),
                    findViewById(R.id.editLengthC1),
                    findViewById(R.id.editParentNumber1),
                    findViewById(R.id.editParentConnect1)
            )

        editorLine2 =
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

    private fun validDeduction(dp: InputParameter): Boolean {
        return isValidName(dp.name) && isValidDimensions(dp)
    }

    private fun isValidName(name: String): Boolean {
        return name.isNotEmpty()
    }

    private fun isValidDimensions(dp: InputParameter): Boolean {
        if (dp.a < 0.1f) return false
        if (dp.type == "Box" && dp.b < 0.1f) return false
        return true
    }

    private fun getList(dMode: Boolean) : EditList {
        return if(dMode) myDeductionList
        else trianglelist
    }

    fun toString( editText: EditText ) : String{
        return editText.text.toString()
    }

    private fun updateElStrings(){
        elsa1 = toString( ela1 )
        elsb1 = toString( editorline1_lengthB )
        elsc1 = toString( elc1 )
        elsa2 = toString( ela2 )
        elsb2 = toString( editorline2_lengthB )
        elsc2 = toString( editorline2_lengthC )
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
        fab_pdfview =   bindingMain.fabPdf
        fab_dxfview =   bindingMain.fabDxfview
        fab_share =     bindingMain.fabShare
        fab_mail =      bindingMain.fabMail
        fab_numreverse = bindingMain.fabNumreverse
        fab_xlsx = bindingMain.fabXlsx
    }

    private fun autosave() {
        isCSVsavedToPrivate = false
        isCSVsavedToPrivate = saveCSVtoPrivate()
    }

    val BLINKSECOND = 1

    private fun setCommonFabListener(fab: FloatingActionButton, isSaveCSV:Boolean=true, action: () -> Unit) {
        fab.setOnClickListener {
            blinkFAB(fab,BLINKSECOND)
            action()
            if(!isSaveCSV) return@setOnClickListener
            autosave()
        }
    }

    fun blinkFAB(fab: FloatingActionButton, totalDurationSeconds: Int) {
        val singleAnimationDuration = 500 // 1回のアニメーション持続時間（ミリ秒）

        // 全体の持続時間をミリ秒に変換し、1回のアニメーション持続時間で割ることで繰り返し回数を計算
        val repeatCount = (totalDurationSeconds * 1000 / singleAnimationDuration) - 1

        val animator = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0.1f, 1f).apply {
            duration = singleAnimationDuration.toLong()
            this.repeatCount = repeatCount
            repeatMode = ValueAnimator.RESTART
        }
        animator.start()
    }


    private fun fabController(){

        val mainViewModel = MainViewModel()

        setCommonFabListener(fab_replace) {
            fabReplace(parameter, false)
        }

        setCommonFabListener(fab_flag) {
            fabFlag()
        }

        setCommonFabListener(fab_dimsidew) {
            mainViewModel.setMember( deductionMode, trianglelist, myDeductionList )
            mainViewModel.fabDimArrange("W", { setListAndResetView( { myview.invalidate() }, false ) } )
        }

        setCommonFabListener(fab_dimsideh) {
            mainViewModel.setMember( deductionMode, trianglelist, myDeductionList )
            mainViewModel.fabDimArrange("H", { setListAndResetView( { myview.invalidate() }, false ) }  )
        }

        setCommonFabListener(fab_nijyuualign) {
            if(!deductionMode && trianglelist.lastTapNumber > 1 ){
                trianglelist.rotateCurrentTriLCR()
                myview.setTriangleList(trianglelist, viewscale, false )
                myview.resetView(myview.toLastTapTriangle())
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
                    trilistUndo = trianglelist.clone()

                    var eraseNum = listLength
                    if(!deductionMode) eraseNum = trianglelist.lastTapNumber

                    getList(deductionMode).remove(eraseNum)

                    myview.setDeductionList(myDeductionList, viewscale)
                    myview.setTriangleList(trianglelist, viewscale)

                    editorResetBy(getList(deductionMode))
                }
                deleteWarning = 0
                bindingMain.fabMinus.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            printDebugConsole()
            colorMovementFabs()
            myview.resetViewToLastTapTriangle()
            setTitles()
        }

        setCommonFabListener(fab_undo){
            if( trilistUndo.size() > 0 ){
                trianglelist = trilistUndo.clone()
                //my_view.undo()
                myview.setTriangleList(trilistUndo, viewscale)
                myview.resetViewToLastTapTriangle()

                trilistUndo.trilist.clear()

                bindingMain.fabUndo.backgroundTintList = getColorStateList(R.color.colorPrimary)
                editorResetBy(getList(deductionMode))
                setTitles()
            }
        }

        setCommonFabListener(fab_fillcolor) {
            if(!deductionMode){
                trianglelist.get(myview.trianglelist.selectedNumber)

                colorindex ++
                if(colorindex == resColors.size) colorindex = 0
                bindingMain.fabFillcolor.backgroundTintList = getColorStateList(resColors[colorindex])

                trianglelist.get(myview.trianglelist.selectedNumber).mycolor = colorindex

                myview.setFillColor(colorindex, trianglelist.selectedNumber)
            }
        }

        setCommonFabListener(fab_texplus) {
            myview.textSize += 5f
            myview.setAllTextSize(myview.textSize)
            myview.invalidate()
        }

        setCommonFabListener(fab_texminus) {
            myview.textSize -= 5f
            myview.setAllTextSize(myview.textSize)

            myview.invalidate()
        }

        setCommonFabListener(fab_rot_l) {
            fabRotate(5f, true )
        }

        setCommonFabListener(fab_rot_r) {
            fabRotate(-5f, true )
        }

        setCommonFabListener(fab_numreverse){
            trilistUndo = trianglelist.clone()
            mainViewModel.setMember( deductionMode, trianglelist, myDeductionList )

            whenTriDed({
                if( BuildConfig.DEBUG ){
                    trianglelist = mainViewModel.fabReverse() as TriangleList
                    setListAndResetView( { myview.toLastTapTriangle() } )
                }
            }, {
                myDeductionList = mainViewModel.fabReverse() as DeductionList
                setListAndResetView( { myview.invalidate() } )
            })
        }

        //ここからはビュー操作用とファイルシェア用のFAB、図形を書き換えないのでオートセーブの対象外
        setCommonFabListener(fab_setB,false) {
            autoConnection(1)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        setCommonFabListener(fab_setC,false) {
            autoConnection(2)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        setCommonFabListener(fab_deduction,false) {
            deleteWarning = 0
            fab_minus.backgroundTintList = getColorStateList(R.color.colorAccent)
            flipDeductionMode()
            colorMovementFabs()
        }

        setCommonFabListener(fab_resetView,false) {
            try{
                if(!deductionMode) myview.resetViewToLastTapTriangle()
                else if( myDeductionList.size() > 0 )  myview.resetViewToCurrentDeduction()

            } catch ( e: NullPointerException ){
                Toast.makeText(this, "List is NUll.", Toast.LENGTH_LONG).show()
            }
        }

        setCommonFabListener(fab_up,false) {
            myEditor.scroll(-1, getList(deductionMode), editorLine2, myELThird)

            if(!deductionMode) moveTrilist()
            else if( myDeductionList.size() > 0 ){
                myview.myDeductionList.current = myDeductionList.current
                myview.resetViewToCurrentDeduction()
            }
            colorMovementFabs()
            printDebugConsole()
            setTitles()
        }

        setCommonFabListener(fab_down,false) {
            myEditor.scroll(1, getList(deductionMode), editorLine2, myELThird)

            if(!deductionMode) moveTrilist()
            else if( myDeductionList.size() > 0 ){
                myview.myDeductionList.current = myDeductionList.current
                myview.resetViewToCurrentDeduction()
            }
            colorMovementFabs()
            printDebugConsole()
            setTitles()
        }

        setCommonFabListener(fab_debug,false) {
            if(!myview.isAreaOff_){
                myview.isAreaOff_ = true
                myview.isDebug_ = false
                fab_debug.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            else{
                myview.isAreaOff_ = false
                myview.isDebug_ = true
                fab_debug.backgroundTintList = getColorStateList(R.color.colorLime)
            }

            myview.invalidate()
        }

        setCommonFabListener(fab_testbasic,false) {
            findViewById<TextView>(R.id.editLengthB1).text = "" // reset
            fabReplace(InputParameter("", "", 1, 7f, 7f, 7f, 0, 0), true)
            findViewById<TextView>(R.id.editLengthB1).text = 0.6f.toString()//"6f" // add
            fabReplace(InputParameter("", "", 2, 7f, 6f, 6f, 1, 2), true)

            findViewById<TextView>(R.id.editLengthA1).text = 0.23f.toString()//"0.23f" // add
            deductionMode = true
            fabReplace(
                InputParameter(
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

        setCommonFabListener(fab_pdfview,false) {
            viewPdf( getAppLocalFile(this, "privateTrilist.pdf") )
        }


        setCommonFabListener(fab_dxfview,false) {
            viewDxf( getAppLocalFile(this, PRIVATE_FILENAME_DXF) )
        }

        setCommonFabListener(fab_xlsx,false){
            viewXlsx( getAppLocalFile(this, "privateTrilist.xlsx") )
        }

        setCommonFabListener(fab_share,false) {
            sendFiles()
        }

        setCommonFabListener(fab_mail,false) {
            sendMail()
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
            myview.setTriangleList( trianglelist, viewscale, moveCenter )
        },{
            myview.setDeductionList(myDeductionList, viewscale)
        })
    }
    private fun setListAndResetView(resetViewMethod:() -> Unit, moveCenter: Boolean = true ) {
        editorResetBy(getList(deductionMode))
        setListByDedMode( moveCenter )
        resetViewMethod()
        printDebugConsole()
        //saveCSVtoPrivate()
    }

    fun setDeductionlist(){
        myview.setDeductionList(myDeductionList, viewscale)
    }

    fun setTrianglelist(){
        myview.setTriangleList(trianglelist, viewscale, false)
    }

    fun flagDeduction(){
        val deduction = myDeductionList.get(parameter.number)
        parameter.pointflag = myview.pressedInModel
        parameter.point = deduction.point
        if(validDeduction(parameter)) {// あまり遠い時はスルー
            myDeductionList.replace(parameter)
            setDeductionlist()
        }
    }

    fun flagTriangle(){
        val triangle = trianglelist.get(parameter.number)
        val tappoint = myview.pressedInModel.scale(PointXY(0f, 0f), 1 / viewscale, -1 / viewscale)
        triangle.setPointNumber( tappoint, true )
        setTrianglelist()
    }

    fun fabFlag(){
        parameter = myEditor.readLineTo(parameter, editorLine2)

        if(deductionMode) flagDeduction()
        else flagTriangle()

        myview.invalidate()
    }

    fun fabRotate(degrees: Float, bSeparateFreeMode: Boolean, isRotateDedBoxShape: Boolean = true ){
        if(!deductionMode) {
            trianglelist.rotate(PointXY(0f, 0f), degrees, trianglelist.lastTapNumber, bSeparateFreeMode )
            myDeductionList.rotate(PointXY(0f, 0f), -degrees )
            myview.setTriangleList(trianglelist, viewscale)
            myview.setDeductionList(myDeductionList, viewscale)
            myview.invalidate()//resetViewToLSTP()
        }
        // ded rotate
        else {
            if( !isRotateDedBoxShape ) return
            val current_deduction_number = myview.myDeductionList.lastTapIndex_+1
            val current_deduction = myDeductionList.get(current_deduction_number)
            current_deduction.rotateShape( current_deduction.point, -degrees )
            myview.setDeductionList(myDeductionList, viewscale)
            myview.invalidate()
        }
    }

    fun fabReplace(params: InputParameter = parameter, useit: Boolean = false ){
        //val editor = myEditor
        trilistSaving(trianglelist)
        val dedMode = deductionMode

        var readedFirst  = InputParameter()
        var readedSecond = InputParameter()
        myEditor.readLineTo(readedFirst, editorline1)
        myEditor.readLineTo(readedSecond, editorLine2)
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
                myview.myDeductionList[readedSecond.number].point
            } else {
                addDeductionBy(readedFirst)
                myview.myDeductionList[readedFirst.number].point
            }
            findViewById<EditText>(R.id.editName1).requestFocus()
        }

        myview.setTriangleList(trianglelist, viewscale)
        setListAndResetView( { whenTriDed( {myview.resetView(myview.toLastTapTriangle())}, {myview.invalidate()} ) } )
        setTitles()


        myview.trianglelist.isDoubleTap = false
        myview.trianglelist.lastTapSide = 0

        //logListCurrent()
        logFabController()

    }


    private fun moveTrilist(){
        myview.getTriangleList().changeSelectedNumber(trianglelist.retrieveCurrent())
        myview.trianglelist.lastTapNumber = trianglelist.retrieveCurrent()
        trianglelist.lastTapNumber = trianglelist.retrieveCurrent()
        myview.resetViewToLastTapTriangle()
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
    private fun logParams(params: InputParameter, tag: String = "ParamsLog") {
        val paramsContents = with(params) {
            """
        |name: $name
        |type: $type
        |n: $number
        |a: $a
        |b: $b
        |c: $c
        |pn: $pn
        |pl: $pl
        |pt: (${point.x}, ${point.y})
        |pts: (${pointflag.x}, ${pointflag.y})
        """.trimMargin()
        }
        Log.d(tag, paramsContents)
    }

    private fun addDeductionBy(params: InputParameter) : Boolean {
        if (!validDeduction(params)) {
            Log.d( "Deduction", "invalid parameters" )
            logParams(params, "add Dedution")
            return false
        }

        // 所属する三角形の判定処理
        val ded = flagDeduction(params)
        if( ded == null ) return false
        myDeductionList.add( ded.clone() )

        myview.setDeductionList(myDeductionList, viewscale)
        myview.trianglelist.dedmapping(myDeductionList, -1)
        lastParams = params

        logFabController()
        return true

    }


    private fun flagDeduction(params: InputParameter): Deduction?{//Params {
        params.point = myview.pressedInModel
        if (params.point == PointXY(0f, 0f)) return null

        //形状の自動判定
        if( params.b > 0f ) params.type = "Box"
        else params.type = "Circle"

        // 所属する三角形の判定処理
        params.pn = myview.trianglelist.isCollide(
            params.point.scale(
                PointXY(
                    1f,
                    -1f
                )
            )
        )

        val ded = Deduction(params)

        if (params.pn != 0) {
            Log.d(
                "Deduction",
                "ptri dedcount" + myview.trianglelist.get(params.pn).dedcount
            )

            val trilistinview = myview.trianglelist
            val parent = trilistinview.get(params.pn)
            Log.d("Deduction", "parent:" + parent.toString() )
            Log.d("Deduction", "params.point:  " + params.point.x + ", " + params.point.y)
            Log.d("Deduction", "params.pointF: " + params.pointflag.x + ", " + params.pointflag.y)

            ded.flag(parent)

            //ビュー空間からモデル空間にする際にY軸を反転する。そこからビュー空間に戻すためにさらにもう一度Y軸反転をかけている。
            //これいらないのでは・・Deductionの管理をビュー空間ベースからモデル空間にすれば
/*                params.ptF = parent.pointUnconnectedSide(
                params.pt.scale(1f,-1f),
                1f,
                -1f,
                PointXY(0f, 0f),
                0.9f
            )*/
        }

        return ded
        //return params
    }

    private fun resetDeductionsBy(params: InputParameter) : Boolean {
        if (!validDeduction(params)) {
            Log.d( "Deduction", "invalid parameters" )
            logParams(params, "reset Dedution")
            return false
        }

        //myTriangleList.current = params.pn

        val ded = flagDeduction(params)
        if( ded == null ) return false

        // 所属する三角形の判定処理
        myDeductionList.replace(params.number, ded )
        myview.trianglelist.dedmapping(myDeductionList, -1)
        return true
    }

    private fun setFabColor(fab: FloatingActionButton, colorIndex: Int ){
        fab.backgroundTintList = getColorStateList( colorIndex )
    }

    private fun trilistSaving( from: TriangleList ){
        trilistUndo = from.clone()
    }
    private fun createNewTriangle(params: InputParameter, parentTri: Triangle ): Triangle{
        val newTri = Triangle(
            parentTri,
            params
        )
        newTri.mynumber = params.number
        return newTri
    }

    private fun trilistAdd(params: InputParameter, triList: TriangleList ){
        val newTri = createNewTriangle( params, triList.getBy(params.pn) )
        triList.add(newTri, true)
        triList.lastTapNumber = triList.size()
    }
    private fun setUI(){
        setFabColor( fab_undo, R.color.colorLime )
        findViewById<EditText>(R.id.editLengthA1).requestFocus()
    }

    private fun addTriangleBy(params: InputParameter) : Boolean {
        if ( isValid( params ) ) {

            trilistSaving( trianglelist )
            trilistAdd( params, trianglelist )
            setUI()

            return true
        }
        return false
    }

    private fun resetTrianglesBy(params: InputParameter) : Boolean {

        return if (isValid(params)){
            trilistUndo = trianglelist.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            trianglelist.resetFromParam(params)
        } // if valid triangle
        else false
    }


//endregion


    //region TapEvent

    fun getTriangleParameter(sideindex: Int): InputParameter{
        val triangle: Triangle = trianglelist.get(parameter.pn)
        return InputParameter(
            parameter.name,
            "",
            trianglelist.size() + 1,
            triangle.getLengthByIndex(sideindex),
            parameter.b,
            parameter.c,
            triangle.mynumber,
            sideindex,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
    }

    fun isDoubleTap(): Boolean{
        return myview.trianglelist.isDoubleTap
    }

    fun connectTriangle(sideindex: Int ){
        var focusTo = editorline1_lengthB

        if( isDoubleTap() ){
            if(sideindex == 1) focusTo = editorline2_lengthB
            if(sideindex == 2) focusTo = editorline2_lengthC
        }

        myEditor.lineRewrite( getTriangleParameter(sideindex), editorline1 )

        if(myview.trianglelist.lastTapSide != -1){
            myview.trianglelist.isDoubleTap = true

            focusTo.requestFocus()
            focusTo.setSelection(focusTo.text.length)
            val inputMethodManager: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(focusTo, 0)


        }
        myview.resetViewToLastTapTriangle()
    }

    fun connectDeduction(sideindex: Int){
        setFabSetBC(sideindex)
        myEditor.lineRewrite( setupDedParameter(sideindex), editorline1 )
    }

    fun setupDedParameter(sideindex: Int): InputParameter{
        return InputParameter(
            parameter.name,
            "",
            myDeductionList.size() + 1,
            parameter.a,
            parameter.b,
            parameter.c,
            parameter.pn,
            sideindex,
            PointXY(
                0f,
                0f
            )
        )
    }

    fun setTriListNumber(){
        if( myview.trianglelist.lastTapNumber < 1 ) myview.trianglelist.lastTapNumber = myview.trianglelist.size()
    }

    private fun autoConnection(sideindex: Int){
        setTriListNumber()
        myview.trianglelist.lastTapSide = sideindex
        parameter = myEditor.readLineTo(parameter, editorline1) //keep them

        if(!deductionMode)
            connectTriangle(sideindex)
        else
            connectDeduction(sideindex)
    }

    private fun setFabSetBC(i: Int){
        if(i == 1) {
            fab_setB.backgroundTintList = getColorStateList(R.color.colorAccent)

        }
    }

    fun getDedTapIndex():Int {
        myview.myDeductionList.setScale(myview.myScale)
        return myview.myDeductionList.getTapIndex(myview.pressedInModel)
    }

    fun targetInDedmode(zoomsize: Float){
        val trilistV = myview.trianglelist

        if ( getDedTapIndex() > -1 ) {
            val tapNumber = myview.myDeductionList.lastTapIndex_+1

            myEditor.scroll(
                tapNumber - myDeductionList.current,
                getList(deductionMode), editorLine2, myELThird
            )
        }

        // 三角形番号が押されたときはセンタリング
        val RANGE = 0.4f / zoomsize
        trilistV.getTapNumber(getPressedInModel(), RANGE )
        if ( trilistV.lastTapNumber != 0 ){
            handleTriangleTap(trilistV, myEditor, trianglelist)
            if( trilistV.lastTapSide == 3 ) myview.resetViewToLastTapTriangle()
        }
    }

    fun getShadowTap(pressedpoint:PointXY, range: Float):Int { return myview.shadowTri_.getTapLength(pressedpoint, range ) }
    fun getPressedInModel():PointXY { return myview.pressedInModel.scale(PointXY(0f, 0f), 1f, -1f) }

    fun shadowTapMode(zoomsize: Float): Boolean {
        val RANGE = 0.8f / zoomsize
        val shadowTapside = getShadowTap(getPressedInModel(), RANGE)

        when (shadowTapside) {
            1 -> {
                findViewById<EditText>(R.id.editLengthB1).requestFocus() // EditTextB1にフォーカスを設定
                return true // trueを返して関数から抜ける
            }
            2 -> {
                findViewById<EditText>(R.id.editLengthC1).requestFocus() // EditTextC1にフォーカスを設定
                return true // trueを返して関数から抜ける
            }
        }

        return false // 何もアクションが起こらなかった場合はfalseを返す
    }

    fun getTriTapNumber(zoomsize: Float):Int {
        val TAPRANGE= myview.textSize / zoomsize * 0.02f
        return myview.trianglelist.getTapNumber( getPressedInModel(), TAPRANGE )
    }

    fun targetInTriMode(zoomsize: Float){
        if(shadowTapMode(zoomsize)) return

        val trilistV = myview.trianglelist

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        updateElStrings()
        myview.setWatchedStrings(elsa1,elsb1,elsc1,elsa2,elsb2,elsc2)

        // タップされた三角形がある場合の処理を行う
        if (getTriTapNumber(zoomsize) != 0) {
            handleTriangleTap(trilistV, myEditor, trianglelist, true ) // タップされた三角形の基本処理を行う
            setTriangleDetails(trianglelist) // タップされた三角形の詳細設定を行う
            setEditTextContent(trianglelist) // EditTextに三角形情報をセットする

            // タップされた辺に応じた処理を行う
            when (myview.trianglelist.lastTapSide) {
                0 -> handleSideZero(inputMethodManager) // 辺0がタップされたときの処理
                1, 2 -> autoConnection(myview.trianglelist.lastTapSide) // 辺1または2がタップされたときの自動接続処理
                3 -> myview.resetViewToLastTapTriangle() // 辺3がタップされたときのビューをリセットする処理
            }
        }
    }

    fun setTargetEditText(zoomsize: Float)
    {
        if(deductionMode)targetInDedmode(zoomsize)
        else targetInTriMode(zoomsize)

        Log.d("SetTarget", "Tap Triangle is : " + myview.trianglelist.lastTapNumber + ", side is :" + myview.trianglelist.lastTapSide )
        logListCurrent()
    }

    // タップされた三角形に関連する基本処理を行う関数
    fun handleTriangleTap(trilistV: TriangleList, myEditor: EditorTable, myTriangleList: TriangleList, isEditorScroll: Boolean = false ) {
        if( isEditorScroll ) myEditor.scroll(trilistV.lastTapNumber - trilistV.selectedNumber, myTriangleList, editorLine2, myELThird) // スクロールしてタップされた三角形を表示
        trilistV.selectedNumber = trilistV.lastTapNumber // 現在の三角形を更新
        myTriangleList.changeSelectedNumber(myview.trianglelist.lastTapNumber) // myTriangleListの現在の三角形を更新
        myTriangleList.lastTapNumber = myview.trianglelist.lastTapNumber // 最後にタップされた三角形の番号を更新
        myTriangleList.lastTapSide = myview.trianglelist.lastTapSide // 最後にタップされた三角形の辺を更新
    }

    // タップされた三角形の詳細設定を行う関数
    fun setTriangleDetails(myTriangleList: TriangleList) {
        colorindex = myTriangleList.get(myTriangleList.lastTapNumber).mycolor // タップされた三角形の色を取得
        colorMovementFabs() // 色の設定を更新
        printDebugConsole() // デバッグコンソールに情報を出力
        setTitles() // タイトルを設定
    }

    // EditTextに三角形情報をセットする関数
    fun setEditTextContent(myTriangleList: TriangleList) {
        findViewById<EditText>(R.id.editParentNumber1).setText(myTriangleList.lastTapNumber.toString()) // 最後にタップされた三角形の番号を設定
        findViewById<EditText>(R.id.editNumber1).setText(myTriangleList.size().toString()) // 三角形リストのサイズを設定
    }

    // 辺0がタップされたときの処理を行う関数
    fun handleSideZero(inputMethodManager: InputMethodManager) {
        val editLengthA2 = findViewById<EditText>(R.id.editLengthA2) // 辺Aの長さを編集するEditTextを取得
        editLengthA2.requestFocus() // フォーカスを設定
        editLengthA2.setSelection(editLengthA2.text.length) // EditTextのテキストを選択状態にする
        inputMethodManager.showSoftInput(editLengthA2, 0) // ソフトキーボードを表示
        myview.resetViewToLastTapTriangle()
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
                            xlsxWriter.write(outputStream, trianglelist, myDeductionList, rosenname)
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
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/pdf")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.d("viewPDF", "PDF viewer is not installed." )
                Toast.makeText(this, "PDF viewer is not installed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewDxf(contentUri: Uri){
        saveDxfToPrivate( PRIVATE_FILENAME_DXF )

        if ( contentUri != Uri.EMPTY ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/dxf")

            // JW_cad viewerのパッケージ名を設定
            intent.setPackage("com.junkbulk.jw_cadviewer")

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.d("viewDxf", "jw cad viewer is not installed." )
                Toast.makeText(this, "jw cad viewer is not installed.", Toast.LENGTH_LONG).show()
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
            if ( fileName == PrivateCSVFileName ) return@forEach //CSV skip
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


    val fileTypes = arrayOf(".csv", ".xlsx", ".dxf", ".sfc", ".pdf")

    private fun showFileTypeSelectionDialog(onComplete: () -> Unit ) {
        val selectedFileTypes = ArrayList<String>()

        // ファイルリストをロード
        val checkedItems = loadListFromFile().toBooleanArray()

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
                // shareFilesを保存
                saveListToFile(checkedItems.toList())
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


    // ファイルにリストを保存する関数
    val SHAREFILENAME = "shareFiles.txt"
    private fun saveListToFile(booleans: List<Boolean>, fileName: String = SHAREFILENAME) {
        try {
            val file = File(filesDir, fileName)
            file.writeText(booleans.joinToString("\n"))
        } catch (e: IOException) {
            // エラーハンドリング: ログ記録やユーザーへの通知など
            e.printStackTrace()
        }
    }

    // ファイルからリストを読み込む関数
    private fun loadListFromFile(fileName: String = SHAREFILENAME): List<Boolean> {
        val file = File(filesDir, fileName)
        if (!file.exists()) return List(fileTypes.size) { false } // fileTypesSize の長さの、false のみを含むリストを返す

        return file.readLines().map { it.toBoolean() }
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

        //想定と違う結果になりえる
        //trianglelist.arrangePointNumbers()

        val writer = DxfFileWriter(trianglelist.clone())
        writer.zumeninfo = rStr
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr
        writer.textscale_ = myview.textSize * 0.016f //25*0.014f=0.35f, 25/0.02f=0.5f

        writer.writer = bWriter
        writer.drawingLength = trianglelist.measureMostLongLine()
        writer.dedlist_ = myDeductionList.clone()
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.isDebug = myview.isDebug_

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = isNumberReverse

        writer.save()
        bWriter.close()

        return bWriter
    }

    private fun saveSFC(out: BufferedOutputStream) {

        val writer = SfcWriter(trianglelist.clone(), myDeductionList.clone(), out, filename, drawingStartNumber)
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.zumeninfo = rStr
        writer.textscale_ = myview.textSize * 20f //25*14f=350f, 25/20f=500f
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = isNumberReverse

        writer.save()
        out.close()

    }

    private fun savePDF(out: OutputStream){
        val writer = PdfWriter(
                trianglelist.getPrintScale(1f),
                trianglelist
        )
        writer.out_ = out
        writer.deductionList_ = myDeductionList

        writer.textscale_ = myview.textSize * 0.5f / writer.printScale_ //25*0.4f=10f, 25/0.3f=7.5f
        writer.initPaints()
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr
        writer.zumeninfo = rStr
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
        myview.drawPDF(
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

        val trilist = TriangleList(tri)
        trianglelist = trilist
        myDeductionList.clear()

        // メニューバーのタイトル
        koujiname = ""
        rosenname = "新規路線"//rStr.eRName_
        gyousyaname =""
        findViewById<EditText>(R.id.rosenname).setText(rosenname)
        setTitles()

        myview.setTriangleList(trilist, viewscale)
        myview.setDeductionList(myDeductionList, viewscale)
        myview.trianglelist.lastTapNumber = myview.trianglelist.size()
        myview.resetViewToLastTapTriangle()

        Log.d("FileLoader", "createNew: " + myview.trianglelist.size() )

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
            XlsxWriter().write( openFileOutput(filename, MODE_PRIVATE ), trianglelist, myDeductionList, rosenname )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveToPrivate(filename: String) {
        // ファイル名から拡張子を取得
        val extension = filename.substringAfterLast('.', "")

        when (extension) {
            "xlsx" -> XlsxWriter().write( openFileOutput(filename, MODE_PRIVATE ), trianglelist, myDeductionList, rosenname )
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
            for (index in 1..trianglelist.size()) {
                val mt: Triangle = trianglelist.getBy(index)
                val pointnumber: PointXY = mt.pointnumber
                val cp = parentBCtoCParam(mt.connectionSide, mt.lengthNotSized[0], mt.cParam_)

                writer.write("${mt.mynumber},${mt.lengthA_},${mt.lengthB_},${mt.lengthC_}," +
                        "${mt.parentnumber},${mt.connectionSide}," +
                        "${mt.name},${pointnumber.x},${pointnumber.y},${mt.pointNumber.flag.isMovedByUser}," +
                        "${mt.mycolor}," +
                        "${mt.dim.horizontal.a},${mt.dim.horizontal.b},${mt.dim.horizontal.c}," +
                        "${mt.dim.vertical.a},${mt.dim.vertical.b},${mt.dim.vertical.c}," +
                        "${cp.side},${cp.type},${cp.lcr}," +
                        "${mt.dim.flag[1].isMovedByUser},${mt.dim.flag[2].isMovedByUser}," +
                        "${mt.angle},${mt.pointCA.x},${mt.pointCA.y},${mt.angleInLocal_}," +
                        "${mt.dim.horizontal.s},${mt.dim.flagS.isMovedByUser}"
                )
                writer.newLine()
            }

            // その他の情報をCSVファイルに書き込み
            writer.apply {
                write("ListAngle, ${trianglelist.angle}")
                newLine()
                write("ListScale, ${trianglelist.scale}")
                newLine()
                write("TextSize, ${myview.textSize}")
                newLine()
            }

            // 減算リストのデータをCSVファイルに書き込み
            for (index in 1..myDeductionList.size()) {
                val dd: Deduction = myDeductionList.get(index)
                val pointAtRealscale = dd.point.scale(PointXY(0f, 0f), 1 / viewscale, -1 / viewscale)
                val pointFlagAtRealscale = dd.pointFlag.scale(PointXY(0f, 0f), 1 / viewscale, -1 / viewscale)

                writer.write("Deduction,${dd.num},${dd.name},${dd.lengthX},${dd.lengthY},${dd.overlap_to},${dd.type},${dd.angle},${pointAtRealscale.x},${pointAtRealscale.y},${pointFlagAtRealscale.x},${pointFlagAtRealscale.y},${dd.shapeAngle}")
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
            //logFilePreview(PrivateCSVFileName, "saveCSVtoPrivate")
            showToast("saveCSVToPrivate: success")

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

    private fun parseCSV(reader: BufferedReader):Boolean{
        val csvloader = CsvLoader()
        val returnValues = csvloader.parseCSV(
            reader,
            this::showToast,
            this::setAllTextSize,
            this::typeToInt,
            viewscale
        )

        if(returnValues == null ) return false

        setEditLists( returnValues.trilist, returnValues.dedlist )
        parseHeaderValues( returnValues.headerValues )
        return true
    }

    private fun parseHeaderValues( headerValues: HeaderValues){
        koujiname = headerValues.koujiname
        rosenname = headerValues.rosenname
        gyousyaname = headerValues.gyousyaname
        zumennum = headerValues.zumennum
        findViewById<EditText>(R.id.rosenname).setText(rosenname)
    }

    private fun setEditLists(trilist: TriangleList, dedlist:DeductionList ){
        trianglelist = trilist
        myDeductionList = dedlist
        turnToBlankTrilistUndo()

        trilist.recoverState(PointXY(0f, 0f))

        myview.setDeductionList(dedlist, viewscale)
        myview.setTriangleList(trilist, viewscale)
        myview.resetViewToLastTapTriangle()

        Log.d( "FileLoader", "my_view.setTriangleList: " + myview.trianglelist.size() )

        deductionMode = true
        editorResetBy(trianglelist)
        flipDeductionMode()
    }

    private fun setAllTextSize(textsize:Float){
        myview.setAllTextSize(textsize)
    }

    private fun resumeCSV() {
        StringBuilder()
        try {
            val reader = openFileAsBufferedReader(PrivateCSVFileName)

            val parseResult = reader?.let { parseCSV(it) }  // parseCSVの結果を一時変数に格納
            if (parseResult == false) createNew()  // 結果がfalseの場合はcreateNewを呼び出す

            // 結果をログに出力
            if(myview.isDebug_)logFilePreview(PrivateCSVFileName, "resumeCSV")
            showToast("resumeCSV: success")

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //endregion

    // region logs and Toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun logListCurrent(tag: String="ui", callerName: String = "unknown"){
        Log.d(tag,callerName+" my_view.trilist.current:"+myview.trianglelist.selectedNumber)
        Log.d(tag,callerName+" my_view.trilist.lastTapNumber:"+myview.trianglelist.lastTapNumber)
        Log.d(tag,callerName+" mainActivity.trilist.current:"+trianglelist.selectedNumber)
        Log.d(tag,callerName+" mainActivity.trilist.lastTapNumber:"+trianglelist.lastTapNumber)
        Log.d(tag,callerName+" mainActivity.dedlist.current:"+myDeductionList.current)
        Log.d(tag,callerName+" mainActivity.dedlist.lastTapIndex_:"+myDeductionList.lastTapIndex_)
    }

    private fun logFabController(tag: String = "ui fab"){
        val info1 = "trilist in mainActivity \n${trianglelist.toStrings()}"
        val info2 = "trilist in myview \n${myview.trianglelist.toStrings()}"
        Log.d(tag,info1)
        Log.d(tag,info2)
    }

    //endregion

} // end of class
