package com.jpaver.trianglelist;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static java.lang.Math.toRadians;

public class Triangle extends EditObject implements Cloneable {
    protected boolean valid_ = false;
    protected float lengthA_ = 0f;
    protected float lengthB_ = 0f;
    protected float lengthC_ = 0f;
    protected float scale_ = 1f;
    protected float lengthAforce_ = 0f;
    protected float lengthBforce_ = 0f;
    protected float lengthCforce_ = 0f;
    protected String sla_ = "";
    protected String slb_ = "";
    protected String slc_ = "";

    protected PointXY pointCA_ = new PointXY(0f, 0f); // base point by calc
    protected PointXY pointAB_ = new PointXY(0f, 0f);
    protected PointXY pointBC_ = new PointXY(0f, 0f);
    protected PointXY pointCenter_ = new PointXY(0f, 0f);
    protected PointXY pointNumber_ = new PointXY(0f, 0f);
    protected boolean isPointNumberMoved_ = false;

    protected PointXY dimPointA_ = new PointXY(0f, 0f);
    protected PointXY dimPointB_ = new PointXY(0f, 0f);
    protected PointXY dimPointC_ = new PointXY(0f, 0f);

    protected PointXY pointName_ = new PointXY(0f, 0f);
    protected int nameAlign_ = 0;
    protected int nameSideAlign_ = 0;

    protected double myTheta_, myAlpha_, myPowA_, myPowB_, myPowC_;
    protected float angleInGlobal_ = 180f;
    protected float angleInnerCA_ = 0f;
    protected float angleInnerAB_ = 0f;
    protected float angleInnerBC_ = 0f;
    protected float dimAngleB_ = 0f;
    protected float dimAngleC_ = 0f;

    protected int parentNumber_ = -1; // 0:root
    protected int parentBC_ = -1;   // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    protected int connectionType_ = 0; // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    protected int connectionLCR_ = 2; // 0:L 1:C 2:R
    protected ConnParam cParam_ = new ConnParam(0, 0, 2, 0f);


    protected int myNumber_ = 1;
    protected int myDimAlign_ = 0;
    protected int myDimAlignA_ = 3;
    protected int myDimAlignB_ = 3;
    protected int myDimAlignC_ = 3;
    protected boolean isChangeDimAlignA_ = false;
    protected boolean isChangeDimAlignB_ = false;
    protected boolean isChangeDimAlignC_ = false;

    protected int dimSideAlignA_ = 0;
    protected int dimSideAlignB_ = 0;
    protected int dimSideAlignC_ = 0;

    protected int lastTapSide_ = -1;

    protected int color_ = 4;

    protected int childSide_ = 0;
    protected String myName_ = "";

    protected Bounds myBP_ = new Bounds(0f,0f,0f,0f);

    protected PathAndOffset pathA_;// = PathAndOffset();
    protected PathAndOffset pathB_;// = PathAndOffset();
    protected PathAndOffset pathC_;// = PathAndOffset();
    protected PathAndOffset pathS_;// = PathAndOffset();
    protected float dimH_ = 0f;

    protected Triangle nodeTriangleA_ = null;
    protected Triangle nodeTriangleB_ = null;
    protected Triangle nodeTriangleC_ = null;
    protected boolean isChildB_ = false;
    protected boolean isChildC_ = false;

    @Override
    public Triangle clone(){
        Triangle b = null;
        try {
            b=(Triangle) super.clone();
            b.lengthA_ = this.lengthA_;
            b.lengthB_ = this.lengthB_;
            b.lengthC_ = this.lengthC_;
            b.lengthAforce_ = this.lengthAforce_;
            b.lengthBforce_ = this.lengthBforce_;
            b.lengthCforce_ = this.lengthCforce_;
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
            b.pointCA_ = this.pointCA_.clone();
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
            b.cParam_ = this.cParam_;
        }catch (Exception e){
            e.printStackTrace();
        }
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
        pointCA_ = new PointXY(0f,0f);
        angleInGlobal_ = 180f;
        initBasicArguments(A, B, C, pointCA_, angleInGlobal_);
        calcPoints(pointCA_, angleInGlobal_);
        //myDimAlign_ = autoSetDimAlign();
    }


    //for first triangle.
    Triangle(float A, float B, float C, PointXY pCA, float angle){
        setNumber(1);
        initBasicArguments(A, B, C, pCA, angle);
        calcPoints(pCA, angle);
    }

    //it use first Triangle.
    Triangle(float A, float B, float C, PointXY pCA, float angle,
             int myParNum, int myParBC, int myConne){

        initBasicArguments(A, B, C, pCA, angle);
        setParentInfo(myParNum, myParBC, myConne);
        myDimAlign_ = 1;
        calcPoints(pCA, angle);
    }

    Triangle(Triangle myParent, int pbc, float A, float B, float C){

        set(myParent, pbc, A, B, C);
        //autoSetDimAlign();
    }

    Triangle(Triangle myParent, ConnParam cParam, float B, float C){
        set(myParent, cParam, B, C);
    }

    Triangle( Triangle child, float A, float B, float C ){
        if( child.validTriangle() == false ) return;
        PointXY basePoint = child.getPointBySide( child.parentBC_ );
        float baseAngle = child.getAngleBySide( child.parentBC_ );
        initBasicArguments( A, B, C, basePoint, baseAngle );
    }

