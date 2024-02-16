package com.jpaver.trianglelist;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.toRadians;

import androidx.annotation.NonNull;

import com.jpaver.trianglelist.util.Params;

public class Triangle extends EditObject implements Cloneable {
    boolean valid_ = false;
    float[] length = new float[3];
    float[] lengthNotSized = new float[3];
    PointXY[] point = new PointXY[3];


    // 各辺に接続されている Triangle オブジェクトの識別子を返す
    @NonNull
    @Override
    public String toString() {
        Triangle[] connectedTriangles = new Triangle[3]; // 各辺に接続された Triangle オブジェクトを保持する配列
        connectedTriangles[0] = nodeTriangleA_;
        connectedTriangles[1] = nodeTriangleB_;
        connectedTriangles[2] = nodeTriangleC_;

        StringBuilder sb = new StringBuilder();
        sb.append("Triangle:").append(myNumber_).append(", ").append(System.identityHashCode(this)).append(", connected to: ");
        for (int i = 0; i < connectedTriangles.length; i++) {
            if (connectedTriangles[i] != null) {
                sb.append(System.identityHashCode(connectedTriangles[i]));
                if (i < connectedTriangles.length - 1) {
                    sb.append(" and ");
                }
            }
        }
        sb.append("%n");
        return sb.toString();
    }

    float getLengthA_(){ return length[0]; }
    float getLengthB_(){ return length[1]; }
    float getLengthC_(){ return length[2]; }
    float getLengthAforce_(){ return lengthNotSized[0]; }
    float getLengthBforce_(){ return lengthNotSized[1]; }
    float getLengthCforce_(){ return lengthNotSized[2]; }
    public PointXY getPointCA_(){ return point[0].clone(); }
    public PointXY getPointAB_() { return new PointXY(this.pointAB_); }
    public PointXY getPointBC_() { return new PointXY(this.pointBC_); }
    float scale_ = 1f;
    float angleInGlobal_ = 180f;
    float angleInLocal_ = 0f;

    float dedcount = 0f;

    String sla_ = "";
    String slb_ = "";
    String slc_ = "";

   // PointXY point[0] = new PointXY(0f, 0f); // base point by calc
    PointXY pointAB_ = new PointXY(0f, 0f);
    PointXY pointBC_ = new PointXY(0f, 0f);
    
    PointXY pointCenter_ = new PointXY(0f, 0f);
    PointXY pointNumber_ = new PointXY(0f, 0f);
    boolean isPointNumberMoved_ = false;

    PointXY dimPointA_ = new PointXY(0f, 0f);
    PointXY dimPointB_ = new PointXY(0f, 0f);
    PointXY dimPointC_ = new PointXY(0f, 0f);

    PointXY pointName_ = new PointXY(0f, 0f);
    int nameAlign_ = 0;
    int nameSideAlign_ = 0;

    protected double myTheta_, myAlpha_, myPowA_, myPowB_, myPowC_;
    float angleInnerCA_ = 0f;
    float angleInnerAB_ = 0f;
    float angleInnerBC_ = 0f;
    float dimAngleB_ = 0f;
    float dimAngleC_ = 0f;

    int parentNumber_ = -1; // 0:root
    int parentBC_ = -1;   // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    int connectionType_ = 0; // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    int connectionLCR_ = 2; // 0:L 1:C 2:R
    ConnParam cParam_ = new ConnParam(0, 0, 2, 0f);


    int myNumber_ = 1;
    int myDimAlign_ = 0;
    int myDimAlignA_ = 3;
    int myDimAlignB_ = 3;
    int myDimAlignC_ = 3;
    boolean isChangeDimAlignB_ = false;
    boolean isChangeDimAlignC_ = false;

    int dimSideAlignA_ = 0;
    int dimSideAlignB_ = 0;
    int dimSideAlignC_ = 0;

    int lastTapSide_ = -1;

    int color_ = 4;

    int childSide_ = 0;
    String myName_ = "";

    Bounds myBP_ = new Bounds(0f,0f,0f,0f);

    PathAndOffset pathA_;// = PathAndOffset();
    PathAndOffset pathB_;// = PathAndOffset();
    PathAndOffset pathC_;// = PathAndOffset();
    PathAndOffset pathS_;// = PathAndOffset();
    float dimH_ = 0f;

    Triangle nodeTriangleA_ = null;
    Triangle nodeTriangleB_ = null;
    Triangle nodeTriangleC_ = null;
    boolean isChildB_ = false;
    boolean isChildC_ = false;
    boolean isFloating_ = false;
    boolean isColored_ = false;

    @NotNull
    @Override
    public Triangle clone(){
        Triangle b = null;
        try {
            b=(Triangle) super.clone();
            b.length = Arrays.copyOf( length, length.length );
            b.lengthNotSized = Arrays.copyOf( lengthNotSized, lengthNotSized.length );
            b.point = Arrays.copyOf( point, point.length );
            b.angleInGlobal_ = this.angleInGlobal_;
            b.myName_ = this.myName_;
            b.myNumber_ = this.myNumber_;
            b.parentBC_ = this.parentBC_;
            b.parentNumber_ = parentNumber_;
            b.dimPointA_ = this.dimPointA_;
            b.myDimAlignA_ = this.myDimAlignA_;
            b.myDimAlignB_ = this.myDimAlignB_;
            b.myDimAlignC_ = this.myDimAlignC_;
            b.dimSideAlignA_ = this.dimSideAlignA_;
            b.dimSideAlignB_ = this.dimSideAlignB_;
            b.dimSideAlignC_ = this.dimSideAlignC_;
            b.point[0] = this.point[0].clone();
            b.pointAB_ = this.pointAB_.clone();
            b.pointBC_ = this.pointBC_.clone();
            b.pointCenter_ = this.pointCenter_.clone();
            b.pointNumber_ = this.pointNumber_.clone();
            b.isPointNumberMoved_ = this.isPointNumberMoved_;
            b.myBP_.setLeft(myBP_.getLeft());
            b.myBP_.setTop(myBP_.getTop());
            b.myBP_.setRight(myBP_.getRight());
            b.myBP_.setBottom(myBP_.getBottom());
            b.nodeTriangleA_ = nodeTriangleA_;
            b.nodeTriangleB_ = nodeTriangleB_;
            b.nodeTriangleC_ = nodeTriangleC_;
            b.childSide_ = this.childSide_;
            b.color_ = this.color_;
            b.connectionLCR_ = this.connectionLCR_;
            b.connectionType_ = this.connectionType_;
            b.cParam_ = this.cParam_.clone();
            b.isFloating_ = this.isFloating_;
            b.isColored_ = this.isColored_;
        }catch (Exception e){
            e.printStackTrace();
        }
        assert b != null;
        return b;
    }

    Triangle(){
    }
/*
    Triangle(int A, int B, int C){
        setNumber(1);
        pointCA = new PointXY(0f,0f);
        myAngle = 180f;
        initBasicArguments(A, B, C, pointCA, myAngle);
        calcPoints(pointCA, myAngle);
        myDimAlign = setDimAlign();
    }
*/
    Triangle(float A, float B, float C){
        setNumber(1);
        point[0] = new PointXY(0f,0f);
        angleInGlobal_ = 180f;
        initBasicArguments(A, B, C, point[0], angleInGlobal_);
        calcPoints(point[0], angleInGlobal_);
        //myDimAlign_ = autoSetDimAlign();
    }


    //for first triangle.
    Triangle(float A, float B, float C, PointXY pCA, float angle){
        setNumber(1);
        initBasicArguments(A, B, C, pCA, angle);
        calcPoints(pCA, angle);
    }

    Triangle(Triangle myParent, int pbc, float A, float B, float C){

        setOn(myParent, pbc, A, B, C);
        //autoSetDimAlign();
    }

    Triangle(Triangle myParent, ConnParam cParam, float B, float C){
        setOn(myParent, cParam, B, C);
    }

    Triangle(Triangle parent, int pbc, float B, float C){
        initBasicArguments(parent.getLengthByIndex(pbc), B, C, parent.getPointBySide(pbc), parent.getAngleBySide(pbc));

        setOn(parent, pbc, B, C);


    }

