package com.jpaver.trianglelist;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.Integer.parseInt;

import com.jpaver.trianglelist.util.Params;

public class TriangleList extends EditList implements Cloneable {
    ArrayList<Triangle> trilist_;
    ArrayList<Triangle> trilistStored_;
    ArrayList<Collision> myCollisionList;
    ArrayList<ArrayList<PointXY>> outlineList_;

    int lastTapNumber_ = 0;
    int lastTapSide_ = -1;
    int lastTapCollideNum_ = 0;

    int current;// = 0;
    float myScale = 1f;

    float myAngle = 0f;

    Bounds myBounds = new Bounds(0f,0f,0f,0f);
    PointXY myCenter = new PointXY(0f,0f);
    PointXY myLength = new PointXY(0f,0f);

    String outlineStr_ = "";

    Boolean isDoubleTap_ = false;

    @NotNull
    @Override
    public TriangleList clone(){

        TriangleList b = new TriangleList();
        try {
            b.setBasepoint(getBasepoint().clone());
            b.myBounds = myBounds;
            b.myCenter = myCenter.clone();
            b.myLength = myLength.clone();
            //b = new TriangleList();
            b.current = this.current;
            b.lastTapNumber_ = this.lastTapNumber_;
            b.lastTapSide_ = this.lastTapSide_;
            b.myScale = this.myScale;
            b.myAngle = this.myAngle;
            b.setBasepoint(this.getBasepoint().clone());
            b.outlineList_ = outlineList_;

            for(int i = 0; i < this.trilist_.size(); i++){
                b.trilist_.add(this.trilist_.get(i).clone());
                //b.myTriListAtView.add(this.myTriListAtView.get(i).clone());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        //ノードポインターのリコネクト
        if( trilist_.size() > 0 ) b.resetAllNodes();

        return b;
    }

    public void undo(){
        //if( trilistStored_ != null ) trilist_ = (ArrayList<Triangle>) trilistStored_.clone();
    }

    public ArrayList<Triangle> getSokutenList(int start, int pitch ){
        ArrayList<Triangle> allSokTriList = getAllSokutenList();
        ArrayList<Triangle> sokTriList = new ArrayList<>();

        for ( int i = 0; i < allSokTriList.size(); i++ ) {
            // No.Stringから数値を抜き出してIntに変換する
            String s = allSokTriList.get(i).myName_;
            int nameInt = parseInt(s.replaceAll("[^0-9]",""));

            // 剰余演算子%を使ってpitch(4)の倍数の場合を判定する
            if( ( nameInt - start ) % pitch == 0 || nameInt == start ) {
                sokTriList.add( allSokTriList.get(i).clone() );
            }
        }

        // 最後にいっこ足す
        if( getSokutenListVector() > 0 && sokTriList.size() > 1 ){
            Triangle lasttri = sokTriList.get( sokTriList.size() - 1 );
            Triangle pasttri = sokTriList.get( sokTriList.size() - 2 );
            float lastx = lasttri.point[0].getX();
            float pastx = pasttri.point[0].getX();
            sokTriList.add( new Triangle( 5f, 5f, 5f) );
            sokTriList.get( sokTriList.size() - 1 ).point[0].setX( lastx + (lastx - pastx) );
            sokTriList.get( sokTriList.size() - 1 ).lengthNotSized[0] = lasttri.lengthNotSized[0];
            sokTriList.get( sokTriList.size() - 1 ).length[0] = lasttri.length[0];
        }

        // 最初にいっこ足す
        if( getSokutenListVector() < 0 && sokTriList.size() > 1 ){
            Triangle firsttri = sokTriList.get( 0 );
            Triangle secondtri = sokTriList.get( 1 );
            PointXY first = firsttri.point[0];
            PointXY second = secondtri.point[0];
            sokTriList.add( 0, new Triangle( 5f, 5f, 5f) );
            sokTriList.get( 0 ).point[0] = first.minus( second ).plus( first );
            sokTriList.get( 0 ).lengthNotSized[0] = firsttri.lengthNotSized[0];
            sokTriList.get( 0 ).length[0] = firsttri.length[0];
        }


        return sokTriList;
    }

    // 全ての測点リストを返す
    public ArrayList<Triangle> getAllSokutenList( ) {
        ArrayList<Triangle> numTriList = new ArrayList<>();
        for (int i = 0; i < trilist_.size(); i++ ){
            if( trilist_.get(i).myName_.contains( "No." ) ) {
                numTriList.add( trilist_.get(i).clone() );
            }
        }
        return  numTriList;
    }

    // 測点のリストの順逆を返す。正の時は三角形リストと同じ方向、負の時は逆方向
    public int getSokutenListVector(){
        ArrayList<Triangle> allSokutenList = getAllSokutenList();
        if( allSokutenList.size() > 1 ){
            // No.Stringから数値を抜き出してIntに変換する
            String sf = allSokutenList.get(0).myName_;
            int fnInt = parseInt(sf.replaceAll("[^0-9]",""));
            String ss = allSokutenList.get(1).myName_;
            int snInt = parseInt(ss.replaceAll("[^0-9]",""));

            return snInt - fnInt;
        }
        return 0;
    }

    public float getPrintTextScale (float drawingScale, String exportFileType){

        Map<Float, Float> textScaleMapPDF = new HashMap<>();
        textScaleMapPDF.put(45f, 3f);
        textScaleMapPDF.put(40f, 5f);
        textScaleMapPDF.put(25f, 6f);
        textScaleMapPDF.put(20f, 8f);
        textScaleMapPDF.put(15f, 8f);

        Map<Float, Float> textScaleMapDXF = new HashMap<>();
        textScaleMapDXF.put(15f, 0.35f);
        textScaleMapDXF.put(5f, 0.25f);

        Map<String, Map<Float,Float>> exportFileTypeMap = new HashMap<>();
        exportFileTypeMap.put( "dxf", textScaleMapDXF );
        exportFileTypeMap.put( "sfc", textScaleMapDXF );
        exportFileTypeMap.put("pdf", textScaleMapPDF );

        Map<String, Float> defaultTextScaleMap = new HashMap<>();
        defaultTextScaleMap.put( "dxf", 0.45f );
        defaultTextScaleMap.put( "sfc", 0.45f );
        defaultTextScaleMap.put("pdf", 10f );

        float defaultValue = Optional.ofNullable(defaultTextScaleMap.get( exportFileType )).orElse(10f);

        Map<Float, Float> selectedMap = Optional.ofNullable( exportFileTypeMap.get( exportFileType ) ).orElse( textScaleMapDXF );

        return Optional.ofNullable( selectedMap.get( getPrintScale(drawingScale)*10 ) ).orElse( defaultValue );

    }

    public float getPrintScale(float drawingScale){ // ex. 1/100 is w40m h27m drawing in range.
        scale( new PointXY(0f,0f), 1/drawingScale );
        float longsidex = measureMostLongLine().getX();
        float longsidey = measureMostLongLine().getY();
        scale( new PointXY(0f,0f), drawingScale );

        float paperWidth = 38;
        float paperHeight = 25;

//        float printScale = 1f; //drawingScale;
        //if( longsideX <= paperWidth*0.2 && longsideY <= paperHeight*0.2 ) return printScale *= 0.2f;
        if( longsidex <= paperWidth*0.5 && longsidey <= paperHeight*0.4 ) return 0.5f;
        if( longsidex <= paperWidth     && longsidey <= paperHeight     ) return 1.0f;
        if( longsidex <= paperWidth*1.5 && longsidey <= paperHeight*1.4 ) return 1.5f;
        if( longsidex <= paperWidth*2.0 && longsidey <= paperHeight*1.9 ) return 2.0f;
        if( longsidex <= paperWidth*2.5 && longsidey <= paperHeight*2.5 ) return 2.5f;
        if( longsidex <= paperWidth*3.0 && longsidey <= paperHeight*3.0 ) return 3.0f;
        if( longsidex <= paperWidth*4.0 && longsidey <= paperHeight*4.0 ) return 4.0f;
        if( longsidex <= paperWidth*4.5 && longsidey <= paperHeight*4.5 ) return 4.5f;
        if( longsidex <= paperWidth*5.0 && longsidey <= paperHeight*5.0 ) return 5.0f;
        if( longsidex <= paperWidth*6.0 && longsidey <= paperHeight*5.0 ) return 6.0f;
        if( longsidex <= paperWidth*7.0 && longsidey <= paperHeight*5.0 ) return 7.0f;
        if( longsidex <= paperWidth*8.0 && longsidey <= paperHeight*5.0 ) return 8.0f;
        if( longsidex <= paperWidth*9.0 && longsidey <= paperHeight*5.0 ) return 9.0f;
        if( longsidex <= paperWidth*10.0 && longsidey <= paperHeight*10.0 ) return 10.0f;

        return 15f;
    }

    public float getAngle(){return myAngle;}

    public void setAngle(float angle) { myAngle = angle;}

    public Bounds calcBounds(){
        myBounds = new Bounds(0,0,0,0);
        for(int i = 0; i< trilist_.size(); i++){
            myBounds = trilist_.get(i).expandBoundaries(myBounds);
        }

        return myBounds;
    }

    public PointXY getCenter(){
        calcBounds();
        myCenter.set((myBounds.getRight()+myBounds.getLeft())/2, (myBounds.getTop()+myBounds.getBottom())/2);
        return myCenter;
    }

    public PointXY measureMostLongLine(){
        calcBounds();
        myLength.set(myBounds.getRight()-myBounds.getLeft(), myBounds.getTop()-myBounds.getBottom());
        return myLength;
    }

    public PointXY measureLongLineNotScaled(){
        return measureMostLongLine().scale( 1/myScale );
    }


    public float rotateByLength(String align) {
        float rot = 0f;
        if(align.equals("laydown")){
            for(;;) {
                rot -= 10f;
                float beforeY = measureMostLongLine().getY();
                rotate(getBasepoint(), rot, 0, false);
                if( measureMostLongLine().getY() >= beforeY){
                    rotate(getBasepoint(), -rot, 0, false);
                    return rot+10f;
                }
            }
        }
        if(align.equals("riseup")){
            for(;;) {
                rot += 10f;
                float beforeY = measureMostLongLine().getY();
                rotate(getBasepoint(), rot, 0, false);
                if( measureMostLongLine().getY() <= beforeY){
                    rotate(getBasepoint(), -rot, 0, false);
                    return rot-10f;
                }
            }
        }

        return rot;
    }


    TriangleList(){
        this.trilist_ = new ArrayList<>();
        this.trilistStored_ = new ArrayList<>();
        this.myCollisionList = new ArrayList<>();
        outlineList_ = new ArrayList<>();
        current = trilist_.size();
    }

    TriangleList(Triangle myFirstTriangle){
        this.trilist_ = new ArrayList<>();
        this.trilistStored_ = new ArrayList<>();
        this.trilist_.add(myFirstTriangle);
        myFirstTriangle.setNumber(1);
        current = trilist_.size();
    }

    public float getScale(){
        return  myScale;
    }

    public void setScale(PointXY bp, float sc) {
        myScale = sc;
        setBasepoint(bp.clone());
//        this.cloneByScale(basepoint, myScale);
    }

    /*public void cloneByScale(PointXY basepoint, float scale){
        myTriListAtView.clear();
        for (int i = 0; i < trilist_.size(); i++ ) {
            myTriListAtView.add(trilist_.get(i).clone());
            myTriListAtView.get(i).scale(basepoint, scale);
        }
    }*/

    public void scale(PointXY basepoint, float scale){
        myScale *= scale;
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).scale(basepoint, scale);
        }
    }

