package com.jpaver.trianglelist;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static java.lang.Integer.parseInt;

public class TriangleList extends EditList implements Cloneable {
    ArrayList<Triangle> trilist_;
    ArrayList<Triangle> myTriListAtView;
    ArrayList<Collision> myCollisionList;

    int current;// = 0;
    float myScale = 1f;
    boolean imScaled = false;

    float myAngle = 0f;
    PointXY basepoint = new PointXY(0f,0f);

    Bounds myBounds = new Bounds(0f,0f,0f,0f);
    PointXY myCenter = new PointXY(0f,0f);
    PointXY myLength = new PointXY(0f,0f);

    int lastTapNum_ = 0;
    int lastTapSide_ = -1;
    int lastTapCollideNum_ = 0;

    Boolean isDoubleTap_ = false;

    @Override
    public TriangleList clone(){
        TriangleList b = new TriangleList();
        try {
            b.basepoint = basepoint.clone();
            b.myBounds = myBounds;
            b.myCenter = myCenter.clone();
            b.myLength = myLength.clone();
            b = new TriangleList();
            b.current = this.current;
            b.lastTapNum_ = this.lastTapNum_;
            b.lastTapSide_ = this.lastTapSide_;
            b.myScale = this.myScale;
            b.myAngle = this.myAngle;
            b.basepoint = this.basepoint.clone();
            for(int i = 0; i < this.trilist_.size(); i++){
                b.trilist_.add(this.trilist_.get(i).clone());
                //b.myTriListAtView.add(this.myTriListAtView.get(i).clone());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return b;
    }

    public ArrayList<Triangle> getSokutenList(int start, int pitch ){
        ArrayList<Triangle> allSokTriList = getAllSokutenList();
        ArrayList<Triangle> sokTriList = new ArrayList<Triangle>();

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
            float lastx = lasttri.pointCA_.getX();
            float pastx = pasttri.pointCA_.getX();
            sokTriList.add( new Triangle( 5f, 5f, 5f) );
            sokTriList.get( sokTriList.size() - 1 ).pointCA_.setX( lastx + (lastx - pastx) );
            sokTriList.get( sokTriList.size() - 1 ).lengthAforce_ = lasttri.lengthAforce_;
            sokTriList.get( sokTriList.size() - 1 ).lengthA_ = lasttri.lengthA_;
        }

        // 最初にいっこ足す
        if( getSokutenListVector() < 0 && sokTriList.size() > 1 ){
            Triangle firsttri = sokTriList.get( 0 );
            Triangle secondtri = sokTriList.get( 1 );
            PointXY first = firsttri.pointCA_;
            PointXY second = secondtri.pointCA_;
            sokTriList.add( 0, new Triangle( 5f, 5f, 5f) );
            sokTriList.get( 0 ).pointCA_ = first.minus( second ).plus( first );
            sokTriList.get( 0 ).lengthAforce_ = firsttri.lengthAforce_;
            sokTriList.get( 0 ).lengthA_ = firsttri.lengthA_;
        }


        return sokTriList;
    }

    // 全ての測点リストを返す
    public ArrayList<Triangle> getAllSokutenList( ) {
        ArrayList<Triangle> numTriList = new ArrayList<Triangle>();
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

    public PointXY getUnScaledPointOfName(String name ){

        for (int i = 1; i < trilist_.size()+1; i++ ){
            if( trilist_.get(i).myName_.equals( name ) ) return trilist_.get(i).pointCA_.scale( 1/myScale );
        }

        return new PointXY( 0f, 0f );
    }

    public float getPrintTextScale (float drawingScale, String type){
        float ts = 10f;
        float dxfts = 0.4f;
        if( type.equals( "dxf" ) ) return dxfts;

        switch((int)(getPrintScale(drawingScale)*10)){
            case 100:
                ts = 2f;
                break;
            case 50:
                ts = 3.5f;
                break;
            case 45:
                ts = 3.5f;
                break;
            case 40:
                ts = 3.5f;
                break;
            case 30:
                ts = 5f;
                break;
            case 25:
                ts = 8f;
                break;
            case 20:
                ts = 8f;
                break;
            case 15:
                ts = 10f;
                break;
            case 10:
                ts = 10f;
                break;
            case 5:
                ts = 12f;
                break;
        }

        return ts;
    }

    public float getPrintScale(float drawingScale){ // ex. 1/100 is w40m h27m drawing in range.
        scale( new PointXY(0f,0f), 1/drawingScale );
        float longsideX = measureMostLongLine().getX();
        float longsideY = measureMostLongLine().getY();
        scale( new PointXY(0f,0f), drawingScale );

        float paperWidth = 38;
        float paperHeight = 25;

        float printScale = 1f; //drawingScale;
        //if( longsideX <= paperWidth*0.2 && longsideY <= paperHeight*0.2 ) return printScale *= 0.2f;
        if( longsideX <= paperWidth*0.5 && longsideY <= paperHeight*0.5 ) return printScale *= 0.5f;
        if( longsideX <= paperWidth     && longsideY <= paperHeight     ) return printScale *= 1.0f;
        if( longsideX <= paperWidth*1.5 && longsideY <= paperHeight*1.5 ) return printScale *= 1.5f;
        if( longsideX <= paperWidth*2.0 && longsideY <= paperHeight*2.0 ) return printScale *= 2.0f;
        if( longsideX <= paperWidth*2.5 && longsideY <= paperHeight*2.5 ) return printScale *= 2.5f;
        if( longsideX <= paperWidth*3.0 && longsideY <= paperHeight*3.0 ) return printScale *= 3.0f;
        if( longsideX <= paperWidth*4.0 && longsideY <= paperHeight*4.0 ) return printScale *= 4.0f;
        if( longsideX <= paperWidth*4.5 && longsideY <= paperHeight*4.5 ) return printScale *= 4.5f;
        if( longsideX <= paperWidth*5.0 && longsideY <= paperHeight*5.0 ) return printScale *= 5.0f;

        return printScale *= 10f;
    }

    public float getAngle(){return myAngle;}

    public void setAngle(float angle) { myAngle = angle;}

    public float roundByUnderTwo(float fp) {
        float ip = fp * 100f;
        float rfp = Math.round(ip) / 100f;
        return rfp;
    }

    public ArrayList<Collision> getNotSelectedDims(){
        myCollisionList = new ArrayList<Collision>();

        for(int i = 0; i< trilist_.size(); i++){
            myCollisionList.add(this.getChildAdress(i));
        }

        return myCollisionList;
    }

    public Collision getChildAdress(int i){
        Collision col = new Collision();


        return new Collision();
    }

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


    public float rotateByLength(String align){
        float rot = 0f;
        if(align == "laydown"){
            for(;;) {
                rot -= 10f;
                float beforeY = measureMostLongLine().getY();
                rotate(basepoint, rot);
                if( measureMostLongLine().getY() >= beforeY){
                    rotate(basepoint, -rot);
                    return rot+10f;
                }
            }
        }
        if(align == "riseup"){
            for(;;) {
                rot += 10f;
                float beforeY = measureMostLongLine().getY();
                rotate(basepoint, rot);
                if( measureMostLongLine().getY() <= beforeY){
                    rotate(basepoint, -rot);
                    return rot-10f;
                }
            }
        }

        return rot;
    }

    @Override // by number start 1, not index start 0.
    public Triangle get(int number){
        return getTriangle(number);
    }

    TriangleList(){
        this.trilist_ = new ArrayList<Triangle>();
        this.myTriListAtView = new ArrayList<Triangle>();
        this.myCollisionList = new ArrayList<Collision>();
        current = trilist_.size();
    }

    TriangleList(Triangle myFirstTriangle){
        this.trilist_ = new ArrayList<Triangle>();
        this.myTriListAtView = new ArrayList<Triangle>();
        this.trilist_.add(myFirstTriangle);
        myFirstTriangle.setNumber(1);
        current = trilist_.size();
    }

    public float getScale(){
        return  myScale;
    }

    public void setScale(PointXY bp, float sc){
        myScale = sc;
        basepoint = bp.clone();
//        this.cloneByScale(basepoint, myScale);
    }

    public void cloneByScale(PointXY basepoint, float scale){
        myTriListAtView.clear();
        for (int i = 0; i < trilist_.size(); i++ ) {
            myTriListAtView.add(trilist_.get(i).clone());
            myTriListAtView.get(i).scale(basepoint, scale);
        }
    }

    public void scale(PointXY basepoint, float scale){
        myScale *= scale;
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).scale(basepoint, scale);
        }
    }