    Triangle(Triangle parent, int pbc, float B, float C){
        initBasicArguments(parent.getLengthByIndex(pbc), B, C, parent.getPointBySide(pbc), parent.getAngleBySide(pbc));

        set(parent, pbc, B, C);


    }

    public void setNode( Triangle node, int side ){
        if( node == null ) return;

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
        set(myParent,dP.getPl(),dP.getA(),dP.getB(),dP.getC());
        myName_ = dP.getName();
        autoSetDimSideAlign();
    }

    Triangle( Params dP, float angle){
        setNumber(dP.getN());
        setMyName_(dP.getName());
        initBasicArguments(dP.getA(), dP.getB(), dP.getC(), dP.getPt(), angle);
        calcPoints(dP.getPt(), angle);
    }

    Triangle(Triangle ta){
        setNumber(ta.getMyNumber_());
        initBasicArguments(ta.getLengthA_(),ta.getLengthB_(),ta.getLengthC_(),ta.getPointCA_(),ta.getAngle());
        calcPoints(ta.getPointCA_(),ta.getAngle());
    }

    // set argument methods
    private void initBasicArguments(float A, float B, float C, PointXY pCA, float angle){
        this.lengthA_ = A;
        this.lengthB_ = B;
        this.lengthC_ = C;
        this.lengthAforce_ = A;
        this.lengthBforce_ = B;
        this.lengthCforce_ = C;
        valid_ = validTriangle();

        this.pointCA_ = new PointXY( pCA.getX(), pCA.getY() );
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

    Triangle set(Triangle parent, int pbc, float B, float C){
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
            lengthA_ = nodeTriangleA_.getLengthB_();
            lengthA_ = nodeTriangleA_.lengthBforce_;
            pointCA_ = nodeTriangleA_.getPointBC_();
            angleInGlobal_ = nodeTriangleA_.getAngleMpAB();
        } else if(pbc == 2){
            parentBC_ = 2;
            lengthA_ = nodeTriangleA_.getLengthC_();
            lengthA_ = nodeTriangleA_.lengthCforce_;
            pointCA_ = nodeTriangleA_.getPointCA_();
            angleInGlobal_ = nodeTriangleA_.getAngleMmCA();
        } else {
            parentBC_ = 0;
            lengthA_ = 0f;
            lengthAforce_ = 0f;
            pointCA_ = new PointXY(0f, 0f);
            angleInGlobal_ = 180f;
        }

        parentNumber_ = nodeTriangleA_.getMyNumber_();
        //nodeTriangleA_.setChild(this, parentBC_);

        initBasicArguments(lengthA_, B, C, pointCA_, angleInGlobal_);
        calcPoints(pointCA_, angleInGlobal_);

        //myDimAlign = setDimAlign();

        return this;
    }

    Triangle set(Triangle parent, int pbc, float A, float B, float C, boolean byNode ) {
        set( parent, pbc, A, B, C );

        if( byNode == true ){
            parent.resetByNode( pbc );
        }

        if( nodeTriangleB_ != null ) nodeTriangleB_.set( this, nodeTriangleB_.parentBC_, this.getLengthByIndex( nodeTriangleB_.getParentSide() ), nodeTriangleB_.lengthB_, nodeTriangleB_.lengthC_ );
        if( nodeTriangleC_ != null ) nodeTriangleC_.set( this, nodeTriangleC_.parentBC_, this.getLengthByIndex( nodeTriangleC_.getParentSide() ), nodeTriangleC_.lengthB_, nodeTriangleC_.lengthC_ );

        return this;
    }

    Triangle set(Triangle parent, int pbc, float A, float B, float C){
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
            lengthA_ = A;
            lengthAforce_ = A;
        } else{
            lengthA_ = parent.getLengthByIndex(pbc);
            lengthAforce_ = parent.getLengthByIndex(pbc);
        }

