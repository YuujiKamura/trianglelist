package com.jpaver.trianglelist

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider.getUriForFile
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_first.*
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

    private val PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )
    private val REQUEST_PERMISSION = 1000

    private fun checkPermission() {
        if (isGranted()) {
            //setEvent()
        } else {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSION)
        }
    }

    private fun isGranted(): Boolean {
        for (i in 0 until PERMISSIONS.size) {
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
        if (requestCode == REQUEST_PERMISSION) {
            checkPermission()
        }
    }

    var TriLists_ = ArrayList<TriangleList>()

    lateinit var myELFirst: EditTextViewLine
    lateinit var myELSecond: EditTextViewLine
    lateinit var myELThird: EditTextViewLine

    val sNumberList = listOf(
        "No.0", "No.1", "No.2", "No.3", "No.4", "No.5", "No.6", "No.7", "No.8", "No.9",
        "No.10", "No.11", "No.12", "No.13", "No.14", "No.15", "No.16", "No.17", "No.18", "No.19",
        "No.20", "No.21", "No.22", "No.23", "No.24", "No.25", "No.26", "No.27", "No.28", "No.29"
    )
    val dedNameListC = listOf(
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
    val dedSizeListC = listOf(
        "0.23",
        "0.23",
        "0.23",
        "0.66",
        "0.70",
        "0.40",
        "0.40",
        "0.30",
        "0.55",
        "0.55",
        "1.0",
        "1.0"
    )
    val dedNameListB = listOf("消火栓B", "基礎", "側溝", "集水桝")

   // val dedmap = MapOf(dedNameList to )

    var myEditor: EditorTable = EditorTable()
    var dParams_: Params = Params("", "", 0, 0f, 0f, 0f, 0, 0, PointXY(0f, 0f))
    var lastParams_: Params = dParams_

    // タイトルパラメータ、stringリソースから構成する

    lateinit var str : String// = getString( R.string.tenkai_koujimei )//this.getString( R.string.tenkai_title )

    lateinit var rStr_ : ResStr
    lateinit var titleTri_: TitleParams//
    lateinit var titleDed_: TitleParams
    lateinit var titleTriStr_ : TitleParamStr
    lateinit var titleDedStr_ : TitleParamStr

    lateinit var myTriangleList: TriangleList //= TriangleList(Triangle(0f,0f,0f,PointXY(0f,0f),180f))
    lateinit var myDeductionList: DeductionList

    var trilistStored_: TriangleList = TriangleList()

    var watcherCount_ = 0

    var fileType: String = "notyet"
    var filename_ = "notyet"
    var deductionMode_: Boolean = false
    var mIsCreateNew: Boolean = false
    val onetohandred_ = 11.9f
    val experience_ = 4f
    val mScale = onetohandred_*experience_

    var koujiname_ = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"
    var drawingStartNumber_ = 1
    var drawingNumberReversal_ = false

    var colorindex_ = 4
    val RColors = arrayOf(
        R.color.colorPink,   //0
        R.color.colorOrange, //1
        R.color.colorYellow, //2
        R.color.colorLime,   //3
        R.color.colorSky     //4
    )

    fun flipDeductionMode(dmode: Boolean){
        myDeductionList.setCurrent(myDeductionList.size())
        myTriangleList.setCurrent(myTriangleList.size())
        //printDebugConsole()
        colorMovementFabs()

        var inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if(deductionMode_ == false) {
            deductionMode_ = true
            Toast.makeText(this, "Edit Mode : Area Deductions", Toast.LENGTH_LONG).show()

            // 入力テーブルの見かけの変更、タイトル行の文字列とカラー
            myEditor.setHeaderTable(
                findViewById<TextView>(R.id.TV_NUM),
                findViewById<TextView>(R.id.TV_Name),
                findViewById<TextView>(R.id.TV_A),
                findViewById<TextView>(R.id.TV_B),
                findViewById<TextView>(R.id.TV_C),
                findViewById<TextView>(R.id.TV_PN),
                findViewById<TextView>(R.id.TV_PL),
                titleDed_
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
            deductionMode_ = false
            Toast.makeText(this, "Edit Mode : Triangles", Toast.LENGTH_LONG).show()
            // 入力テーブルの見かけの変更、タイトル行の文字列とカラー
            myEditor.setHeaderTable(
                findViewById<TextView>(R.id.TV_NUM),
                findViewById<TextView>(R.id.TV_Name),
                findViewById<TextView>(R.id.TV_A),
                findViewById<TextView>(R.id.TV_B),
                findViewById<TextView>(R.id.TV_C),
                findViewById<TextView>(R.id.TV_PN),
                findViewById<TextView>(R.id.TV_PL),
                titleTri_
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
        EditorClear(getList(deductionMode_), getList(deductionMode_).size())
        //inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0)

    }

    fun EditorClear(elist: EditList, currentNum: Int){
        loadEditTable()
        my_view.setParentSide(elist.size(), 0)
        myEditor.LineRewrite(
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
        myEditor.LineRewrite(elist.get(currentNum).getParams(), myELSecond)
        if(currentNum > 1) myEditor.LineRewrite(elist.get(currentNum - 1).getParams(), myELThird)
        if(currentNum == 1) myEditor.LineRewrite(
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

    fun loadEditTable(){
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

    fun validDeduction(dp: Params): Boolean {
        if( dp.name == "" || dp.a < 0.1f ) return false
        if( dp.type == "Box" && ( dp.a < 0.1f || dp.b < 0.1f ) ) return false
        return true
    }

    fun getList(dMode: Boolean) :EditList{
        if(dMode == true) return myDeductionList
        else return myTriangleList
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //val col = my_view.collision(event!!)
       // if(col.isHit == true) autoConnection(col.side)
        return super.onTouchEvent(event)
    }

    lateinit var mAdView : AdView
    lateinit var mInterstitialAd : InterstitialAd
    private val isAdTEST_ = true
    private val isAdVisible_ = true
    private val TestAdID_ = "ca-app-pub-3940256099942544/6300978111"
    private val UnitAdID_ = "ca-app-pub-6982449551349060/2369695624"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_NoActionBar)
        setContentView(R.layout.activity_main)


        if( BuildConfig.FLAVOR == "free" ){
            // must after setContentView
            MobileAds.initialize(this) {}
            mAdView = findViewById(R.id.adView)
            //mAdView.adSize = AdSize.BANNER

            if (isAdTEST_ == false) {
                // 本番
                //mAdView.adUnitId = UnitAdID_
            } else {
                // Test Mode
                //mAdView.adUnitId = TestAdID_
            }

            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)

        }

        setSupportActionBar(toolbar)
        myDeductionList = DeductionList()
        //Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()


        fab_replace.setOnClickListener { view ->
            fabReplace(dParams_, false)
        }

        fab_flag.setOnClickListener { view ->
            dParams_ = myEditor.ReadLineTo(dParams_, myELSecond)// 200703 // if式の中に入っていると当然ながら更新されない時があるので注意

            var resetPoint: PointXY = PointXY(0f, 0f)

            if(deductionMode_ == true){
                dParams_.pts = my_view.getTapPoint()
                dParams_.pt = myDeductionList.get(dParams_.n).point
                var ded = myDeductionList.get(dParams_.n)
                val tp = my_view.getTapPoint().scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
                resetPoint = tp
                if( validDeduction(dParams_) == true ) {// あまり遠い時はスルー
                    myDeductionList.replace(dParams_.n, dParams_)
//                    EditorReset(getList(myDeductionMode),getList(myDeductionMode).length())
                    my_view.setDeductionList(myDeductionList, mScale)
                }
            }
            else{
                var tri = myTriangleList.get(dParams_.n)
                var tp = my_view.getTapPoint().scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
                if( tp.lengthTo(tri.pointCenter_) < 10f ){ // あまり遠い時はスルー
                    tri.pointNumber_ = tp
                    tri.isPointNumberMoved_ = true
                    my_view.setTriangleList(myTriangleList, mScale)
                }
            }
            resetPoint = my_view.getTapPoint().scale(PointXY(0f, 0f), 1f, -1f)

            //my_view.invalidate()
            my_view.resetView(resetPoint)

            if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(
                this,
                " Test Done.",
                Toast.LENGTH_SHORT
            ).show()

            AutoSaveCSV()
        }

        fab_dimsidew.setOnClickListener { view ->
            if(!deductionMode_){
                var dimside = my_view.myTriangleList.lastTapSide_
                var trinum  = my_view.myTriangleList.lastTapNum_
                var tri = myTriangleList.get(trinum)

                if( dimside == 0 && ( tri.parentBC_ == 1 ||  tri.parentBC_ == 2 ) ) {
                    trinum = tri.parentNumber_
                    dimside = tri.parentBC_
                }

                myTriangleList.get(trinum).rotateDimSideAlign(dimside)
                my_view.myTriangleList.get(trinum).rotateDimSideAlign(dimside)//setTriangleList(myTriangleList, mScale)
                my_view.invalidate()
                AutoSaveCSV()

            }
        }

        fab_dimsideh.setOnClickListener { view ->
            if(!deductionMode_){
                var dimside = my_view.myTriangleList.lastTapSide_
                var trinum  = my_view.myTriangleList.lastTapNum_
                var tri = myTriangleList.get(trinum)

                if( dimside == 0 && ( tri.parentBC_ == 1 ||  tri.parentBC_ == 2 ) ) {
                    trinum = tri.parentNumber_
                    dimside = tri.parentBC_
                }

                myTriangleList.get(trinum).flipDimAlignH(dimside)
                my_view.myTriangleList.get(trinum).flipDimAlignH(dimside)//setTriangleList(myTriangleList, mScale)
                my_view.invalidate()
                AutoSaveCSV()

            }
        }

        fab_nijyuualign.setOnClickListener { view ->
            if(!deductionMode_ && myTriangleList.lastTapNum_ > 1 ){
                myTriangleList.rotateCurrentTriLCR()
                //myTriangleList.resetTriConnection(myTriangleList.lastTapNum_, );
                my_view.setTriangleList(myTriangleList, mScale)
                my_view.resetView(my_view.lstp())

                AutoSaveCSV()
            }
        }

        var deleteWarning: Int = 0
        fab_minus.setOnClickListener { view ->
            val len = getList(deductionMode_).size()

            if(len > 0 && deleteWarning == 0) {
                deleteWarning = 1
                fab_minus.backgroundTintList = getColorStateList(R.color.colorTT2)

            }
            else {
                if (len > 0) {
                    trilistStored_ = myTriangleList.clone()

                    getList(deductionMode_).remove(len)
                    //my_view.removeTriangle()
                    my_view.setDeductionList(myDeductionList, mScale)
                    my_view.setTriangleList(myTriangleList, mScale)

                    EditorClear(getList(deductionMode_), getList(deductionMode_).size())
                }
                deleteWarning = 0
                fab_minus.backgroundTintList = getColorStateList(R.color.colorAccent)
            }
            printDebugConsole()
            colorMovementFabs()
            my_view.resetViewToLSTP()
            setTitles()

        }

        fab_undo.setOnClickListener{ view ->
            if( trilistStored_.size() > 0 ){
                myTriangleList = trilistStored_.clone()
                my_view.undo()
                my_view.resetViewToLSTP()

                trilistStored_.trilist_.clear()

                fab_undo.backgroundTintList = getColorStateList(R.color.colorPrimary)
                EditorClear(getList(deductionMode_), getList(deductionMode_).getCurrent())
                setTitles()
            }
        }

        fab_fillcolor.setOnClickListener { view ->
            if(!deductionMode_){
                colorindex_ ++
                if(colorindex_ == RColors.size) colorindex_ = 0
                fab_fillcolor.backgroundTintList = getColorStateList(RColors.get(colorindex_))

                //dParams_ = myEditor.ReadLine(dParams_, myELSecond)
                myTriangleList.get(my_view.myTriangleList.current).color_ = colorindex_

                my_view.setFillColor(colorindex_, myTriangleList.current)
                AutoSaveCSV()
            }
        }

        fab_texplus.setOnClickListener { view ->
            my_view.ts_ += 5f
            my_view.setAllTextSize(my_view.ts_)

//            my_view.paintTexS.textSize = my_view.ts_
            my_view.invalidate()
        }

        fab_texminus.setOnClickListener { view ->
            my_view.ts_ -= 5f
            my_view.setAllTextSize(my_view.ts_)

            my_view.invalidate()
        }

        fab_setB.setOnClickListener { view ->
            autoConnection(1)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        fab_setC.setOnClickListener { view ->
            autoConnection(2)
            findViewById<EditText>(R.id.editLengthB1).requestFocus()
        }

        var angle: Float = 0f
        fab_rot_l.setOnClickListener { view ->
            if( deductionMode_ == false ) {
                myTriangleList.rotate(PointXY(0f, 0f), 5f)
                myDeductionList.rotate(PointXY(0f, 0f), -5f)
                my_view.setTriangleList(myTriangleList, mScale)
                my_view.setDeductionList(myDeductionList, mScale)
                my_view.resetViewToLSTP()
                printDebugConsole()
            }
            // ded rotate
            else {
                val vdltip = my_view.myDeductionList.lastTapIndex_+1
                myDeductionList.get(vdltip).rotateShape(myDeductionList.get(vdltip).point, -5f)
                my_view.setDeductionList(myDeductionList, mScale)
                my_view.invalidate()
            }
            AutoSaveCSV()
        }

        fab_rot_r.setOnClickListener { view ->
            if( deductionMode_ == false ) {
                myTriangleList.rotate(PointXY(0f, 0f), -5f)
                myDeductionList.rotate(PointXY(0f, 0f), 5f)
                my_view.setTriangleList(myTriangleList, mScale)
                my_view.setDeductionList(myDeductionList, mScale)
                my_view.resetViewToLSTP()
                printDebugConsole()
                AutoSaveCSV()
            }
            // ded rotate
            else {
                val vdltip = my_view.myDeductionList.lastTapIndex_+1
                myDeductionList.get(vdltip).rotateShape(myDeductionList.get(vdltip).point, 5f)
                my_view.setDeductionList(myDeductionList, mScale)
                my_view.invalidate()
            }
            AutoSaveCSV()
        }

        fab_deduction.setOnClickListener { view ->
            deleteWarning = 0
            fab_minus.backgroundTintList = getColorStateList(R.color.colorAccent)
            flipDeductionMode(deductionMode_)
            colorMovementFabs()
        }

        fab_resetView.setOnClickListener { view ->

            if(deductionMode_ == false ) my_view.resetViewToLSTP()
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

        fab_up.setOnClickListener { view ->
            myEditor.scroll(-1, getList(deductionMode_), myELSecond, myELThird)

            if(deductionMode_ == false ) moveTrilist()
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

        fab_down.setOnClickListener { view ->
            myEditor.scroll(1, getList(deductionMode_), myELSecond, myELThird)

            if(deductionMode_ == false ) moveTrilist()
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

        fab_debug.setOnClickListener { view ->
            //my_view.isDebug_ = !my_view.isDebug_

            if( my_view.isAreaOff_ == false ){
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

        fab_testbasic.setOnClickListener { view ->
            //CreateNew()

            findViewById<TextView>(R.id.editLengthB1).text = "" // reset
            fabReplace(Params("", "", 1, 7f, 7f, 7f, 0, 0), true)
            findViewById<TextView>(R.id.editLengthB1).text = "6f" // add
            fabReplace(Params("", "", 2, 7f, 6f, 6f, 1, 2), true)

            findViewById<TextView>(R.id.editLengthA1).text = "0.23f" // add
            deductionMode_ = true
            fabReplace(
                Params(
                    "仕切弁", "Circle", 1, 0.23f, 0f, 0f, 1, 0, PointXY(1f, 0f), PointXY(
                        0f,
                        0f
                    )
                ), true
            )
            deductionMode_ = false


            Toast.makeText(this, "Basic Senario Test Done.", Toast.LENGTH_SHORT).show()
        }

        fab_pdf.setOnClickListener { view ->
            ViewPdf(this)
        }

        fab_share.setOnClickListener { view ->
            sendPdf(this)
        }

        fab_mail.setOnClickListener { view ->
            //findViewById<ProgressBar>(R.id.indeterminateBar).visibility = View.VISIBLE
            //progressBar.visibility = View.VISIBLE

            sendMail()

        //    progressBar.visibility = View.INVISIBLE

        }

        fab_numreverse.setOnClickListener{ view ->
            trilistStored_ = myTriangleList.clone()

            myTriangleList = myTriangleList.reverse()
            my_view.setTriangleList(myTriangleList, mScale)
            my_view.resetView(my_view.lstp())
            EditorClear(myTriangleList, myTriangleList.current)
        }
    }

    fun fabReplace(params: Params, useit: Boolean){
        val editor = myEditor
        val dedmode = deductionMode_
        val editlist = getList(deductionMode_)

        var readedFirst  = Params()
        var readedSecond = Params()
        myEditor.ReadLineTo(readedFirst, myELFirst)
        myEditor.ReadLineTo(readedSecond, myELSecond)
        if( useit == true ){
            readedFirst = params
            readedSecond = params
        }
        val strTopA = findViewById<TextView>(R.id.editLengthA1).text.toString()
        val strTopB = findViewById<TextView>(R.id.editLengthB1).text.toString()
        val strTopC = findViewById<TextView>(R.id.editLengthC1).text.toString()

        var usedDedPoint = params.pt.clone()

        var isSucceed = false

        if( dedmode == false ) {

            if( strTopB == "" ) isSucceed = resetTrianglesBy(readedSecond)
            else
                if( strTopC == "" && useit == false ) return
                else isSucceed = addTriangleBy(readedFirst)

        } else { // if in deduction mode
            //if (validDeduction(params) == false) return


            if( strTopA == "" ) {
                isSucceed = resetDeductionsBy(readedSecond)
                usedDedPoint = my_view.myDeductionList.get(readedSecond.n).point
            }
            else{
                isSucceed = addDeductionBy(readedFirst)
                usedDedPoint = my_view.myDeductionList.get(readedFirst.n).point
            }
            findViewById<EditText>(R.id.editName1).requestFocus()
        }

        EditorClear(editlist, editlist.getCurrent())
        my_view.setTriangleList(myTriangleList, mScale)
        my_view.setDeductionList(myDeductionList, mScale)
        printDebugConsole()
        AutoSaveCSV()
        setTitles()
        if( dedmode == false ) my_view.resetView(my_view.lstp())
        if( dedmode == true  ) my_view.resetView(usedDedPoint.scale(PointXY(0f, 0f), 1f, -1f))//resetViewToTP()

        my_view.myTriangleList.isDoubleTap_ = false
        my_view.myTriangleList.lastTapSide_ = 0
        /*if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(
                this,
                isSucceed.toString(),
                Toast.LENGTH_SHORT
        ).show()*/
    }

    fun moveTrilist(){
        my_view.getTriangleList().setCurrent(myTriangleList.getCurrent())
        my_view.myTriangleList.lastTapNum_ = myTriangleList.getCurrent()
        myTriangleList.lastTapNum_ = myTriangleList.getCurrent()
        my_view.resetViewToLSTP()
    }

    fun addDeductionBy(params: Params) : Boolean {
        params.pt = my_view.getTapPoint()
        params.pts = params.pt //PointXY(0f, 0f)
        params.pn = my_view.myTriangleList.isCollide(dParams_.pt.scale(PointXY(1f, -1f)))

        //形状の自動判定
        if( params.b > 0f ) params.type = "Box"
        else params.type = "Circle"

        if (validDeduction(params) == true) {
            // 所属する三角形の判定処理
            if( params.pt != PointXY(0f, 0f) ) {
                params.pn = my_view.myTriangleList.isCollide( params.pt.scale( PointXY(1f, -1f ) ) )
            }

            myDeductionList.add(params)
            my_view.setDeductionList(myDeductionList, mScale)
            lastParams_ = params
            return true
        }
        else return false
    }

    fun addTriangleBy(params: Params) : Boolean {
        if (validTriangle(params)) {
            trilistStored_ = myTriangleList.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            var myTri: Triangle = Triangle(
                myTriangleList.getTriangle(params.pn),
                params
            )
            myTri.myNumber_ = params.n
            myTriangleList.add(myTri)
            findViewById<EditText>(R.id.editLengthA1).requestFocus()
            myTriangleList.lastTapNum_ = myTriangleList.size()
            //my_view.resetView()
            return true
        }
        return false
    }

    fun resetTrianglesBy(params: Params) : Boolean {

        if (validTriangle(params) == true){
            trilistStored_ = myTriangleList.clone()
            fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            return myTriangleList.resetConnectedTriangles(params)
        } // if valid triangle
        else return false
    }

    fun resetDeductionsBy(params: Params) : Boolean {
        val prms = params
        //myEditor.ReadLineTo(prms, myELSecond)
        prms.pt = my_view.getTapPoint()
        prms.pts = myDeductionList.get(prms.n).pointFlag

        myTriangleList.current = prms.pn

        if( validDeduction(prms) == true ) {
            // 所属する三角形の判定処理
            if( prms.pt != PointXY(0f, 0f) ) {
                prms.pn = my_view.myTriangleList.isCollide( prms.pt.scale( PointXY(1f, -1f ) ) )
            }

            myDeductionList.replace(prms.n, prms)
            return true
        }
        else return false
    }

    fun autoConnection(i: Int){
        // 広告の再表示
        //if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = INVISIBLE

        my_view.myTriangleList.lastTapSide_ = i
        dParams_ = myEditor.ReadLineTo(dParams_, myELFirst) //keep them
        var focusTo = findViewById<EditText>(R.id.editLengthB1)


        if( my_view.myTriangleList.isDoubleTap_ == true ){
            if(i == 1) focusTo = findViewById<EditText>(R.id.editLengthB2)
            if(i == 2) focusTo = findViewById<EditText>(R.id.editLengthC2)
        }

        if(deductionMode_ == false) {
            my_view.watchedB_ = findViewById<EditText>(R.id.editLengthB1).text.toString()
            my_view.watchedC_ = findViewById<EditText>(R.id.editLengthC1).text.toString()

            var t:Triangle = myTriangleList.get(dParams_.pn)
            myEditor.LineRewrite(
                Params(
                    dParams_.name,
                    "",
                    myTriangleList.size() + 1,
                    t.getLengthByIndex(i),
                    dParams_.b,
                    dParams_.c,
                    t.getMyNumber_(),
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
                var inputMethodManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(focusTo, 0)
            }
        }
        else{
            setFabSetBC(i)
            myEditor.LineRewrite(
                Params(
                    dParams_.name,
                    "",
                    myDeductionList.size() + 1,
                    dParams_.a,
                    dParams_.b,
                    dParams_.c,
                    dParams_.pn,
                    i,
                    PointXY(
                        0f,
                        0f
                    )
                ), myELFirst
            )
        }
    }

    fun setFabSetBC(i: Int){
        if(i == 1) {
            fab_setB.backgroundTintList = getColorStateList(R.color.colorAccent)

        }
    }

    fun setTargetEditText(str: String){
        var inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


        if(deductionMode_ == true ){
            my_view.myDeductionList.setScale(my_view.myScale)
            my_view.myDeductionList.getTapIndex(my_view.localPressPoint)

            if ( my_view.myDeductionList.lastTapIndex_ > -1 ) {
                val tapIndex = my_view.myDeductionList.lastTapIndex_+1
                if( -1 < tapIndex ) {
                    //Toast.makeText(this, "deduction tap", Toast.LENGTH_SHORT).show()
                    myEditor.scroll(
                        tapIndex - myDeductionList.getCurrent(),
                        getList(deductionMode_), myELSecond, myELThird
                    )
                    //my_view.resetView( my_view.myDeductionList.get( tapIndex ).point.scale( PointXY(1f,-1f ) ) )
                }
            }

            // 三角形番号が押されたときはセンタリング
            my_view.myTriangleList.getTap(my_view.localPressPoint.scale(PointXY(0f, 0f), 1f, -1f))
            if ( my_view.myTriangleList.lastTapNum_ != 0 ) {
                if( my_view.myTriangleList.lastTapSide_ == 3 ) my_view.resetViewToLSTP()
            }

        }
        else {
            val lpp = my_view.localPressPoint.scale(PointXY(0f, 0f), 1f, -1f)

            val slpp = my_view.shadowTri_.getTapLength( lpp )
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

            my_view.myTriangleList.getTap( lpp )

            if ( my_view.myTriangleList.lastTapNum_ != 0 ) {
                //Toast.makeText(this, "Triangle tap", Toast.LENGTH_SHORT).show()
                myEditor.scroll(
                    my_view.getTriangleList().lastTapNum_ - my_view.getTriangleList().getCurrent(),
                    getList(deductionMode_), myELSecond, myELThird
                )

                my_view.getTriangleList().setCurrent(my_view.getTriangleList().lastTapNum_)
                myTriangleList.setCurrent(my_view.getTriangleList().lastTapNum_)
                myTriangleList.lastTapNum_ = my_view.getTriangleList().lastTapNum_
                myTriangleList.lastTapSide_ = my_view.getTriangleList().lastTapSide_
                findViewById<EditText>(R.id.editParentNumber1).setText(myTriangleList.lastTapNum_.toString())
                findViewById<EditText>(R.id.editNumber1).setText(myTriangleList.size().toString())

                colorindex_ = myTriangleList.get(myTriangleList.lastTapNum_).color_
                colorMovementFabs()
                printDebugConsole()
                setTitles()
                if( my_view.myTriangleList.lastTapSide_ == 0 ) {

                    findViewById<EditText>(R.id.editLengthA2).requestFocus()
                    findViewById<EditText>(R.id.editLengthA2).setSelection(findViewById<EditText>(R.id.editLengthA2).text.length)
                    inputMethodManager.showSoftInput(findViewById(R.id.editLengthA2), 0)
                    my_view.setParentSide(my_view.getTriangleList().lastTapNum_, 3)
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

                if( my_view.myTriangleList.lastTapSide_ == 3 ) my_view.resetViewToLSTP()
            }
        }
    }

    fun printDebugConsole(){
        val tvd: TextView = findViewById(R.id.debugconsole)
        //面積(控除なし): ${myTriangleList.getArea()}㎡　(控除あり):${myTriangleList.getArea()-myDeductionList.getArea()}㎡
        tvd.text = """ myView.Center: ${my_view.myTriangleList.center.x} ${my_view.myTriangleList.center.y}
                        |TriCurrent: ${my_view.getTriangleList().getCurrent()} T1.color ${
            my_view.getTriangleList().get(
                1
            ).color_
        } ${myTriangleList.get(1).color_} 
                        |TapTL: ${my_view.tapTL_} , lastTapNum: ${my_view.getTriangleList().lastTapNum_}, lastTapSide: ${my_view.getTriangleList().lastTapSide_}                                 
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
            """.trimMargin()
    }

    fun colorMovementFabs() : Int{
        val max: Int = getList(deductionMode_).size()
        val current: Int = getList(deductionMode_).getCurrent()
        val min: Int = 1
        var movable: Int = 0
        //fab_zoomin.setBackgroundTintList(getColorStateList(R.color.colorSky))
        //fab_zoomout.setBackgroundTintList(getColorStateList(R.color.colorSky))
        fab_resetView.backgroundTintList = getColorStateList(R.color.colorSky)
        //色
        fab_fillcolor.backgroundTintList = getColorStateList(RColors.get(colorindex_))

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

    fun setEditNameAdapter(namelist: List<String>){
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
        textView.addTextChangedListener(MyTextWatcher(myELFirst, myEditor, lastParams_))

    }

    private class MyTextWatcher(
        val mELine: EditTextViewLine,
        val myEditor: EditorTable,
        var lastParams: Params
    ) : TextWatcher {
        //private val afterTextChanged_: TextView = findViewById<TextView>(R.id.afterTextChanged)
        //private val beforeTextChanged_: TextView = findViewById<TextView>(R.id.beforeTextChanged)
        //private val onTextChanged_: TextView = findViewById<TextView>(R.id.onTextChanged)
        override fun afterTextChanged(s: Editable) {
            val input = mELine.name.text.toString()
//            myEditor.LineRewrite(Params(input,"",myDeductionList.length()+1,dP.a, dP.b, dP.c, dP.pn, i, PointXY(0f,0f)), myELFirst)

            if(input == "仕切弁" || input == "ソフト弁" || input == "ドレーン") {
                mELine.a.setText("0.23")
                mELine.b.setText("")
                mELine.pl.setSelection(2)
            }
            if(input == "消火栓" || input == "空気弁") {
                mELine.a.setText("0.55")
                mELine.b.setText("")
                mELine.pl.setSelection(2)
            }
            if(input == "下水") {
                mELine.a.setText("0.72")
                mELine.b.setText("")
                mELine.pl.setSelection(2)
            }
            if(input == "汚水") {
                mELine.a.setText("0.67")
                mELine.b.setText("")
                mELine.pl.setSelection(2)
            }
            if(input == "雨水枡" || input == "電柱"){
                mELine.a.setText("0.40")
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
                mELine.a.setText("0.35")
                mELine.b.setText("0.45")
                mELine.pl.setSelection(1)
            }
            if(input == "基礎") {
                mELine.a.setText("0.50")
                mELine.b.setText("0.50")
                mELine.pl.setSelection(1)
            }
            if(input == "集水桝") {
                mELine.a.setText("0.70")
                mELine.b.setText("0.70")
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
            val input = ("start=" + start
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
            val input = ("start=" + start
                    + ", before=" + before
                    + ", count=" + count
                    + ", s=" + s.toString())
            //onTextChanged_.text = input
        }
    }

    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        rStr_ = ResStr(
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
        titleTri_ = TitleParams(
            R.string.menseki,
            R.string.editor_number,
            R.string.editor_sokuten,
            R.string.editor_lA,
            R.string.editor_lB,
            R.string.editor_lC,
            R.string.editor_parent,
            R.string.editor_setuzoku
        )
        titleDed_ = TitleParams(
            R.string.menseki,
            R.string.editor_number,
            R.string.editor_name,
            R.string.editor_lA,
            R.string.editor_lB,
            R.string.editor_lC,
            R.string.editor_syozoku,
            R.string.editor_form
        )
        titleTriStr_ = TitleParamStr(
            getString(titleTri_.type), getString(titleTri_.n), getString(
                titleTri_.name
            ), getString(titleTri_.a), getString(titleTri_.b), getString(titleTri_.c), getString(
                titleTri_.pn
            ), getString(titleTri_.pl)
        )
        titleDedStr_ = TitleParamStr(
            getString(titleDed_.type), getString(titleDed_.n), getString(
                titleDed_.name
            ), getString(titleDed_.a), getString(titleDed_.b), getString(titleDed_.c), getString(
                titleDed_.pn
            ), getString(titleDed_.pl)
        )

        val filepath = this.filesDir.absolutePath + "/" + "myLastTriList.csv"
        val file: File = File(filepath)
        if(file.exists() == true) ResumeCSV()
        else                      CreateNew()
        loadEditTable()
        colorMovementFabs()
        //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
        fab_replace.backgroundTintList = getColorStateList(R.color.colorLime)
        setEditNameAdapter(sNumberList)

        checkPermission()

        // リスナーを登録
        var etB1 = findViewById<EditText>(R.id.editLengthB1)
        etB1.addTextChangedListener(object: CustomTextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if( etB1.isFocused == true ) my_view.watchedB_ = p0.toString()
                my_view.invalidate()
            }
        })
        val etC1 = findViewById<EditText>(R.id.editLengthC1)
        etC1.addTextChangedListener(object: CustomTextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if( etC1.isFocused == true ) my_view.watchedC_ = p0.toString()
                my_view.invalidate()
            }
        })

        Log.d("OnAttachedToWindow", "Process Done.")

    }

    fun rotateColor(index: Int): Int{
        return R.color.colorPink
    }

    fun CreateNew(){
        val tri = Triangle(5f, 5f, 5f, PointXY(0f, 0f), 0f)
        val trilist = TriangleList(tri)
        myTriangleList = trilist
        myDeductionList.clear()

        // メニューバーのタイトル
        koujiname_ = ""
        rosenname_ = rStr_.eRName_
        gyousyaname_ =""
        setTitles()

        my_view.setTriangleList(trilist, mScale)
        my_view.setDeductionList(myDeductionList, mScale)
        my_view.myTriangleList.lastTapNum_ = my_view.myTriangleList.size()
        my_view.resetViewToLSTP()

        fab_fillcolor.backgroundTintList = getColorStateList(RColors.get(colorindex_))

        printDebugConsole()
        EditorClear(getList(deductionMode_), getList(deductionMode_).size())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
  //      CreateNew()
  //      loadEditTable()
   //     EditorReset(getList(myDeductionMode),getList(myDeductionMode).length())

    }

    override fun onRestart() {
        super.onRestart()

        // 広告の非表示
        if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 上部のOptionsMenuの表示　Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDialogPositiveClick(dialog: androidx.fragment.app.DialogFragment?) {
        mIsCreateNew = true
        fileType = "CSV"
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.type = "text/csv"
        i.putExtra(Intent.EXTRA_TITLE, rosenname_ + " " + LocalDate.now() + ".csv")
        startActivityForResult(i, 1)
        setResult(RESULT_OK, i)

    }

    override fun onDialogNegativeClick(dialog: androidx.fragment.app.DialogFragment?) {
        EditorClear(getList(deductionMode_), getList(deductionMode_).size())
        CreateNew()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //インタースティシャル広告の読み込み
        if( BuildConfig.FLAVOR == "free"){
            mInterstitialAd = InterstitialAd(this)
            if( BuildConfig.BUILD_TYPE == "debug" ) mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
            else if( BuildConfig.BUILD_TYPE == "release" ) mInterstitialAd.adUnitId = "ca-app-pub-6982449551349060/2369695624"
            mInterstitialAd.loadAd(AdRequest.Builder().build())
        }

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_new -> {
                var dialog: MyDialogFragment = MyDialogFragment()
                dialog.show(supportFragmentManager, "dialog.basic")
                return true
            }
            R.id.action_save_csv -> {
                fileType = "CSV"
                var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "text/csv"
                i.putExtra(
                    Intent.EXTRA_TITLE,
                    rosenname_ + " " + LocalDate.now().toString() + ".csv"
                )
                startActivityForResult(i, 1)
                setResult(RESULT_OK, i)
                //finish()

                return true
            }
            R.id.action_load_csv -> {
                checkPermission()
                var i: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "text/csv"
                i.putExtra(Intent.EXTRA_TITLE, ".csv")
                startActivityForResult(i, 2)

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
                editText.hint = hCname + " " + hSpace
                val filter = arrayOf(InputFilter.LengthFilter(50))
                editText.filters = filter
                editText.setText(koujiname_)
                val editText2 = EditText(this)
                editText2.hint = hRname
                rosenname_ = findViewById<EditText>(R.id.rosenname).text.toString()
                editText2.setText(rosenname_)
                editText2.filters = filter
                val editText3 = EditText(this)
                editText3.hint = hAname
                editText3.setText(gyousyaname_)
                editText3.filters = filter
                val editText4 = EditText(this)
                editText4.hint = hRnum
                editText4.setText(zumennum_)
                editText4.filters = filter

                AlertDialog.Builder(this)
                    .setTitle("Save PDF")
                    .setMessage(R.string.inputcname)
                    .setView(editText)
                    .setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialog, which ->
                            koujiname_ = editText.text.toString()

                            AlertDialog.Builder(this)
                                .setTitle("Save PDF")
                                .setMessage(R.string.inputdname)
                                .setView(editText2)
                                .setPositiveButton("OK",
                                    DialogInterface.OnClickListener { dialog, which ->
                                        rosenname_ = editText2.text.toString()

                                        AlertDialog.Builder(this)
                                            .setTitle("Save PDF")
                                            .setMessage(R.string.inputaname)
                                            .setView(editText3)
                                            .setPositiveButton("OK",
                                                DialogInterface.OnClickListener { dialog, which ->
                                                    gyousyaname_ =
                                                        editText3.text.toString()

                                                    AlertDialog.Builder(this)
                                                        .setTitle("Save PDF")
                                                        .setMessage(R.string.inputdnum)
                                                        .setView(
                                                            editText4
                                                        )
                                                        .setPositiveButton(
                                                            "OK",
                                                            DialogInterface.OnClickListener { dialog, which ->
                                                                zumennum_ =
                                                                    editText4.text.toString()

                                                                fileType =
                                                                    "PDF"
                                                                var i: Intent =
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
                                                                            rosenname_ + "_" + LocalDate.now() + ".pdf"
                                                                        )

                                                                    }
                                                                startActivityForResult(
                                                                    i,
                                                                    1
                                                                )

                                                            })
                                                        .show()
                                                })
                                            .show()
                                    })
                                .show()
                        })
                    .show()

                return true
            }

            R.id.action_send_mail -> {
                sendMail()
                return true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showExportDialog(fileprefix: String, title: String, filetype: String, intentType: String): Boolean{
        val hTstart = getString(R.string.inputtnum)
        val editText5 = EditText(this)
        editText5.hint = hTstart
        val filter2 = arrayOf(InputFilter.LengthFilter(3))
        editText5.filters = filter2
        editText5.setText(drawingStartNumber_.toString())

        filename_ = rosenname_ + " " + LocalDate.now() + fileprefix
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.inputtnum)
            .setView(editText5)
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, which ->
                    drawingStartNumber_ = editText5.text.toString().toInt()
                    drawingNumberReversal_ = false

                    fileType = filetype
                    var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    i.type = intentType
                    i.putExtra(
                        Intent.EXTRA_TITLE,
                        rosenname_ + " " + LocalDate.now() + fileprefix
                    )
                    startActivityForResult(i, 1)
                }
            )
            .setNegativeButton("NumReverse",
                DialogInterface.OnClickListener { dialog, which ->
                    drawingStartNumber_ = editText5.text.toString().toInt()
                    drawingNumberReversal_ = true

                    fileType = filetype
                    var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    i.type = intentType
                    i.putExtra(
                        Intent.EXTRA_TITLE,
                        rosenname_ + " " + LocalDate.now() + fileprefix
                    )
                    startActivityForResult(i, 1)
                }
            ).show()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data?.data == NULL || resultCode == RESULT_CANCELED) return
        var title: Uri = Objects.requireNonNull(data?.data)!!
        if(requestCode ==1 && resultCode == RESULT_OK) {
            try {
                var charset: String = "Shift-JIS"
                var writer: BufferedWriter = BufferedWriter(
                    OutputStreamWriter(contentResolver.openOutputStream(title), charset)
                )

                if (fileType == "DXF") saveDXF(writer)
                if (fileType == "CSV") saveCSV(writer)
                if (fileType == "PDF") savePDF(contentResolver.openOutputStream(title)!!, true)
                if (fileType == "SFC") saveSFC(
                    BufferedOutputStream(
                        contentResolver.openOutputStream(
                            title
                        )
                    ), true
                )

                AutoSaveCSV() // オートセーブ
                if( BuildConfig.FLAVOR == "free" ) showInterStAd()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if(requestCode ==2 && resultCode == RESULT_OK) {
            var str: StringBuilder = StringBuilder()
            try {
                var reader: BufferedReader = BufferedReader(
                    InputStreamReader(contentResolver.openInputStream(title), "Shift-JIS")
                )
                loadCSV(reader)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if(mIsCreateNew == true){
            CreateNew()
            mIsCreateNew = false
        }

        setTitles()
    }

    fun showInterStAd(){
        if ( mInterstitialAd.isLoaded && BuildConfig.FLAVOR == "free" ) {
            // 広告の再表示
            //mInterstitialAd.loadAd(AdRequest.Builder().build())
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    fun makeRStr() : ResStr {
        return ResStr(
            getString(R.string.tenkai_title),
            koujiname_,
            getString(R.string.tenkai_zumenmei),
            rosenname_,
            getString(R.string.tenkai_nengappi),
            gyousyaname_,
            getString(R.string.menseki),
            getString(R.string.menseki_title),
            getString(R.string.menseki_koujimei),
            getString(R.string.menseki_syoukei),
            getString(R.string.menseki_goukei),
        )
    }

    fun ViewPdf(context: Context){
        AutoSavePDF()

        val contentUri = getAppLocalFile(context, "myLastTriList.pdf")

        if ( contentUri != Uri.EMPTY ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(contentUri, "application/pdf")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                //if user doesn't have pdf reader instructing to download a pdf reader
            }
        }
    }

    fun sendMail(){

        AutoSaveCSV()
        AutoSavePDF()
        //AutoSaveDXF()
        //AutoSaveSFC()

        var intent: Intent = Intent(Intent.ACTION_SEND_MULTIPLE)
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

    fun getAppLocalFile(context: Context, filename: String) :Uri {
        val newFile = File(context.filesDir, filename)
        if (newFile.exists())
            return getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)
        else return Uri.EMPTY
    }

    fun sendPdf(context: Context){
        AutoSavePDF()

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

    fun saveDXF(bWriter: BufferedWriter) :BufferedWriter{

        val writer = DxfFileWriter(
            myTriangleList
        )
        writer.rStr_ = rStr_
        writer.titleTri_ = titleTriStr_
        writer.titleDed_ = titleDedStr_
        writer.textscale_ = my_view.ts_ * 0.016f //25*0.014f=0.35f, 25/0.02f=0.5f

        writer.writer_ = bWriter
        writer.drawingLength_ = myTriangleList.measureMostLongLine()
        writer.dedlist_ = myDeductionList
        writer.setNames(koujiname_, rosenname_, gyousyaname_, zumennum_)
        writer.isDebug_ = my_view.isDebug_

        writer.setStartNumber(drawingStartNumber_)
        writer.isReverse_ = drawingNumberReversal_;

        writer.save()
        bWriter.close()

        return bWriter
    }

    fun saveSFC(out: BufferedOutputStream, isShowAd: Boolean) {

        val writer = SfcWriter(myTriangleList, myDeductionList, out, filename_, drawingStartNumber_)
        writer.setNames(koujiname_, rosenname_, gyousyaname_, zumennum_)
        writer.rStr_ = rStr_
        writer.textscale_ = my_view.ts_ * 20f //25*14f=350f, 25/20f=500f
        writer.titleTri_ = titleTriStr_
        writer.titleDed_ = titleDedStr_

        writer.setStartNumber(drawingStartNumber_)
        writer.isReverse_ = drawingNumberReversal_;

        writer.save()
        out.close()

    }

    fun savePDF(out: OutputStream, isShowAd: Boolean){
        var writer: PdfWriter = PdfWriter(
            myTriangleList.getPrintScale(1f),
            myTriangleList
        )
        writer.out_ = out
        writer.deductionList_ = myDeductionList

        writer.textscale_ = my_view.ts_ * 0.5f / writer.printScale_ //25*0.4f=10f, 25/0.3f=7.5f
        writer.initPaints()
        writer.titleTri_ = titleTriStr_
        writer.titleDed_ = titleDedStr_
        writer.rStr_ = rStr_
        writer.setNames(koujiname_, rosenname_, gyousyaname_, zumennum_)

        //my_view.isAreaOff_ = false
        writer.isRulerOff_ = true

        writer.startNewPage(
            writer.sizeX_.toInt(),
            writer.sizeY_.toInt(),
            writer.currentPageIndex_
        )
        writer.translateCenter(writer.currentCanvas_)

        val viewPointer =
        my_view.drawPDF(
            writer,
            writer.currentCanvas_,
            writer.paintTri_,
            writer.paintTexS_,
            writer.paintRed_,
            writer.textscale_,//my_view.myTriangleList.getPrintTextScale(my_view.myScale, "pdf"),
            experience_
        )

        // translate back by view pointer
        writer.translate(writer.currentCanvas_, -viewPointer.x, -viewPointer.y)
        writer.writeTitleFrame(writer.currentCanvas_)
        writer.closeCurrentPage()

        //writer.writeAllCalcSheets()

        writer.closeDocAndStream()
    }

    fun AutoSavePDF(){
        try {
            savePDF(openFileOutput("myLastTriList.pdf", Context.MODE_PRIVATE), false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // show ad.
    fun AutoSaveDXF(){
        try {
            val charset: String = "Shift-JIS"
            var writer: BufferedWriter = BufferedWriter(
                OutputStreamWriter(
                    openFileOutput("myLastTriList.dxf", Context.MODE_PRIVATE),
                    charset
                )
            )
            writer = saveDXF(writer)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun AutoSaveSFC(){
        try {
            saveSFC(
                BufferedOutputStream(openFileOutput("myLastTriList.sfc", Context.MODE_PRIVATE)),
                false
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun AutoSaveCSV(){
        try {
            var writer: BufferedWriter = BufferedWriter(
                OutputStreamWriter(openFileOutput("myLastTriList.csv", Context.MODE_PRIVATE))
            )
            saveCSV(writer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 広告の再表示
        //if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = VISIBLE

    }

    fun ResumeCSV(){
        var str: StringBuilder = StringBuilder()
        try {
            var reader: BufferedReader = BufferedReader(
                InputStreamReader(openFileInput("myLastTriList.csv"))
            )
            val ok = loadCSV(reader)
            if(ok == false) CreateNew()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveCSV(writer: BufferedWriter){
        //myTriangleList.scale(PointXY(0f,0f),1/myTriangleList.getScale())
        rosenname_ = findViewById<EditText>(R.id.rosenname).text.toString()

        writer.write("koujiname, " + koujiname_)
        writer.newLine()
        writer.write("rosenname, " + rosenname_)
        writer.newLine()
        writer.write("gyousyaname, " + gyousyaname_)
        writer.newLine()
        writer.write("zumennum, " + zumennum_)
        writer.newLine()

        for (index in 1 .. myTriangleList.size()){
            var mt: Triangle = myTriangleList.getTriangle(index)
            var pt: PointXY = mt.pointNumber_
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
                        mt.pointCA_.y                           //24
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
            var pointAtRealscale = dd.point.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            var pointFlagAtRealscale = dd.pointFlag.scale(PointXY(0f, 0f), 1 / mScale, -1 / mScale)
            dd.scale(PointXY(0f, 0f), 1f, -1f)
            writer.write(
                "Deduction, " +              //0
                        dd.num.toString() + ", " +         //1
                        dd.name + ", " +                   //2
                        dd.lengthX.toString() + ", " +     //3
                        dd.lengthY.toString() + ", " +     //4
                        dd.parentNum.toString() + ", " +   //5
                        dd.type.toString() + ", " +        //6
                        dd.angle.toString() + ", " +       //7
                        pointAtRealscale.x.toString() + ", " +     //8
                        pointAtRealscale.y.toString() + ", " +     //9
                        pointFlagAtRealscale.x.toString() + ", " + //10
                        pointFlagAtRealscale.y.toString() + ", " + //11
                        dd.shapeAngle_.toString()        //12
            )
            writer.newLine()
            dd.scale(PointXY(0f, 0f), 1f, -1f)
        }
        writer.close()
    }

    fun loadCSV(reader: BufferedReader) :Boolean{
//        myDeductionMode = true
//        setDeductionMode(myDeductionMode)
        var str: StringBuilder = StringBuilder()
        var line: String? = reader.readLine()
        if(line == null) return false
        var chunks: List<String?> = line.split(",").map { it.trim() }
        if(chunks[0]!! == "koujiname") {
            koujiname_= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "rosenname") {
            rosenname_= chunks[1]!!.toString()
            findViewById<EditText>(R.id.rosenname).setText(rosenname_)
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "gyousyaname") {
            gyousyaname_= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }
        if(chunks[0]!! == "zumennum") {
            zumennum_= chunks[1]!!.toString()
            line = reader.readLine()
            chunks = line?.split(",")!!.map { it.trim() }
        }

        var trilist: TriangleList = TriangleList()

        var pointfirst = PointXY( 0f, 0f )
        var anglefirst = 180f
        if( chunks.size > 22 ) {
            if( chunks[22]!!.toFloat() != 180f ){
                //pointfirst = PointXY( -chunks[23]!!.toFloat(), -chunks[24]!!.toFloat() )
                //anglefirst = chunks[22]!!.toFloat() - 180f
            }
        }

        trilist.add(
            Triangle(
                chunks[1]!!.toFloat(), chunks[2]!!.toFloat(), chunks[3]!!.toFloat(), pointfirst, anglefirst
            )
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
            mt.cParam_ = ConneParam(
                chunks[17]!!.toInt(),
                chunks[18]!!.toInt(),
                chunks[19]!!.toInt(),
                chunks[1]!!.toFloat()
            )
        }


        var dedlist: DeductionList = DeductionList()
        var revScale: Float = 1f

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
                revScale = mScale/ chunks[1].toFloat()
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
                            chunks[2].toString(), chunks[6].toString(), chunks[1].toInt(),
                            chunks[3].toFloat(), chunks[4].toFloat(), 0f,
                            chunks[5].toInt(), typeToInt(chunks[6].toString()),
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
                if( chunks[12].isEmpty() == false ) dedlist.get(dedlist.size()).shapeAngle_ = chunks[12].toFloat()
                continue
            }
            //Connection Params
            if( chunks.size > 17 ) {

                if( chunks[5].toInt() == 0 ){
                    trilist.add(
                        Triangle(
                            chunks[1]!!.toFloat(), chunks[2]!!.toFloat(), chunks[3]!!.toFloat(), PointXY(
                                -chunks[23]!!.toFloat(),
                                -chunks[24]!!.toFloat()
                            ), chunks[22]!!.toFloat() - 180f
                        )
                    )
                }
                else {

                    val ptri = trilist.getTriangle(chunks[4].toInt())
                    val cp = ConneParam(
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
                        )
                    )

                }
                trilist.getTriangle(trilist.size()).parentBC_ = chunks[5].toInt()
            }
            else{

                    val cp = parentBCtoCParam(
                        chunks[5].toInt(), chunks[1].toFloat(), ConneParam(
                            0,
                            0,
                            0,
                            0f
                        )
                    )

                    trilist.add(
                        Triangle(
                            trilist.getTriangle(chunks[4].toInt()), ConneParam(
                                cp.side,
                                cp.type,
                                cp.lcr,
                                cp.lenA
                            ),
                            chunks[2].toFloat(),
                            chunks[3].toFloat()
                        )
                    )

                trilist.getTriangle(trilist.size()).parentBC_ = chunks[5].toInt()
                // trilist.getTriangle(trilist.size()).setCParamFromParentBC( chunks[5]!!.toInt() )
            }
            val mT = trilist.getTriangle(trilist.size())
            mT.setMyName_(chunks[6].toString())
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
        trilistStored_ = myTriangleList.clone()
        myDeductionList = dedlist
        //trilist.scale(PointXY(0f,0f), 5f)
        //if( anglefirst != 180f )
            trilist.recoverState(PointXY(0f, 0f))
        trilist.setChildsToAllParents()
//        myDeductionList.scale(PointXY(0f,0f), 1f, 1f)
        my_view.setDeductionList(dedlist, mScale)
        my_view.setTriangleList(trilist, mScale)
        my_view.resetViewToLSTP()

        // メニューバーのタイトル
        //setTitles()

        deductionMode_ = true
        EditorClear(myTriangleList, myTriangleList.size())
        //colorMovementFabs()
        flipDeductionMode(deductionMode_)
        //printDebugConsole()
        return true
    }

    fun typeToInt(type: String) :Int{
        var pl: Int = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }

    fun parentBCtoCParam(pbc: Int, lenA: Float, cp: ConneParam) : ConneParam{
        when(pbc){
            1 -> return ConneParam(1, 0, 2, 0f)//B
            2 -> return ConneParam(2, 0, 2, 0f)//C
            3 -> return ConneParam(1, 1, 2, lenA)//BR
            4 -> return ConneParam(1, 1, 0, lenA)//BL
            5 -> return ConneParam(2, 1, 2, lenA)//CR
            6 -> return ConneParam(2, 1, 0, lenA)//CL
            7 -> return ConneParam(1, 1, 1, lenA)//BC
            8 -> return ConneParam(2, 1, 1, lenA)//CC
            9 -> return ConneParam(1, 2, cp.lcr, lenA)//BF
            10 -> return ConneParam(2, 2, cp.lcr, lenA)//CF
        }

        return ConneParam(0, 0, 0, 0f)
    }

    fun setTitles(){
        findViewById<EditText>(R.id.rosenname).setText(rosenname_)

        val dedArea = myDeductionList.getArea()
        val triArea = myTriangleList.getArea()
        val totalArea = roundByUnderTwo( triArea - dedArea )
        title = rStr_.menseki_ + ": ${ totalArea.formattedString(2) } m^2"

        if( myTriangleList.lastTapNum_ > 0 ) title = rStr_.menseki_ + ": ${myTriangleList.getArea() - myDeductionList.getArea()} m^2" + " (${ myTriangleList.getAreaI( myTriangleList.lastTapNum_ ) - myDeductionList.getAreaN(myTriangleList.lastTapNum_) } m^2)"
    }

    fun roundByUnderTwo(fp: Float) :Float {
        val ip: Float = fp * 100f
        ip.roundToInt()
        val rfp: Float = ip / 100
        return rfp
    }

} // end of class
