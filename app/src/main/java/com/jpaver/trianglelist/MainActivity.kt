package com.jpaver.trianglelist

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
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
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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


data class ResStr (
    // 文字列リソースをstringに変換して保持する。
    var tTitle_ : String = "",
    var eRName_ : String = "",
    var tCname_ : String = "",
    var tDtype_ : String = "",
    var tDname_ : String = "",
    var tScale_ : String = "",
    var tNum_ : String = "",
    var tDateHeader_ : String = "",
    var tDate_ : String = "",
    var tAname_ : String = "",
    var menseki_ : String = "",
    var mTitle_ : String = "",
    var mCname_ : String = "",
    var mSyoukei_ : String = "",
    var mGoukei_ : String = "",
    var tCredit_ : String = ""
)

class MainActivity : AppCompatActivity(),
        MyDialogFragment.NoticeDialogListener {

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

    var fileType: String = "notyet"
    var deductionMode_: Boolean = false
    var mIsCreateNew: Boolean = false
    val onetohandred_ = 11.9f
    val experience_ = 4f
    val mScale = onetohandred_*experience_

    var koujiname_ = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"
    var dxfStartNumber_ = 1

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
            fab_replace.setBackgroundTintList(getColorStateList(R.color.colorTT2))
            fab_deduction.setBackgroundTintList(getColorStateList(R.color.colorWhite))
            fab_flag.setBackgroundTintList(getColorStateList(R.color.colorWhite))
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
            findViewById<Spinner>(R.id.editParent).setAdapter(spinnerArrayAdapter)
            findViewById<Spinner>(R.id.editParent2).setAdapter(spinnerArrayAdapter)
            findViewById<Spinner>(R.id.editParent3).setAdapter(spinnerArrayAdapter)
            findViewById<EditText>(R.id.editName1).requestFocus()
            inputMethodManager.showSoftInput(findViewById(R.id.editName1), 0)
            setEditNameAdapter(dedNameListC)

            //クロスヘアラインを画面中央に描画
//            my_view.drawCrossHairLine()

        } else {
            deductionMode_ = false

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
            fab_replace.setBackgroundTintList(getColorStateList(R.color.colorLime))
            fab_deduction.setBackgroundTintList(getColorStateList(R.color.colorAccent))
            fab_flag.setBackgroundTintList(getColorStateList(R.color.colorAccent))
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
            findViewById<Spinner>(R.id.editParent).setAdapter(spinnerArrayAdapter)
            findViewById<Spinner>(R.id.editParent2).setAdapter(spinnerArrayAdapter)
            findViewById<Spinner>(R.id.editParent3).setAdapter(spinnerArrayAdapter)
            findViewById<EditText>(R.id.editText).requestFocus()
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
        if (dp.a + dp.b <= dp.c ||
            dp.b + dp.c <= dp.a ||
            dp.c + dp.a <= dp.b ||
            (dp.n > 1 && dp.pl == 0)
        ) return false
        return true
    }

    fun loadEditTable(){
        myELFirst =
            EditTextViewLine(
                findViewById(R.id.triNumber),
                findViewById(R.id.editName1),
                findViewById(R.id.editText),
                findViewById(R.id.editText2),
                findViewById(R.id.editText3),
                findViewById(R.id.ParentNumber),
                findViewById(R.id.editParent)
            )

        myELSecond =
            EditTextViewLine(
                findViewById(R.id.triNumber2),
                findViewById(R.id.editName2),
                findViewById(R.id.editText4),
                findViewById(R.id.editText5),
                findViewById(R.id.editText6),
                findViewById(R.id.ParentNumber2),
                findViewById(R.id.editParent2)
            )

        myELThird =
            EditTextViewLine(
                findViewById(R.id.triNumber3),
                findViewById(R.id.editName3),
                findViewById(R.id.editText7),
                findViewById(R.id.editText8),
                findViewById(R.id.editText9),
                findViewById(R.id.ParentNumber3),
                findViewById(R.id.editParent3)
            )
        Log.d("EditorTable", "Load Success.")
    }

    fun validDeduction(dp: Params): Boolean {
        if(dp.name == "" || dp.a < 0.1f || dp.pl == 0 ) return false
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

//        setContentView(R.layout.activity_main)

        var locale = Locale.getDefault()
        locale = if (locale.equals(Locale.JAPAN)) Locale.US else Locale.JAPAN
        //言語を環境に合わせて変える。
        //locale.setDefault(locale)
        //言語を設定
        val config = Configuration()
        //config.locale = locale
        val resources: Resources = baseContext.resources
        //resources.updateConfiguration(config, null)
        //言語ファイルなどのリソースを更新する。
        setContentView(R.layout.activity_main)
        //新たにレイアウトを描画しなおす

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

        //インタースティシャル広告の読み込み
        if( BuildConfig.FLAVOR == "free"){
            mInterstitialAd = InterstitialAd(this)
            if( BuildConfig.BUILD_TYPE == "debug" ) mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
            else if( BuildConfig.BUILD_TYPE == "release" ) mInterstitialAd.adUnitId = "ca-app-pub-6982449551349060/2369695624"
            mInterstitialAd.loadAd(AdRequest.Builder().build())
        }

        setSupportActionBar(toolbar)
        myDeductionList = DeductionList()

        fab_replace.setOnClickListener { view ->
            fabReplace( dParams_, false)
/*
            if(deductionMode_ == false) {
                if( findViewById<TextView>(R.id.editText2).getText().toString() == "" )
                    resetTrianglesBy( myEditor.ReadLineTo( dParams_, myELSecond ) )
                else
                    if( findViewById<TextView>(R.id.editText3).getText().toString() == "" ) return@setOnClickListener
                    addTriangleBy( myEditor.ReadLineTo( dParams_, myELFirst ) )

            } else { // if deduction mode
                if( findViewById<TextView>(R.id.editText).getText().toString() == "" )
                    resetDeductionsBy( myEditor.ReadLineTo( dParams_, myELSecond ) )
                else
                    addDeductionBy( myEditor.ReadLineTo( dParams_, myELFirst ) )
            }
            EditorClear(getList(deductionMode_), getList(deductionMode_).getCurrent())
            my_view.setTriangleList(myTriangleList, mScale)
            my_view.setDeductionList(myDeductionList, mScale )
            printDebugConsole()
            AutoSaveCSV()
            setTitles()
            if(!deductionMode_) my_view.resetViewNotMove()
*/
        }

        fab_flag.setOnClickListener { view ->
            dParams_ = myEditor.ReadLineTo(dParams_, myELSecond)// 200703 // if式の中に入っていると当然ながら更新されない時があるので注意

            if(deductionMode_ == true){
                dParams_.pts = my_view.getTapPoint()
                dParams_.pt = myDeductionList.get(dParams_.n).point
                var ded = myDeductionList.get(dParams_.n)
                val tp = my_view.getTapPoint().scale( PointXY(0f, 0f ), 1 / mScale, -1 / mScale )
                if( validDeduction(dParams_) == true ) {// あまり遠い時はスルー
                    myDeductionList.replace(dParams_.n, dParams_)
//                    EditorReset(getList(myDeductionMode),getList(myDeductionMode).length())
                    my_view.setDeductionList(myDeductionList, mScale )
                }
            }
            else{
                var tri = myTriangleList.get(dParams_.n)
                var tp = my_view.getTapPoint().scale( PointXY(0f, 0f ), 1 / mScale, -1 / mScale )
                if( tp.lengthTo( tri.pointCenter_ ) < 10f ){ // あまり遠い時はスルー
                    tri.pointNumber_ = tp
                    tri.isPointNumberMoved_ = true
                    my_view.setTriangleList(myTriangleList, mScale)
                }
            }

            my_view.invalidate()
            if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(this, " Test Done.", Toast.LENGTH_SHORT).show()

            AutoSaveCSV()
        }


        fab_dimsidew.setOnClickListener { view ->
            if(!deductionMode_){
                var dimside = my_view.myTriangleList.lastTapSide_
                var trinum  = my_view.myTriangleList.lastTapNum_
                var tri = myTriangleList.get(trinum)

                if( dimside == 0 && ( tri.myParentBC_ == 1 ||  tri.myParentBC_ == 2 ) ) {
                    trinum --
                    dimside = tri.myParentBC_
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

                if( dimside == 0 && ( tri.myParentBC_ == 1 ||  tri.myParentBC_ == 2 ) ) {
                    trinum --
                    dimside = tri.myParentBC_
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
                my_view.resetView( my_view.lstp() )

                AutoSaveCSV()
            }
        }

        var deleteWarning: Int = 0
        fab_minus.setOnClickListener { view ->
            val len = getList(deductionMode_).size()

            if(len > 0 && deleteWarning == 0) {
                deleteWarning = 1
                fab_minus.setBackgroundTintList(getColorStateList(R.color.colorTT2))

            }
            else {
                if (len > 0) {
                    getList(deductionMode_).remove(len)
                    //my_view.removeTriangle()
                    my_view.setDeductionList(myDeductionList, mScale )
                    my_view.setTriangleList(myTriangleList, mScale)

                    EditorClear(getList(deductionMode_), getList(deductionMode_).size())
                }
                deleteWarning = 0
                fab_minus.setBackgroundTintList(getColorStateList(R.color.colorAccent))
            }
            printDebugConsole()
            colorMovementFabs()
            my_view.resetViewToLSTP()
            setTitles()

        }

        fab_resetView.setOnClickListener { view ->
            my_view.resetViewToLSTP()
        }

        fab_fillcolor.setOnClickListener { view ->
            if(!deductionMode_){
                colorindex_ ++
                if(colorindex_ == RColors.size) colorindex_ = 0
                fab_fillcolor.setBackgroundTintList(getColorStateList(RColors.get(colorindex_)))

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
            findViewById<EditText>(R.id.editText2).requestFocus()
        }

        fab_setC.setOnClickListener { view ->
            autoConnection(2)
            findViewById<EditText>(R.id.editText2).requestFocus()
        }

        var angle: Float = 0f
        fab_rot_l.setOnClickListener { view ->
            if( deductionMode_ == false ) {
                myTriangleList.rotate(PointXY(0f, 0f), 5f)
                myDeductionList.rotate(PointXY(0f, 0f), -5f)
                my_view.setTriangleList(myTriangleList, mScale)
                my_view.setDeductionList(myDeductionList, mScale )
                my_view.resetViewToLSTP()
                printDebugConsole()
            }
            // ded rotate
            else {
                val vdltip = my_view.myDeductionList.lastTapIndex_+1
                myDeductionList.get( vdltip ).rotateShape( myDeductionList.get( vdltip ).point, -5f)
                my_view.setDeductionList(myDeductionList, mScale )
                my_view.invalidate()
            }
            AutoSaveCSV()
        }

        fab_rot_r.setOnClickListener { view ->
            if( deductionMode_ == false ) {
                myTriangleList.rotate(PointXY(0f, 0f), -5f)
                myDeductionList.rotate(PointXY(0f, 0f), 5f)
                my_view.setTriangleList(myTriangleList, mScale)
                my_view.setDeductionList(myDeductionList, mScale )
                my_view.resetViewToLSTP()
                printDebugConsole()
                AutoSaveCSV()
            }
            // ded rotate
            else {
                val vdltip = my_view.myDeductionList.lastTapIndex_+1
                myDeductionList.get( vdltip ).rotateShape( myDeductionList.get( vdltip ).point, 5f)
                my_view.setDeductionList(myDeductionList, mScale )
                my_view.invalidate()
            }
            AutoSaveCSV()
        }


        fab_deduction.setOnClickListener { view ->
            deleteWarning = 0
            fab_minus.setBackgroundTintList(getColorStateList(R.color.colorAccent))
            flipDeductionMode(deductionMode_)
            colorMovementFabs()
        }

        fab_up.setOnClickListener { view ->
            myEditor.scroll(-1, getList(deductionMode_), myELSecond, myELThird)

            if(deductionMode_ == false ) moveTrilist()

            colorMovementFabs()
            printDebugConsole()
        }

        fab_down.setOnClickListener { view ->
            myEditor.scroll(1, getList(deductionMode_), myELSecond, myELThird)

            if(deductionMode_ == false ) moveTrilist()

            colorMovementFabs()
            printDebugConsole()

        }

        fab_debug.setOnClickListener { view ->
            //my_view.isDebug_ = !my_view.isDebug_
            //my_view.invalidate()

            if(my_view.isDebug_==true) fab_debug.setBackgroundTintList(getColorStateList(R.color.colorLime))
            else  fab_debug.setBackgroundTintList(getColorStateList(R.color.colorAccent))

            // オートセーブpdf, dxf
            if( BuildConfig.BUILD_TYPE == "debug" ) {
                AutoSavePDF()
                //AutoSaveDXF()
            }

        }

        fab_testbasic.setOnClickListener { view ->
            //CreateNew()

            findViewById<TextView>(R.id.editText2).setText("") // reset
            fabReplace( Params("", "", 1, 7f, 7f, 7f, 0, 0, ), true )
            findViewById<TextView>(R.id.editText2).setText("6f") // add
            fabReplace( Params("", "", 2, 7f, 6f, 6f, 1, 2, ), true )

            findViewById<TextView>(R.id.editText).setText("0.23f") // add
            deductionMode_ = true
            fabReplace( Params("仕切弁", "Circle", 1, 0.23f, 0f, 0f, 1, 0, PointXY( 1f, 0f ), PointXY( 0f, 0f ) ), true )
            deductionMode_ = false


            Toast.makeText(this, "Basic Senario Test Done.", Toast.LENGTH_SHORT).show()
        }

    }

    fun fabReplace( params: Params, useit: Boolean ){
        val editor = myEditor
        val dedmode = deductionMode_
        val editlist = getList(deductionMode_)

        var readedFirst  = Params()
        var readedSecond = Params()
        myEditor.ReadLineTo( readedFirst, myELFirst )
        myEditor.ReadLineTo( readedSecond, myELSecond )
        if( useit == true ){
            readedFirst = params
            readedSecond = params
        }
        val strTopA = findViewById<TextView>(R.id.editText).getText().toString()
        val strTopB = findViewById<TextView>(R.id.editText2).getText().toString()
        val strTopC = findViewById<TextView>(R.id.editText3).getText().toString()

        var usedDedPoint = params.pt.clone()

        var isSucceed = false

        if( dedmode == false ) {
            if( strTopB == "" ) isSucceed = resetTrianglesBy( readedSecond )
            else
                if( strTopC == "" && useit == false ) return
                else isSucceed = addTriangleBy( readedFirst )

        } else { // if in deduction mode
            if( strTopA == "" ) {
                isSucceed = resetDeductionsBy( readedSecond )
                usedDedPoint = my_view.myDeductionList.get( readedSecond.n ).point
            }
            else{
                addDeductionBy( readedFirst )
                usedDedPoint = my_view.myDeductionList.get( readedFirst.n ).point
            }
        }

        EditorClear(editlist, editlist.getCurrent())
        my_view.setTriangleList(myTriangleList, mScale)
        my_view.setDeductionList(myDeductionList, mScale )
        printDebugConsole()
        AutoSaveCSV()
        setTitles()
        if( dedmode == false ) my_view.resetView( my_view.lstp() )
        if( dedmode == true  ) my_view.resetView( usedDedPoint.scale( PointXY( 0f, 0f ), 1f, -1f ) )//resetViewToTP()
        if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(this, isSucceed.toString(), Toast.LENGTH_SHORT).show()
    }

    fun moveTrilist(){
        my_view.getTriangleList().setCurrent(myTriangleList.getCurrent())
        my_view.myTriangleList.lastTapNum_ = myTriangleList.getCurrent()
        my_view.resetViewToLSTP()
    }

    fun addDeductionBy(params: Params) : Boolean {
        params.pt = my_view.getTapPoint()
        params.pts = PointXY(0f, 0f)
        params.pn = my_view.myTriangleList.isCollide(dParams_.pt)
        if (validDeduction(params) == true) {

            myDeductionList.add(params)
            my_view.setDeductionList(myDeductionList, mScale )
            lastParams_ = params
            return true
        }
        else return false
    }

    fun addTriangleBy(params: Params) : Boolean {
        if (validTriangle(params)) {
            var myTri: Triangle = Triangle(
                myTriangleList.getTriangle(params.pn),
                params
            )
            myTri.myNumber_ = params.n
            myTriangleList.add(myTri)
            findViewById<EditText>(R.id.editText).requestFocus()
            myTriangleList.lastTapNum_ = myTriangleList.size()
            //my_view.resetView()
            return true
        }
        return false
    }

    fun resetTrianglesBy(params: Params) : Boolean {

        if (validTriangle(params) == true){
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
        prms.pn = my_view.myTriangleList.isCollide(prms.pt)
        myTriangleList.current = prms.pn

        if( validDeduction(prms) == true ) {
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
        var side = findViewById<EditText>(R.id.editText5)
        if(i == 2) side = findViewById<EditText>(R.id.editText6)

        if( my_view.myTriangleList.isDoubleTap_ == true ){
            side = findViewById<EditText>(R.id.editText2)
        }

        if(deductionMode_ == false) {

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

                side.requestFocus()
                side.setSelection(side.getText().length)
                var inputMethodManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(side, 0)
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
            fab_setB.setBackgroundTintList(getColorStateList(R.color.colorAccent))

        }
    }

    fun setTargetEditText(str: String){
        var inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


        if(deductionMode_ == true ){
            my_view.myDeductionList.setScale( my_view.myScale )
            my_view.myDeductionList.getTapIndex( my_view.localPressPoint )

            if ( my_view.myDeductionList.lastTapIndex_ > -1 ) {
                val tapIndex = my_view.myDeductionList.lastTapIndex_+1
                if( -1 < tapIndex ) {
                    //Toast.makeText(this, "deduction tap", Toast.LENGTH_SHORT).show()
                    myEditor.scroll(
                        tapIndex - myDeductionList.getCurrent(),
                        getList(deductionMode_), myELSecond, myELThird
                    )
                }
            }

        }
        else {
            my_view.myTriangleList.getTap(my_view.localPressPoint.scale(PointXY(0f, 0f), 1f, -1f))

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
                findViewById<EditText>(R.id.ParentNumber).setText(myTriangleList.lastTapNum_.toString())
                findViewById<EditText>(R.id.triNumber).setText(myTriangleList.size().toString())

                colorindex_ = myTriangleList.get(myTriangleList.lastTapNum_).color_
                colorMovementFabs()
                printDebugConsole()
                if( my_view.myTriangleList.lastTapSide_ == 0 ) {

                    findViewById<EditText>(R.id.editText4).requestFocus()
                    findViewById<EditText>(R.id.editText4).setSelection(findViewById<EditText>(R.id.editText4).getText().length)
                    inputMethodManager.showSoftInput(findViewById(R.id.editText4), 0)
                    my_view.setParentSide(my_view.getTriangleList().lastTapNum_, 3)
                }
                if(my_view.myTriangleList.lastTapSide_ == 1) {
                    autoConnection(1)
                    //findViewById<EditText>(R.id.editText5).requestFocus()
                    //inputMethodManager.showSoftInput(findViewById(R.id.editText5), 0)
                }
                if(my_view.myTriangleList.lastTapSide_ == 2) {
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
        tvd.setText(
            """ myView.Center: ${my_view.myTriangleList.center.x} ${my_view.myTriangleList.center.y}
                        |TriCurrent: ${my_view.getTriangleList().getCurrent()} T1.color ${
                my_view.getTriangleList().get(
                    1
                ).color_
            } ${myTriangleList.get(1).color_} 
                        |TapTL: ${my_view.tapTL_} , lastTapNum: ${my_view.getTriangleList().lastTapNum_}, lastTapSide: ${my_view.getTriangleList().lastTapSide_}                                 
                        |viewX: ${my_view.getViewSize().x}, viewY ${my_view.getViewSize().y}, zoomsize: ${my_view.zoomSize}
                        |mtsX: ${
                myTriangleList.measureMostLongLine().getX()
            } , mtsY: ${
                myTriangleList.measureMostLongLine().getY()
            }  mtcX: ${myTriangleList.getCenter().getX()} , mtcY: ${
                myTriangleList.getCenter().getY()
            }
                        |mtscl: ${myTriangleList.getScale()} , mtc: ${myTriangleList.getCurrent()}  mdl: ${myDeductionList.size()} , mdc: ${myDeductionList.getCurrent()}
                        |currentname: ${
                myTriangleList.get(myTriangleList.size()).getMyName_()
            }  cur-1name: ${
                myTriangleList.get(
                    myTriangleList.size() - 1
                ).getMyName_()
            }
                        |myAngle: ${myTriangleList.myAngle}
            """.trimMargin()
        )
    }


    fun colorMovementFabs() : Int{
        val max: Int = getList(deductionMode_).size()
        val current: Int = getList(deductionMode_).getCurrent()
        val min: Int = 1
        var movable: Int = 0
        //fab_zoomin.setBackgroundTintList(getColorStateList(R.color.colorSky))
        //fab_zoomout.setBackgroundTintList(getColorStateList(R.color.colorSky))
        fab_resetView.setBackgroundTintList(getColorStateList(R.color.colorSky))
        //色
        fab_fillcolor.setBackgroundTintList(getColorStateList(RColors.get(colorindex_)))

        if(max > current) {
            fab_down.setBackgroundTintList(getColorStateList(R.color.colorSky))
            movable++
        }
        else fab_down.setBackgroundTintList(getColorStateList(R.color.colorAccent))

        if(min < current){
            fab_up.setBackgroundTintList(getColorStateList(R.color.colorSky))
            movable += 2
        }
        else fab_up.setBackgroundTintList(getColorStateList(R.color.colorAccent))

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
        textView.setThreshold(1)
        textView2.setThreshold(1)
        textView3.setThreshold(1)
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
            val input = mELine.name.getText().toString()
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

        rStr_ = ResStr( getString( R.string.tenkai_title ), getString( R.string.rosen1 ), getString( R.string.tenkai_koujimei ), getString( R.string.tenkai_zumenmei ), getString( R.string.tenkai_rosenmei ), getString( R.string.tenkai_syukusyaku ), getString( R.string.tenkai_zuban ), getString( R.string.tenkai_sakuseibi ), getString( R.string.tenkai_nengappi ), getString( R.string.tenkai_sekousya ), getString( R.string.menseki ), getString( R.string.menseki_title ), getString( R.string.menseki_koujimei ), getString( R.string.menseki_syoukei ), getString( R.string.menseki_goukei ), getString( R.string.credit ) )
        titleTri_ = TitleParams( R.string.menseki, R.string.editor_number, R.string.editor_sokuten, R.string.editor_lA, R.string.editor_lB, R.string.editor_lC, R.string.editor_parent, R.string.editor_setuzoku )
        titleDed_ = TitleParams( R.string.menseki, R.string.editor_number, R.string.editor_name, R.string.editor_lA, R.string.editor_lB, R.string.editor_lC, R.string.editor_syozoku, R.string.editor_form )
        titleTriStr_ = TitleParamStr( getString( titleTri_.type ), getString( titleTri_.n ), getString( titleTri_.name ), getString( titleTri_.a ), getString( titleTri_.b ), getString( titleTri_.c ), getString( titleTri_.pn ), getString( titleTri_.pl ) )
        titleDedStr_ = TitleParamStr( getString( titleDed_.type ), getString( titleDed_.n ), getString( titleDed_.name ), getString( titleDed_.a ), getString( titleDed_.b ), getString( titleDed_.c ), getString( titleDed_.pn ), getString( titleDed_.pl ) )

        val filepath = this.filesDir.absolutePath + "/" + "myLastTriList.csv"
        val file: File = File(filepath)
        if(file.exists() == true) ResumeCSV()
        else                      CreateNew()
        loadEditTable()
        colorMovementFabs()
        //fab.setBackgroundTintList(getColorStateList(R.color.colorLime))
        fab_replace.setBackgroundTintList(getColorStateList(R.color.colorLime))
        setEditNameAdapter(sNumberList)

    }


    fun rotateColor(index: Int): Int{
        return R.color.colorPink
    }

    fun CreateNew(){
        val tri: Triangle = Triangle(5f, 5f, 5f, PointXY(0f, 0f), 0f)
        var trilist: TriangleList = TriangleList(tri)
        myTriangleList = trilist
        myDeductionList.clear()

        //trilist.recoverState(PointXY(0f,0f))

        // メニューバーのタイトル
        koujiname_ = ""
        rosenname_ = rStr_.eRName_
        setTitles()

        my_view.setTriangleList(trilist, mScale)
        my_view.setDeductionList(myDeductionList, mScale )
        my_view.myTriangleList.lastTapNum_ = my_view.myTriangleList.size()
        my_view.resetViewToLSTP()

        fab_fillcolor.setBackgroundTintList(getColorStateList(RColors.get(colorindex_)))

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

        // 広告の再表示
        if( BuildConfig.FLAVOR == "free" ) mAdView.visibility = INVISIBLE
    }

    override fun onStop(){
        super.onStop()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onPause(){
        super.onPause()
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
        var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.setType("text/csv")
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_new -> {
                var dialog: MyDialogFragment = MyDialogFragment()
                dialog.show(getSupportFragmentManager(), "dialog.basic")
                return true
            }
            R.id.action_save_csv -> {
                fileType = "CSV"
                var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                i.setType("text/csv")
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
                var i: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                i.setType("text/csv")
                i.putExtra(Intent.EXTRA_TITLE, ".csv")
                startActivityForResult(i, 2)

                return true
            }

            R.id.action_save_dxf -> {

                val hTstart = getString( R.string.inputtnum )
                val editText5 = EditText(this)
                editText5.hint = hTstart
                val filter2 = arrayOf(InputFilter.LengthFilter(3))
                editText5.setFilters(filter2)
                editText5.setText( dxfStartNumber_.toString() )

                AlertDialog.Builder(this)
                    .setTitle("Save DXF")
                    .setMessage(R.string.inputtnum)
                    .setView(editText5)
                    .setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialog, which ->
                            dxfStartNumber_ = editText5.getText().toString().toInt()

                            fileType = "DXF"
                            var i: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            i.setType("text/dxf")
                            i.putExtra(Intent.EXTRA_TITLE, rosenname_ + " " + LocalDate.now() + ".dxf")
                            startActivityForResult(i, 1)

                        })
                    .show()


                return true
            }

            R.id.action_save_pdf -> {
                // 入力ヒントの表示
                val hCname = getString( R.string.inputcname )
                val hSpace = getString( R.string.space )
                val hRname = getString( R.string.inputdname )
                val hAname = getString( R.string.inputaname )
                val hRnum = getString( R.string.inputdnum )
                val editText = EditText(this)
                editText.hint = hCname + " " + hSpace
                val filter = arrayOf(InputFilter.LengthFilter(50))
                editText.setFilters(filter)
                editText.setText(koujiname_)
                val editText2 = EditText(this)
                editText2.hint = hRname
                rosenname_ = findViewById<EditText>(R.id.rosenname).getText().toString()
                editText2.setText(rosenname_)
                editText2.setFilters(filter)
                val editText3 = EditText(this)
                editText3.hint = hAname
                editText3.setText(gyousyaname_)
                editText3.setFilters(filter)
                val editText4 = EditText(this)
                editText4.hint = hRnum
                editText4.setText(zumennum_)
                editText4.setFilters(filter)

                AlertDialog.Builder(this)
                    .setTitle("Save PDF")
                    .setMessage(R.string.inputcname)
                    .setView(editText)
                    .setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialog, which ->
                            koujiname_ = editText.getText().toString()

                            AlertDialog.Builder(this)
                                .setTitle("Save PDF")
                                .setMessage(R.string.inputdname)
                                .setView(editText2)
                                .setPositiveButton("OK",
                                    DialogInterface.OnClickListener { dialog, which ->
                                        rosenname_ = editText2.getText().toString()

                                        AlertDialog.Builder(this)
                                            .setTitle("Save PDF")
                                            .setMessage(R.string.inputaname)
                                            .setView(editText3)
                                            .setPositiveButton("OK",
                                                DialogInterface.OnClickListener { dialog, which ->
                                                    gyousyaname_ = editText3.getText().toString()

                                                    AlertDialog.Builder(this)
                                                        .setTitle("Save PDF")
                                                        .setMessage(R.string.inputdnum)
                                                        .setView(editText4)
                                                        .setPositiveButton("OK",
                                                            DialogInterface.OnClickListener { dialog, which ->
                                                                zumennum_ =
                                                                    editText4.getText().toString()

                                                                fileType = "PDF"
                                                                var i: Intent =
                                                                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                                        type = "application/pdf"
                                                                        putExtra(
                                                                            Intent.EXTRA_TITLE,
                                                                            rosenname_ + "_" + LocalDate.now() + ".pdf"
                                                                        )

                                                                    }
                                                                startActivityForResult(i, 1)

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
                //saveDXF()
                var intent: Intent = Intent(Intent.ACTION_SEND)
                ///myIntent.putExtra(Intent.EXTRA_STREAM,"myTriangleListDXF.dxf")
                intent.putExtra(Intent.EXTRA_EMAIL, "Input Mail Addres")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Input Subject")
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name))
                var attachments: Uri =
                    Uri.parse("../data/com.example.myapplication/files/myTriangleListDXF.dxf")
                intent.putExtra(Intent.EXTRA_STREAM, attachments)
                intent.setType("message/rfc822")
                intent.setPackage("com.google.android.gm")
                startActivity(intent)
                return true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data?.getData() == NULL || resultCode == RESULT_CANCELED) return
        var title: Uri = Objects.requireNonNull(data?.getData())!!
        if(requestCode ==1 && resultCode == RESULT_OK) {
            try {
                var charset: String = "Shift-JIS"
                var writer: BufferedWriter = BufferedWriter(
                    OutputStreamWriter(getContentResolver().openOutputStream(title), charset)
                )
                if (fileType == "DXF") saveDXF(writer)
                if (fileType == "CSV") saveCSV(writer)
                if (fileType == "PDF") savePDF(getContentResolver().openOutputStream(title)!!, true )

                AutoSaveCSV() // オートセーブ
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if(requestCode ==2 && resultCode == RESULT_OK) {
            var str: StringBuilder = StringBuilder()
            try {
                var reader: BufferedReader = BufferedReader(
                    InputStreamReader(getContentResolver().openInputStream(title), "Shift-JIS")
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

    fun makeRStr() : ResStr {
        return ResStr(
            getString( R.string.tenkai_title ),
            koujiname_,
            getString( R.string.tenkai_zumenmei ),
            rosenname_,
            getString( R.string.tenkai_nengappi ),
            gyousyaname_,
            getString( R.string.menseki ),
            getString( R.string.menseki_title ),
            getString( R.string.menseki_koujimei ),
            getString( R.string.menseki_syoukei ),
            getString( R.string.menseki_goukei ),
        )
    }

    fun savePDF(out: OutputStream, isShowAd: Boolean){
        var myWriter: PdfWriter = PdfWriter(
            myTriangleList.getPrintScale(1f),
            myTriangleList
        )

        myWriter.startNewPage(myWriter.sizeX_.toInt(), myWriter.sizeY_.toInt(), myWriter.currentPageIndex_)
        myWriter.initPaints()
        myWriter.titleTri_ = titleTriStr_
        myWriter.titleDed_ = titleDedStr_
        myWriter.rStr_ = rStr_

        myWriter.out_ = out
        myWriter.deductionList_ = myDeductionList

        myWriter.setNames(koujiname_, rosenname_, gyousyaname_, zumennum_)
//        myWriter.setScale(myTriangleList.measureMostLongLine())
        myWriter.translateCenter()
//        myWriter.writeTitleFrame(myWriter.currentCanvas_)
        //myWriter.writeRuler(myWriter.currentCanvas_)

        my_view.isAreaOff_ = false
        myWriter.isRulerOff_ = true

        val viewPointer =
        my_view.drawPDF(
            myWriter,
            myWriter.currentCanvas_,
            myWriter.paintTri_,
            myWriter.paintTexS_,
            myWriter.paintRed_,
            my_view.myTriangleList.getPrintTextScale(my_view.myScale, "pdf"),
            experience_
        )

        // translate back by view pointer
        myWriter.translate( myWriter.currentCanvas_, -viewPointer.x, -viewPointer.y )
        myWriter.writeTitleFrame(myWriter.currentCanvas_)

        myWriter.close()

        if( isShowAd == true && BuildConfig.FLAVOR == "free" ){
            showInterStAd()
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

    fun showInterStAd(){
        if ( mInterstitialAd.isLoaded && BuildConfig.FLAVOR == "free" ) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    fun AutoSavePDF(){
        try {
            savePDF(openFileOutput("myLastTriList.pdf", Context.MODE_PRIVATE ), false )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun AutoSaveDXF(){
        try {
            var writer: BufferedWriter = BufferedWriter(
                OutputStreamWriter(openFileOutput("myLastTriList.dxf", Context.MODE_PRIVATE))
            )
            saveDXF(writer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
        rosenname_ = findViewById<EditText>(R.id.rosenname).getText().toString()

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
            val cp = parentBCtoCParam( mt.parentBC, mt.lengthAforce_, mt.cParam_ )
            //if( mt.isPointNumberMoved_ == true ) pt.scale(PointXY(0f,0f),1f,-1f)
            writer.write(
                mt.getMyNumber_().toString() + ", " +           //0
                        mt.getLengthA_().toString() + ", " +        //1
                        mt.getLengthB_().toString() + ", " +        //2
                        mt.getLengthC_().toString() + ", " +        //3
                        mt.getParentNumber().toString() + ", " +   //4
                        mt.getParentBC().toString() + ", " +       //5
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
                        cp.lcr                               //19
            )
            writer.newLine()
        }

        writer.write("ListAngle, " + myTriangleList.getAngle())
        writer.newLine()
        writer.write("ListScale, " + myTriangleList.getScale())
        writer.newLine()
        writer.write("TextSize, " + my_view.ts_ )
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

    fun typeToInt(type: String) :Int{
        var pl: Int = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }

    fun parentBCtoCParam(pbc: Int, lenA: Float, cp: ConneParam ) : ConneParam{
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

    fun loadCSV(reader: BufferedReader) :Boolean{
//        myDeductionMode = true
//        setDeductionMode(myDeductionMode)
        var str: StringBuilder = StringBuilder()
        var line: String? = reader.readLine()
        if(line == null) return false
        var chunks: List<String?> = line?.split(",")!!.map { it.trim() }
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

        trilist.add(
            Triangle(
                chunks[1]!!.toFloat(), chunks[2]!!.toFloat(), chunks[3]!!.toFloat(), PointXY(
                    0f,
                    0f
                ), 180f
            )
        )
        trilist.getTriangle(trilist.size()).setMyName_(chunks[6]!!.toString())
        if(chunks[9]!! == "true") trilist.getTriangle(trilist.size()).setPointNumberMoved_(
            PointXY(
                chunks[7]!!.toFloat(),
                chunks[8]!!.toFloat()
            )
        )
        // 色
        if( chunks.size > 10 ) trilist.getTriangle(trilist.size()).setColor(chunks[10]!!.toInt())

        // dimaligns
        if( chunks.size > 11 ) {
            trilist.getTriangle(trilist.size()).setDimAligns(
                chunks[11]!!.toInt(), chunks[12]!!.toInt(), chunks[13]!!.toInt(),
                chunks[14]!!.toInt(), chunks[15]!!.toInt(), chunks[16]!!.toInt()
            )
        }

        if( chunks.size > 17 ) {
            trilist.getTriangle(trilist.size()).cParam_ = ConneParam(
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
            chunks = line?.split(",")!!.map { it.trim() }
            if(chunks[0]!! == "ListAngle") {
                trilist.setAngle(chunks[1]!!.toFloat())
                continue
            }
            if(chunks[0]!! == "ListScale") {
                trilist.setScale(PointXY(0f, 0f), chunks[1]!!.toFloat())
                revScale = mScale/chunks[1]!!.toFloat()
                continue
            }
            if(chunks[0]!! == "TextSize") {
                my_view.setAllTextSize( chunks[1]!!.toFloat() )
                continue
            }
            if(chunks[0]!! == "Deduction"){
//                dedlist.add(Params(chunks[2]!!.toString(),chunks[6]!!.toString(), chunks[1]!!.toInt(),
  //                  chunks[3]!!.toFloat(),chunks[4]!!.toFloat(),0f,
    //                chunks[5]!!.toInt(),typeToInt(chunks[6]!!.toString()),
      //              PointXY(chunks[8]!!.toFloat(),-chunks[9]!!.toFloat()).scale(mScale),
        //            PointXY(chunks[10]!!.toFloat(),-chunks[11]!!.toFloat()).scale(mScale)))
                dedlist.add(
                    Deduction(
                        Params(
                            chunks[2]!!.toString(), chunks[6]!!.toString(), chunks[1]!!.toInt(),
                            chunks[3]!!.toFloat(), chunks[4]!!.toFloat(), 0f,
                            chunks[5]!!.toInt(), typeToInt(chunks[6]!!.toString()),
                            PointXY(
                                chunks[8]!!.toFloat(),
                                -chunks[9]!!.toFloat()
                            ).scale(mScale),
                            PointXY(
                                chunks[10]!!.toFloat(),
                                -chunks[11]!!.toFloat()
                            ).scale(mScale)
                        )
                    )
                )
                if( chunks[12].isEmpty() == false ) dedlist.get(dedlist.size()).shapeAngle_ = chunks[12]!!.toFloat()
                continue
            }
            //Connection Params
            if( chunks.size > 17 ) {
                val ptri = trilist.getTriangle(chunks[4]!!.toInt())
                val cp = ConneParam(
                    chunks[17]!!.toInt(),
                    chunks[18]!!.toInt(),
                    chunks[19]!!.toInt(),
                    chunks[1]!!.toFloat()
                )
                trilist.add(
                    Triangle(
                        ptri, cp,
                        chunks[2]!!.toFloat(),
                        chunks[3]!!.toFloat()
                    )
                )
                trilist.getTriangle(trilist.size()).myParentBC_ = chunks[5]!!.toInt()
            }
            else{
                val cp = parentBCtoCParam(chunks[5]!!.toInt(), chunks[1]!!.toFloat(), ConneParam( 0, 0, 0, 0f ) )

                trilist.add(
                    Triangle(
                        trilist.getTriangle(chunks[4]!!.toInt()), ConneParam(
                            cp.side,
                            cp.type,
                            cp.lcr,
                            cp.lenA
                        ),
                        chunks[2]!!.toFloat(),
                        chunks[3]!!.toFloat()
                    )
                )
                trilist.getTriangle(trilist.size()).myParentBC_ = chunks[5]!!.toInt()
               // trilist.getTriangle(trilist.size()).setCParamFromParentBC( chunks[5]!!.toInt() )
            }
            trilist.getTriangle(trilist.size()).setMyName_(chunks[6]!!.toString())
            if( trilist.size() > 1 ) trilist.get(trilist.size() - 1).childSide_ = chunks[5]!!.toInt()

            if(chunks[9]!! == "true") trilist.getTriangle(trilist.size()).setPointNumberMoved_(
                PointXY(
                    chunks[7]!!.toFloat(),
                    chunks[8]!!.toFloat()
                )
            )

            // 色
            if( chunks.size > 10 ) trilist.getTriangle(trilist.size()).setColor(chunks[10]!!.toInt())

            // dimaligns
            if( chunks.size > 11 ) {
                trilist.getTriangle(trilist.size()).setDimAligns(
                    chunks[11]!!.toInt(), chunks[12]!!.toInt(), chunks[13]!!.toInt(),
                    chunks[14]!!.toInt(), chunks[15]!!.toInt(), chunks[16]!!.toInt()
                )
            }




            str.append(line)
            str.append(System.getProperty("line.separator"))
        }
        //dedlist.scale(PointXY(0f,0f),3f,3f)
        myTriangleList = trilist
        myDeductionList = dedlist
        //trilist.scale(PointXY(0f,0f), 5f)
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

    fun setTitles(){
        findViewById<EditText>(R.id.rosenname).setText(rosenname_)
        setTitle( rStr_.menseki_ + ": ${myTriangleList.getArea() - myDeductionList.getArea()} m^2");
    }

    fun saveDXF(writer: BufferedWriter) :BufferedWriter{

        val dxfWriter = DxfFileWriter(
            myTriangleList
        )
        dxfWriter.rStr_ = rStr_

        dxfWriter.writer_ = writer
        dxfWriter.drawingLength_ = myTriangleList.measureMostLongLine()
        dxfWriter.deductionList_ = myDeductionList
        dxfWriter.setNames(koujiname_, rosenname_, gyousyaname_, zumennum_)
        dxfWriter.isDebug_ = my_view.isDebug_

        dxfWriter.startTriNumber_ = dxfStartNumber_

        dxfWriter.saveDXF(writer)
        if( BuildConfig.FLAVOR == "free" ) showInterStAd()
        return writer
    }

} // end of class