    public void scaleAndSetPath(PointXY basepoint, float scale, float ts ){
        myScale *= scale;
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).scale(basepoint, scale);
            trilist_.get(i).setDimPath(ts);
        }
    }

    public void setDimPathTextSize(float ts ){
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).setDimPath(ts);
        }
    }


    public void rotate(PointXY bp, float angle, int startnumber, Boolean separationFreeMode) {
        int startindex = startnumber -1;

        // 0番からすべて回転させる場合
        if(!(startnumber > 1 && trilist_.get(startindex).parentBC_ >= 9) || !separationFreeMode ) {
            myAngle += angle;
            setBasepoint(bp.clone());
            startindex = 0;
        }

        for (int i = startindex; i < trilist_.size(); i++ ) {
            trilist_.get(i).rotate( trilist_.get(startindex).point[0], angle, false );
            trilist_.get(i).pointNumber_ = trilist_.get(i).pointNumber_.rotate(getBasepoint(), angle);
        }

    }

    public void recoverState(PointXY bp) {
        setBasepoint(bp.clone());
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).rotate(getBasepoint(), myAngle-180, false);

        }
    }

    @Override
    public int size(){ return trilist_.size(); }

    @Override
    public int addCurrent(int i){
        current = current + i;
        return current;
    }

    @Override
    public int retrieveCurrent(){return current;}


    public void setCurrent(int c){current = c;}

    public boolean validTriangle(Triangle tri){
        if (tri.length[0] <= 0.0f || tri.length[1] <= 0.0f || tri.length[2] <=0.0f) return false;
        return !((tri.length[0] + tri.length[1]) <= tri.length[2]) &&
                !((tri.length[1] + tri.length[2]) <= tri.length[0]) &&
                !((tri.length[2] + tri.length[0]) <= tri.length[1]);
    }

    public boolean add( int pnum, int pbc, float A, float B, float C ) {
        return add( new Triangle(  get( pnum ), pbc, A, B, C ), true);
    }

    public boolean add( int pnum, int pbc, float B, float C ) {
        return add( new Triangle(  get( pnum ), pbc, B, C ), true);
    }

    public boolean add(Triangle nextTriangle, Boolean numbering){
        if(!validTriangle(nextTriangle)) return false;

        //trilistStored_ = (ArrayList<Triangle>) trilist_.clone();

        // 番号を受け取る
        if( numbering ) nextTriangle.myNumber_ = trilist_.size() + 1;

        int pbc = nextTriangle.parentBC_;

        if( nextTriangle.parentNumber_ > 0 ){
            Triangle parent = getMemberByIndex(nextTriangle.parentNumber_);
            if(parent.alreadyHaveChild(pbc)){
                // すでに親の接続辺上に子供がいたら、挿入処理
                //nextTriangle.myNumber_ = nextTriangle.parentNumber_ +1;
                insertAndSlide(nextTriangle);
            }
            // いなければ、末尾に足す
            else{
                trilist_.add( nextTriangle);   // add by arraylist
                // 自身の番号が付く
                //nextTriangle.setNumber(myTriList.size());
            }

            // 親に告知する
            //if( nextTriangle.myNumber_ > 1 ) trilist_.get( nextTriangle.parentNumber_ -1 ).setChild(nextTriangle, nextTriangle.getParentBC());
        }
        // 親がいない場合もある
        else{
            trilist_.add( nextTriangle);   // add by arraylist
        }

        current = nextTriangle.myNumber_;
        //lastTapNum_ = 0;
        //lastTapSide_ = -1;

        return true;
    }

    public void resetAllNodes(){
        for(int i = 0; i < trilist_.size(); i++) {
            Triangle tri = trilist_.get(i);

            if( tri.nodeTriangleA_ != null ){
                tri.setNode( trilist_.get( tri.nodeTriangleA_.myNumber_ - 1 ), 0 );
            }
            if( tri.nodeTriangleB_ != null ){
                tri.setNode( trilist_.get( tri.nodeTriangleB_.myNumber_ - 1 ), 1 );
            }
            if( tri.nodeTriangleC_ != null ){
                tri.setNode( trilist_.get( tri.nodeTriangleC_.myNumber_ - 1 ), 2 );
            }
        }
    }

    public void setChildsToAllParents(){
            for(int i = 0; i < trilist_.size(); i++){
                try{
                    int pnForMe = trilist_.get(i).parentNumber_;
                    Triangle me = trilist_.get(i);
                    if( pnForMe > -1 ){
                        // 改善版
                        Triangle parent = trilist_.get( pnForMe - 1 );
                        // 親に対して、
                        parent.setChild( me, me.getParentBC() );
                    }
                }
                catch ( NullPointerException e) {
                    System.out.println( "NullPointerException!! trilistsize:" + trilist_.size() + " index:" + i );
                }

            }
    }

    public void insertAndSlide( Triangle nextTriangle ){
        trilist_.add(nextTriangle.myNumber_-1, nextTriangle);
        getMemberByIndex(nextTriangle.parentNumber_).setChild(nextTriangle, nextTriangle.getParentBC());

        //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
        rewriteAllNodeFrom(nextTriangle, +1);

        resetTriangles(nextTriangle.myNumber_, nextTriangle);
    }

    //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
    // この関数自体は、どの三角形も書き換えない。
    public void rewriteAllNodeFrom(Triangle target, int numberChange){
        for(int i = target.myNumber_; i < trilist_.size(); i++){

            Triangle parent = trilist_.get(i);

            if (parent.hasConstantParent() || parent.parentNumber_ > target.myNumber_) {
                parent.parentNumber_ += numberChange;
            }
            parent.myNumber_ += numberChange;
        }
    }

    @Override
    public void remove( int number ){
        //number = lastTapNum_;

        //１番目以下は消せないことにする。
        if (number <= 1) return;

        trilist_.get( number - 1 ).nodeTriangleA_.removeNode( trilist_.get( number - 1 ) );//removeTheirNode();
        trilist_.remove(number-1);

        //ひとつ前の三角形を基準にして
        Triangle parentTriangle = trilist_.get(number-2);
        //次以降の三角形の親番号を全部書き換える
        rewriteAllNodeFrom( parentTriangle, -1 );

        resetTriangles(parentTriangle.myNumber_, parentTriangle);

        current = number - 1;
        lastTapNumber_ = number - 1;
        lastTapSide_ = -1;

        //this.cloneByScale(basepoint, myScale);
    }

    public ConnParam rotateCurrentTriLCR(){
        if( lastTapNumber_ < 2 ) return null;

        Triangle curTri = trilist_.get(lastTapNumber_ -1);
        ConnParam cParam = curTri.rotateLCRandGet().cParam_.clone();
        //if( lastTapNum_ < myTriList.size() ) myTriList.get(lastTapNum_).resetByParent( curTri, myTriList.get(lastTapNum_).cParam_ );

        // 次以降の三角形を書き換えている
        for(int i = 0; i < trilist_.size(); i++) {
            Triangle nextTri = trilist_.get(i);
            if( i > 0) {
                int npmi = nextTri.nodeTriangleA_.myNumber_-1;
                if( npmi == curTri.myNumber_ -1 ) nextTri.setParent(curTri.clone());
                nextTri.resetByParent(trilist_.get(npmi), nextTri.cParam_);
            }
        }
        //resetMyChild( myTriList.get(lastTapNum_-1), myTriList.get(lastTapNum_-1).cParam_ );

        return cParam;
    }

    public void resetNodeByID(Params prms ){

        ArrayList<Triangle> doneObjectList = new ArrayList<>();

        trilist_.get( prms.getN() - 1 ).resetNode( prms, trilist_.get( prms.getPn() - 1 ), doneObjectList );

    }

    public boolean resetFromParam(Params prms ){

        int ci = prms.getN()-1;
        Triangle tri = trilist_.get( ci );

        // 親番号が書き換わっている時は入れ替える。ただし現在のリストの範囲外の番号は除く。
        int pn = prms.getPn();
        if( pn != tri.parentNumber_ && pn < trilist_.size() && pn > 0 )
            tri.setNode( trilist_.get( pn - 1 ), 0 );

        tri.reset( prms );

        // 関係する三角形を書き換える
        resetTriangles( prms.getN(), tri);

        return true;
    }

    public boolean resetTriangles(int cNum, Triangle curtri){
        if(curtri == null)  return false;
        if(!curtri.validTriangle()) return false;

        //ノードの配列として定義した方がすっきりまとまる気がする。
        //親方向に走査するなら0、自身の接続方向がCなら２、二つの引数を渡して、総当たりに全方向走査する

        //trilistStored_ = (ArrayList<Triangle>) trilist_.clone();

        curtri.myNumber_ = cNum; //useless?

        // 親がいるときは、子接続を書き換える
        if( curtri.parentNumber_ > 0 && cNum - 2 >= 0 ){
            Triangle parent = trilist_.get( curtri.parentNumber_ - 1 );
            parent.childSide_ = curtri.parentBC_;
            curtri.nodeTriangleA_ = parent; //再リンクしないと位置と角度が連動しない。

            int curPareNum = trilist_.get(cNum-1).parentNumber_;
            // 自分の親番号と、親の三角形の番号が一致したとき？
            if(cNum > 1 && curPareNum == parent.myNumber_ ){ //trilist_.get(curPareNum-1)

                parent.resetByChild(curtri);
            }

        }

        // 自身の書き換え
        Triangle me = trilist_.get(cNum-1);
        //me.setNode( trilist_.get( ));


        // 浮いてる場合、さらに自己番が最後でない場合、一個前の三角形の位置と角度を自分の変化にあわせて動かしたい。
        if( curtri.parentNumber_ <= 0 && trilist_.size() != cNum && notHave(curtri.nodeTriangleA_)) {
            curtri.resetByChild( trilist_.get(cNum) );
        }

        me.reset(curtri);// ここがへん！ 親は自身の基準角度に基づいて形状変更をするので、それにあわせてもう一回呼んでいる。


        // 子をすべて書き換える
        if( trilist_.size() > 1 && cNum < trilist_.size() && curtri.hasChild()) resetMyChild(trilist_.get(cNum-1));

        //lastTapNum_ = 0;
        //lastTapSide_ = -1;

        return true;
    }

    //　ターゲットポインターがリストの中にいたらtrue
    public boolean notHave(Triangle target ){
        for ( int i = 0; i < trilist_.size(); i++ ){
            if( trilist_.get( i ) == target ) return true;
        }
        return false;
        //return trilist_.contains( target );
    }

    public void resetMyChild(Triangle newParent){
        //myTriList.get(2).reset(newParent, 1);
        if(newParent == null) return;

        // 新しい親の次から
        for(int i = newParent.myNumber_; i < trilist_.size(); i++) {
            // 連番と派生接続で分岐。
            if( trilist_.get(i).parentNumber_ == newParent.myNumber_) {
                // ひとつ前の三角形の子接続を書き換える？
                if( i+1 < trilist_.size() ) newParent.childSide_ = trilist_.get(i+1).parentBC_;

                if(trilist_.get(i).resetByParent(newParent, trilist_.get(i).parentBC_)) return;
                // 自身が次の親になる
                newParent = trilist_.get(i);
            }
            else { //連番でないとき
                //親を番号で参照する
                if(trilist_.get(i).resetByParent(trilist_.get(trilist_.get(i).parentNumber_ - 1), trilist_.get(i).parentBC_)) return;
                trilist_.get(trilist_.get(i).parentNumber_ -1).childSide_ = trilist_.get(i).parentBC_;
            }
            //else myTriList.get(i).reload();
        }
    }


    public boolean replace(int number, int pnum){
        if( trilist_.get(number-1) == null ) return false;
        Triangle me = trilist_.get(number-1);
        Triangle pr = trilist_.get(pnum-1);

        me.setOn( pr, me.parentBC_, me.length[0], me.length[1], me.length[2] );

        return true;
    }

    public boolean validTriangle(Params prm){
        if (prm.getA()<=0.0f || prm.getB()<=0.0f || prm.getC()<=0.0f) return false;
        return !((prm.getA() + prm.getB()) <= prm.getC()) &&
                !((prm.getB() + prm.getC()) <= prm.getA()) &&
                !((prm.getC() + prm.getA()) <= prm.getB());
    }

    // オブジェクトポインタを返す。
    public Triangle getMemberByIndex(int number){
        if( number < 1 || number > trilist_.size() ) return new Triangle();  //under 0 is empty. cant hook null. can hook lenA is not 0.
        else return this.trilist_.get(number-1);
    }

    @NotNull
    @Override // by number start 1, not index start 0.
    public Triangle get(int number){
        return getMemberByIndex(number);
    }

    public ArrayList<TriangleList> spritByColors(){
        ArrayList<TriangleList> listByColors = new ArrayList<>();

        listByColors.add(new TriangleList()); //0 val lightColors_ = arrayOf(LightPink_, LightOrange_, LightYellow_, LightGreen_, LightBlue_)
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList()); //4

        for(int colorindex = 4; colorindex > -1; colorindex -- ) {

            TriangleList listByColor = listByColors.get( colorindex );

            for (int i = 0; i < trilist_.size(); i++) {
                if( trilist_.get( i ) != null ) {
                    Triangle t = trilist_.get( i ).clone();
                    if( t.color_ == colorindex ) listByColor.add( t, false ); // 番号変更なしで追加する deep or shallow

                    t.isColored();
                }
            }

            if( listByColor.trilist_.size() > 0 ) listByColor.outlineList();
        }

        return listByColors;
    }

    public void outlineList(){
        if( trilist_.size() < 1 ) return;

        ArrayList<ArrayList<PointXY>> olplists = outlineList_;

        StringBuilder sb = new StringBuilder();

        for( int i = 0; i < trilist_.size(); i ++ ){
            sb.setLength(0);
            Triangle t = trilist_.get( i );

            if( i == 0 || t.isFloating() || t.isColored_ ) {
                olplists.add( new ArrayList<>() );
                trace( olplists.get( olplists.size()-1 ), t, true );
                olplists.get( olplists.size()-1 ).add( trilist_.get( i ).pointAB_ ); //今のindexでtriを取り直し
                outlineStr_ = sb.append(outlineStr_).append(( trilist_.get( i ).myNumber_)).append("ab, ").toString();//outlineStr_ + ( trilist_.get( i ).myNumber_ + "ab, " );

            }
        }

    }

    public ArrayList<PointXY> trace(ArrayList<PointXY> olp, Triangle tri, Boolean first){
        if( tri == null ) return olp;
        if( ( tri.isFloating() || tri.isColored() ) && !first ) return olp;

        addOlp(tri.pointAB_, "ab,", olp, tri);
        trace( olp, tri.nodeTriangleB_, false);

        addOlp(tri.pointBC_, "bc,", olp, tri);
        trace( olp, tri.nodeTriangleC_, false);

        addOlp(tri.point[0], "ca,", olp, tri);
        //trace( olp, tri.nodeTriangleA_, -1 ); //ネスト抜けながら勝手にトレースしていく筈

        return olp;
    }

    public void addOlp(PointXY pt, String str, ArrayList<PointXY> olp, Triangle node ){
        if( notHave(pt, olp)){
            olp.add( pt );
            outlineStr_ += node.myNumber_ + str;
        }
    }

    // 同じポイントは二ついらない
    private boolean notHave(PointXY it, ArrayList<PointXY> inthis ){
        //if( inthis.size() < 1 ) return false;

        for( int i=0; i<inthis.size(); i++ )
            if( it.nearBy(inthis.get(i), 0.001f) ) return false;

        return true;
    }

    public void traceOrNot( Triangle t, Triangle node, int origin, ArrayList<PointXY> olp ){
        if( node != null && !node.isFloating() && !node.isColored() ) traceOrJumpForward( node.myNumber_ - 1, origin, olp, node );
    }

    public void branchOrNot( Triangle t, int origin, ArrayList<PointXY> olp ){
        if( t.nodeTriangleB_ != null &&  t.nodeTriangleC_ != null )
            if( notHave( t.nodeTriangleC_.point[0], olp ) && ( !t.nodeTriangleC_.isFloating_ && !t.nodeTriangleC_.isColored_ ) )
                traceOrJumpForward( t.nodeTriangleC_.myNumber_ - 1, origin, olp, t.nodeTriangleC_ );
    }

    public int isCollide(PointXY tapP){
        for(int i = 0; i < trilist_.size(); i++) {
            if( trilist_.get(i).isCollide(tapP) ) return lastTapCollideNum_ = i+1;
        }

        return 0;
    }

    public void dedmapping(DeductionList dedlist, int axisY){
        for(int i = 0; i < trilist_.size(); i++) trilist_.get(i).dedcount = 0;

        for(int i = 0; i < dedlist.size(); i++ ){
            for(int ii = 0; ii < trilist_.size(); ii++) {
                boolean isCol = trilist_.get(ii).isCollide( dedlist.get(i+1).getPoint().scale( new PointXY(1f, axisY ) ) );
                if( isCol ) {
                    trilist_.get(ii).dedcount++;
                    dedlist.get(i+1).setParentNum( trilist_.get(ii).myNumber_ );
                }
            }
        }
    }

    public int[] getTapIndexArray(PointXY tapP){
        int[] tapIndexArray = new int[trilist_.size()];
        for(int i = 0; i < trilist_.size(); i++) {
            tapIndexArray[i] = trilist_.get(i).getTapLength( tapP, 0.6f);
        }
        return tapIndexArray;
    }

    public int getTapHitCount(PointXY tapP ){
        int hitC = 0;
        for(int i = 0; i < trilist_.size(); i++) {
            if( trilist_.get(i).getTapLength( tapP, 0.6f) != -1 ) hitC++;
        }

        return hitC;
    }

    public int getTap(PointXY tapP, float rangeRadius){
        int ltn = lastTapNumber_ +lastTapSide_;

        isCollide(tapP);

        for(int i = 0; i < trilist_.size(); i++) {

            if( trilist_.get(i).getTapLength(tapP, rangeRadius) != -1 ) {

                lastTapNumber_ = i + 1;
                lastTapSide_ = trilist_.get(i).getTapLength(tapP, rangeRadius);

                //if( i > 0 && lastTapSide_ == 0 ){
                //                        lastTapNumber_ = i;
                //                      lastTapSide_ = trilist_.get(i-1).getTapLength(tapP);
                //}
                isDoubleTap_ = ltn == lastTapSide_ + lastTapNumber_;
            }
        }

        if( getTapHitCount( tapP ) == 0 ) {
            lastTapNumber_ = 0;
            isDoubleTap_ = false;
        }
        //Log.d("TriangleList", "Tap Triangle num: " + lastTapNumber_ + ", side:" + lastTapSide_ );

        return lastTapNumber_ * 10 + lastTapSide_;
    }

    public ArrayList<Triangle> move(PointXY to){
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).move(to);
        }
        getBasepoint().add(to);
        return trilist_;
    }

    @NotNull
    @Override
    public Params getParams(int i) {
        if(i> trilist_.size())return new Params("","",0,0f,0f,0f,0,0, new PointXY(0f,0f),new PointXY(0f,0f));
        Triangle t = trilist_.get(i-1);
        return new Params(t.getMyName_(),"",t.getMyNumber_(), t.getLengthA(), t.getLengthB(), t.getLengthC(), t.getParentNumber(), t.getParentBC(), t.getPointCenter_(), t.getPointNumberAutoAligned_());
    }

    @Override
    public float getArea( ){
        float area = 0f;
        for(int i = 0; i< trilist_.size(); i++){
            area += trilist_.get(i).getArea();
        }
        return (float)(Math.round( area * 100.0) * 0.01 );
    }

    public float getAreaI( int number ){
        float area = 0f;
        for(int i = 0; i < number; i++){
            area += trilist_.get(i).getArea();
        }
        return (float)(Math.round( area * 100.0) * 0.01 );
    }

    public float getAreaC( int number ){
        if( number < 1 ) return 0f;
        int index = number - 1;

        int clr = trilist_.get( index ).color_;

        TriangleList listByColor = spritByColors().get( clr );

        System.out.printf( "listbycolor[%s], size %s%n", clr, listByColor.size() );

        return (float)(Math.round( listByColor.getArea() * 100.0 ) * 0.01 );
    }

    public String getInfo() {
        String angle = String.valueOf(getAngle());
        String area = String.valueOf(getArea());
        return "Trilist- num:" + trilist_.size() + " angle:" + angle + " area:" + area;
    }

    public void addOutlinePoint(PointXY pt, String str, ArrayList<PointXY> olp, Triangle node){
        if( node != null && notHave(pt, olp)){
            olp.add( pt );
            outlineStr_ += node.myNumber_ + str;
        }
    }

    public ArrayList<PointXY> traceOrJumpForward(int startindex, int origin, ArrayList<PointXY> olp, Triangle node){
        if( startindex < 0 || startindex >= trilist_.size() ) return null;

        // AB点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointAB_, "ab,",olp, node);

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node, node.nodeTriangleB_, origin, olp);

        // BC点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointBC_, "bc,",olp, node);

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node, node.nodeTriangleC_, origin, olp);

        // 折り返し
        traceOrJumpBackward(startindex, origin, olp, node);

        return olp;
    }

    public void traceOrJumpBackward(int startindex, int origin, ArrayList<PointXY> olp, Triangle node){

        // 派生（ふたつとも接続）していたらそっちに伸びる、フロート接続だったり、すでに持っている点を見つけたらスルー
        branchOrNot(node, origin, olp );

        //BC点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointBC_, "bc,",olp, node);

        //CA点を取る。すでにあったらキャンセル
        addOutlinePoint(node.point[0], "ca,",olp, node);

        //AB点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointAB_, "ab,",olp, node);

        //AB点を取る。ダブりを許容
        //if( t.nodeTriangleA_ == null || t.isColored_ || t.isFloating_ ){
          //  olp.add( t.pointAB_ );
            //outlineStr_ += t.myNumber_ + "ab, ";
        //}

        // 0まで戻る。同じ色でない時はリターン
        if( node.myNumber_ <= node.parentNumber_ ) return;
        if( node.nodeTriangleA_ != null && !node.isColored() && !node.isFloating() ) traceOrJumpBackward(node.parentNumber_ - 1, origin, olp, node.nodeTriangleA_ );

    }




    public TriangleList resetNumReverse( ){

        TriangleList rev = this.clone();

        int iBackward = trilist_.size();
        //マイナンバーの書き換え
        for( int i = 0; i < trilist_.size(); i++ ) {
            rev.trilist_.get( i ).myNumber_ = iBackward;
            iBackward--;
        }

        return rev;
    }


    @Override
    public TriangleList reverse(){

        for( int i = 0; i < trilist_.size(); i++ ) {
            //終端でなければ
            trilist_.size();// 連番でない時は
//return this;//akirameru
        }

        // 番号だけを全部逆順に書き換え
        TriangleList numrev = resetNumReverse();

        for( int i = 0; i < trilist_.size(); i++ ) {
            Triangle me = numrev.trilist_.get(i);

            //接続情報の書き換え
            //終端でなければ
            if (i + 1 < trilist_.size()) {
                Triangle next = numrev.trilist_.get(i+1);

                // 連番の時は
                if( i + 1 == next.parentNumber_){
                    // 自身の番号-1を親とする
                    me.parentNumber_ = me.myNumber_ - 1;
                    me.rotateLengthBy( next.getParentSide() );

                    me.setNode( next, 0 );

                    //たとえば次がB辺接続だった場合、自分のB辺がA辺となるように全体の辺長配置を時計回りに回す。
                    //それから、次の接続辺指定を自身に移植する。
                    //このとき、二重断面指定はたぶん逆になる。
                    //次がB辺接続だったとしても、そのまた次もB辺接続だったら、次のやつの辺長が配置換えされるので、接続がおかしくなる
                    //次のやつのB辺はC辺にあった。
                    //同じ接続方向が続いている時は、PBCをCtoB、BtoCに反転させる。
                    if( i + 2 < trilist_.size() ){
                        Triangle nextnext = numrev.trilist_.get(i+2);
                        if( next.getParentSide() == nextnext.getParentSide() ) me.setReverseDefSide( next.parentBC_, true );
                        else if( i + 2 != nextnext.parentNumber_ && next.parentBC_ != 1 ) me.setReverseDefSide( - next.parentBC_ + 3, false );
                        else me.setReverseDefSide( next.parentBC_, false);
                    }
                    else me.setReverseDefSide( next.parentBC_, false);

                    next.setNode( me, me.getParentSide() );
                }
                else{
                    //連番でないときは
                    me.parentNumber_ = -1; //next.parentNumber_;
                    me.parentBC_ = -1;

                    me.rotateLengthBy( 2 );

                    me.setNode( me.nodeTriangleA_, me.getParentSide() );

                    //next.parentNumber_ = trilist_.get( next.parentNumber_ - 1 ).myNumber_;
                    //next.parentBC_ =  trilist_.get( next.parentNumber_ - 1 ).parentBC_;
                }

            } else if (i + 1 == trilist_.size()) {
                // 終端の時、つまり一番最初のやつ。
                me.parentNumber_ = 0;
                // 終端は、自身の接続辺をもとにいっこまえの接続が決まっているので、それに合わせて辺長を回す。
                // 最初の三角形になるので、接続情報は要らない。
                me.rotateLengthBy( - me.getParentSide() + 3 );
            }
        }

        // 逆順にソートし、新規に足していく。リビルドした方が考え方が楽。
        TriangleList rev = this.clone();
        rev.trilist_.clear();

        for( int i = trilist_.size() - 1; i > -1; i-- ) {
            Triangle it = numrev.trilist_.get( i );

            // 自身の番号よりも親番号が大きい場合を許容する
            // 子の三角形を指定して生成する。
            //rev.add( new Triangle( trilist_.get( it.parentNumber_), it.parentBC_, it.lengthAforce_, it.lengthBforce_, it.lengthCforce_ ) );

            if( it.parentNumber_ < 1 ) rev.add( it.clone(), true); //new Triangle( it.lengthAforce_, it.lengthBforce_, it.lengthCforce_, it.point[0], it.angleInGlobal_)
            else rev.add( new Triangle( rev.get( it.parentNumber_), it.parentBC_, it.getLengthAforce_(), it.getLengthBforce_(), it.getLengthCforce_() ), true);
        }

        // 名前の書き換え、反転後のひとつ先の三角形に移設し、1番の名前は消去
        for( int i = 0; i < trilist_.size(); i++ ) {
            Triangle it = numrev.trilist_.get( trilist_.size() - i - 1 );
            if( i + 2 <= trilist_.size() ) rev.get( i + 2 ).myName_ = it.myName_;
        }
        rev.get( 1 ).myName_ = "";

        return rev;
    }

    public TriangleList numbered( int start ){
        TriangleList rev = new TriangleList();

        for( int i = 0; i < trilist_.size(); i++ ) {
            Triangle ti = trilist_.get( i ).clone();
            ti.myNumber_ = start + i;
            rev.trilist_.add( ti );
        }

            return rev;
        }

}// end of class