        setCParamFromParentBC( pbc );
        pointCA_ = getParentPointByType( cParam_ );


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
            lengthA_ = 0f;
            lengthAforce_ = 0f;
            pointCA_ = new PointXY(0f, 0f);
            angleInGlobal_ = 180f;
        }

        parentNumber_ = parent.getMyNumber_();

        initBasicArguments(lengthA_, B, C, pointCA_, angleInGlobal_);
        if(!validTriangle()) return null;
        calcPoints(pointCA_, angleInGlobal_);
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

        //myDimAlign = setDimAlign();

        return this.clone();
    }

    public void set(Triangle myParent, int pbc){

        this.set(myParent, pbc, this.lengthB_, this.lengthC_);

    }

    public void set(Triangle myParent, Params dP){
        set(myParent,dP.getPl(),dP.getA(),dP.getB(),dP.getC());
        myName_ = dP.getName();

    }

    Triangle set(Triangle parent, ConnParam cParam, float B, float C) {
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
        angleInGlobal_ = nodeTriangleA_.getAngleBySide( cParam.getSide() );

        setConnectionType( cParam );


        initBasicArguments(lengthA_, B, C, pointCA_, angleInGlobal_);
        if(!validTriangle()) return null;

        calcPoints(pointCA_, angleInGlobal_);

        setDimAlignByChild();

        //nodeTriangleA_.setChild(this, cParam.getSide() );

        return this.clone();
    }

    public void reload() {
        if(nodeTriangleA_ != null){
            nodeTriangleA_.reload();
            calcPoints(pointCA_ = nodeTriangleA_.getPointBySide(parentBC_), angleInGlobal_ = nodeTriangleA_.getAngleBySide(parentBC_));
        }
    }

    Triangle reset( Params prm ){
        //ConneParam thisCP = cParam_.clone();
        lengthA_ = prm.getA();
        lengthAforce_ = prm.getA();
        setCParamFromParentBC( prm.getPl() );
        parentBC_ = prm.getPl();
        parentNumber_ = prm.getPn();

        if( nodeTriangleA_ == null || parentNumber_ < 1 ) resetLength( prm.getA(), prm.getB(), prm.getC() );
        else{
            set(nodeTriangleA_, cParam_, prm.getB(), prm.getC() );
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        this.myName_ = prm.getName();
        return this;
    }

    public void resetByNode( int pbc ){

        Triangle node = getNode( pbc );

        if( node != null ) {
            float length = getLengthByIndex( pbc );
            if( node.parentBC_ < 3 ) length = node.lengthA_;

            switch ( pbc ) {
                case 0:
                    //initBasicArguments( length, lengthB_, lengthC_, node.pointAB_, -node.angleInGlobal_ );
                    break;
                case 1:
                    initBasicArguments( lengthA_, length, lengthC_, node.pointAB_, -node.angleInGlobal_ );
                    break;
                case 2:
                    initBasicArguments( lengthA_, lengthB_, length, node.pointBC_, node.angleInGlobal_+angleInnerBC_ );
                    break;
            }

            calcPoints( pointCA_, angleInGlobal_ );
        }

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

    public void resetNode(Params prms, ArrayList<Triangle> doneObjectList ){

        reset( prms );
        doneObjectList.add( this );
        //nodeTriangleA_.resetNode( )

    }


    Triangle reset(Triangle newTri){
        ConnParam thisCP = cParam_.clone();
        if( nodeTriangleA_ == null || parentNumber_ < 1 ) resetLength( newTri.lengthA_, newTri.lengthB_, newTri.lengthC_);
        else set(nodeTriangleA_, newTri.parentBC_, newTri.lengthA_, newTri.lengthB_, newTri.lengthC_);
        cParam_ = thisCP.clone();
        this.myName_ = newTri.myName_;
        return this.clone();
    }

    Triangle reset(Triangle newTri, ConnParam cParam){
        if(nodeTriangleA_ == null) resetLength( newTri.lengthA_, newTri.lengthB_, newTri.lengthC_);
        else set(nodeTriangleA_, cParam, newTri.lengthB_, newTri.lengthC_);
        this.myName_ = newTri.myName_;
        return this.clone();
    }

    Triangle resetLength(float A, float B, float C){
        //lengthA = A; lengthB = B; lengthC = C;
        initBasicArguments(A, B, C, pointCA_, angleInGlobal_);
        calcPoints(pointCA_, angleInGlobal_);
        return this.clone();
    }


    public boolean resetByParent(Triangle prnt, ConnParam cParam){
        if( !isValidLengthes( prnt.getLengthByIndex( getParentSide() ), lengthB_, lengthC_ ) ) return false;

        Triangle triIsValid = set(prnt, cParam, lengthB_, lengthC_);
        return triIsValid != null;
    }

    public void resetByChild(Triangle myChild, ConnParam cParam){
        int cbc = myChild.cParam_.getSide();
        childSide_ = myChild.parentBC_;
        if(nodeTriangleA_ == null) {
            if( cbc == 1 ) resetLength(lengthA_, myChild.lengthA_, lengthC_);
            if( cbc == 2 ) resetLength(lengthA_, lengthB_, myChild.lengthA_);
            return;
        }
        if( cbc == 1 ) {
            set(nodeTriangleA_, cParam, myChild.lengthA_, lengthC_);
            nodeTriangleB_ = myChild.clone();
        }
        if( cbc == 2 ) {
            set(nodeTriangleA_, cParam, lengthB_, myChild.lengthA_);
            nodeTriangleC_ = myChild.clone();
        }
        setDimAlignByChild();
    }

    // reset by parent.
    public boolean resetByParent(Triangle prnt, int pbc){
        Triangle triIsValid = null;
        float parentLength = prnt.getLengthByIndex(pbc);

        //if(pbc == 1 ) triIsValid = set(prnt, pbc, lengthA_, parentLength, );
        if(pbc <= 2 ){
            if( !isValidLengthes( parentLength, lengthB_, lengthC_ ) ){
                triIsValid = set(prnt, pbc, lengthA_, lengthB_, lengthC_ );
                return false;
            }
            else triIsValid = set(prnt, pbc, parentLength, lengthB_, lengthC_ );
        }
        if(pbc >  2 ) triIsValid = set(prnt, pbc, lengthA_, lengthB_, lengthC_);
        return triIsValid != null;
    }

    // 子のA辺が書き換わったら、それを写し取ってくる。同一辺長接続のとき（１か２）以外はリターン。
    public void resetByChild(Triangle myChild){
        if( myChild.cParam_.getType() != 0 ) return;

        int cbc = myChild.parentBC_;
        if( cbc == 1 && !isValidLengthes( lengthA_, myChild.lengthA_, lengthC_ ) ) return;
        if( cbc == 2 && !isValidLengthes( lengthA_, lengthB_, myChild.lengthA_ ) ) return;

        childSide_ = myChild.parentBC_;
        if( nodeTriangleA_ == null || parentNumber_ < 1 ) {
            if( cbc == 1 ) resetLength(lengthA_, myChild.lengthA_, lengthC_);
            if( cbc == 2 ) resetLength(lengthA_, lengthB_, myChild.lengthA_);
            return;
        }
        if( cbc == 1 ) {
            set(nodeTriangleA_, parentBC_, lengthA_, myChild.lengthA_, lengthC_);
            //nodeTriangleB_ = myChild;
        }
        if( cbc == 2 ) {
            set(nodeTriangleA_, parentBC_, lengthA_, lengthB_, myChild.lengthA_);
            //nodeTriangleC_ = myChild;
        }
        setDimAlignByChild();
    }

    public float getLengthByType(){
        return 0f;
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

        switch ( cParam.getType() ){
            default:
                if( cParam.getLenA() != 0.0f ) {
                    lengthA_ = cParam.getLenA();
                    lengthAforce_ = cParam.getLenA();
                }
                pointCA_ = getParentPointByType( cParam.getSide() , cParam.getType(), cParam.getLcr() );

                break;
            case 0:
                if( cParam.getLenA() != 0.0f ) {
                    lengthA_ = cParam.getLenA();
                    lengthAforce_ = cParam.getLenA();
                    pointCA_ = getParentPointByType( cParam.getSide() , cParam.getType(), cParam.getLcr() );
                }
                else{
                    lengthA_ = nodeTriangleA_.getLengthByIndex( cParam.getSide() );
                    lengthAforce_ = nodeTriangleA_.getLengthByIndex( cParam.getSide() );
                    pointCA_ = nodeTriangleA_.getPointByCParam( cParam, nodeTriangleA_);
                }
                break;
        }
    }

    public PointXY getPointByCParam(ConnParam cparam, Triangle prnt ) {
        if( prnt == null ) return new PointXY( 0f, 0f );
        int cside = cparam.getSide();
        int ctype = cparam.getType();
        int clcr  = cparam.getLcr();
        PointXY pp = getPointBySide( cside );
        //pp.add( getPointBy( pp, lengthA_, clcr ) );

        return pp;
    }


    public PointXY getPointBy( PointXY p, float la, int lcr){
        if( lcr == 2 ) return p;
        else return p;
//        if( lcr == 1 ) return p.offset(  );
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
        pointCA_ = getParentPointByType( cParam.getSide(), cParam.getType(), cParam.getLcr() );
        connectionType_ = cParam.getType();
        connectionLCR_ = cParam.getLcr();
        calcPoints( pointCA_, angleInGlobal_);
        return pointCA_;
    }


    public PointXY setBasePoint(  int pbc, int pct, int lcr ){
        pointCA_ = getParentPointByType( pbc, pct, lcr );
        connectionType_ = pct;
        connectionLCR_ = lcr;
        calcPoints( pointCA_, angleInGlobal_);
        return pointCA_;
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
        if(getSideByIndex(i) == "B") return getPointAB_();
        if(getSideByIndex(i) == "C") return getPointBC_();
        return null;
    }

    public PointXY crossOffset( int pbc ){
        if( pbc == 1 ) return getPointCA_().crossOffset(getPointBC_(),1.0f);
        if( pbc == 2 ) return getPointBC_().crossOffset(getPointAB_(),1.0f);
        return pointCA_;
    }

    public PointXY getParentPointByLCR( int pbc, int lcr ){
        if( nodeTriangleA_ == null ) return new PointXY(0f,0f);
        switch(pbc){
            case 1:
                switch(lcr){
                    case 0:
                        return nodeTriangleA_.pointAB_.offset(nodeTriangleA_.pointBC_, lengthA_);
                    case 1:
                        return getParentOffsetPointBySide(pbc);
                    case 2:
                        return nodeTriangleA_.pointBC_.clone();
                }
                break;
            case 2:
                switch(lcr){
                    case 0:
                        return nodeTriangleA_.pointBC_.offset(nodeTriangleA_.pointCA_, lengthA_);
                    case 1:
                        return getParentOffsetPointBySide(pbc);
                    case 2:
                        return nodeTriangleA_.pointCA_.clone();
                }
                break;
        }

        return new PointXY(0f,0f);
    }

    public PointXY getParentOffsetPointBySide(int pbc){
        if( nodeTriangleA_ == null ) return new PointXY(0f,0f);
        switch(pbc){
            case 1:
                return nodeTriangleA_.pointAB_.offset(nodeTriangleA_.pointBC_, nodeTriangleA_.getLengthB_()/2 + lengthA_ /2);
            case 2:
                return nodeTriangleA_.pointBC_.offset(nodeTriangleA_.pointCA_, nodeTriangleA_.getLengthC_()/2 + lengthA_ /2);
        }
        return nodeTriangleA_.getPointBySide(pbc);
    }

    public void setParent(Triangle parent, int pbc){
        nodeTriangleA_ = parent.clone();
        //myParentBC_ = pbc;
    }

    public void setCParamFromParentBC(int pbc){

        int curLCR = cParam_.getLcr();
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if( cParam_.getSide() == 0 && (pbc == 4 || pbc == 6 ) ) curLCR = 0;
        if( cParam_.getSide() == 0 && (pbc == 7 || pbc == 8 ) ) curLCR = 1;

        switch (pbc){
            case 1:
                cParam_ = new ConnParam( 1 ,0, 2, lengthAforce_);
                break;
            case 2:
                cParam_ = new ConnParam( 2 ,0, 2, lengthAforce_);
                break;
            case 3:
                cParam_ = new ConnParam( 1 ,1, curLCR, lengthAforce_);
                break;
            case 4:
                cParam_ = new ConnParam( 1 ,1, curLCR, lengthAforce_);
                break;
            case 5:
                cParam_ = new ConnParam( 2 ,1, curLCR, lengthAforce_);
                break;
            case 6:
                cParam_ = new ConnParam( 2 ,1, curLCR, lengthAforce_);
                break;
            case 7:
                cParam_ = new ConnParam( 1 ,1, curLCR, lengthAforce_);
                break;
            case 8:
                cParam_ = new ConnParam( 2 ,1, curLCR, lengthAforce_);
                break;
            case 9:
                cParam_ = new ConnParam( 1 ,2, curLCR, lengthAforce_);
                break;
            case 10:
                cParam_ = new ConnParam( 2 ,2, curLCR, lengthAforce_);
                break;
        }
    }

    public void setDimAlignByChild(){
        if( isChangeDimAlignB_ == false ){
            if( isChildB_ == false ) myDimAlignB_ = 1;
            else myDimAlignB_ = 3;
        }
        if( isChangeDimAlignC_ == false ){
            if( isChildC_ == false ) myDimAlignC_ = 1;
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
        if(nodeTriangleA_ != null ) return new Triangle(nodeTriangleA_);
        return null;
    }

    public boolean collision(float x, float y){
        return true;
    }

    private void setMyBound(){
        PointXY lb = pointCA_.min(pointAB_);
                lb = pointAB_.min(pointBC_);
        myBP_.setLeft(lb.getX());
        myBP_.setBottom(lb.getY());

        PointXY rt = pointCA_.max(pointAB_);
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
        float sumABC = lengthAforce_ + lengthBforce_ + lengthCforce_;
        float myArea = (sumABC*0.5f)*((sumABC*0.5f)- lengthAforce_)*((sumABC*0.5f)- lengthBforce_)*((sumABC*0.5f)- lengthCforce_);
        //myArea = roundByUnderTwo( myArea );
        return roundByUnderTwo((float)Math.pow(myArea, 0.5));
    }

    public float roundByUnderTwo(float fp) {
        float ip = fp * 100f;
        float rfp = Math.round(ip) / 100f;
        return rfp;
    }


    //maybe not use.
    private void setParentInfo(int myParNum, int myParBC, int myConne){
        this.parentNumber_ = myParNum;
        this.parentBC_ = myParBC;
    }

    public boolean validTriangle(){
        if (lengthA_ <=0.0f || lengthB_ <=0.0f || lengthC_ <=0.0f) return false;
        return isValidLengthes( lengthA_, lengthB_, lengthC_ );
        //!((this.lengthA_ + this.lengthB_) <= this.lengthC_) &&
          //      !((this.lengthB_ + this.lengthC_) <= this.lengthA_) &&
            //    !((this.lengthC_ + this.lengthA_) <= this.lengthB_);
    }

    public boolean isValidLengthes( float A, float B, float C ){
        return !( ( A + B ) <= C ) && !( ( B + C ) <= A ) && !( ( C + A ) <= B );
    }

    public double calculateInternalAngle(PointXY p1, PointXY p2, PointXY p3) {
        PointXY v1 = p1.subtract(p2);
        PointXY v2 = p3.subtract(p2);
        double angleRadian = Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()));
        double angleDegree = angleRadian * 180 / Math.PI;
            return angleDegree;
    }

    private void calcPoints(PointXY pCA, float angle){
        this.pointAB_.set((float) (pCA.getX()+(this.lengthA_ *Math.cos(toRadians(angle)))),
                         (float) (pCA.getY()+(this.lengthA_ *Math.sin(toRadians(angle)))));

        this.myTheta_ = Math.atan2( pCA.getY()- pointAB_.getY(), pCA.getX()-this.pointAB_.getX() );

        this.myPowA_ = Math.pow(this.lengthA_, 2);
        this.myPowB_ = Math.pow(this.lengthB_, 2);
        this.myPowC_ = Math.pow(this.lengthC_, 2);

        this.myAlpha_ = Math.acos((this.myPowA_ + this.myPowB_ - this.myPowC_)/(2* lengthA_ * lengthB_));

        this.pointBC_.set((float)(this.pointAB_.getX()+(this.lengthB_ *Math.cos(this.myTheta_ +this.myAlpha_))),
                         (float)(this.pointAB_.getY()+(this.lengthB_ *Math.sin(this.myTheta_ +this.myAlpha_))));

        this.angleInnerAB_ = (float)calculateInternalAngle(this.pointCA_, this.pointAB_, this.pointBC_);
        this.angleInnerBC_ = (float)calculateInternalAngle(this.pointAB_, this.pointBC_, this.pointCA_);
        this.angleInnerCA_ = (float)calculateInternalAngle(this.pointBC_, this.pointCA_, this.pointAB_);

        this.pointCenter_.set((pointAB_.getX()+ pointBC_.getX()+ pointCA_.getX())/3,
                                (pointAB_.getY()+ pointBC_.getY()+ pointCA_.getY())/3 );
        setMyBound();

        if( isPointNumberMoved_ == false ) autoAlignPointNumber();

        dimPointA_ = pointCA_.calcMidPoint(pointAB_);//.crossOffset(pointBC_, 0.2f*scale_);
        dimPointB_ = pointAB_.calcMidPoint(pointBC_);
        dimPointC_ = pointBC_.calcMidPoint(pointCA_);
        setDimPoint();

        dimAngleB_ = getAngleMpAB();
        dimAngleC_ = getAngleMmCA();
    }

    private void autoAlignPointNumber() {
        if( isPointNumberMoved_ == false ) pointNumber_ = pointCenter_; //とりあえず重心にする
        float offset2 = 1.5f;
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

        myDimAlignA_ = calcDimAlignByInnerAngleOf(0, angleInGlobal_);
        myDimAlignB_ = calcDimAlignByInnerAngleOf(1, getAngleMpAB());
        myDimAlignC_ = calcDimAlignByInnerAngleOf(2, getAngleMmCA());

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

    public int calcDimAlignByInnerAngleOf(int ABC, float angle){    // 夾角の、1:外 　3:内
            if (ABC == 0 ){
                if( parentBC_ == 9 || parentBC_ == 10 ) return 1;
                if( parentBC_ > 2 || nodeTriangleB_ != null || nodeTriangleC_ != null ) return 3;
            }
            if (ABC == 1 && ( nodeTriangleB_ != null ) ) return 1;
            if (ABC == 2 && ( nodeTriangleC_ != null ) ) return 1;
            return 3; // if ABC = 0
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
        if(num>2)num = 0;
        return num;
    }

    public int flipOneToThree( int num ){
        if( num == 1 ) return num = 3;
        if( num == 3 ) return num = 1;
        return num; // sonomama kaesu.
    }

    // 呼び出す場所によって、強制になってしまう。
    public void autoSetDimSideAlign(){
        if( lengthCforce_ < 1.5f || lengthBforce_ < 1.5f ){
            myDimAlignB_ = 1;
            myDimAlignC_ = 1;
        }
        if( lengthCforce_ < 1.5f ) dimSideAlignB_ = 1;
        if( lengthBforce_ < 1.5f ) dimSideAlignC_ = 2;

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

    public int zeroTwoRotate(int num){
        num = num + 1;
        if( num > 2 ) return num = 0;
        return num;
    }

    public int getDimAlignA(){
        return calcDimAlignByInnerAngleOf(0, angleInGlobal_);
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
        return calcDimAlignByInnerAngleOf(1, getAngleMpAB());
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
        return calcDimAlignByInnerAngleOf(2, getAngleMmCA());
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


    public PointXY getDimPointA_(){
        return dimPointA_;
    }

    public PointXY getDimPointB_(){
        return dimPointB_;
    }
    public PointXY getDimPointC_(){
        return dimPointC_;
    }

    public Bounds getMyBP_(){ return myBP_; }

    public PointXY getPointAB_() { return new PointXY(this.pointAB_); }
    public PointXY getPointBC_() { return new PointXY(this.pointBC_); }
    public PointXY getPointCA_() { return new PointXY(this.pointCA_); }
    public PointXY getPointCenter_() { return new PointXY(pointCenter_); }
    public PointXY getPointNumberAutoAligned_() {
        if( isPointNumberMoved_ == true ) return pointNumber_;
        //if( (lengthA+lengthB+lengthC)/scale_ < 7 ){
            //if( childSide_ == 0 ) return pointNumber.offset(pointBC, pointCA.calcMidPoint(pointAB).vectorTo(pointBC).lengthXY()*1.5f);
            //if( childSide_ == 1 ) return  pointNumber.offset(pointAB, pointCA.calcMidPoint(pointBC).vectorTo(pointAB).lengthXY()*1.5f);
            //if( childSide_ == 2 ) return  pointNumber.offset(pointCA, pointAB.calcMidPoint(pointBC).vectorTo(pointCA).lengthXY()*1.5f);
        //}

        //if( myAngleBC > 90 ) return pointNumber.offset(pointBC, pointNumber.calcMidPoint(pointAB).vectorTo(pointBC).lengthXY()*0.2f);
        //if( myAngleAB > 90 ) return pointNumber.offset(pointAB, pointNumber.calcMidPoint(pointCA).vectorTo(pointAB).lengthXY()*0.2f);
        //if( myAngleCA > 90 ) return pointNumber.offset(pointCA, pointNumber.calcMidPoint(pointBC).vectorTo(pointCA).lengthXY()*0.2f);

        if( lengthAforce_ < 2.5f ) return pointNumber_.offset(pointBC_, pointNumber_.vectorTo(pointBC_).lengthXY()*-0.3f);
        if( lengthBforce_ < 2.5f ) return pointNumber_.offset(pointCA_, pointNumber_.vectorTo(pointCA_).lengthXY()*-0.3f);
        if( lengthCforce_ < 2.5f ) return pointNumber_.offset(pointAB_, pointNumber_.vectorTo(pointAB_).lengthXY()*-0.3f);

        return pointNumber_;
    }


    public float getLengthA_() { return this.lengthA_; }
    public float getLengthB_() { return this.lengthB_; }
    public float getLengthC_() { return this.lengthC_; }
    public float getLengthAS(float s) { return this.lengthA_ *s; }
    public float getLengthBS(float s) { return this.lengthB_ *s; }
    public float getLengthCS(float s) { return this.lengthC_ *s; }

    public float getLengthByIndex(int i) {
        if( i == 1 ) return lengthB_;
        if( i == 2 ) return lengthC_;
        else return 0f;
    }

    public float getLengthByIndexForce(int i) {
        if( i == 1 ) return lengthBforce_;
        if( i == 2 ) return lengthCforce_;
        else return 0f;
    }


    public PointXY getPointBySide(int i){
        if(getSideByIndex(i) == "B") return getPointBC_();
        if(getSideByIndex(i) == "C") return getPointCA_();
        return null;
    }

    public float getAngleBySide(int i){
        if(getSideByIndex(i) == "B") return getAngleMpAB();
        if(getSideByIndex(i) == "C") return getAngleMmCA();
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
        int i = pbc;
        if(i==1 || i==3 || i==4 || i==7 || i==9) return 1;
        if(i==2 || i==5 || i==6 || i==8 || i==10) return 2;
        else return 0;
    }

    public float getAngle() { return this.angleInGlobal_; }
    public float getAngleAB() { return this.angleInnerAB_; }
    public float getAngleBC() { return this.angleInnerBC_; }
    public float getAngleCA() { return this.angleInnerCA_; }
    public float getAngleMmCA() { return this.angleInGlobal_ - this.angleInnerCA_; }
    public float getAngleMpAB() { return this.angleInGlobal_ + this.angleInnerAB_; }
    public float getAngleMpBC() { return this.angleInGlobal_ + this.angleInnerBC_; }

    public int getParentBC() { return parentBC_; }
    public int getParentNumber() { return parentNumber_; }
    public int getMyNumber_() { return myNumber_; }
    public void setNumber(int num) { myNumber_ = num; }
    public void setColor(int num) { color_ = num; }

    public Boolean isName( String name ){
        return myName_.equals( name );
    }

    public int getChildSide_(){return childSide_;}
    public void setChildSide_(int childside){ childSide_ = childside;}

    public void setChild(Triangle newchild, int cbc ){
        childSide_ = cbc;
        if( newchild.getPbc(cbc) == 1 ) {
                nodeTriangleB_ = newchild.clone();
                isChildB_ = true;
        }
        if( newchild.getPbc(cbc) == 2 ) {
                nodeTriangleC_ = newchild.clone();
                isChildC_ = true;
        }
        setDimAlignByChild();
    }

    public boolean alreadyHaveChild(int pbc){
        if( pbc < 1 ) return false;
        if( getSideByIndex(pbc) == "B" && isChildB_ == true ) return true;
        return getSideByIndex(pbc) == "C" && isChildC_ == true;
    }

    public Boolean hasChildIn(int cbc ){
        if ( ( nodeTriangleB_ != null || isChildB_ == true ) && cbc == 1 ) return true;
        return (nodeTriangleC_ != null || isChildC_ == true) && cbc == 2;
    }

    public void move(PointXY to){
        pointAB_.add(to);
        pointBC_.add(to);
        pointCA_.add(to);
        pointCenter_ = pointCenter_.plus(to);
        pointNumber_ = pointNumber_.plus(to);
        dimPointA_.add(to);
        dimPointB_.add(to);
        dimPointC_.add(to);
        myBP_.setLeft(myBP_.getLeft()+to.getX());
        myBP_.setRight(myBP_.getRight()+to.getX());
        myBP_.setTop(myBP_.getTop()+to.getY());
        myBP_.setBottom(myBP_.getBottom()+to.getX());
    }

    public void setScale( float scale){
        scale_ = scale;
        lengthA_ *= scale;
        lengthB_ *= scale;
        lengthC_ *= scale;
        calcPoints(pointCA_, angleInGlobal_);
    }

    public void scale(PointXY basepoint, float scale){
        scale_ *= scale;
        //pointAB_.scale(basepoint, scale);
        //pointBC_.scale(basepoint, scale);
        pointCA_.scale(basepoint, scale);
        pointCenter_.scale(basepoint, scale);
        pointNumber_.scale(basepoint, scale);
        lengthA_ *= scale;
        lengthB_ *= scale;
        lengthC_ *= scale;
        calcPoints(pointCA_, angleInGlobal_);

    }

    // 自分の次の番号がくっついている辺を調べてA辺にする。
    // 他の番号にあって自身の辺上に無い場合は、A辺を変更しない。
    public Triangle rotateLengthBy( int side ){
        //Triangle this = this.clone();
        float pf = 0f;
        int pi = 0;
        PointXY pp = new PointXY(0f,0f);

        if( side == 1 ){ // B to A
            pf = this.lengthA_;
            this.lengthA_ = this.lengthB_;
            this.lengthB_ = this.lengthC_;
            this.lengthC_ = pf;

            pf = this.lengthAforce_;
            this.lengthAforce_ = this.lengthBforce_;
            this.lengthBforce_ = this.lengthCforce_;
            this.lengthCforce_ = pf;

            pp = pointCA_.clone();
            this.pointCA_ = this.pointAB_;
            this.pointAB_ = this.pointBC_;
            this.pointBC_ = pp.clone();

            pf = this.angleInnerCA_;
            this.angleInnerCA_ = this.angleInnerAB_;
            this.angleInnerAB_ = this.angleInnerBC_;
            this.angleInnerBC_ = pf;

            this.angleInGlobal_ = getAngleMmCA() - this.angleInnerAB_;

            if( angleInGlobal_ < 0 ) angleInGlobal_   += 360f;
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
            pf = this.lengthA_;
            this.lengthA_ = this.lengthC_;
            this.lengthC_ = this.lengthB_;
            this.lengthB_ = pf;

            pf = this.lengthAforce_;
            this.lengthAforce_ = this.lengthCforce_;
            this.lengthCforce_ = this.lengthBforce_;
            this.lengthBforce_ = pf;

            pp = pointCA_.clone();
            this.pointCA_ = this.pointBC_;
            this.pointBC_ = this.pointAB_;
            this.pointAB_ = pp.clone();

            float ga = angleInGlobal_;
            pf = this.angleInnerCA_;
            this.angleInnerCA_ = this.angleInnerBC_;
            this.angleInnerBC_ = this.angleInnerAB_;
            this.angleInnerAB_ = pf;
            this.angleInGlobal_ += this.angleInnerCA_ + this.angleInnerBC_;

            if( angleInGlobal_ < 0 ) angleInGlobal_   += 360f;
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

        return this;
    }

    public void setReverseDefSide(int pbc, Boolean BtoC) {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if( BtoC == false ){
            if( pbc == 3 ) parentBC_ = 4;
            else if( pbc == 4 ) parentBC_ = 3;
            else if( pbc == 5 ) parentBC_ = 6;
            else if( pbc == 6 ) parentBC_ = 5;
            else if( pbc == 9 ) parentBC_ = 9;
            else if( pbc == 10 ) parentBC_ = 10;
            else parentBC_ = pbc;
        }
        if( BtoC == true ){
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
        return new Params(myName_,"", myNumber_, lengthA_, lengthB_, lengthC_, parentNumber_, parentBC_, pointCA_, pointCenter_);
    }

    public void setMyName_(String name){ myName_ = name;}
    public String getMyName_(){return myName_;}

    public int getTapLength( PointXY tapP ){
        setDimPoint();

        float range = 0.6f*scale_;

        if( true == tapP.nearBy(pointName_, range) ) return lastTapSide_ = 4;

        if( true == tapP.nearBy(dimPointA_, range) ) return lastTapSide_ = 0;
        if( true == tapP.nearBy(dimPointB_, range) ) return lastTapSide_ = 1;
        if( true == tapP.nearBy(dimPointC_, range) ) return lastTapSide_ = 2;
        if( true == tapP.nearBy(getPointNumberAutoAligned_(), range) ) return lastTapSide_ = 3;

        return lastTapSide_ = -1;
    }

    public boolean isCollide( PointXY p ) {
        return p.isCollide(pointAB_, pointBC_, pointCA_);
    }

    public void setDimPoint(){
        dimPointA_ = dimSideRotation( dimSideAlignA_, pointCA_.calcMidPoint(pointAB_), pointAB_, pointCA_);
        dimPointB_ = dimSideRotation( dimSideAlignB_, pointAB_.calcMidPoint(pointBC_), pointBC_, pointAB_);
        dimPointC_ = dimSideRotation( dimSideAlignC_, pointBC_.calcMidPoint(pointCA_), pointCA_, pointBC_);
    }

    public PointXY dimSideRotation(int side, PointXY dimPoint, PointXY offsetLeft, PointXY offsetRight) {
        if( side == 0 ) return dimPoint;

        PointXY offsetTo = offsetRight;
        float haba = dimPoint.lengthTo( offsetRight ) * 0.5f;

        if( side == 1 ){
            offsetTo = offsetLeft;
        }

        return dimPoint.offset(offsetTo, haba);
    }

    public void rotate(PointXY basepoint, float degree){
        pointCA_ = pointCA_.rotate(basepoint, degree);
        angleInGlobal_ += degree;

        calcPoints(pointCA_, angleInGlobal_);
        //setDimAlign();

    }

    public void setPointNumberMoved_(PointXY p){
        this.pointNumber_ = p;
        isPointNumberMoved_ = true;
    }


    public void setDimPath( float ts ){
        dimH_ = ts;
        pathA_ = new PathAndOffset(scale_, pointAB_, pointCA_, pointBC_, lengthAforce_, myDimAlignA_, dimSideAlignA_, dimH_);
        pathB_ = new PathAndOffset(scale_, pointBC_, pointAB_, pointCA_, lengthBforce_, myDimAlignB_, dimSideAlignB_, dimH_);
        pathC_ = new PathAndOffset(scale_, pointCA_, pointBC_, pointAB_, lengthCforce_, myDimAlignC_, dimSideAlignC_, dimH_);
        pathS_ = new PathAndOffset(scale_, pointAB_, pointCA_, pointBC_, lengthAforce_, 4, 0, dimH_);

        //sla_ = formattedString( lengthAforce_, 3);
        //slb_ = formattedString( lengthBforce_, 3);
        //slc_ = formattedString( lengthCforce_, 3);
    }

    public String formattedString(float digit, int fractionDigits ){
        // 0の場合は空文字
        if(digit == 0) return "0.00";
        String formatter = "%.${fractionDigits}f";
        return formatter.format( Float.toString(digit) );
    }

    public PathAndOffset getPath(int side) {
        //setDimPath();
        if(side == 0 ) return pathA_;
        if(side == 1 ) return pathB_;
        if(side == 2 ) return pathC_;
        return pathC_;
    }

    public boolean isFloating(){
        return parentBC_ == 9 || parentBC_ == 10;
    }
}