    public void setNode( Triangle node, int side ){
        if( node == null ) return;
        if( side > 2 ) side = getParentSide();

        switch ( side ){
            case -1:
                break;
            case 0:
                nodeTriangleA_ = node;
                if( node == nodeTriangleB_ ) nodeTriangleB_ = null;
                if( node == nodeTriangleC_ ) nodeTriangleC_ = null;
                break; // これ忘れがち。ないと全てのケースが入る！
            case 1:
                nodeTriangleB_ = node;
                if( node == nodeTriangleA_ ) nodeTriangleA_ = null;
                if( node == nodeTriangleC_ ) nodeTriangleC_ = null;
                break;
            case 2:
                nodeTriangleC_ = node;
                if( node == nodeTriangleB_ ) nodeTriangleB_ = null;
                if( node == nodeTriangleA_ ) nodeTriangleA_ = null;
                break;
        }
    }

    Triangle(Triangle myParent, Params dP){
        setOn(myParent,dP.getPl(),dP.getA(),dP.getB(),dP.getC());
        myName_ = dP.getName();
        autoSetDimSideAlign();
    }

    Triangle( Params dP, float angle){
        setNumber(dP.getN());
        setMyName_(dP.getName());
        initBasicArguments(dP.getA(), dP.getB(), dP.getC(), dP.getPt(), angle);
        calcPoints(dP.getPt(), angle);
    }

    public float[] getUnScaledLength(){
        lengthNotSized[0] = length[0]/scale_;
        lengthNotSized[1] = length[1]/scale_;
        lengthNotSized[2] = length[2]/scale_;

        return new float[]{ length[0]/scale_, length[1]/scale_, length[2]/scale_ };
    }

    // set argument methods
    private void initBasicArguments(float A, float B, float C, PointXY pCA, float angle){
        this.length[0] = A;
        this.length[1] = B;
        this.length[2] = C;
        this.lengthNotSized[0] = A;
        this.lengthNotSized[1] = B;
        this.lengthNotSized[2] = C;
        valid_ = validTriangle();

        this.point[0] = new PointXY( pCA.getX(), pCA.getY() );
        this.pointAB_ = new PointXY(0.0f, 0.0f);
        this.pointBC_ = new PointXY(0.0f, 0.0f);
        this.pointCenter_ = new PointXY(0.0f, 0.0f);
        //this.pointNumber_ = new PointXY(0.0f, 0.0f);

        this.angleInGlobal_ = angle;
        this.angleInnerCA_ = 0;
        this.angleInnerAB_ = 0;
        this.angleInnerBC_ = 0;
        //childSide_ = 0;
        //myDimAlignA = 0;
        //myDimAlignB = 0;
        //myDimAlignC = 0;

    }

    Triangle setOn(Triangle parent, int pbc, float B, float C){
        //myNumber_ = parent.myNumber_ + 1;
        parentBC_ = pbc;

        if(parent == null) {
            resetLength(pbc,B,C);
            return this.clone();
        }
        else {
            setNode( parent, 0 );
            parent.setNode( this, getParentSide() );
        }

        //setParent(parent, A);

        if(pbc == 1) {
            parentBC_ = 1;
            length[0] = nodeTriangleA_.lengthNotSized[1];
            point[0] = nodeTriangleA_.getPointBC_();
            angleInGlobal_ = nodeTriangleA_.getAngleMpAB();
        } else if(pbc == 2){
            parentBC_ = 2;
            length[0] = nodeTriangleA_.lengthNotSized[2];
            point[0] = nodeTriangleA_.getPointCA_();
            angleInGlobal_ = nodeTriangleA_.getAngleMmCA();
        } else {
            parentBC_ = 0;
            length[0] = 0f;
            lengthNotSized[0] = 0f;
            point[0] = new PointXY(0f, 0f);
            angleInGlobal_ = 180f;
        }

        parentNumber_ = nodeTriangleA_.getMyNumber_();
        //nodeTriangleA_.setChild(this, parentBC_);

        initBasicArguments(length[0], B, C, point[0], angleInGlobal_);
        calcPoints(point[0], angleInGlobal_);

        //myDimAlign = setDimAlign();

        return this;
    }
/*
    void set(Triangle parent, int pbc, float A, float B, float C, boolean byNode ) {
        set( parent, pbc, A, B, C );

        if(byNode){
            parent.resetByNode( pbc );
        }

        if( nodeTriangleB_ != null ) nodeTriangleB_.set( this, nodeTriangleB_.parentBC_, this.getLengthByIndex( nodeTriangleB_.getParentSide() ), nodeTriangleB_.length[1], nodeTriangleB_.length[2] );
        if( nodeTriangleC_ != null ) nodeTriangleC_.set( this, nodeTriangleC_.parentBC_, this.getLengthByIndex( nodeTriangleC_.getParentSide() ), nodeTriangleC_.length[1], nodeTriangleC_.length[2] );

    }
*/
    Triangle setOn(Triangle parent, int pbc, float A, float B, float C){
        //myNumber_ = parent.myNumber_ + 1;
        parentBC_ = pbc;

        if(parent == null) {
            resetLength(pbc,B,C);
            return this.clone();
        }
        else {
            setNode( parent, 0 );
            parent.setNode( this, getParentSide() );
        }

        //setParent(parent, pbc);
        //nodeTriangleA_.setChild(this, pbc );

        // if user rewrite A
        if(A != parent.getLengthByIndex(pbc)) {
            length[0] = A;
            lengthNotSized[0] = A;
        } else{
            length[0] = parent.getLengthByIndex(pbc);
            lengthNotSized[0] = parent.getLengthByIndex(pbc);
        }

        setCParamFromParentBC( pbc );
        point[0] = getParentPointByType( cParam_ );


        if(pbc == 1) { // B
            parentBC_ = 1;

            angleInGlobal_ = parent.getAngleMpAB();
        } else if(pbc == 2) { // C
            parentBC_ = 2;
            angleInGlobal_ = parent.getAngleMmCA();
        } else if(pbc == 3){ // B-R
            parentBC_ = 3;
            angleInGlobal_ = parent.getAngleMpAB();
        } else if(pbc == 4) { //B-L
            parentBC_ = 4;
            angleInGlobal_ = parent.getAngleMpAB();
        } else if(pbc == 5) { //C-R
            parentBC_ = 5;
            angleInGlobal_ = parent.getAngleMmCA();
        } else if(pbc == 6) { //C-L
            parentBC_ = 6;
            angleInGlobal_ = parent.getAngleMmCA();
        } else if(pbc == 7) { //B-Center
            parentBC_ = 7;
            angleInGlobal_ = parent.getAngleMpAB();
        } else if(pbc == 8) { //C-Center
            parentBC_ = 8;
            angleInGlobal_ = parent.getAngleMmCA();
        } else if(pbc == 9) { //B-Float-R
            parentBC_ = 9;
            angleInGlobal_ = parent.getAngleMpAB();
        } else if(pbc == 10) { //C-Float-R
            parentBC_ = 10;
            angleInGlobal_ = parent.getAngleMmCA();
        } else {
            parentBC_ = 0;
            length[0] = 0f;
            lengthNotSized[0] = 0f;
            point[0] = new PointXY(0f, 0f);
            angleInGlobal_ = 180f;
        }

        parentNumber_ = parent.getMyNumber_();

        initBasicArguments(length[0], B, C, point[0], angleInGlobal_);
        if(!validTriangle()) return null;
        calcPoints(point[0], angleInGlobal_);
        if(parentBC_ == 4){
            PointXY vector = new PointXY( parent.getPointAB_().getX()- pointAB_.getX(),
                    parent.getPointAB_().getY()- pointAB_.getY());
            move(vector);
        }
        if(parentBC_ == 6){
            PointXY vector = new PointXY( parent.getPointBC_().getX()- pointAB_.getX(),
                    parent.getPointBC_().getY()- pointAB_.getY());
            move(vector);
        }
        //if( parentBC_ >= 9 ) rotate( point[0], angleInLocal_, true );

        //myDimAlign = setDimAlign();

        return this.clone();
    }

    public void setOn(Triangle myParent, int pbc){

        this.setOn(myParent, pbc, this.length[1], this.length[2]);

    }

    public void setOn(Triangle myParent, Params dP){
        setOn(myParent,dP.getPl(),dP.getA(),dP.getB(),dP.getC());
        myName_ = dP.getName();

    }

