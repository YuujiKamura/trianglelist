package com.jpaver.trianglelist;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TriangleTest {


    @Test
    public void testResetNodeChain(){
        Triangle t = new Triangle(5f, 5f, 5f );
        Triangle t2 = new Triangle( t, 1, 5f, 5f );
        Triangle t3 = new Triangle( t2, 1, 5f, 5f );

        t2.set( t, 1, 6f, 6f,6f, true );

        assertEquals(6f, t.getLengthB_(), 0.001f );
        //assertEquals(6f, t3.lengthA_, 0.001f );

    }

    @Test
    public void testValid(){
        Triangle t = new Triangle(5f, 5f, 5f );
        assertEquals(true, t.validTriangle() );

        Triangle t2 = new Triangle(5f, 5f, 15f );
        assertEquals(false, t2.validTriangle() );

    }

    @Test
    public void testSetPointersFromCParams() {
        Triangle one = new Triangle( new Params( "", "", 1, 5f, 5f, 5f, -1, -1, new PointXY( 0f, 0f), new PointXY( 0f, 0f) ), 0f );
        ConnParam connection = new ConnParam( 1, 0, 0, 5f );
        Triangle two = new Triangle( one, connection, 6f, 6f );

        // 内容の一致
        assertEquals( one.myNumber_, two.nodeTriangleA_.myNumber_ );
        assertEquals( one.nodeTriangleB_.myNumber_, two.myNumber_ );

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleB_, two );

        two.set( one, 2, 5f, 5f);

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleC_, two );

    }

    @Test
    public void testSetPointersFromParams() {
        Triangle one = new Triangle( new Params( "", "", 1, 5f, 5f, 5f, -1, -1, new PointXY( 0f, 0f), new PointXY( 0f, 0f) ), 0f );
        Triangle two = new Triangle( one, new Params( "", "", 2, 5f, 5f, 5f, 1, 1, new PointXY( 0f, 0f), new PointXY( 0f, 0f) ) );

        // 内容の一致
        assertEquals( one.myNumber_, two.nodeTriangleA_.myNumber_ );
        assertEquals( one.nodeTriangleB_.myNumber_, two.myNumber_ );

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleB_, two );

        two.set( one, 2, 5f, 5f);

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleC_, two );

    }

    @Test
    public void testSetObjectPointers(){
        Triangle one = new Triangle( 5f, 5f,5f, new PointXY(0f,0f), 0f);
        Triangle two = new Triangle( one, 1, 5f, 5f );

        // 内容の一致
        assertEquals( one.myNumber_, two.nodeTriangleA_.myNumber_ );
        assertEquals( one.nodeTriangleB_.myNumber_, two.myNumber_ );

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleB_, two );

        two.set( one, 2, 5f, 5f);

        // オブジェクトポインタの一致。
        assertEquals( one, two.nodeTriangleA_ );
        assertEquals( one.nodeTriangleC_, two );

    }

    @Test
    public void testGetArea(){
        Triangle t = new Triangle(5.71f,9.1f,6.59f);
        assertEquals(18.74f, t.getArea(), 0.01f);
    }

    @Test
    public void testSideEffectInBasicTypes(){
        Triangle t = new Triangle(5f,5f,5f);
        int me = 0;
        me = t.rotateZeroToThree( me );
        // こうやって代入しない限り、meは渡された時点でクローンに代わり、副作用っぽく変化したりしない。
        // オブジェクトは違う。引数として生で渡された時、ポインタとして渡されるので、副作用が起きる。
        // t.sideEffectGo( object.clone ) とかすれば予防できる。

        assertEquals(1, me);
    }

    @Test
    public void testCalcDimAngle() {
        Triangle tri = new Triangle(5, 5, 5);
        float angle = tri.pointCA_.calcAngle(tri.pointAB_,tri.pointBC_);
        assertEquals( -120f, angle, 0.0001f);
        assertEquals( -120f, tri.pointBC_.calcAngle(tri.pointCA_,tri.pointAB_), 0.0001f);
        assertEquals( -120f, tri.pointAB_.calcAngle(tri.pointBC_,tri.pointCA_), 0.0001f);
}

    @Test
    public void testDimSideAlign(){
        Triangle tri1 = new Triangle(3f,4f,5f);

        tri1.rotateDimSideAlign(0);
        assertEquals(1, tri1.dimSideAlignA_);

        tri1.setDimPoint();
        assertEquals( -2.25f, tri1.dimPointA_.getX(), 0.001f);

        PointXY dim = new PointXY(-1.5f,0f);
        PointXY offsetLeft = new PointXY(-3f,0f);
        PointXY offsetRight = new PointXY(0f,0f);

        float haba = dim.lengthTo(offsetLeft) *0.5f;
        assertEquals( 0.75f, haba, 0.001f);
        haba = dim.lengthTo(offsetRight) *0.5f;
        assertEquals( 0.75f, haba, 0.001f);

        dim = dim.offset(offsetLeft, haba);
        assertEquals( -2.25f, dim.getX(), 0.001f);

        tri1.rotateDimSideAlign(0);
        tri1.setDimPoint();
        assertEquals( -0.75f, tri1.dimPointA_.getX(), 0.001f);

        tri1.flipDimAlignH(0);
        assertEquals( 1, tri1.myDimAlignA_);
        tri1.flipDimAlignH(0);
        assertEquals( 3, tri1.myDimAlignA_);
    }

    @Test
    public void testAutoAlignPointNumber2(){
        Triangle t1 = new Triangle(6.0f,3.5f,3.5f);
        assertEquals( -3.0f, t1.getPointNumberAutoAligned_().getX(), 0.001f);
        assertEquals( 118f, t1.getAngleBC(), 0.1f);
        assertEquals( 0.600f, t1.getPointNumberAutoAligned_().getY(), 0.01f);
    }

    @Test
    public void testAutoAlignPointNumber(){
        Triangle t1 = new Triangle(5,5,5);
        assertEquals( -2.5, t1.getPointNumberAutoAligned_().getX(), 0.001);
        assertEquals(  1.4433, t1.getPointNumberAutoAligned_().getY(), 0.001);

        Triangle t2 = new Triangle(5f,1.5f,5f);
        //t2.setChildSide(1);
        assertEquals( -4.235f, t2.getPointNumberAutoAligned_().getX(), 0.001f);

    }

    @Test
    public void testConnection(){
        Triangle t1 = new Triangle(3,4,5);

        Triangle t2 = new Triangle(t1,7,3,5,4); // connection 7 is set to B-Center
        assertEquals( 3.5f, t2.pointCA_.getY(), 0.001);

        Triangle t3 = new Triangle(t2,8,3,4,5); // connection 8 is set to C-Center
        assertEquals( -6.5f, t3.pointAB_.getX(), 0.001);

    }

    @Test
    public void testTriBounds(){
        Triangle myT = new Triangle(3,4,5);
        assertEquals(4f, myT.getMyBP_().getTop(), 0.001f);

        myT.move(new PointXY(5f,5f));
        assertEquals(9f, myT.getMyBP_().getTop(), 0.001f);

        Params myDParam = new Params( "集水桝", "Box",3,0.8f, 0.8f, 0f,0, 0, new PointXY(0.5f, 0.5f), new PointXY(0f,0f));
        Deduction myD = new Deduction(myDParam);

        myD.move(new PointXY(5f,5f));
        assertEquals(5.5f, myD.getPoint().getX(), 0.001f);

    }



    @Test
    public void testDimPathAndOffset(){
        Triangle t1 = new Triangle(3,4,5);
        Triangle t2 = new Triangle(t1,8, 4,4,4); //8:CC

        // 1下 3上 -> // 夾角の、1:外 　3:内
        assertEquals(3, t1.myDimAlignA_ );//getPath(0).getAlign_());
        assertEquals(3, t1.myDimAlignB_ );// t1.getPath(1).getAlign_());
        assertEquals(3, t1.myDimAlignC_ );// t1.getPath(2).getAlign_());
        assertEquals(3, t1.myDimAlignA_ );//getPath(0).getAlign_());
        assertEquals(3, t1.myDimAlignB_ );// t1.getPath(1).getAlign_());
        assertEquals(3, t1.myDimAlignC_ );// t1.getPath(2).getAlign_());
    }



    @Test
    public void testCalcAngleOfLength(){
        Triangle mytri = new Triangle(3.0f, 4.0f, 5.0f, new PointXY(0,0), 180.0f);

        assertEquals(0f, mytri.getPointCA_().calcDimAngle(mytri.getPointAB_()), 0.001);
        assertEquals(-1.5, mytri.getPointCA_().calcMidPoint(mytri.getPointAB_()).getX(), 0.01f);

    }



    @Test
    public void テストは成功する() {
        Triangle mytri1 = new Triangle(3.0f, 4.0f, 5.0f, new PointXY(0,0), 180.0f);
        assertThat(mytri1, is(mytri1));
    }


    @Test
    public void testTrianglePoint(){
        PointXY myXY0 = new PointXY(0.0f, 0.0f);
        PointXY myXY1 = new PointXY(-3.0f, 0.0f);
        Triangle myTriangle = new Triangle(3.0f, 4.0f, 5.0f, myXY0, 180.0f);
        assertEquals(myTriangle.getPointCA_().getX(), myXY0.getX(), 0.001);
        assertEquals(myTriangle.getPointCA_().getY(), myXY0.getY(), 0.001);

        assertEquals(myTriangle.getPointAB_().getX(), -3.0, 0.001);
        assertEquals(myTriangle.getPointAB_().getY(), -0.0, 0.001);

    }

    @Test
    public void testInvalidTriangle(){
        Triangle myTriT = new Triangle(3.0f, 4.0f, 5.0f, new PointXY(0f,0f), 180.0f);
        Triangle myTriT2 = new Triangle(1.111f, 4.0f, 5.0f, new PointXY(0f,0f), 180.0f);

        Triangle myTriF = new Triangle(0.0f, 4.0f, 5.0f, new PointXY(0f,0f), 180.0f);
        Triangle myTriF2 = new Triangle(0.999f, 4.0f, 5.0f, new PointXY(0f,0f), 180.0f);
        Triangle myTriF3 = new Triangle(1.0f, 4.0f, 5.0f, new PointXY(0f,0f), 180.0f);
        Triangle myTriF4 = new Triangle(4.0f, 5.0f, 1.0f, new PointXY(0f,0f), 180.0f);
        Triangle myTriF5 = new Triangle(4.0f, 1.0f, 5.0f, new PointXY(0f,0f), 180.0f);


        assertTrue(myTriT.validTriangle());
        assertTrue(myTriT2.validTriangle());

        assertFalse(myTriF.validTriangle());
        assertFalse(myTriF2.validTriangle());
        assertFalse(myTriF3.validTriangle());
        assertFalse(myTriF4.validTriangle());
        assertFalse(myTriF5.validTriangle());

        TriangleList myList = new TriangleList();
        assertTrue(myList.add(myTriT));
        //assertTrue(myList.add(myTriT2));
        assertFalse(myList.add(myTriF));
        assertFalse(myList.add(myTriF2));
        assertFalse(myList.add(myTriF3));

    }


    @Test
    public void testCalcThetaAlpha(){
        PointXY pCA = new PointXY(0.0f, 0.0f);
        Triangle myTriangle = new Triangle(3.0f, 4.0f, 5.0f, pCA, 180.0f);

        // atan2( y, x );
        double theta = Math.atan2(pCA.getY()-myTriangle.getPointAB_().getY(), pCA.getX()-myTriangle.getPointAB_().getX());
        double alpha = Math.acos(Math.pow(myTriangle.getLengthA_(),2)+Math.pow(myTriangle.getLengthB_(),2)-Math.pow(myTriangle.getLengthC_(),2));

        assertEquals( pCA.getX()-myTriangle.getPointAB_().getX(), 3.0, 0.005);
        assertEquals( pCA.getY()-myTriangle.getPointAB_().getY(), 0.0, 0.005);


        assertEquals(myTriangle.getPointAB_().getX(), -3.0, 0.005);
        assertEquals(theta, 0.0, 0.005);
        assertEquals(alpha, 1.570796, 0.005);

    }

    @Test
    public void testNewCalcPointB_CParam(){
        Triangle tri1 = new Triangle(3f, 5f, 4f, new PointXY(0f,0f), 0.0f);
        Triangle tri2 = new Triangle( tri1, 3, 6f, 5f, 4f );
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        assertEquals( true, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( true, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( true, tri1.pointBC_.equals( 0f, -4f ) );
        assertEquals( true, tri2.pointCA_.equals( 0f, -4f ) );
        assertEquals( false, tri2.pointAB_.equals( 3f, 0f ) );
        assertEquals( false, tri2.pointBC_.equals( 0f, -4f ) );

        tri1.calcPoints( tri2, 1 );
        assertEquals( false, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( false, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( false, tri1.pointBC_.equals( 0f, -4f ) );

    }

    @Test
    public void testNewCalcPointC(){
        Triangle tri1 = new Triangle(3f, 4f, 5f, new PointXY(0f,0f), 0.0f);
        Triangle tri2 = new Triangle( tri1, 2, 3f, 4f );

        assertEquals( true, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( true, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( true, tri1.pointBC_.equals( 3f, -4f ) );

        tri1.calcPoints( tri2, 2 );
        assertEquals( true, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( true, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( true, tri1.pointBC_.equals( 3f, -4f ) );

    }

    @Test
    public void testNewCalcPointB(){
        Triangle tri1 = new Triangle(3f, 5f, 4f, new PointXY(0f,0f), 0.0f);
        Triangle tri2 = new Triangle( tri1, 1, 4f, 3f );

        assertEquals( true, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( true, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( true, tri1.pointBC_.equals( 0f, -4f ) );

        tri1.calcPoints( tri2, 1 );
        assertEquals( true, tri1.pointCA_.equals( 0f, 0f ) );
        assertEquals( true, tri1.pointAB_.equals( 3f, 0f ) );
        assertEquals( true, tri1.pointBC_.equals( 0f, -4f ) );

    }

    @Test
    public void testCalcPoint(){
        PointXY myXY0 = new PointXY(0.0f, 0.0f);
        Triangle myTriangle = new Triangle(3.0f, 4.0f, 5.0f, myXY0, 90.0f);

        assertEquals(myTriangle.getPointAB_().getX(), 0.0, 0.001);
        assertEquals(myTriangle.getPointAB_().getY(), 3.0, 0.001);
        assertEquals(myTriangle.getPointBC_().getX(), 4.0, 0.001);
        assertEquals(myTriangle.getPointBC_().getY(), 3.0, 0.001);

        assertEquals(myTriangle.getAngleCA(), 53.130, 0.001);
        assertEquals(myTriangle.getAngleAB(), 90.000, 0.001);
        assertEquals(myTriangle.getAngleBC(), 36.869, 0.001);

    }
}