    public void rotate(PointXY bp, float angle){
        myAngle += angle;
        basepoint = bp.clone();
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).rotate(basepoint, angle);
            trilist_.get(i).pointNumber_ = trilist_.get(i).pointNumber_.rotate(basepoint, angle);
        }
    }

    public void recoverState(PointXY bp){
        basepoint = bp.clone();
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).rotate(basepoint, myAngle-180);

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
    public int getCurrent(){return current;}

    @Override
    public void setCurrent(int c){current = c;}

    public boolean validTriangle(Triangle tri){
        if (tri.lengthA_ <= 0.0f || tri.lengthB_ <= 0.0f || tri.lengthC_ <=0.0f) return false;
        if ((tri.lengthA_ + tri.lengthB_) <= tri.lengthC_ ||
                (tri.lengthB_ + tri.lengthC_) <= tri.lengthA_ ||
                (tri.lengthC_ + tri.lengthA_) <= tri.lengthB_) return false;
        else
         return true;
    }

    public PointXY vectorToNextFrom( int i ){
        if( i < 0 || i >= trilist_.size() ) return new PointXY(0f, 0f);
        return trilist_.get( i ).pointCA_.vectorTo( trilist_.get( i + 1 ).pointCA_ );
    }

    public boolean add( int pnum, int pbc, float A, float B, float C ) {
        return add( new Triangle(  get( pnum ), pbc, A, B, C ) );
    }

    public boolean add( int pnum, int pbc, float B, float C ) {
        return add( new Triangle(  get( pnum ), pbc, B, C ) );
    }

    public boolean add(Triangle nextTriangle){
        if( validTriangle(nextTriangle) == false ) return false;

        // 番号を受け取る
        nextTriangle.myNumber_ = trilist_.size() + 1;

        // すでに親の接続辺上に子供がいたら、挿入処理
        int pbc = nextTriangle.myParentBC_;
        Triangle parent = getTriangle(nextTriangle.myParentNumber_);
        if( parent.alreadyHaveChild( pbc ) == true ){
            nextTriangle.myNumber_ = nextTriangle.myParentNumber_ +1;
            insertAndSlide(nextTriangle);
        }
        // いなければ、末尾に足す
        else{
            trilist_.add( nextTriangle);   // add by arraylist
            // 自身の番号が付く
            //nextTriangle.setNumber(myTriList.size());
        }

        // 親に告知する
        if( nextTriangle.myNumber_ > 1 ) trilist_.get( nextTriangle.myParentNumber_-1 ).setChild(nextTriangle, nextTriangle.getParentBC());

        current = nextTriangle.myNumber_;
        //lastTapNum_ = 0;
        //lastTapSide_ = -1;

        return true;
    }

    public void setChildsToAllParents(){
        for(int i = 0; i < trilist_.size(); i++){
            int pnForMe = trilist_.get(i).myParentNumber_;
            Triangle me = trilist_.get(i);
            if( pnForMe > -1 ){
                // 改善版
                Triangle parent = trilist_.get( pnForMe - 1 ); //batu->//index指定のget関数をnumberで呼んでいる。しかしなぜか上手くいっている。ふしぎー
                // 親に対して、
                parent.setChild( me, me.getParentBC() );
            }

        }
    }

    public void insertAndSlide( Triangle nextTriangle ){
        trilist_.add(nextTriangle.myNumber_-1, nextTriangle);
        getTriangle(nextTriangle.myParentNumber_ ).setChild(nextTriangle, nextTriangle.getParentBC());

        //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
        rewriteAllNumbersFrom(nextTriangle, +1);

        resetConnectedTriangles(nextTriangle.myNumber_, nextTriangle);
    }

    //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
    // この関数自体は、どの三角形も書き換えない。
    public void rewriteAllNumbersFrom( Triangle nextTriangle, int count){
        for(int i = nextTriangle.myNumber_; i < trilist_.size(); i++){
            Triangle tri2 = trilist_.get(i);
            if( tri2.hasConstantParent() == false && tri2.myParentNumber_ < nextTriangle.myNumber_ ) {
                tri2.myNumber_ += count;
            }
            else{
                tri2.myParentNumber_ += count;
                tri2.myNumber_ += count;
            }
        }
    }

    public boolean insert(int index, Triangle nextTriangle){
        if(nextTriangle == null || !nextTriangle.validTriangle())  return false;

        this.trilist_.add(index-1, nextTriangle);   // add(insert) by arraylist
        this.getTriangle(index).setNumber(index);

        // all child node change
        for(int iNext = index+1; iNext <= trilist_.size(); iNext++){

            if(this.getTriangle(iNext).getParentNumber() == this.getTriangle(index).getMyNumber_()) {
                this.getTriangle(iNext).resetByParent( this.getTriangle(iNext).parent_, this.getTriangle(iNext).getParentBC() );
                getTriangle(iNext-1).setChildSide_(getTriangle(iNext).getParentBC());
            }
            index++;
        }

        return true;
    }

    @Override
    public void remove(int number){
        number = lastTapNum_;

        //１番目以下は消せないことにする。
        if (number <= 1) return;

        trilist_.remove(number-1);

        //ひとつ前の三角形を基準にして
        Triangle nextTriangle = trilist_.get(number-2);
        //次以降の三角形の親番号を全部書き換える
        rewriteAllNumbersFrom( nextTriangle, -1 );

        resetConnectedTriangles(nextTriangle.myNumber_, nextTriangle);

        current = number -1;
        lastTapNum_ = number -1;
        lastTapSide_ = -1;

        //this.cloneByScale(basepoint, myScale);
    }

    public ConneParam rotateCurrentTriLCR(){
        if( lastTapNum_ < 2 ) return null;

        Triangle curTri = trilist_.get(lastTapNum_-1);
        ConneParam cParam = curTri.rotateLCRandGet().cParam_.clone();
        //if( lastTapNum_ < myTriList.size() ) myTriList.get(lastTapNum_).resetByParent( curTri, myTriList.get(lastTapNum_).cParam_ );

        for(int i = 0; i < trilist_.size(); i++) {
            Triangle nextTri = trilist_.get(i);
            if( i > 0) {
                int npmi = nextTri.parent_.myNumber_-1;
                if( npmi == curTri.myNumber_ -1 ) nextTri.setParent(curTri.clone(), nextTri.myParentBC_);
                nextTri.resetByParent(trilist_.get(npmi), nextTri.cParam_);
            }
        }
        //resetMyChild( myTriList.get(lastTapNum_-1), myTriList.get(lastTapNum_-1).cParam_ );

        return cParam;
    }

    public boolean resetTriConnection(int index, ConneParam cParam){

        //myTriList.get(index-1).reset(myTriList.get(index-1), cParam);
        //if( myTriList.size() > 1 ) resetChildsFrom(myTriList.get(index-1));
        //lastTapNum_ = 0;
        //lastTapSide_ = -1;

        return true;
    }

    public boolean resetConnectedTriangles(int index, Triangle rTri, ConneParam cParam){
        if(rTri == null || !rTri.validTriangle())  return false;

        rTri.myNumber_ = index;
        if(index > 1) trilist_.get(index-2).childSide_ = rTri.cParam_.getSide();
        if(index > 1) trilist_.get(index-2).resetByChild(rTri, cParam);
        trilist_.get(index-1).reset(rTri, cParam);
        if( trilist_.size() > 1 ) resetChildsFrom(rTri);

        return true;
    }

    public boolean resetConnectedTriangles(Params tParams ){

        Triangle curTri = trilist_.get( tParams.getN()-1 );

        // 親番号が書き換わっている時は入れ替える。ただし現在のリストの範囲外の番号は除く。
        int pn = tParams.getPn();

        if( pn != curTri.myParentNumber_ && pn < trilist_.size() && pn > 0 )
            curTri.parent_ = trilist_.get( pn - 1 );

        curTri.reset( tParams );

        // 関係する三角形を書き換える
        resetConnectedTriangles( tParams.getN(), curTri);

        return true;
    }


    public boolean resetConnectedTriangles(int index, Triangle nextTriangle){
        if(nextTriangle == null)  return false;
        if(!nextTriangle.validTriangle()) return false;

        nextTriangle.myNumber_ = index; //useless?

        // ひとつ前の三角形の子接続を書き換える？
        if(index > 1) trilist_.get(index-2).childSide_ = nextTriangle.myParentBC_;

        // 自分の親番号と、親の三角形の番号が一致したとき？
        int parentNumber = trilist_.get(index-1).myParentNumber_;
        if(index > 1 && parentNumber == trilist_.get(parentNumber-1).myNumber_ ){

            trilist_.get(parentNumber-1).resetByChild(nextTriangle);
        }

        // 自身の書き換え
        trilist_.get(index-1).reset(nextTriangle);

        // 子をすべて書き換える
        if( trilist_.size() > 1 ) resetMyChild(trilist_.get(index-1));

        //lastTapNum_ = 0;
        //lastTapSide_ = -1;

        return true;
    }

    public void resetMyChild(Triangle newParent){
        //myTriList.get(2).reset(newParent, 1);
        if(newParent == null) return;

        // 2番から全て
        for(int i = 1; i < trilist_.size(); i++) {
            // 連番と派生接続で分岐。
            if( trilist_.get(i).myParentNumber_ == newParent.myNumber_) {
                // ひとつ前の三角形の子接続を書き換える？
                if( i+1 < trilist_.size() ) newParent.childSide_ = trilist_.get(i+1).myParentBC_;

                if( !trilist_.get(i).resetByParent(newParent, trilist_.get(i).myParentBC_) ) return;
                // 自身が次の親になる
                newParent = trilist_.get(i);
            }
            else { //連番でないとき
                //親を番号で参照する
                if( !trilist_.get(i).resetByParent(trilist_.get(trilist_.get(i).myParentNumber_ -1), trilist_.get(i).myParentBC_) ) return;
                trilist_.get(trilist_.get(i).myParentNumber_ -1).childSide_ = trilist_.get(i).myParentBC_;
            }
            //else myTriList.get(i).reload();
        }
    }

    public void resetChildsFrom(Triangle rTri){
        if( lastTapNum_ <= 0 ) return;

        for(int i = lastTapNum_; i < trilist_.size(); i++) {
            Triangle nextTri = trilist_.get(i);
            nextTri.resetByParent(nextTri.parent_, nextTri.cParam_);
        }
    }


    public boolean replace(Params prm, Triangle ptri){
        if(validTriangle(prm)==false) return false;
        trilist_.get(prm.getN()-1).set(ptri, prm);
        trilist_.get(prm.getN()-2).setChildSide_(prm.getPl());

/*        if( prm.getN() < myTriList.size() ){
            Triangle childT = myTriList.get(prm.getN()+1);
            Params childP = childT.getParams();
            childP.setA(getLengthFromSide(childT.getParentBC(), prm));
            childT.set(myTriList.get(prm.getN()), childP);
        }
*/
        return true;
    }

    public float getLengthFromSide(int sideNum, Triangle tri){
        if(sideNum == 1 || sideNum == 3 || sideNum == 4 || sideNum == 7 || sideNum == 9) return tri.getLengthB_();
        if(sideNum == 2 || sideNum == 5 || sideNum == 6 || sideNum == 8 || sideNum == 10) return tri.getLengthC_();
        return 0f;
    }

    public boolean validTriangle(Params prm){
        if (prm.getA()<=0.0f || prm.getB()<=0.0f || prm.getC()<=0.0f) return false;
        if ((prm.getA() + prm.getB()) <= prm.getC() ||
                (prm.getB() + prm.getC()) <= prm.getA() ||
                (prm.getC() + prm.getA()) <= prm.getB() ) return false;
        else return true;
    }

    public Triangle getTriangle(int index){
        if( index < 1 || index > trilist_.size() )
            return new Triangle();  //under 0 is empty. cant hook null. can hook lenA is not 0.
        else return this.trilist_.get(index-1);
    }


    public ArrayList<TriangleList> spritByColors(){
        ArrayList<TriangleList> listByColors = new ArrayList<TriangleList>();

        listByColors.add(new TriangleList()); //0
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList());
        listByColors.add(new TriangleList()); //4

        for(int colorindex = 4; colorindex > -1; colorindex -- ) {
            for (int i = 0; i < trilist_.size(); i++) {
                if( trilist_.get( i ).color_ == colorindex ) listByColors.get( colorindex ).add( trilist_.get( i ) );
            }
        }

        return listByColors;
    }

    public int isCollide(PointXY tapP){
        for(int i = 0; i < trilist_.size(); i++) {
            if( trilist_.get(i).isCollide(tapP) ) return lastTapCollideNum_ = i+1;
        }

        return 0;
    }

    public int[] getTapIndexArray(PointXY tapP){
        int tapIndexArray[] = new int[trilist_.size()];
        for(int i = 0; i < trilist_.size(); i++) {
            tapIndexArray[i] = trilist_.get(i).getTapLength( tapP );
        }
        return tapIndexArray;
    }

    public int getTapHitCount(PointXY tapP ){
        int hitC = 0;
        for(int i = 0; i < trilist_.size(); i++) {
            if( trilist_.get(i).getTapLength( tapP ) != -1 ) hitC++;
        }

        return hitC;
    }

    public void getTap(PointXY tapP){
        int ltn = lastTapNum_+lastTapSide_;

        isCollide(tapP);

        for(int i = 0; i < trilist_.size(); i++) {

            if( trilist_.get(i).getTapLength(tapP) != -1 ) {

                lastTapNum_ = i + 1;
                lastTapSide_ = trilist_.get(i).getTapLength(tapP);

                if(ltn == lastTapSide_+lastTapNum_) {

                    isDoubleTap_ = true;

                    if( i > 0 && lastTapSide_ == 0 ){
                        lastTapNum_ = i;
                        lastTapSide_ = trilist_.get(i-1).getTapLength(tapP);
                    }

                }
                else isDoubleTap_ = false;
            }

        }

        if( getTapHitCount( tapP ) == 0 ) {
            lastTapNum_ = 0;
            isDoubleTap_ = false;
        }

    }

    public ArrayList<Triangle> move(PointXY to){
        for (int i = 0; i < trilist_.size(); i++ ) {
            trilist_.get(i).move(to);
        }
        basepoint.add(to);
        return trilist_;
    }

    @NotNull
    @Override
    public Params getParams(int i) {
        if(i> trilist_.size())return new Params("","",0,0f,0f,0f,0,0, new PointXY(0f,0f),new PointXY(0f,0f));
        Triangle t = trilist_.get(i-1);
        Params pm = new Params(t.getMyName_(),"",t.getMyNumber_(), t.getLengthA_(), t.getLengthB_(), t.getLengthC_(), t.getParentNumber(), t.getParentBC(), t.getPointCenter_(), t.getPointNumberAutoAligned_());
        return pm;
    }

    @Override
    public float getArea(){
        float area = 0f;
        for(int i = 0; i< trilist_.size(); i++){
            area += trilist_.get(i).getArea();
        }
        return (float)(Math.round( area * 100.0) / 100.0);
    }

    public ArrayList<ArrayList<PointXY>> getOutlineLists(){
        ArrayList<ArrayList<PointXY>> olplists = new ArrayList<ArrayList<PointXY>>();
        olplists.add( new ArrayList<>() );

        traceOrJumpForward( 0, olplists.get(0) );

        for( int i = 0; i < trilist_.size(); i ++ ){
            if( trilist_.get( i ).isFloating() ) {
                olplists.add( new ArrayList<>() );
                traceOrJumpForward( i, olplists.get(i) );
            }
        }

        return olplists;
    }

    //壁に右手をあてて洞窟を回る方式。
    public ArrayList<PointXY> getOutLinePoints( int start ){
        ArrayList<PointXY> olp = new ArrayList<PointXY>();

        int endnum = searchFloatintTriangleFrom( start );

        //olp = outlineForward( trilist_, start)
        //olp = outlineBackward( trilist_, endnum)
        // 右回りに前進。順繰り戻るのではなく子の番号に移る。接続がなくなるまで。フロート接続を含む。
        // 右回りに後退。順繰り戻るのではなく親の番号に移る。子接続があったら、上に戻って前進する。接続がなくなるまで。フロート接続を含む。

        //往路
        olp = goAroundCaveBySetRightHand( start, endnum );

        // 戻る場合。派生接続に行き当たった時は
        for( int i = endnum - 1; i > start - 2; i-- ){
            Triangle t = trilist_.get( i );
            PointXY before = olp.get( olp.size() - 1);

            // 先端に行き当たったら全点足す ただし図形の終点ではないとき。
            if( t.isChildB_ == false &&  t.isChildC_ == false && t.myNumber_ != endnum ){
                if( before.equals( t.pointAB_ ) == false ) olp.add( t.pointAB_ );
                if( before.equals( t.pointBC_ ) == false ) olp.add( t.pointBC_ );
                if( before.equals( t.pointCA_ ) == false ) olp.add( t.pointCA_ );
                continue;
            }

            // いっこ先のが二重断面ならBC点を足す
            if( i + 1 < trilist_.size() ) if( trilist_.get( i + 1 ).myParentBC_ == 4 || trilist_.get( i + 1 ).myParentBC_ == 7 ) olp.add( t.pointBC_ );

            // いっこ先のが非連続のときは回す
            if( i + 1 < trilist_.size() ) if( trilist_.get( i + 1 ).myParentNumber_ != t.myNumber_ ) continue;

            // 基本CA点がダブらないときは足していく方式。
            if( before.equals( t.pointCA_ ) == false ) olp.add( t.pointCA_ );

            // C接続があるときは。しかしこれだと次の番号がいっこ減る。たいていの場合接続先は進んだ番号なので、違う処理が必要。
            if( t.isChildC_ == true ) if( t.childC_.isFloating() == false ) continue;
            olp.add( t.pointCA_ );
        }

        return olp;
    }

    public ArrayList<PointXY> traceOrJumpForward( int startindex, ArrayList<PointXY> olp ){
        Triangle t = trilist_.get( startindex );

        //フロート接続はリターン
        if( t.isFloating() ) return olp;

        //AB点を取る。すでにあったらキャンセル
        if( exist( t.pointAB_, olp )  == false ) olp.add( t.pointAB_ );

        // 再起呼び出しで派生方向に右手伝いにのびていく
        if( t.isChildB_ == true ) traceOrJumpForward( t.childB_.myNumber_ - 1, olp );

        //BC点を取る。すでにあったらキャンセル
        if( exist( t.pointBC_, olp )  == false ) olp.add( t.pointBC_ );

        if( t.isChildC_ == true ) traceOrJumpForward( t.childC_.myNumber_ - 1, olp );

        traceOrJumpBackward( startindex, olp );

        return olp;
    }

    public ArrayList<PointXY> traceOrJumpBackward( int startindex, ArrayList<PointXY> olp ){
        Triangle t = trilist_.get( startindex );

        //CA点を取る。すでにあったらキャンセル
        if( exist( t.pointCA_, olp ) == false ) olp.add( t.pointCA_ );

        // 0まで戻る。
        if( t.myParentNumber_ > 0 ) traceOrJumpBackward( t.myParentNumber_ - 1, olp );

        return olp;
    }

    // 同じポイントは二ついらない
    public boolean exist( PointXY it, ArrayList<PointXY> inthis ){
        for( int i=0; i<inthis.size(); i++ )
         if( true == it.nearBy( inthis.get(i), 0.001f ) ) return true;

        return false;
    }

    public ArrayList<PointXY> goAroundCaveBySetRightHand( int start, int endnum ){
        ArrayList<PointXY> olp = new ArrayList<PointXY>();

        Triangle t1 = trilist_.get( start );
        olp.add( t1.pointAB_ );

        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        for( int i = start; i < endnum; i++ ){
            Triangle t = trilist_.get( i );
            PointXY before = olp.get( olp.size() - 1);
            //AB点を取る。すでにあったらキャンセル
            if( before.equals( t.pointAB_ ) == false ) olp.add( t.pointAB_ );

            //単独の突点の場合。
            if( t.isChildB_ == false &&  t.isChildC_ == false && t.myNumber_ != endnum  ){
                if( before.equals( t.pointBC_ ) == false ) olp.add( t.pointBC_ );
                if( before.equals( t.pointCA_ ) == false ) olp.add( t.pointCA_ );
                continue;
            }

            //派生接続のとき。B接続に沿って進む。
            if( t.isChildB_ == true &&  t.isChildC_ == true ){
                i = t.childB_.myNumber_ - 2;
                continue;
            }

            //B接続がなかったらBC点を取る。
            //if( t.isChildB_ == true ) if( t.childB_.isFloating() == false ) continue;
            //olp.add( t.pointBC_ );

            //次の三角形が終点を越えなくて、次の親番が自分でなかったら、CA点を取る。単独の突点の場合の別表現。
            //int nextnum = i + 1;
            //if( nextnum < trilist_.size() ) if( trilist_.get( nextnum ).myParentNumber_ != t.myNumber_ ) olp.add( t.pointCA_ );

        }

        return olp;
    }

    public int searchFloatintTriangleFrom( int start ){

        start += 1;

        for( int i=start; i < trilist_.size(); i++ ) {
            Triangle t = trilist_.get( i );

            if( t.isFloating() ) return t.myParentNumber_;
        }

        return trilist_.size();
    }

    public TriangleList reverse(){
        TriangleList rev = new TriangleList();

        int iforward = 1;

        for( int iback = trilist_.size(); iback > 0; iback-- ){
            rev.trilist_.add( trilist_.get( iback - 1 ).clone() ); // arraylist add. no sideEffect.
            rev.get(iforward).myNumber_ = iforward;
            iforward++;
        }

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


