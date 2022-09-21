package com.jpaver.trianglelist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.jpaver.trianglelist.databinding.ActivityMainBinding
import com.jpaver.trianglelist.util.AdManager
import org.json.JSONObject.NULL
import java.io.*
import java.time.LocalDate
import java.util.*
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

interface CustomTextWatcher: TextWatcher{
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
}

class MainActivity : AppCompatActivity(),
        MyDialogFragment.NoticeDialogListener {

    private lateinit var prefSetting: SharedPreferences

    private lateinit var bMyAct: ActivityMainBinding
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

    private fun checkPermission() {
        if (isGranted()) {

        } else {
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

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTPERMISSION) {
            checkPermission()
        }
    }

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
    private var dParams: Params = Params("", "", 0, 0f, 0f, 0f, 0, 0, PointXY(0f, 0f))
    private var lastParams: Params = dParams

    // タイトルパラメータ、stringリソースから構成する

    private lateinit var rStr : ResStr
    private lateinit var titleTri: TitleParams//
    private lateinit var titleDed: TitleParams
    private lateinit var titleTriStr : TitleParamStr
    private lateinit var titleDedStr : TitleParamStr

    private lateinit var myTriangleList: TriangleList //= TriangleList(Triangle(0f,0f,0f,PointXY(0f,0f),180f))
    private lateinit var myDeductionList: DeductionList

    private var trilistStored: TriangleList = TriangleList()

    private var fileType: String = "notyet"
    private var filename = "notyet"
    private var deductionMode: Boolean = false
    private var mIsCreateNew: Boolean = false
    private val onetohandred = 11.9f
    private val experience = 4f
    private val mScale = onetohandred*experience

    private var koujiname = ""
    private var rosenname = ""
    private var gyousyaname = ""
    private var zumennum = "1/1"
    private var drawingStartNumber = 1
    private var drawingNumberReversal = false

    private var colorindex = 4
    private val resColors = arrayOf(
            R.color.colorPink,   //0
            R.color.colorOrange, //1
            R.color.colorYellow, //2
            R.color.colorLime,   //3
            R.color.colorSky     //4
    )

    private lateinit var mAdView : AdView
    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "MainActivity"
    //private val isAdTEST_ = true
    //private val TestAdID_ = "ca-app-pub-3940256099942544/6300978111"
    //private val UnitAdID_ = "ca-app-pub-6982449551349060/2369695624"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_NoActionBar) //Note that this should be called before any views are instantiated in the Context (for example before calling Activity.setContentView(View) or LayoutInflater.inflate(int, ViewGroup)).

        bMyAct = ActivityMainBinding.inflate(layoutInflater)

        val view = bMyAct.root

        setSupportActionBar(bMyAct.toolbar)
        setContentView(view)

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

        // must after setContentView
        if( BuildConfig.FLAVOR == "free" ) {
            mAdView = findViewById(R.id.adView)

            MobileAds.initialize(this) {}

            if( BuildConfig.FLAVOR == "debug" ) {
                val testDeviceIds = Arrays.asList("ca-app-pub-3940256099942544/6300978111")//33BE2250B43518CCDA7DE426D04EE231")
                val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
                MobileAds.setRequestConfiguration(configuration)
            }

            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            Log.d("adMob", "adMob Loaded.")

        }

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        myDeductionList = DeductionList()
        //Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()

        fab_replace =   bMyAct.fabReplace
        fab_flag =      bMyAct.fabFlag
        fab_dimsidew =  bMyAct.fabDimsidew
        fab_dimsideh =  bMyAct.fabDimsideh
        fab_nijyuualign = bMyAct.fabNijyuualign
        fab_minus =     bMyAct.fabMinus
        fab_undo =      bMyAct.fabUndo
        fab_fillcolor = bMyAct.fabFillcolor
        fab_texplus =   bMyAct.fabTexplus
        fab_texminus =  bMyAct.fabTexminus
        fab_setB =      bMyAct.fabSetB
        fab_setC =      bMyAct.fabSetC
        fab_rot_l =     bMyAct.fabRotL
        fab_rot_r =     bMyAct.fabRotR
        fab_deduction = bMyAct.fabDeduction
        fab_resetView = bMyAct.fabResetView
        fab_up =        bMyAct.fabUp
        fab_down =      bMyAct.fabDown
        fab_debug =     bMyAct.fabDebug
        fab_testbasic = bMyAct.fabTestbasic
        fab_pdf =       bMyAct.fabPdf
        fab_share =     bMyAct.fabShare
        fab_mail =      bMyAct.fabMail
        fab_numreverse = bMyAct.fabNumreverse


        fab_replace.setOnClickListener {
            fabReplace(dParams, false)
        }

        fab_flag.setOnClickListener {

            fabFlag()

            autoSaveCSV()
        }

        fab_dimsidew.setOnClickListener {


            if(!deductionMode){
                var dimside = my_view.myTriangleList.lastTapSide_
                val trinum  = my_view.myTriangleList.lastTapNumber_
                Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

                var tri = myTriangleList.get(trinum)
                if( dimside == 0 && ( tri.parentBC_ == 1 ||  tri.parentBC_ == 2 ) && trinum > 1 ) {
                    dimside = tri.parentBC_
                    tri = myTriangleList.get( trinum - 1 )
                    Log.d("TriangleList", "Triangle dim rot w : " + tri.myNumber_ + dimside )
                }

                tri.rotateDimSideAlign(dimside)
                my_view.setTriangleList(myTriangleList, mScale, false)
                my_view.invalidate()
                autoSaveCSV()
            }
        }

        fab_dimsideh.setOnClickListener {
            if(!deductionMode){
                var dimside = my_view.myTriangleList.lastTapSide_
                val trinum  = my_view.myTriangleList.lastTapNumber_
                Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

                var tri = myTriangleList.get(trinum)
                if( dimside == 0 && ( tri.parentBC_ == 1 ||  tri.parentBC_ == 2 ) && trinum > 1 ) {
                    dimside = tri.parentBC_
                    tri = myTriangleList.get( trinum - 1 )
                    Log.d("TriangleList", "Triangle dim rot w : " + tri.myNumber_ + dimside )
                }

                tri.flipDimAlignH(dimside)
                my_view.setTriangleList(myTriangleList, mScale, false )
                my_view.invalidate()
                autoSaveCSV()
            }
        }

        fab_nijyuualign.setOnClickListener {
            if(!deductionMode && myTriangleList.lastTapNumber_ > 1 ){
                myTriangleList.rotateCurrentTriLCR()
                //myTriangleList.resetTriConnection(myTriangleList.lastTapNum_, );
                my_view.setTriangleList(myTriangleList, mScale, false )
                my_view.resetView(my_view.toLastTapTriangle())
                editorClear(getList(deductionMode), getList(deductionMode).getCurrent())

                autoSaveCSV()
            }
        }

        var deleteWarning = 0
        fab_minus.setOnClickListener {
            val listLength = getList(deductionMode).size()

            if(listLength > 0 && deleteWarning == 0) {
                deleteWarning = 1
                bMyAct.fabMinus.backgroundTintList = getColorStateList(R.color.colorTT2)

            }
            else {
                if (listLength > 0) {
                    trilistStored = myTriangleList.clone()

                    var eraseNum = listLength
                    if(!deductionMode) eraseNum = myTriangleList.lastTapNumber_

                    getList(deductionMode).remove(eraseNum)

                    //my_view.removeTriangle()
                    my_view.setDeductionList(myDeductionList, mScale)
                    my_view.setTriangleList(myTriangleList, mScale)

                    editorClear(getList(deductionMode), getList(deductionMode).size())
                }
                deleteWarning = 0
                bMyAct.fabMinus.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            printDebugConsole()
            colorMovementFabs()
            my_view.resetViewToLastTapTriangle()
            setTitles()

        }

        fab_undo.setOnClickListener{
            if( trilistStored.size() > 0 ){
                myTriangleList = trilistStored.clone()
                //my_view.undo()
                my_view.setTriangleList(trilistStored, mScale)
                my_view.resetViewToLastTapTriangle()

                trilistStored.trilist_.clear()

                bMyAct.fabUndo.backgroundTintList = getColorStateList(R.color.colorPrimary)
                editorClear(getList(deductionMode), getList(deductionMode).getCurrent())
                setTitles()
            }
        }

        fab_fillcolor.setOnClickListener {
            if(!deductionMode){
                myTriangleList.get(my_view.myTriangleList.current)


                colorindex ++
                if(colorindex == resColors.size) colorindex = 0
                bMyAct.fabFillcolor.backgroundTintList = getColorStateList(resColors[colorindex])

                //dParams_ = myEditor.ReadLine(dParams_, myELSecond)
                myTriangleList.get(my_view.myTriangleList.current).color_ = colorindex

                my_view.setFillColor(colorindex, myTriangleList.current)
                autoSaveCSV()
            }
        }

        fab_texplus.setOnClickListener {
            my_view.ts_ += 5f
            my_view.setAllTextSize(my_view.ts_)

//            my_view.paintTexS.textSize = my_view.ts_
            my_view.invalidate()
        }

        fab_texminus.setOnClickListener {
            my_view.ts_ -= 5f
            my_view.setAllTextSize(my_view.ts_)

            my_view.invalidate()
        }

        fab_setB.setOnClickListener {
            autoConnection(1)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        fab_setC.setOnClickListener {
            autoConnection(2)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        fab_rot_l.setOnClickListener {
            fabRotate(5f, true )
            autoSaveCSV()
        }

        fab_rot_r.setOnClickListener {
            fabRotate(-5f, true )
            autoSaveCSV()
        }

        fab_deduction.setOnClickListener {
            deleteWarning = 0
            fab_minus.backgroundTintList = getColorStateList(R.color.colorAccent)
            flipDeductionMode()
            colorMovementFabs()
        }

        fab_resetView.setOnClickListener {

            if(!deductionMode) my_view.resetViewToLastTapTriangle()
            else if( myDeductionList.size() > 0 ){
                val currentIndex = my_view.myDeductionList.getCurrent()
                my_view.resetView(
                    my_view.myDeductionList.get(currentIndex).point.scale(
                        PointXY(
                            1f,
                            -1f
                        )
                    )
                )
            }
        }

        fab_up.setOnClickListener {
            myEditor.scroll(-1, getList(deductionMode), myELSecond, myELThird)

            if(!deductionMode) moveTrilist()
            else if( myDeductionList.size() > 0 ){
                my_view.myDeductionList.setCurrent(myDeductionList.getCurrent())
                val currentIndex = my_view.myDeductionList.getCurrent()
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
                my_view.myDeductionList.setCurrent(myDeductionList.getCurrent())
                val currentIndex = my_view.myDeductionList.getCurrent()
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
                    "仕切弁", "Circle", 1, 0.23f, 0f, 0f, 1, 0, PointXY(1f, 0f), PointXY(
                        0f,
                        0f
                    )
                ), true
            )
            deductionMode = false


            Toast.makeText(this, "Basic Senario Test Done.", Toast.LENGTH_SHORT).show()
        }

        fab_pdf.setOnClickListener {
            viewPdf(getAppLocalFile(this, "myLastTriList.pdf"))
        }

        fab_share.setOnClickListener {
            sendPdf(this)
        }

        fab_mail.setOnClickListener {
            //findViewById<ProgressBar>(R.id.indeterminateBar).visibility = View.VISIBLE
            //progressBar.visibility = View.VISIBLE

            sendMail()

            //    progressBar.visibility = View.INVISIBLE

        }

        fab_numreverse.setOnClickListener{
            trilistStored = myTriangleList.clone()

            myTriangleList = myTriangleList.reverse()
            my_view.setTriangleList(myTriangleList, mScale)
            my_view.resetView(my_view.toLastTapTriangle())
            editorClear(myTriangleList, myTriangleList.current)
        }
    }

    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        //bMyView = FragmentFirstBinding.bind( findViewById(R.id.my_view) )//inflate(layoutInflater)
        my_view = findViewById(R.id.my_view)//bMyView.myView
        Log.d("myView", "Instance check in MainActivity: " + my_view )


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

        val filepath = this.filesDir.absolutePath + "/" + "myLastTriList.csv"
        val file = File(filepath)
        if(file.exists()) resumeCSV()
        else createNew()
        loadEditTable()
        colorMovementFabs()
        //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
        fab_replace.backgroundTintList = getColorStateList(R.color.colorLime)
        setEditNameAdapter(sNumberList)

        checkPermission()

        // リスナーを登録
        val etB1 = findViewById<EditText>(R.id.editLengthB1)
        etB1.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (etB1.isFocused) my_view.watchedB_ = p0.toString()
                my_view.invalidate()
            }
        })
        val etC1 = findViewById<EditText>(R.id.editLengthC1)
        etC1.addTextChangedListener(object : CustomTextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (etC1.isFocused) my_view.watchedC_ = p0.toString()
                my_view.invalidate()
            }
        })

        Log.d("MainActivity", "OnAttachedToWindow Process Done.")

        showInterStAd()

    }

    override fun onResume() {
        super.onResume()
        Log.d("AdMob", "OnResume")

        // 広告の非表示
        if( BuildConfig.FLAVOR == "free" ){
            val adManager = AdManager()
            //adManager.disableAd(mAdView)
            //findViewById<EditText>(R.id.editLengthC1).requestFocus()
            //mAdView.visibility = VISIBLE
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 上部のOptionsMenuの表示　Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDialogPositiveClick(dialog: DialogFragment?) {
        mIsCreateNew = true
        fileType = "CSV"
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.type = "text/csv"
        i.putExtra(Intent.EXTRA_TITLE,LocalDate.now().monthValue.toString() + "." + LocalDate.now().dayOfMonth.toString() + " " + rosenname + ".csv" )
        saveContent.launch( i )
        setResult(RESULT_OK, i)

    }

    override fun onDialogNegativeClick(dialog: DialogFragment?) {
        editorClear(getList(deductionMode), getList(deductionMode).size())
        createNew()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()



        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_new -> {
                val dialog = MyDialogFragment()
                dialog.show(supportFragmentManager, "dialog.basic")
                return true
            }

            R.id.action_save_csv -> {
                fileType = "CSV"
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/csv"
                intent.putExtra(
                    Intent.EXTRA_TITLE,
                    LocalDate.now().monthValue.toString() + "." + LocalDate.now().dayOfMonth.toString() + " " + rosenname + ".csv"
                )

                saveContent.launch( intent )

                return true
            }
            R.id.action_load_csv -> {
                //actionSelectDir()
                openDocumentPicker()
//                actionLoadCSV()
                return true
            }

            R.id.action_save_dxf -> {
                showExportDialog(".dxf", "Export DXF", "DXF", "text/dxf")
                return true
            }

            R.id.action_save_sfc -> {
                showExportDialog(".sfc", "Export SFC", "SFC", "text/sfc")
                return true
            }

            R.id.action_save_pdf -> {
                // 入力ヒントの表示
                val hCname = getString(R.string.inputcname)
                val hSpace = getString(R.string.space)
                val hRname = getString(R.string.inputdname)
                val hAname = getString(R.string.inputaname)
                val hRnum = getString(R.string.inputdnum)
                val editText = EditText(this)
                editText.hint = "$hCname $hSpace"
                val filter = arrayOf(InputFilter.LengthFilter(50))
                editText.filters = filter
                editText.setText(koujiname)
                val editText2 = EditText(this)
                editText2.hint = hRname
                editText2.setText(rosenname)
                editText2.filters = filter
                val editText3 = EditText(this)
                editText3.hint = hAname
                editText3.setText(gyousyaname)
                editText3.filters = filter
                val editText4 = EditText(this)
                editText4.hint = hRnum
                editText4.setText(zumennum)
                editText4.filters = filter

                AlertDialog.Builder(this)
                    .setTitle("Save PDF")
                    .setMessage(R.string.inputcname)
                    .setView(editText)
                    .setPositiveButton("OK"
                    ) { _, _ ->
                        koujiname = editText.text.toString()

                        AlertDialog.Builder(this)
                            .setTitle("Save PDF")
                            .setMessage(R.string.inputdname)
                            .setView(editText2)
                            .setPositiveButton("OK"
                            ) { _, _ ->
                                rosenname = editText2.text.toString()

                                AlertDialog.Builder(this)
                                    .setTitle("Save PDF")
                                    .setMessage(R.string.inputaname)
                                    .setView(editText3)
                                    .setPositiveButton("OK"
                                    ) { _, _ ->
                                        gyousyaname =
                                            editText3.text.toString()

                                        AlertDialog.Builder(this)
                                            .setTitle("Save PDF")
                                            .setMessage(R.string.inputdnum)
                                            .setView(
                                                editText4
                                            )
                                            .setPositiveButton(
                                                "OK"
                                            ) { _, _ ->
                                                zumennum =
                                                    editText4.text.toString()

                                                fileType =
                                                    "PDF"
                                                val i: Intent =
                                                    Intent(
                                                        Intent.ACTION_CREATE_DOCUMENT
                                                    ).apply {
                                                        addCategory(
                                                            Intent.CATEGORY_OPENABLE
                                                        )
                                                        type =
                                                            "application/pdf"
                                                        putExtra(
                                                            Intent.EXTRA_TITLE,
                                                            LocalDate.now().monthValue.toString() + "." + LocalDate.now().dayOfMonth.toString() + " " + rosenname + ".pdf"
                                                        )

                                                    }
                                                saveContent.launch( i )

                                            }
                                            .show()
                                    }
                                    .show()
                            }
                            .show()
                    }
                    .show()

                return true
            }

            R.id.action_send_mail -> {
                sendMail()
                return true
            }

            R.id.action_usage -> {
                playMedia( Uri.parse("https://trianglelist.home.blog") )
/*                if (BuildConfig.FLAVOR == "free") {
                    viewPdf(
                            AssetsFileProvider.CONTENT_URI_FREE.buildUpon()
                                    .appendPath("pdf")
                                    .appendPath("trilistusage.pdf")
                                    .build()
                    )
                }
                if (BuildConfig.FLAVOR == "full") {
                    viewPdf(
                            AssetsFileProvider.CONTENT_URI_FULL.buildUpon()
                                    .appendPath("pdf")
                                    .appendPath("trilistusage.pdf")
                                    .build()
                    )
                }
*/
                return true
            }

            R.id.action_privacy -> {
                playMedia( Uri.parse("https://drive.google.com/file/d/1C7xlXZGvabeQoNEjmVpOCAxQGrFCXS60/view?usp=sharing") )
                return true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

        //return true
    }

    private fun flipDeductionMode() {
        myDeductionList.setCurrent(myDeductionList.size())
        myTriangleList.setCurrent(myTriangleList.size())
        //printDebugConsole()
        colorMovementFabs()

        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        if(!deductionMode) {
            deductionMode = true
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
            val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, dArray
            )
            findViewById<Spinner>(R.id.editParentConnect1).adapter = spinnerArrayAdapter
            findViewById<Spinner>(R.id.editParentConnect2).adapter = spinnerArrayAdapter
            findViewById<Spinner>(R.id.editParentConnect3).adapter = spinnerArrayAdapter
            findViewById<EditText>(R.id.editName1).requestFocus()
            inputMethodManager.showSoftInput(findViewById(R.id.editName1), 0)
            setEditNameAdapter(dedNameListC)

            //クロスヘアラインを画面中央に描画
//            my_view.drawCrossHairLine()

        } else {
            deductionMode = false
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
            val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, tArray
            )
            findViewById<Spinner>(R.id.editParentConnect1).adapter = spinnerArrayAdapter
            findViewById<Spinner>(R.id.editParentConnect2).adapter = spinnerArrayAdapter
            findViewById<Spinner>(R.id.editParentConnect3).adapter = spinnerArrayAdapter
            findViewById<EditText>(R.id.editLengthA1).requestFocus()
            setEditNameAdapter(sNumberList)
        }
        editorClear(getList(deductionMode), getList(deductionMode).size())
        //inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0)

    }

    private fun editorClear(elist: EditList, currentNum: Int){
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

    fun validTriangle(dp: Params) : Boolean{
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
        if( dp.name == "" || dp.a < 0.1f ) return false
        if( dp.type == "Box" && ( dp.a < 0.1f || dp.b < 0.1f ) ) return false
        return true
    }

    private fun getList(dMode: Boolean) :EditList{
        return if(dMode) myDeductionList
        else myTriangleList
    }

    fun fabFlag(){
        dParams = myEditor.readLineTo(dParams, myELSecond)// 200703 // if式の中に入っていると当然ながら更新されない時があるので注意

        if(deductionMode){
            val d = myDeductionList.get(dParams.n)
            dParams.pts = my_view.getTapPoint()
            dParams.pt = d.point
            //var ded = myDeductionList.get(dParams_.n)
            my_view.getTapPoint().scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            if(validDeduction(dParams)) {// あまり遠い時はスルー
                myDeductionList.replace(dParams.n, dParams)
//                    EditorReset(getList(myDeductionMode),getList(myDeductionMode).length())
                my_view.setDeductionList(myDeductionList, mScale)
            }
        }
        else{
            val tri = myTriangleList.get(dParams.n)
            val tp = my_view.getTapPoint().scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            if( tp.lengthTo(tri.pointCenter_) < 10f ){ // あまり遠い時はスルー
                tri.pointNumber_ = tp
                tri.isPointNumberMoved_ = true
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
        val dedmode = deductionMode
        val editlist = getList(deductionMode)

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

        var usedDedPoint = params.pt.clone()

        //var isSucceed = false

        if(!dedmode) {

            if( strTopB == "" ) resetTrianglesBy(readedSecond)
            else
                if(strTopC == "" && !useit) return
                else  addTriangleBy(readedFirst)

        } else { // if in deduction mode
            //if (validDeduction(params) == false) return


            usedDedPoint = if( strTopA == "" ) {
                resetDeductionsBy(readedSecond)
                my_view.myDeductionList.get(readedSecond.n).point
            } else{
                addDeductionBy(readedFirst)
                my_view.myDeductionList.get(readedFirst.n).point
            }
            findViewById<EditText>(R.id.editName1).requestFocus()
        }

        editorClear(editlist, editlist.getCurrent())
        my_view.setTriangleList(myTriangleList, mScale)
        my_view.setDeductionList(myDeductionList, mScale)
        printDebugConsole()
        autoSaveCSV()
        setTitles()
        if(!dedmode) my_view.resetView(my_view.toLastTapTriangle())
        if(dedmode) my_view.resetView(usedDedPoint.scale(PointXY(0f, 0f), 1f, -1f))//resetViewToTP()

        my_view.myTriangleList.isDoubleTap_ = false
        my_view.myTriangleList.lastTapSide_ = 0
        /*if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(
                this,
                isSucceed.toString(),
                Toast.LENGTH_SHORT
        ).show()*/
    }

    private fun moveTrilist(){
        my_view.getTriangleList().setCurrent(myTriangleList.getCurrent())
        my_view.myTriangleList.lastTapNumber_ = myTriangleList.getCurrent()
        myTriangleList.lastTapNumber_ = myTriangleList.getCurrent()
        my_view.resetViewToLastTapTriangle()
    }

    private fun addDeductionBy(params: Params) : Boolean {
        params.pt = my_view.getTapPoint()
        params.pts = params.pt //PointXY(0f, 0f)
        params.pn = my_view.myTriangleList.isCollide(dParams.pt.scale(PointXY(1f, -1f)))

        //形状の自動判定
        if( params.b > 0f ) params.type = "Box"
        else params.type = "Circle"

        if (validDeduction(params)) {
            // 所属する三角形の判定処理
            if( params.pt != PointXY(0f, 0f) ) {
                params.pn = my_view.myTriangleList.isCollide(params.pt.scale(PointXY(1f, -1f)))

                if( params.pn != 0 ) {
                    my_view.myTriangleList.dedmapping(myDeductionList, -1)
                    Log.d( "DeductionList", "ptri dedcount" + my_view.myTriangleList.get(params.pn).dedcount )
                    Log.d( "DeductionList", "params.pts" + params.pts.x + ", " + params.pts.y )

                    val trilistinview = my_view.myTriangleList
                    val ptri = trilistinview.get(params.pn)
                    params.pts = ptri.hataage(params.pt, 30f, -1f, params.n.toFloat() )
                }
            }

            myDeductionList.add(params)
            my_view.setDeductionList(myDeductionList, mScale)
            lastParams = params
            return true
        }
        else return false
    }

    private fun addTriangleBy(params: Params) : Boolean {
        if (validTriangle(params)) {
            trilistStored = myTriangleList.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            val myTri = Triangle(
                    myTriangleList.getTriangle(params.pn),
                    params
            )
            myTri.myNumber_ = params.n
            myTriangleList.add(myTri, true)
            findViewById<EditText>(R.id.editLengthA1).requestFocus()
            myTriangleList.lastTapNumber_ = myTriangleList.size()
            //my_view.resetView()
            return true
        }
        return false
    }

    private fun resetTrianglesBy(params: Params) : Boolean {

        return if (validTriangle(params)){
            trilistStored = myTriangleList.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            myTriangleList.resetFromParam(params)
        } // if valid triangle
        else false
    }

    private fun resetDeductionsBy(params: Params) : Boolean {
        //myEditor.ReadLineTo(prms, myELSecond)
        params.pt = my_view.getTapPoint()
        params.pts = myDeductionList.get(params.n).pointFlag

        myTriangleList.current = params.pn

        if(validDeduction(params)) {
            // 所属する三角形の判定処理
            if( params.pt != PointXY(0f, 0f) ) {
                params.pn = my_view.myTriangleList.isCollide(params.pt.scale(PointXY(1f, -1f)))

                if( params.pn != 0 ) {
                    my_view.myTriangleList.dedmapping(myDeductionList, -1)
                    Log.d( "DeductionList", "ptri dedcount" + my_view.myTriangleList.get(params.pn).dedcount )
                    Log.d( "DeductionList", "params.pts" + params.pts.x + ", " + params.pts.y )

                    val ptri = my_view.myTriangleList.get(params.pn)
                    params.pts = ptri.hataage(params.pt, 30f, -1f, params.n.toFloat() )
                }

            }

            myDeductionList.replace(params.n, params)
            return true
        }
        else return false
    }

    private fun autoConnection(i: Int){
        // 広告の再表示
        //if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = VISIBLE

        my_view.myTriangleList.lastTapSide_ = i
        dParams = myEditor.readLineTo(dParams, myELFirst) //keep them
        val elb1 = findViewById<EditText>(R.id.editLengthB1)
        var focusTo = elb1

        if( my_view.myTriangleList.isDoubleTap_ == true ){
            if(i == 1) focusTo = findViewById(R.id.editLengthB2)
            if(i == 2) focusTo = findViewById(R.id.editLengthC2)
        }

        if(!deductionMode) {
            my_view.watchedB_ = elb1.text.toString()
            my_view.watchedC_ = findViewById<EditText>(R.id.editLengthC1).text.toString()

            val t:Triangle = myTriangleList.get(dParams.pn)
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
        val trilist  = myTriangleList
        if(deductionMode){
            my_view.myDeductionList.setScale(my_view.myScale)
            my_view.myDeductionList.getTapIndex(my_view.pressedInModel)

            if ( my_view.myDeductionList.lastTapIndex_ > -1 ) {
                val tapIndex = my_view.myDeductionList.lastTapIndex_+1
                if( -1 < tapIndex ) {
                    //Toast.makeText(this, "deduction tap", Toast.LENGTH_SHORT).show()
                    myEditor.scroll(
                            tapIndex - myDeductionList.getCurrent(),
                            getList(deductionMode), myELSecond, myELThird
                    )
                    //my_view.resetView( my_view.myDeductionList.get( tapIndex ).point.scale( PointXY(1f,-1f ) ) )
                }
            }

            // 三角形番号が押されたときはセンタリング
            trilistV.getTap(
                my_view.pressedInModel.scale(PointXY(0f, 0f), 1f, -1f),
                0.8f / zoomsize
            )
            if ( trilistV.lastTapNumber_ != 0 ) {
                if( trilistV.lastTapSide_ == 3 ) my_view.resetViewToLastTapTriangle()
            }

        }
        else {
            val lpp = my_view.pressedInModel.scale(PointXY(0f, 0f), 1f, -1f)

            val slpp = my_view.shadowTri_.getTapLength(lpp, 0.8f / zoomsize)
            if( slpp == 1) {
                findViewById<EditText>(R.id.editLengthB1).requestFocus()
//                my_view.myTriangleList.lastTapSide_ = 1
                return
            }
            if( slpp == 2){
                findViewById<EditText>(R.id.editLengthC1).requestFocus()
//                my_view.myTriangleList.lastTapSide_ = 2
                return
            }

            // view　の　trilistのlastTapとcurrentをずらして editorTableを移動させる
            trilistV.getTap(lpp, my_view.ts_ * 0.02f / zoomsize )


            if ( trilistV.lastTapNumber_ != 0 ) {
                //Toast.makeText(this, "Triangle tap", Toast.LENGTH_SHORT).show()
                myEditor.scroll(
                        trilistV.lastTapNumber_ - trilistV.current,
                    trilist, myELSecond, myELThird
                )

                trilistV.current = trilistV.lastTapNumber_
                myTriangleList.setCurrent(my_view.getTriangleList().lastTapNumber_)

                myTriangleList.lastTapNumber_ = my_view.getTriangleList().lastTapNumber_
                myTriangleList.lastTapSide_ = my_view.getTriangleList().lastTapSide_
                findViewById<EditText>(R.id.editParentNumber1).setText(myTriangleList.lastTapNumber_.toString())
                findViewById<EditText>(R.id.editNumber1).setText(myTriangleList.size().toString())

                colorindex = myTriangleList.get(myTriangleList.lastTapNumber_).color_
                colorMovementFabs()
                printDebugConsole()
                setTitles()
                if( my_view.myTriangleList.lastTapSide_ == 0 ) {

                    findViewById<EditText>(R.id.editLengthA2).requestFocus()
                    findViewById<EditText>(R.id.editLengthA2).setSelection(findViewById<EditText>(R.id.editLengthA2).text.length)
                    inputMethodManager.showSoftInput(findViewById(R.id.editLengthA2), 0)
                    my_view.setParentSide(my_view.getTriangleList().lastTapNumber_, 3)
                }
                if( my_view.myTriangleList.lastTapSide_ == 1 || slpp == 1 ) {
                    autoConnection(1)
                    //findViewById<EditText>(R.id.editText5).requestFocus()
                    //inputMethodManager.showSoftInput(findViewById(R.id.editText5), 0)
                }
                if( my_view.myTriangleList.lastTapSide_ == 2 || slpp == 2 ) {
                    autoConnection(2)
                    //findViewById<EditText>(R.id.editText6).requestFocus()
                    //inputMethodManager.showSoftInput(findViewById(R.id.editText6), 0)
                }

                if( my_view.myTriangleList.lastTapSide_ == 3 ) my_view.resetViewToLastTapTriangle()


            }
        }

        Log.d("SetTarget", "Tap Triangle is : " + my_view.myTriangleList.lastTapNumber_ + ", side is :" + my_view.myTriangleList.lastTapSide_ )

    }

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

    private fun colorMovementFabs() : Int{
        val max: Int = getList(deductionMode).size()
        val current: Int = getList(deductionMode).getCurrent()
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

    private fun createNew(){
        val tri = Triangle(5f, 5f, 5f, PointXY(0f, 0f), 0f)
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
        editorClear(getList(deductionMode), getList(deductionMode).size())

    }

    fun playMedia(file: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = file
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    val loadContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        if (result.resultCode == Activity.RESULT_OK && result.data != NULL) {
            val resultIntent = result.data
            val title: Uri? = Objects.requireNonNull(resultIntent?.data)

            StringBuilder()
            try {
                val reader = BufferedReader(
                    InputStreamReader(title?.let { contentResolver.openInputStream(it) }, "Shift-JIS")
                )
                loadCSV(reader)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        setTitles()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val saveContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        //Log.d( "FileManager", "StartActivityForResult: " + result )

        if (result.resultCode == Activity.RESULT_OK && result.data != NULL) {
            val resultIntent = result.data
            val title: Uri? = Objects.requireNonNull(resultIntent?.data)

            // Uriは再起すると使えなくなるので対策
            if (title != null) {
                contentResolver.takePersistableUriPermission(
                    title,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            // Uri保存。これでアプリ再起動後も使えます。
            prefSetting.edit {
                putString("uri", title.toString())
            }

            try {
                autoSaveCSV() // オートセーブ

                val charset = "Shift-JIS"
                val writer = BufferedWriter(
                    OutputStreamWriter(title?.let { contentResolver.openOutputStream(it) }, charset)
                )

                if (fileType == "DXF") saveDXF(writer)
                if (fileType == "CSV") saveCSV(writer)
                if (fileType == "PDF") savePDF(title?.let { contentResolver.openOutputStream(it) }!!)
                if (fileType == "SFC") saveSFC(
                    BufferedOutputStream(
                        title?.let {
                            contentResolver.openOutputStream(
                                it
                            )
                        }
                    )
                )

                if( BuildConfig.FLAVOR == "free" ) showInterStAd()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        setTitles()
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

        //startActivityForResult( intent, 2 )
        loadContent.launch( intent )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showExportDialog(fileprefix: String, title: String, filetype: String, intentType: String): Boolean{
        val hTstart = getString(R.string.inputtnum)
        val editText5 = EditText(this)
        editText5.hint = hTstart
        val filter2 = arrayOf(InputFilter.LengthFilter(3))
        editText5.filters = filter2
        editText5.setText(drawingStartNumber.toString())

        filename = rosenname + " " + LocalDate.now() + fileprefix
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.inputtnum)
            .setView(editText5)
            .setPositiveButton("OK"
            ) { _, _ ->
                drawingStartNumber = editText5.text.toString().toInt()
                drawingNumberReversal = false

                fileType = filetype
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                i.type = intentType
                i.putExtra(
                        Intent.EXTRA_TITLE,
                    LocalDate.now().monthValue.toString() + "." + LocalDate.now().dayOfMonth.toString() + " " + rosenname + fileprefix
                )
                i.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION// or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED//flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                saveContent.launch( i )
            }
                .setNegativeButton("NumReverse"
                ) { _, _ ->
                    drawingStartNumber = editText5.text.toString().toInt()
                    drawingNumberReversal = true

                    fileType = filetype
                    val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    i.type = intentType
                    i.putExtra(
                            Intent.EXTRA_TITLE,
                            rosenname + " " + LocalDate.now() + fileprefix
                    )

                    i.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION// or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED//flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    saveContent.launch( i )
                }.show()
        return true
    }
/*
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data?.data == NULL || resultCode == RESULT_CANCELED) return
        val title: Uri = Objects.requireNonNull(data?.data)!!

        if(requestCode == 1 && resultCode == RESULT_OK) {
            try {
                val charset = "Shift-JIS"
                val writer = BufferedWriter(
                        OutputStreamWriter(contentResolver.openOutputStream(title), charset)
                )

                if (fileType == "DXF") saveDXF(writer)
                if (fileType == "CSV") saveCSV(writer)
                if (fileType == "PDF") savePDF(contentResolver.openOutputStream(title)!!)
                if (fileType == "SFC") saveSFC(
                        BufferedOutputStream(
                                contentResolver.openOutputStream(
                                        title
                                )
                        )
                )

                autoSaveCSV() // オートセーブ
                if( BuildConfig.FLAVOR == "free" ) showInterStAd()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if(requestCode ==2 && resultCode == RESULT_OK) {
            StringBuilder()
            try {
                val reader = BufferedReader(
                        InputStreamReader(contentResolver.openInputStream(title), "Shift-JIS")
                )
                loadCSV(reader)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //フォルダ選択
        if (requestCode == REQUESTCODE && resultCode == RESULT_OK) {
            // リクエストコードが一致&成功のとき
            val uri = data?.data ?: return
            // Uriは再起すると使えなくなるので対策
            contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Uri保存。これでアプリ再起動後も使えます。
            prefSetting.edit {
                putString("uri", uri.toString())
            }

            openDocumentPicker()
        }

        if(mIsCreateNew){
            createNew()
            mIsCreateNew = false
        }

        setTitles()
    }
*/
    private fun showInterStAd(){
        //インタースティシャル広告の読み込み
        if( BuildConfig.FLAVOR == "free") {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this,
                "ca-app-pub-3940256099942544/1033173712",
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdMob", "Ad was loaded.")
                        mInterstitialAd = interstitialAd
                    }
                })

            mInterstitialAd?.show(this)
        }

    }

    private fun viewPdf(contentUri: Uri){
        autoSavePDF()

        if ( contentUri != Uri.EMPTY ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION// or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED//flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            //intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/pdf")
            //intent.setPackage("com.adobe.reader")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                //if user doesn't have pdf reader instructing to download a pdf reader
            }
        }
    }

    private fun sendMail(){

        autoSaveCSV()
        autoSavePDF()
        //AutoSaveDXF()
        //AutoSaveSFC()

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        val contentUri = getAppLocalFile(this, "myLastTriList.pdf")
        val contentUri2 = getAppLocalFile(this, "myLastTriList.csv")
        //val contentUri3 = getAppLocalFile( this, "myLastTriList.dxf" )
        //val contentUri4 = getAppLocalFile( this, "myLastTriList.sfc" )

        val ar = ArrayList<Uri>()
        ar.add(contentUri)
        ar.add(contentUri2)
        //ar.add( contentUri3 )
        //ar.add( contentUri4 )

        intent.putExtra(Intent.EXTRA_STREAM, ar)

        intent.type = "message/rfc822"
        intent.setPackage("com.google.android.gm")

        startActivity(intent)
        return

    }

    private fun getAppLocalFile(context: Context, filename: String) :Uri {
        val newFile = File(context.filesDir, filename)
        return if (newFile.exists())
            getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)
        else Uri.EMPTY
    }

    private fun sendPdf(context: Context){
        autoSavePDF()

        val contentUri = getAppLocalFile(context, "myLastTriList.pdf")

        if ( contentUri != Uri.EMPTY ) {
           val intent = Intent(Intent.ACTION_SEND)
            intent.setDataAndType(contentUri, "application/pdf")
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                //if user doesn't have pdf reader instructing to download a pdf reader
            }
        }

        //showInterStAd()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveDXF(bWriter: BufferedWriter) :BufferedWriter{

        val writer = DxfFileWriter(
                myTriangleList
        )
        writer.rStr_ = rStr
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr
        writer.textscale_ = my_view.ts_ * 0.016f //25*0.014f=0.35f, 25/0.02f=0.5f

        writer.writer = bWriter
        writer.drawingLength = myTriangleList.measureMostLongLine()
        writer.dedlist_ = myDeductionList
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.isDebug = my_view.isDebug_

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = drawingNumberReversal

        writer.save()
        bWriter.close()

        return bWriter
    }

    private fun saveSFC(out: BufferedOutputStream) {

        val writer = SfcWriter(myTriangleList, myDeductionList, out, filename, drawingStartNumber)
        writer.setNames(koujiname, rosenname, gyousyaname, zumennum)
        writer.rStr_ = rStr
        writer.textscale_ = my_view.ts_ * 20f //25*14f=350f, 25/20f=500f
        writer.titleTri_ = titleTriStr
        writer.titleDed_ = titleDedStr

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = drawingNumberReversal

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

        writer.textscale_ = my_view.ts_ * 0.5f / writer.printScale_ //25*0.4f=10f, 25/0.3f=7.5f
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

    private fun autoSavePDF(){
        try {
            savePDF(openFileOutput("myLastTriList.pdf", MODE_PRIVATE))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun autoSaveCSV(){
        try {
            setTitles()

            val writer = BufferedWriter(
                    OutputStreamWriter(openFileOutput("myLastTriList.csv", MODE_PRIVATE))
            )
            saveCSV(writer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 広告の再表示
        //if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = VISIBLE

    }

    private fun resumeCSV(){
        StringBuilder()
        try {
            val reader = BufferedReader(
                    InputStreamReader(openFileInput("myLastTriList.csv"))
            )
            val ok = loadCSV(reader)

            if(!ok) createNew()

            Log.d( "FileLoader", "Resume CSV is:" + ok )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveCSV(writer: BufferedWriter){
        //myTriangleList.scale(PointXY(0f,0f),1/myTriangleList.getScale())
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()

        writer.write("koujiname, $koujiname")
        writer.newLine()
        writer.write("rosenname, $rosenname")
        writer.newLine()
        writer.write("gyousyaname, $gyousyaname")
        writer.newLine()
        writer.write("zumennum, $zumennum")
        writer.newLine()

        for (index in 1 .. myTriangleList.size()){
            val mt: Triangle = myTriangleList.getTriangle(index)
            val pt: PointXY = mt.pointNumber_
            val cp = parentBCtoCParam(mt.parentBC, mt.lengthAforce_, mt.cParam_)
            //if( mt.isPointNumberMoved_ == true ) pt.scale(PointXY(0f,0f),1f,-1f)
            writer.write(
                    mt.getMyNumber_().toString() + ", " +           //0
                            mt.getLengthA_().toString() + ", " +        //1
                            mt.getLengthB_().toString() + ", " +        //2
                            mt.getLengthC_().toString() + ", " +        //3
                            mt.parentNumber.toString() + ", " +   //4
                            mt.parentBC.toString() + ", " +       //5
                            mt.getMyName_() + ", " +                    //6
                            pt.x + ", " +             //7
                            pt.y + ", " +             //8
                            mt.isPointNumberMoved_ + ", " +             //9
                            mt.color_ + ", " +                          //10
                            mt.dimSideAlignA_ + ", " +                  //11
                            mt.dimSideAlignB_ + ", " +                  //12
                            mt.dimSideAlignC_ + ", " +                  //13
                            mt.myDimAlignA_ + ", " +                       //14
                            mt.myDimAlignB_ + ", " +                       //15
                            mt.myDimAlignC_ + ", " +                       //16
                            cp.side + ", " +                       //17
                            cp.type + ", " +                       //18
                            cp.lcr + ", " +                               //19
                            mt.isChangeDimAlignB_ + ", " +                //20
                            mt.isChangeDimAlignC_ + ", " +                //21
                            mt.angleInGlobal_ + ", " +              //22
                            mt.pointCA_.x + ", " +                  //23
                            mt.pointCA_.y + ", " +                  //24
                            mt.angleInLocal_               //25
            )
            writer.newLine()
        }

        writer.write("ListAngle, " + myTriangleList.angle)
        writer.newLine()
        writer.write("ListScale, " + myTriangleList.scale)
        writer.newLine()
        writer.write("TextSize, " + my_view.ts_)
        writer.newLine()

        for(index in 1 .. myDeductionList.size()){
            val dd: Deduction = myDeductionList.get(index)
            val pointAtRealscale = dd.point.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            val pointFlagAtRealscale = dd.pointFlag.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            dd.scale(PointXY(0f, 0f), 1f, -1f)
            writer.write(
                    "Deduction, " +              //0
                            dd.num.toString() + ", " +         //1
                            dd.name + ", " +                   //2
                            dd.lengthX.toString() + ", " +     //3
                            dd.lengthY.toString() + ", " +     //4
                            dd.parentNum.toString() + ", " +   //5
                            dd.type + ", " +        //6
                            dd.angle.toString() + ", " +       //7
                            pointAtRealscale.x.toString() + ", " +     //8
                            pointAtRealscale.y.toString() + ", " +     //9
                            pointFlagAtRealscale.x.toString() + ", " + //10
                            pointFlagAtRealscale.y.toString() + ", " + //11
                            dd.shapeAngle.toString()        //12
            )
            writer.newLine()
            dd.scale(PointXY(0f, 0f), 1f, -1f)
        }
        writer.close()
    }

    private fun loadCSV(reader: BufferedReader) :Boolean{
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
                    chunks[1]!!.toFloat(),
                    chunks[2]!!.toFloat(),
                    chunks[3]!!.toFloat(),
                    pointfirst,
                    anglefirst
            ),
            true
        )
        val mt = trilist.getTriangle(trilist.size())


        mt.setMyName_(chunks[6]!!.toString())
        if(chunks[9]!! == "true") mt.setPointNumberMoved_(
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
                    val pt = PointXY( 0f, 0f )
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

                    val ptri = trilist.getTriangle(chunks[4].toInt())
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
                trilist.getTriangle(trilist.size()).parentBC_ = chunks[5].toInt()
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
                                trilist.getTriangle(chunks[4].toInt()), ConnParam(
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

                val mT = trilist.getTriangle(trilist.size())
                mT.parentBC_ = chunks[5].toInt()
                /*
                if( chunks.size > 25 && mT.parentBC_ >= 9 ) mT.rotate(
                    mT.pointCA_,
                    chunks[25].toFloat(),
                    false
                )*/
                // trilist.getTriangle(trilist.size()).setCParamFromParentBC( chunks[5]!!.toInt() )
            }

            val mT = trilist.getTriangle(trilist.size())
            mT.setMyName_(chunks[6])
            if( trilist.size() > 1 ) trilist.get(trilist.size() - 1).childSide_ = chunks[5].toInt()

            if(chunks[9] == "true") mT.setPointNumberMoved_(
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
        trilistStored = myTriangleList.clone()
        myDeductionList = dedlist
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
        editorClear(myTriangleList, myTriangleList.size())
        //colorMovementFabs()
        flipDeductionMode()
        //printDebugConsole()
        return true
    }

    private fun typeToInt(type: String) :Int{
        var pl = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }

    private fun parentBCtoCParam(pbc: Int, lenA: Float, cp: ConnParam) : ConnParam{
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

    private fun setTitles(){
        rosenname = findViewById<EditText>(R.id.rosenname).text.toString()
        //findViewById<EditText>(R.id.rosenname).setText(rosenname)

        val dedArea = myDeductionList.getArea()
        val triArea = myTriangleList.getArea()
        val totalArea = roundByUnderTwo(triArea - dedArea).formattedString(2)
        title = rStr.menseki_ + ": ${ totalArea } m^2"

        if( myTriangleList.lastTapNumber_ > 0 ){
            val coloredArea = myTriangleList.getAreaC( myTriangleList.lastTapNumber_ )
            val colorStr = arrayOf( "red: ", "orange: ", "yellow: ", "green: ", "blue: " )
            val tapped = myTriangleList.get( myTriangleList.lastTapNumber_ )
            title = rStr.menseki_ + ": ${ totalArea } m^2" + " ( ${ colorStr[tapped.color_]+coloredArea } m^2 )"
        }

    }

    private fun roundByUnderTwo(fp: Float) :Float {
        val ip: Int = ( fp * 100f ).roundToInt()
        return ip * 0.01f
    }

    companion object {
        private const val REQUESTCODE = 816 // 2
        private val PERMISSIONS = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                //Manifest.permission.MANAGE_DOCUMENTS,
                Manifest.permission.INTERNET
        )
        private const val REQUESTPERMISSION = 1000
    }

} // end of class