    Triangle setOn(Triangle parent, ConnParam cParam, float B, float C) {
        //myNumber_ = parent.myNumber_ + 1;
        //parentBC_ = cParam.getSide();

        if(parent == null) {
            resetLength(cParam.getLenA(),B,C);
            return this.clone();
        }
        else {
            setNode( parent, 0 );
            parent.setNode( this, cParam.getSide() );
        }

        //setParent( parent, cParam.getSide() );
        angleInGlobal_ = parent.getAngleBySide( cParam.getSide() );

        setConnectionType( cParam );


        initBasicArguments(length[0], B, C, point[0], angleInGlobal_);
        if(!validTriangle()) return null;

        calcPoints(point[0], angleInGlobal_);

        //if( parentBC_ >= 9 ) rotate( point[0], angleInLocal_, true );

        setDimAlignByChild();

        //nodeTriangleA_.setChild(this, cParam.getSide() );

        return this.clone();
    }

    void reset(Params prm ){
        //ConneParam thisCP = cParam_.clone();
        length[0] = prm.getA();
        lengthNotSized[0] = prm.getA();
        setCParamFromParentBC( prm.getPl() );
        parentBC_ = prm.getPl();
        parentNumber_ = prm.getPn();

        if( nodeTriangleA_ == null || parentNumber_ < 1 ) resetLength( prm.getA(), prm.getB(), prm.getC() );
        else{
            setOn(nodeTriangleA_, cParam_, prm.getB(), prm.getC() );
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        this.myName_ = prm.getName();

    }

    public void resetElegant( Params prm ){
        reset( prm );

        if( nodeTriangleA_ != null ) nodeTriangleA_.resetByNode( getParentSide() );

    }

    public void resetByNode( int pbc ){

        Triangle node = getNode( pbc );

        if( node != null ) {
            float lengthConnected = getLengthByIndex( pbc );
            if( node.parentBC_ < 3 ) lengthConnected = node.length[0];

            switch ( pbc ) {
                case 0:
                    //initBasicArguments( length, length[1], length[2], node.pointAB_, -node.angleInGlobal_ );
                    break;
                case 1:
                    initBasicArguments( length[0], lengthConnected, length[2], node.pointBC_, -node.angleInGlobal_);
                    break;
                case 2:
                    initBasicArguments( length[0], length[1], lengthConnected, node.point[0], node.angleInGlobal_ +angleInnerBC_ );
                    break;
            }

            calcPoints( point[0], angleInGlobal_);
        }

    }

    public boolean hasChild(){
        return nodeTriangleB_ != null || nodeTriangleC_ != null;
    }

    public Triangle getNode( int pbc ){
        switch ( pbc ){
            case 0:
                return nodeTriangleA_;
            case 1:
                return nodeTriangleB_;
            case 2:
                return nodeTriangleC_;
            case -1:
                return this;
        }
        return null;
    }

    public void resetNode(Params prms, Triangle parent, ArrayList<Triangle> doneObjectList ){

        // 接続情報の変更、とりあえず挿入処理は考慮しない、すでに他のノードがあるときは上書きする。
        nodeTriangleA_.removeNode( this );
        nodeTriangleA_ = parent;
        nodeTriangleA_.setNode( this, prms.getPl() );
        reset( prms );
        doneObjectList.add( this );

    }

    public void removeNode( Triangle target ){
        if( nodeTriangleA_ == target ) nodeTriangleA_ = null;
        if( nodeTriangleB_ == target ) nodeTriangleB_ = null;
        if( nodeTriangleC_ == target ) nodeTriangleC_ = null;
    }

    void reset(Triangle newTri){
        ConnParam thisCP = cParam_.clone();
        if( nodeTriangleA_ == null || parentNumber_ < 1 ) {
            angleInGlobal_ = newTri.angleInGlobal_;
            angleInLocal_ = newTri.angleInLocal_;
            resetLength( newTri.length[0], newTri.length[1], newTri.length[2]);
        }
        else setOn(nodeTriangleA_, newTri.parentBC_, newTri.length[0], newTri.length[1], newTri.length[2]);
        cParam_ = thisCP.clone();
        this.myName_ = newTri.myName_;
        this.clone();
    }

    Triangle reset(Triangle newTri, ConnParam cParam){
        if(nodeTriangleA_ == null) resetLength( newTri.length[0], newTri.length[1], newTri.length[2]);
        else setOn(nodeTriangleA_, cParam, newTri.length[1], newTri.length[2]);
        this.myName_ = newTri.myName_;
        return this.clone();
    }

    Triangle resetLength(float A, float B, float C){
        //lengthA = A; lengthB = B; lengthC = C;
        initBasicArguments(A, B, C, point[0], angleInGlobal_);
        calcPoints(point[0], angleInGlobal_);
        return this.clone();
    }


    public boolean resetByParent(Triangle prnt, ConnParam cParam){
        if( !isValidLengthes( prnt.getLengthByIndex( getParentSide() ), length[1], length[2] ) ) return false;

        Triangle triIsValid = setOn(prnt, cParam, length[1], length[2]);

        return triIsValid != null;
    }

    // reset by parent.
    public boolean resetByParent(Triangle prnt, int pbc){
        Triangle triIsValid = null;
        float parentLength = prnt.getLengthByIndex(pbc);

        //if(pbc == 1 ) triIsValid = set(prnt, pbc, length[0], parentLength, );
        if(pbc <= 2 ){
            if( !isValidLengthes( parentLength, length[1], length[2] ) ){
                return true;
            }
            else triIsValid = setOn(prnt, pbc, parentLength, length[1], length[2] );
        }
        if(pbc >  2 ) triIsValid = setOn(prnt, pbc, length[0], length[1], length[2]);

        return triIsValid == null;
    }

    // 子のA辺が書き換わったら、それを写し取ってくる。同一辺長接続のとき（１か２）以外はリターン。
    public void resetByChild(Triangle myChild){
        setDimAlignByChild();
        if( myChild.cParam_.getType() != 0 ) return;

        int cbc = myChild.parentBC_;
        if( cbc == 1 && !isValidLengthes( length[0], myChild.length[0], length[2] ) ) return;
        if( cbc == 2 && !isValidLengthes( length[0], length[1], myChild.length[0] ) ) return;

        childSide_ = myChild.parentBC_;
        if( nodeTriangleA_ == null || parentNumber_ < 1 ) {
            if( cbc == 1 ) resetLength(length[0], myChild.length[0], length[2]);
            if( cbc == 2 ) resetLength(length[0], length[1], myChild.length[0]);
            return;
        }
        if( cbc == 1 ) {
            setOn(nodeTriangleA_, parentBC_, length[0], myChild.length[0], length[2]);
            //nodeTriangleB_ = myChild;
        }
        if( cbc == 2 ) {
            setOn(nodeTriangleA_, parentBC_, length[0], length[1], myChild.length[0]);
            //nodeTriangleC_ = myChild;
        }
    }

    public void setConnectionType(ConnParam cParam ){
        //myParentBC_= cParam.getSide();
        parentNumber_ = nodeTriangleA_.getMyNumber_();
        connectionType_ = cParam.getType();
        connectionLCR_ = cParam.getLcr();
        cParam_ = cParam.clone();

        switch( cParam.getSide() ){
            case 1:
                angleInGlobal_ = nodeTriangleA_.getAngleMpAB();
                break;
            case 2:
                angleInGlobal_ = nodeTriangleA_.getAngleMmCA();
                break;
        }

        if (cParam.getType() == 0) {
            if (cParam.getLenA() != 0.0f) {
                length[0] = cParam.getLenA();
                lengthNotSized[0] = cParam.getLenA();
                point[0] = getParentPointByType(cParam.getSide(), cParam.getType(), cParam.getLcr());
            } else {
                length[0] = nodeTriangleA_.getLengthByIndex(cParam.getSide());
                lengthNotSized[0] = nodeTriangleA_.getLengthByIndex(cParam.getSide());
                point[0] = nodeTriangleA_.getPointByCParam(cParam, nodeTriangleA_);
            }
        } else {
            if (cParam.getLenA() != 0.0f) {
                length[0] = cParam.getLenA();
                lengthNotSized[0] = cParam.getLenA();
            }
            point[0] = getParentPointByType(cParam.getSide(), cParam.getType(), cParam.getLcr());
        }
    }

    public PointXY getPointByCParam(ConnParam cparam, Triangle prnt ) {
        if( prnt == null ) return new PointXY( 0f, 0f );
        int cside = cparam.getSide();
        //pp.add( getPointBy( pp, length[0], clcr ) );

        return getPointBySide( cside );
    }


    public Triangle rotateLCRandGet(){
        rotateLCR();
        return this;
    }

    public PointXY rotateLCR(){
        //if(myParentBC_ < 3) return new PointXY(0f,0f);

        connectionLCR_ --;
        if( connectionLCR_ < 0 ) connectionLCR_ = 2;
        cParam_.setLcr(connectionLCR_);
        setParentBCFromCLCR();

        return setBasePoint( cParam_ );
    }

    public void setParentBCFromCLCR(){
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        if( cParam_.getType() == 2 ) return;

        switch ( cParam_.getLcr() ){
            case 0:
                switch ( cParam_.getSide() ) {
                case 1:
                    parentBC_ = 4;
                    break;
                case 2:
                    parentBC_ = 6;
                    break;
                }
                break;
            case 1:
                switch ( cParam_.getSide() ) {
                    case 1:
                        parentBC_ = 7;
                        break;
                    case 2:
                        parentBC_ = 8;
                        break;
                }
                break;
            case 2:
                switch ( cParam_.getSide() ) {
                    case 1:
                        parentBC_ = 3;
                        break;
                    case 2:
                        parentBC_ = 5;
                        break;
                }
                break;
        }

    }

    public PointXY setBasePoint(ConnParam cParam ){
        point[0] = getParentPointByType( cParam.getSide(), cParam.getType(), cParam.getLcr() );
        connectionType_ = cParam.getType();
        connectionLCR_ = cParam.getLcr();
        calcPoints( point[0], angleInGlobal_);
        return point[0];
    }


    public PointXY setBasePoint(  int pbc, int pct, int lcr ){
        point[0] = getParentPointByType( pbc, pct, lcr );
        connectionType_ = pct;
        connectionLCR_ = lcr;
        calcPoints( point[0], angleInGlobal_);
        return point[0];
    }

    public PointXY getParentPointByType(ConnParam cParam) {
        return getParentPointByType( cParam.getSide(), cParam.getType(), cParam.getLcr() );
    }

    public PointXY getParentPointByType(int pbc, int pct, int lcr){
        if( nodeTriangleA_ == null ) return new PointXY(0f,0f);

        switch (pct){
            default:
                return nodeTriangleA_.getPointBySide(pbc);
            case 1:
                return getParentPointByLCR( pbc, lcr );
            case 2:
                return getParentPointByLCR( pbc, lcr ).crossOffset(nodeTriangleA_.getPointByBackSide(pbc), -1.0f);
        }

    }

    public Boolean hasConstantParent(){
        int iscons = myNumber_ - parentNumber_;
        return iscons <= 1;
    }

    public PointXY getPointByBackSide(int i){
        if(getSideByIndex(i).equals("B")) return getPointAB_();
        if(getSideByIndex(i).equals("C")) return getPointBC_();
        return null;
    }

    public PointXY getParentPointByLCR( int pbc, int lcr ){
        if( nodeTriangleA_ == null ) return new PointXY(0f,0f);
        switch(pbc){
            case 1:
                switch(lcr){
                    case 0:
                        return nodeTriangleA_.pointAB_.offset(nodeTriangleA_.pointBC_, length[0]);
                    case 1:
                        return getParentOffsetPointBySide(pbc);
                    case 2:
                        return nodeTriangleA_.pointBC_.clone();
                }
                break;
            case 2:
                switch(lcr){
                    case 0:
                        return nodeTriangleA_.pointBC_.offset(nodeTriangleA_.point[0], length[0]);
                    case 1:
                        return getParentOffsetPointBySide(pbc);
                    case 2:
                        return nodeTriangleA_.point[0].clone();
                }
                break;
        }

        return new PointXY(0f,0f);
    }

    public PointXY getParentOffsetPointBySide(int pbc){
        if( nodeTriangleA_ == null ) return new PointXY(0f,0f);
        switch(pbc){
            case 1:
                return nodeTriangleA_.pointAB_.offset(nodeTriangleA_.pointBC_, nodeTriangleA_.getLengthB() * 0.5f + length[0] * 0.5f );
            case 2:
                return nodeTriangleA_.pointBC_.offset(nodeTriangleA_.point[0], nodeTriangleA_.getLengthC() * 0.5f + length[0] * 0.5f );
        }
        return nodeTriangleA_.getPointBySide(pbc);
    }

    public void setParent(Triangle parent){
        nodeTriangleA_ = parent.clone();
        //myParentBC_ = pbc;
    }

    public void setCParamFromParentBC(int pbc){

        int curLCR = cParam_.getLcr();
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if( cParam_.getSide() == 0 && (pbc == 4 || pbc == 6 ) ) curLCR = 0;
        if( cParam_.getSide() == 0 && (pbc == 7 || pbc == 8 ) ) curLCR = 1;

        switch (pbc){
            case -1:// standalone
            case 0:
                cParam_ = new ConnParam( 0 ,0, 2, lengthNotSized[0]);
                break;
            case 1://B
                cParam_ = new ConnParam( 1 ,0, 2, lengthNotSized[0]);
                break;
            case 2://C
                cParam_ = new ConnParam( 2 ,0, 2, lengthNotSized[0]);
                break;
            case 3://BR
                cParam_ = new ConnParam( 1 ,1, 2, lengthNotSized[0]);
                break;
            case 4://BL
                cParam_ = new ConnParam( 1 ,1, 0, lengthNotSized[0]);
                break;
            case 5://CR
                cParam_ = new ConnParam( 2 ,1, 2, lengthNotSized[0]);
                break;
            case 6://CL
                cParam_ = new ConnParam( 2 ,1, 0, lengthNotSized[0]);
                break;
            case 7://BC
                cParam_ = new ConnParam( 1 ,1, 1, lengthNotSized[0]);
                break;
            case 8://CC
                cParam_ = new ConnParam( 2 ,1, 1, lengthNotSized[0]);
                break;
            case 9://BF
                cParam_ = new ConnParam( 1 ,2, curLCR, lengthNotSized[0]);
                break;
            case 10://CF
                cParam_ = new ConnParam( 2 ,2, curLCR, lengthNotSized[0]);
                break;
        }
    }

    public void setDimAlignByChild(){
        if(!isChangeDimAlignB_){
            if( nodeTriangleB_ == null ) myDimAlignB_ = 1;
            else myDimAlignB_ = 3;
        }
        if(!isChangeDimAlignC_){
            if( nodeTriangleC_ == null ) myDimAlignC_ = 1;
            else myDimAlignC_ = 3;
        }
    }


    public void setDimAligns(int sa, int sb, int sc, int ha, int hb, int hc){
        dimSideAlignA_ = sa;
        dimSideAlignB_ = sb;
        dimSideAlignC_ = sc;
        myDimAlignA_ = ha;
        myDimAlignB_ = hb;
        myDimAlignC_ = hc;
    }

    public Triangle getParent(){
        if(nodeTriangleA_ != null ) return nodeTriangleA_.clone();
        return null;
    }

    public boolean collision(){
        return true;
    }

    private void setMyBound(){
        PointXY lb;
                lb = pointAB_.min(pointBC_);
        myBP_.setLeft(lb.getX());
        myBP_.setBottom(lb.getY());

        PointXY rt;
                rt = pointAB_.max(pointBC_);
        myBP_.setRight(rt.getX());
        myBP_.setTop(rt.getY());
    }

    public Bounds expandBoundaries(Bounds listBound){
        setMyBound();
        Bounds newB = new Bounds(myBP_.getLeft(), myBP_.getTop(), myBP_.getRight(), myBP_.getBottom());
        // 境界を比較し、広い方に置き換える
        if(myBP_.getBottom() > listBound.getBottom()) newB.setBottom(listBound.getBottom());
        if(myBP_.getTop()    < listBound.getTop())    newB.setTop(listBound.getTop());
        if(myBP_.getLeft()   > listBound.getLeft())   newB.setLeft(listBound.getLeft());
        if(myBP_.getRight()  < listBound.getRight())  newB.setRight(listBound.getRight());

        return newB;
    }

    @Override
    public float getArea(){
        float sumABC = lengthNotSized[0] + lengthNotSized[1] + lengthNotSized[2];
        float myArea = (sumABC*0.5f)*((sumABC*0.5f)- lengthNotSized[0])*((sumABC*0.5f)- lengthNotSized[1])*((sumABC*0.5f)- lengthNotSized[2]);
        //myArea = roundByUnderTwo( myArea );
        return roundByUnderTwo((float)Math.pow(myArea, 0.5));
    }

    public float roundByUnderTwo(float fp) {
        float ip = fp * 100f;
        return Math.round(ip) / 100f;
    }

    public boolean validTriangle(){
        if (length[0] <=0.0f || length[1] <=0.0f || length[2] <=0.0f) return false;
        return isValidLengthes( length[0], length[1], length[2] );
        //!((this.length[0] + this.length[1]) <= this.length[2]) &&
          //      !((this.length[1] + this.length[2]) <= this.length[0]) &&
            //    !((this.length[2] + this.length[0]) <= this.length[1]);
    }

    public boolean isValidLengthes( float A, float B, float C ){
        return !( ( A + B ) <= C ) && !( ( B + C ) <= A ) && !( ( C + A ) <= B );
    }

    public double calculateInternalAngle(PointXY p1, PointXY p2, PointXY p3) {
        PointXY v1 = p1.subtract(p2);
        PointXY v2 = p3.subtract(p2);
        double angleRadian = Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()));
        return angleRadian * 180 / Math.PI;
    }

    public void calcMyAngles(){
        angleInnerAB_ = (float)calculateInternalAngle( point[0], pointAB_, pointBC_ );
        angleInnerBC_ = (float)calculateInternalAngle( pointAB_, pointBC_, point[0] );
        angleInnerCA_ = (float)calculateInternalAngle( pointBC_, point[0], pointAB_ );
    }

    public void calcPoints( Triangle ref, int refside ){
        setNode( ref, refside);

        PointXY[] plist;
        float[]   llist;
        double[]  powlist;
        float     angle;

        switch(refside){
            case 0:
                angle    = angleInGlobal_;
                plist    = new PointXY[] { point[0], pointAB_, pointBC_ };
                llist    = new float[]   { length[0], length[1], length[2] };
                powlist  = new double[]  { Math.pow( length[0], 2 ), Math.pow( length[1], 2 ), Math.pow( length[2], 2 ) };
                break;
            case 1:
                angle    = nodeTriangleB_.angleInGlobal_ +180f;//- nodeTriangleB_.angleInnerCA_;
                plist    = new PointXY[] { nodeTriangleB_.pointAB_, pointBC_, point[0] };
                llist    = new float[]   { length[1], length[2], length[0] };
                powlist  = new double[]  { Math.pow( length[1], 2 ), Math.pow( length[2], 2 ), Math.pow( length[0], 2 ) };
                pointAB_ = plist[0];
                break;
            case 2:
                angle    = nodeTriangleC_.angleInGlobal_ +180f;//- nodeTriangleB_.angleInnerCA_;
                plist    = new PointXY[] { nodeTriangleC_.pointAB_, point[0], pointAB_ };
                llist    = new float[]   { length[2], length[0], length[1] };
                powlist  = new double[]  { Math.pow( length[2], 2 ), Math.pow( length[0], 2 ), Math.pow( length[1], 2 ) };
                pointBC_ = plist[0];
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + refside);
        }

        plist[1].set( (float)( plist[0].getX() + ( llist[0] * Math.cos( toRadians( angle ) ) ) ),
                        (float) ( plist[0].getY() + ( llist[0] * Math.sin( toRadians( angle ) ) ) ) );

        myTheta_ = Math.atan2( plist[0].getY() - plist[1].getY(), plist[0].getX() - plist[1].getX() );
        myAlpha_ = Math.acos( ( powlist[0] + powlist[1] - powlist[2] ) / ( 2 * llist[0] * llist[1] ) );

        plist[2].set( (float)( plist[1].getX() + ( llist[1] * Math.cos( myTheta_ + myAlpha_ ) ) ),
                        (float)( plist[1].getY() + ( llist[1] * Math.sin( myTheta_ + myAlpha_ ) ) ) );

        calcMyAngles();

        if( refside == 1 ) angleInGlobal_ = nodeTriangleB_.angleInGlobal_ - angleInnerCA_;
        if( refside == 2 ) angleInGlobal_ = nodeTriangleC_.angleInGlobal_ + angleInnerCA_;

    }

    public void calcPoints(PointXY pCA, float angle) {
        calculatePointAB(pCA, angle);
        calculateTheta(pCA);
        calculateSidesSquared();
        calculateAlpha();
        calculatePointBC();
        calculateInternalAngles();
        calculatePointCenter();
        finalizeCalculations();
    }

    private void calculatePointAB(PointXY pCA, float angle) {
        this.pointAB_.set((float) (pCA.getX() + (this.length[0] * Math.cos(Math.toRadians(angle)))),
                (float) (pCA.getY() + (this.length[0] * Math.sin(Math.toRadians(angle)))));
    }

    private void calculateTheta(PointXY pCA) {
        this.myTheta_ = Math.atan2(pCA.getY() - this.pointAB_.getY(), pCA.getX() - this.pointAB_.getX());
    }

    private void calculateSidesSquared() {
        this.myPowA_ = Math.pow(this.length[0], 2);
        this.myPowB_ = Math.pow(this.length[1], 2);
        this.myPowC_ = Math.pow(this.length[2], 2);
    }

    private void calculateAlpha() {
        this.myAlpha_ = Math.acos((this.myPowA_ + this.myPowB_ - this.myPowC_) / (2 * this.length[0] * this.length[1]));
    }

    private void calculatePointBC() {
        this.pointBC_.set((float) (this.pointAB_.getX() + (this.length[1] * Math.cos(this.myTheta_ + this.myAlpha_))),
                (float) (this.pointAB_.getY() + (this.length[1] * Math.sin(this.myTheta_ + this.myAlpha_))));
    }

    private void calculateInternalAngles() {
        this.angleInnerAB_ = (float) calculateInternalAngle(this.point[0], this.pointAB_, this.pointBC_);
        this.angleInnerBC_ = (float) calculateInternalAngle(this.pointAB_, this.pointBC_, this.point[0]);
        this.angleInnerCA_ = (float) calculateInternalAngle(this.pointBC_, this.point[0], this.pointAB_);
    }

    private void calculatePointCenter() {
        this.pointCenter_.set((this.pointAB_.getX() + this.pointBC_.getX() + this.point[0].getX()) / 3,
                (this.pointAB_.getY() + this.pointBC_.getY() + this.point[0].getY()) / 3);
    }

    private void finalizeCalculations() {
        if (!isPointNumberMoved_) {
            autoAlignPointNumber();
        }
        setMyBound();
        setDimPath(dimH_);
        setDimPoint();
        dimAngleB_ = getAngleMpAB();
        dimAngleC_ = getAngleMmCA();
    }

    private void autoAlignPointNumber() {
        if(!isPointNumberMoved_) pointNumber_ = pointCenter_; //とりあえず重心にする
        /*
        if (myAngleBC < 30 && lengthA / scale_ < 3) { //BC30°以下、LengthAが2m以下のとき、
            PointXY midA = pointAB.calcMidPoint(pointCA); //LengthAの中点
            float lengthCenterA = pointCenter.vectorTo(midA).lengthXY(); //センターからＬＡ中点までの距離ベクトル
            // LA中点方向に移動、ＢＣ角が小さいほど大きく移動
            pointNumber = pointCenter.offset(pointAB.calcMidPoint(pointCA), lengthCenterA * ((90 - myAngleBC) * 0.006f));

            //さらに、外に出す時
            if (lengthB / scale_ < 3 || myAngleBC < 5)
                pointNumber = pointNumber.offset(midA, offset2 * scale_);
            return;
        }
        if (myAngleAB < 30 && lengthC / scale_ < 3) {
            PointXY midC = pointBC.calcMidPoint(pointCA);
            float lengthCenterC = pointCenter.vectorTo(midC).lengthXY();

            pointNumber = pointCenter.offset(pointBC.calcMidPoint(pointCA), lengthCenterC * ((90 - myAngleAB) * 0.006f));
            if (lengthA / scale_ < 3 || myAngleAB < 5)
                pointNumber = pointNumber.offset(midC, offset2 * scale_);
            return;
        }
        if (myAngleCA < 30 && lengthB / scale_ < 3) {
            PointXY midB = pointAB.calcMidPoint(pointBC);
            float lengthCenterB = pointCenter.vectorTo(midB).lengthXY();
            pointNumber = pointCenter.offset(pointAB.calcMidPoint(pointBC), lengthCenterB * ((90 - myAngleCA) * 0.006f));

            if (lengthC / scale_ < 3 || myAngleCA < 5)
                pointNumber = pointNumber.offset(midB, offset2 * scale_);
            return;
        }*/
    }

    public int autoSetDimAlign(){ // 1:下 3:上

        myDimAlignA_ = calcDimAlignByInnerAngleOf(0);
        myDimAlignB_ = calcDimAlignByInnerAngleOf(1);
        myDimAlignC_ = calcDimAlignByInnerAngleOf(2);

        /*
        if(  getAngle() <= 90 || getAngle() >= 270 ) { // 基線の角度が90°～270°の間
            myDimAlign = 3;
            myDimAlignA = 3;
        } else { // それ以外(90°以下～270°の間)
            myDimAlign = 1;
            myDimAlignA = 1;
            myDimAlignB = 1;
            myDimAlignC = 1;
        }

        //AB
        if( getAngleMpAB() <= 90 || getAngle() >= 270 ) { // 基線+ABの角度が90°～270°の間
            myDimAlignB = 3;
        } else {
            myDimAlignB = 1;
        }

        if( getAngleMmCA() <= 90 || getAngle() >= 270 ) { // 基線-CAの角度が90°～270°の間
            myDimAlignC = 3;
        } else {
            myDimAlignC = 1;
        }
*/
        setDimPath(dimH_);

        return myDimAlign_;

    }

    public int calcDimAlignByInnerAngleOf(int ABC){    // 夾角の、1:外 　3:内
        // ABC == 0 の場合の特定の条件で 1 を返す
        if (ABC == 0 && (parentBC_ == 9 || parentBC_ == 10 || parentBC_ > 2 ||
                nodeTriangleB_ != null || nodeTriangleC_ != null)) {
            return 1;
        }

        // ABC == 1 または ABC == 2 の場合、それぞれ nodeTriangleB_ と nodeTriangleC_ が null でなければ 1 を返す
        if ((ABC == 1 && nodeTriangleB_ != null) ||
                (ABC == 2 && nodeTriangleC_ != null)) {
            return 1;
        }

        // 上記のいずれの条件にも当てはまらない場合は 3 を返す
        return 3;
    }


    public void rotateDimSideAlign(int side){
        if(side == 0) dimSideAlignA_ = rotateZeroToThree( dimSideAlignA_ );
        if(side == 1) dimSideAlignB_ = rotateZeroToThree( dimSideAlignB_ );
        if(side == 2) dimSideAlignC_ = rotateZeroToThree( dimSideAlignC_ );
        if(side == 4) nameSideAlign_ = rotateZeroToThree( nameSideAlign_ );
        setDimPath( dimH_ );
    }

    public int rotateZeroToThree( int num){
        num++;
        if(num>4)num = 0;
        return num;
    }

    public int flipOneToThree( int num ){
        if( num == 1 ) return 3;
        if( num == 3 ) return 1;
        return num; // sonomama kaesu.
    }

    // 呼び出す場所によって、強制になってしまう。
    public void autoSetDimSideAlign(){
        // 短い時は外側？
        if( lengthNotSized[2] < 1.5f || lengthNotSized[1] < 1.5f ){
            myDimAlignB_ = 1;
            myDimAlignC_ = 1;
        }
        // 和が短い時は寄せる
        //if( lengthNotSized[2]+lengthNotSized[0] < 7.0f ) dimSideAlignB_ = 1;
        //if( lengthNotSized[1]+lengthNotSized[2] < 7.0f ) dimSideAlignC_ = 2;
        // 和が短い時は外に出す
        //if( lengthNotSized[2]+lengthNotSized[0] < 5.0f ) dimSideAlignB_ = 4;
        //if( lengthNotSized[1]+lengthNotSized[2] < 5.0f ) dimSideAlignC_ = 4;

    }

    public void flipDimAlignH(int side){
        if(side == 0) myDimAlignA_ = flipOneToThree( myDimAlignA_ );
        if(side == 1) {
            myDimAlignB_ = flipOneToThree( myDimAlignB_ );
            isChangeDimAlignB_ = true;
        }
        if(side == 2){
            myDimAlignC_ = flipOneToThree( myDimAlignC_ );
            isChangeDimAlignC_ = true;
        }
        if(side == 4) nameAlign_ = flipOneToThree( nameAlign_ );
        setDimPath( dimH_ );
    }

    public int getDimAlignA(){
        return calcDimAlignByInnerAngleOf(0);
       /*
        if( myAngle <= 90 || getAngle() >= 270 ) {
            if( lengthA*scale_ > 1.5f ) return myDimAlignA = 3;
            else return myDimAlignA = 1;
        }
        else {
            if( lengthA*scale_ > 1.5f ) return myDimAlignA = 1;
            else return myDimAlignA = 3;
        }*/
    }

    public int getDimAlignB(){
        return calcDimAlignByInnerAngleOf(1);
/*        if( getAngleMpAB() <= 450f || getAngleMpAB() >= 270f ||
                getAngleMpAB() <= 90f || getAngleMpAB() >= -90f ) {
            if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 3;
            if( lengthB*scale_ > 1.5f ) return myDimAlignB = 3;
            else return myDimAlignB = 1;
        }

        if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 1;
        return  myDimAlignB = 3;*/
    }

    public int getDimAlignC(){
        return calcDimAlignByInnerAngleOf(2);
/*
        if( getAngleMmCA() <= 450f || getAngleMmCA() >= 270f ||
                getAngleMmCA() <= 90f || getAngleMmCA() >= -90f ) {
            if( childSide_ == 5 || childSide_ == 6 ) return myDimAlignC = 3;
            if( lengthC*scale_ > 1.5f ) return myDimAlignC = 3;
            else return myDimAlignC = 1;
        }

        if( childSide_ == 5 || childSide_ == 6 ) return myDimAlignC = 1;
        return  myDimAlignC = 3;*/
    }


    public Bounds getMyBP_(){ return myBP_; }

    public PointXY getPointCenter_() { return new PointXY(pointCenter_); }


    public PointXY weightedMidpoint( float bias) {

        // 角度が大きいほど重みを大きくするための調整
        float weight1 = angleInnerAB_ + bias; // 角度が大きいほど重みが大きくなる
        float weight2 = angleInnerBC_ + bias;
        float weight3 = angleInnerCA_ + bias;

        // 重みの合計で正規化
        float totalWeight = weight1 + weight2 + weight3;
        weight1 /= totalWeight;
        weight2 /= totalWeight;
        weight3 /= totalWeight;

        PointXY p1 = pointAB_;
        PointXY p2 = pointBC_;
        PointXY p3 = point[0];

        // 重み付き座標の計算
        float weightedX = p1.getX() * weight1 + p2.getX() * weight2 + p3.getX() * weight3;
        float weightedY = p1.getY() * weight1 + p2.getY() * weight2 + p3.getY() * weight3;

        return new PointXY(weightedX, weightedY);
    }

    public PointXY getPointNumberAutoAligned_() {
        // 点が手動で移動された場合は、その位置をそのまま返す
        if(isPointNumberMoved_) return pointNumber_;

        return weightedMidpoint(25f);
        /*
        // 固定値を意味のある名前の変数に置き換え
        final float CLOSENESS_THRESHOLD = 2.5f; // 辺に「近い」と判断する距離の閾値
        final float OFFSET_MULTIPLIER = -0.2f; // オフセットする距離の倍率


        // 点が辺BCに近い場合、点を辺から適切にオフセットする
        if( lengthNotSized[0] < CLOSENESS_THRESHOLD )
            return pointNumber_.offset(pointBC_, pointNumber_.vectorTo(pointBC_).lengthXY() * OFFSET_MULTIPLIER);

        // 点が辺A（point[0]）に近い場合、点を辺から適切にオフセットする
        if( lengthNotSized[1] < CLOSENESS_THRESHOLD )
            return pointNumber_.offset(point[0], pointNumber_.vectorTo(point[0]).lengthXY() * OFFSET_MULTIPLIER);

        // 点が辺ABに近い場合、点を辺から適切にオフセットする
        if( lengthNotSized[2] < CLOSENESS_THRESHOLD )
            return pointNumber_.offset(pointAB_, pointNumber_.vectorTo(pointAB_).lengthXY() * OFFSET_MULTIPLIER);

        // どの辺にも近くない場合は、点の現在位置をそのまま返す
        return pointNumber_;*/
    }


    public float getLengthA() { return this.length[0]; }
    public float getLengthB() { return this.length[1]; }
    public float getLengthC() { return this.length[2]; }

    public float getLengthByIndex(int i) {
        if( i == 1 ) return length[1];
        if( i == 2 ) return length[2];
        else return 0f;
    }

    public float getLengthByIndexForce(int i) {
        if( i == 1 ) return lengthNotSized[1];
        if( i == 2 ) return lengthNotSized[2];
        else return 0f;
    }


    public PointXY getPointBySide(int i){
        if(getSideByIndex(i).equals("B")) return getPointBC_();
        if(getSideByIndex(i).equals("C")) return getPointCA_();
        return null;
    }

    public float getAngleBySide(int i){
        if(getSideByIndex(i).equals("B")) return getAngleMpAB();
        if(getSideByIndex(i).equals("C")) return getAngleMmCA();
        else return 0f;
    }

    public String getSideByIndex(int i) {
        if(i==1 || i==3 || i==4 || i==7 || i==9) return "B";
        if(i==2 || i==5 || i==6 || i==8 || i==10) return "C";
        else if( i == 0 ) return "not connected";
        return "not connected";
    }

    public int getParentSide() {
        int i = parentBC_;
        if(i==1 || i==3 || i==4 || i==7 || i==9) return 1;
        if(i==2 || i==5 || i==6 || i==8 || i==10) return 2;
        else return 0;
    }

    public int getPbc(int pbc) {
        if(pbc ==1 || pbc ==3 || pbc ==4 || pbc ==7 || pbc ==9) return 1;
        if(pbc ==2 || pbc ==5 || pbc ==6 || pbc ==8 || pbc ==10) return 2;
        else return 0;
    }

    public String getInfo(){
        return "triangle name:"+getMyName_()+" num:"+getMyNumber_()+" node0:"+getNode(0)+" node1:"+getNode(1)+" node2:"+getNode(2);
    }

    public PointXY pointMiddleOuterUnconnecterdSide( PointXY ref, float Coefficient ){
        return this.pointUnconnectedSide(ref);//.crossOffset(ref, -Coefficient*scale_ );
    }

    public float getAverageLength(){
        return (getLengthA_()+getLengthB_()+getLengthC_())*0.33f;
    }

    public PointXY pointUnconnectedSide( PointXY ref){
        float Coefficient = -0.4f;
        if( nodeTriangleB_ == null ) return ref.mirroredAndScaledPoint(pointAB_,pointBC_,1f,-1f).scale(1/scale_);//.scale(Coefficient);//offset( midPointB, getAverageLength()*Coefficient);
        if( nodeTriangleC_ == null ) return ref.mirroredAndScaledPoint(pointBC_,point[0],1f,-1f).scale(1/scale_);//offset( midPointC, -getAverageLength()*Coefficient);
        else return ref;
    }
    public float getAngle() { return this.angleInGlobal_; }
    public float getAngleAB() { return this.angleInnerAB_; }
    public float getAngleBC() { return this.angleInnerBC_; }
    public float getAngleCA() { return this.angleInnerCA_; }
    public float getAngleMmCA() { return this.angleInGlobal_ - this.angleInnerCA_; }
    public float getAngleMpAB() { return this.angleInGlobal_ + this.angleInnerAB_; }

    public int getParentBC() { return parentBC_; }
    public int getParentNumber() { return parentNumber_; }
    public int getMyNumber_() { return myNumber_; }
    public void setNumber(int num) { myNumber_ = num; }
    public void setColor(int num) { color_ = num; isColored_ = true; }

    public void setChild(Triangle newchild, int cbc ){
        childSide_ = cbc;
        if( newchild.getPbc(cbc) == 1 ) {
                nodeTriangleB_ = newchild;
                isChildB_ = true;
        }
        if( newchild.getPbc(cbc) == 2 ) {
                nodeTriangleC_ = newchild;
                isChildC_ = true;
        }
        setDimAlignByChild();
    }

    public boolean alreadyHaveChild(int pbc){
        if( pbc < 1 ) return false;
        if(getSideByIndex(pbc).equals("B") && isChildB_) return true;
        return getSideByIndex(pbc).equals("C") && isChildC_;
    }

    public Boolean hasChildIn(int cbc ){
        if ( ( nodeTriangleB_ != null || isChildB_) && cbc == 1 ) return true;
        return (nodeTriangleC_ != null || isChildC_) && cbc == 2;
    }

    public void move(PointXY to){
        pointAB_.add(to);
        pointBC_.add(to);
        point[0].add(to);
        pointCenter_ = pointCenter_.plus(to);
        pointNumber_ = pointNumber_.plus(to);
        dimPointA_.add(to);
        dimPointB_.add(to);
        dimPointC_.add(to);
        myBP_.setLeft(myBP_.getLeft()+to.getX());
        myBP_.setRight(myBP_.getRight()+to.getX());
        myBP_.setTop(myBP_.getTop()+to.getY());
        myBP_.setBottom(myBP_.getBottom()+to.getX());
        pathA_.move(to);
        pathB_.move(to);
        pathC_.move(to);
        pathS_.move(to);

    }

    public void setScale( float scale){
        scale_ = scale;
        length[0] *= scale;
        length[1] *= scale;
        length[2] *= scale;
        calcPoints(point[0], angleInGlobal_);
    }

    public void scale(PointXY basepoint, float scale){
        scale_ *= scale;
        //pointAB_.scale(basepoint, scale);
        //pointBC_.scale(basepoint, scale);
        point[0].scale(basepoint, scale);
        pointCenter_.scale(basepoint, scale);
        pointNumber_.scale(basepoint, scale);
        length[0] *= scale;
        length[1] *= scale;
        length[2] *= scale;
        calcPoints(point[0], angleInGlobal_);

    }

    // 自分の次の番号がくっついている辺を調べてA辺にする。
    // 他の番号にあって自身の辺上に無い場合は、A辺を変更しない。
    public void rotateLengthBy(int side ){
        //Triangle this = this.clone();
        float pf;
        int pi;
        PointXY pp;

        if( side == 1 ){ // B to A
            pf = this.length[0];
            this.length[0] = this.length[1];
            this.length[1] = this.length[2];
            this.length[2] = pf;

            pf = this.lengthNotSized[0];
            this.lengthNotSized[0] = this.lengthNotSized[1];
            this.lengthNotSized[1] = this.lengthNotSized[2];
            this.lengthNotSized[2] = pf;

            pp = point[0].clone();
            this.point[0] = this.pointAB_;
            this.pointAB_ = this.pointBC_;
            this.pointBC_ = pp.clone();

            pf = this.angleInnerCA_;
            this.angleInnerCA_ = this.angleInnerAB_;
            this.angleInnerAB_ = this.angleInnerBC_;
            this.angleInnerBC_ = pf;

            this.angleInGlobal_ = getAngleMmCA() - this.angleInnerAB_;

            if( angleInGlobal_ < 0 ) angleInGlobal_ += 360f;
            if( angleInGlobal_ > 360 ) angleInGlobal_ -= 360f;

            pp = this.dimPointA_.clone();
            this.dimPointA_ = this.dimPointB_;
            this.dimPointB_ = this.dimPointC_;
            this.dimPointC_ = pp.clone();

            pi = this.myDimAlignA_;
            this.myDimAlignA_ = this.myDimAlignB_;
            this.myDimAlignB_ = this.myDimAlignC_;
            this.myDimAlignC_ = pi;

            pi = this.dimSideAlignA_;
            this.dimSideAlignA_ = this.dimSideAlignB_;
            this.dimSideAlignB_ = this.dimSideAlignC_;
            this.dimSideAlignC_ = pi;
        }
        if( side == 2 ){ // C to A
            pf = this.length[0];
            this.length[0] = this.length[2];
            this.length[2] = this.length[1];
            this.length[1] = pf;

            pf = this.lengthNotSized[0];
            this.lengthNotSized[0] = this.lengthNotSized[2];
            this.lengthNotSized[2] = this.lengthNotSized[1];
            this.lengthNotSized[1] = pf;

            pp = point[0].clone();
            this.point[0] = this.pointBC_;
            this.pointBC_ = this.pointAB_;
            this.pointAB_ = pp.clone();

            pf = this.angleInnerCA_;
            this.angleInnerCA_ = this.angleInnerBC_;
            this.angleInnerBC_ = this.angleInnerAB_;
            this.angleInnerAB_ = pf;
            this.angleInGlobal_ += this.angleInnerCA_ + this.angleInnerBC_;

            if( angleInGlobal_ < 0 ) angleInGlobal_ += 360f;
            if( angleInGlobal_ > 360 ) angleInGlobal_ -= 360f;

            pp = this.dimPointA_.clone();
            this.dimPointA_ = this.dimPointC_;
            this.dimPointC_ = this.dimPointB_;
            this.dimPointB_ = pp.clone();

            pi = this.myDimAlignA_;
            this.myDimAlignA_ = this.myDimAlignC_;
            this.myDimAlignC_ = this.myDimAlignB_;
            this.myDimAlignB_ = pi;
            pi = this.dimSideAlignA_;
            this.dimSideAlignA_ = this.dimSideAlignC_;
            this.dimSideAlignC_ = this.dimSideAlignB_;
            this.dimSideAlignB_ = pi;

        }

    }

    public void setReverseDefSide(int pbc, Boolean BtoC) {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if(!BtoC){
            if( pbc == 3 ) parentBC_ = 4;
            else if( pbc == 4 ) parentBC_ = 3;
            else if( pbc == 5 ) parentBC_ = 6;
            else if( pbc == 6 ) parentBC_ = 5;
            else parentBC_ = pbc;
        }
        if(BtoC){
            if( pbc == 3 ) parentBC_ = 6;
            else if( pbc == 4 ) parentBC_ = 5;
            else if( pbc == 5 ) parentBC_ = 4;
            else if( pbc == 6 ) parentBC_ = 3;
            else if( pbc == 9 ) parentBC_ = 10;
            else if( pbc == 10 ) parentBC_ = 9;
            else parentBC_ = - pbc + 3;
        }
    }

    @NotNull
    @Override
    public Params getParams(){
        return new Params(myName_,"", myNumber_, length[0], length[1], length[2], parentNumber_, parentBC_, point[0], pointCenter_);
    }

    public void setMyName_(String name){ myName_ = name;}
    public String getMyName_(){return myName_;}

    public int getTapLength(PointXY tapP, float rangeRadius){
        setDimPoint();

        float range = rangeRadius*scale_;

        if(tapP.nearBy(pointName_, range)) return lastTapSide_ = 4;

        if(tapP.nearBy(dimPointA_, range)) return lastTapSide_ = 0;
        if(tapP.nearBy(dimPointB_, range)) return lastTapSide_ = 1;
        if(tapP.nearBy(dimPointC_, range)) return lastTapSide_ = 2;
        if(tapP.nearBy(getPointNumberAutoAligned_(), range)) return lastTapSide_ = 3;

        return lastTapSide_ = -1;
    }

    public boolean isCollide( PointXY p ) {
        return p.isCollide(pointAB_, pointBC_, point[0]);
    }

    public void setDimPoint(){
        dimPointA_ = pathA_.getPointD();//dimSideRotation( dimSideAlignA_, point[0].calcMidPoint(pointAB_), pointAB_, point[0]);
        dimPointB_ = pathB_.getPointD();//dimSideRotation( dimSideAlignB_, pointAB_.calcMidPoint(pointBC_), pointBC_, pointAB_);
        dimPointC_ = pathC_.getPointD();//dimSideRotation( dimSideAlignC_, pointBC_.calcMidPoint(point[0]), point[0], pointBC_);
    }

    public PointXY dimSideRotation(int side, PointXY dimPoint, PointXY pointLeft, PointXY pointRight) {

        float centerKaranoNagasa = dimPoint.lengthTo( pointRight ) * 0.3f;
        float centerKaraSotonoNagasa = dimPoint.lengthTo( pointRight ) + 2.5f;

        switch( side ){
            default:
                return dimPoint;
            case 0:
                return dimPoint;
            case 1:
                return dimPoint.offset(pointLeft, centerKaranoNagasa);
            case 2:
                return dimPoint.offset(pointRight, centerKaranoNagasa);
            case 3:
                return dimPoint.offset(pointLeft, centerKaraSotonoNagasa);
            case 4:
                return dimPoint.offset(pointRight, centerKaraSotonoNagasa);

        }

    }

    public void rotate(PointXY basepoint, float addDegree, boolean recover){
        if( parentBC_ < 9 && recover) return;

        if(!recover) angleInLocal_ += addDegree;
        else angleInLocal_ = addDegree;

        point[0] = point[0].rotate(basepoint, addDegree);
        angleInGlobal_ += addDegree;

        calcPoints(point[0], angleInGlobal_);
        setDimPath( dimH_ );
        //setDimAlign();
        //Log.d("Triangle", "num:" + myNumber_ + "pCA: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "pAB: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "pBC: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "angleInGlobal/Local: " + angleInGlobal_ + " , " + angleInLocal_ );

    }

    public void setPointNumberMoved_(PointXY p){
        this.pointNumber_ = p;
        isPointNumberMoved_ = true;
    }


    public void setDimPath( float ts ){
        if( point[0] == null || pointAB_ == null || pointBC_ == null ) return;

        dimH_ = ts;
        pathA_ = new PathAndOffset(scale_, pointAB_, point[0], myDimAlignA_, dimSideAlignA_, dimH_);
        pathB_ = new PathAndOffset(scale_, pointBC_, pointAB_, myDimAlignB_, dimSideAlignB_, dimH_);
        pathC_ = new PathAndOffset(scale_, point[0], pointBC_, myDimAlignC_, dimSideAlignC_, dimH_);
        pathS_ = new PathAndOffset(scale_, pointAB_, point[0], 4, 0, dimH_);

        //sla_ = formattedString( lengthNotSized[0], 3);
        //slb_ = formattedString( lengthNotSized[1], 3);
        //slc_ = formattedString( lengthNotSized[2], 3);
    }

    public boolean isFloating(){
        isFloating_ = nodeTriangleA_ != null && parentBC_ > 8;

        return isFloating_;
    }

    public boolean isColored(){
        isColored_ = nodeTriangleA_ != null && color_ != nodeTriangleA_.color_;

        return isColored_;
    }


    public PointXY hataage(PointXY p, float offset, float axisY, Float number){
        float distanceFromCenter = p.getY() - pointCenter_.getY() * axisY;
        float direction = 1;
        if( distanceFromCenter < 0 ) direction = -1;

        PointXY hataage =  p.clone();
        float compareY = compareY( direction, axisY );
        hataage.set( p.getX(), p.getY() + ( compareY - p.getY() ) + ( offset * direction * ( dedcount * number ) ) );

        //Log.d("Triangle Deduction", "p.getY: " + p.getY() + " , pointCenterY: " + pointCenter_.getY()+ " , axisY: " + axisY );
        //Log.d("Triangle Deduction", "DistanceFromCenter: " + distanceFromCenter + " , direction: " + direction );
        //Log.d("Triangle Deduction", "compareY: " + compareY + " , hataage.y: " + hataage.getY() );


        return hataage;
    }

    public float compareY( float direction, float axisY ){
        float pCAy = point[0].getY() * axisY;
        float pABy = pointAB_.getY() * axisY;
        float pBCy = pointBC_.getY() * axisY;
        //Log.d("Triangle Deduction", "pCAy: " + pCAy + " , pABy: " + pABy + " , pBCy: " + pBCy );

        float Y = pCAy;
        if( direction == -1 ){
            if( Y > pABy ) Y = pABy;
            if( Y > pBCy ) Y = pBCy;
        }
        else if( direction == 1 ){
            if( Y <= pABy ) Y = pABy;
            if( Y <= pBCy ) Y = pBCy;
        }

        return Y;
    }

    public boolean trimming( ArrayList<PointXY> trimline){
        return isCollide( pathA_, trimline );
    }

    public boolean isCollide( PathAndOffset path, ArrayList<PointXY> trimline ){
        return true;
    }
}
