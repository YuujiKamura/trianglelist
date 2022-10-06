package com.jpaver.trianglelist;

import org.jetbrains.annotations.NotNull;

import static java.lang.Math.atan2;

public class PointXY implements Cloneable {
    private float X, Y;

    PointXY(float x, float y){
        this.X = x;
        this.Y = y;
    }

    PointXY(float x, float y, float s){
        this.X = x * s;
        this.Y = y * s;
    }

    PointXY(PointXY p){
        this.X = p.getX();
        this.Y = p.getY();
    }

    @NotNull
    @Override
    public PointXY clone() {
        try {
            return (PointXY)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PointXY flip(PointXY p2){
        PointXY p3 = new PointXY(p2.getX(),p2.getY());

        p2.set(X,Y);
        this.set( p3.X, p3.Y );

        return p3;
    }

    public PointXY min(PointXY p){
        PointXY sp = new PointXY(X,Y);
        if(X > p.getX())sp.setX(p.getX());
        if(Y > p.getY())sp.setY(p.getY());

        return sp;
    }

    public PointXY max(PointXY p){
        PointXY sp = new PointXY(X,Y);
        if(X < p.getX())sp.setX(p.getX());
        if(Y < p.getY())sp.setY(p.getY());

        return sp;
    }

    public Boolean equals( float x, float y){
        float range = 0.001f;
        return X < x + range && X > x - range && Y < y + range && Y > y - range;
    }

    public float[] equal( float x, float y){
        float[] data = new float[2];
        data[0] = X - x;
        data[1] = Y - y;
        return data;
    }


    public float getX(){ return this.X; }
    public float getY(){ return this.Y; }

    public String info(){
        return getX() + " " + getY();
    }

    public PointXY set(float x, float y){
        this.X = x;
        this.Y = y;
        return this;
    }

    public void setX(float x){
        this.X = x;
    }

    public void setY(float y){
        this.Y = y;
    }

    public int side(){
        if( X < 0 ) return -1;
        else return 1;
    }

    public float calcAngle( PointXY p2, PointXY p3){

        PointXY v1 = p2.subtract(this);
        PointXY v2 = p2.subtract(p3);
        double angleRadian = Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()));
        double angleDegree = angleRadian * 180 / Math.PI;
        if (v1.outerProduct(v2) > 0) {
            return (float)angleDegree - 180;
        } else {
            return 180 - (float)angleDegree;
        }
    }

    public void set(PointXY sp){
        this.X = sp.getX();
        this.Y = sp.getY();
    }

    public PointXY convertToLocal(PointXY baseInView, PointXY centerInModel, PointXY scalebase, float zoom){
        PointXY inLocal = this.clone();
        inLocal.addminus( baseInView ).scale( new PointXY(0f,0f), 1/zoom); // // 左上起点座標を自身(pressedInView)から引く
        inLocal.add( centerInModel.scale(1f,-1f) );
        //inLocal.scale( baseInView.add(centerInModel.scale(1f,-1f)), 1/zoom);
        return inLocal;
    }

    public PointXY add(PointXY a){
        X = X + a.X;
        Y = Y + a.Y;
        return this;
    }

    public PointXY add(float a, float b){
        X = X + a;
        Y = Y + b;
        return this;
    }

    public PointXY plus(float x, float y){
        return new PointXY(X+x, Y+y);
    }

    public PointXY plus(PointXY a){
        return new PointXY(X+a.X, Y+a.Y);
    }


    public PointXY minus(PointXY a){
        return new PointXY(X-a.X, Y-a.Y);
    }

    public PointXY addminus(PointXY a){
        X = X - a.X;
        Y = Y - a.Y;
        return this;
    }

    public PointXY calcMidPoint(PointXY a){
        return new PointXY((X+a.X)/2, (Y+a.Y)/2);
    }

    public PointXY offset(PointXY p2, float movement){
        PointXY vector = this.vectorTo(p2);
        PointXY normalizedVector = vector.normalize();
        PointXY itsScaled = normalizedVector.scale( movement );

        return new PointXY( itsScaled ).add( this );

    }

    // 直交方向へのオフセット p2 is base line, p3 is cross vector align
    public PointXY crossOffset(PointXY p2, float movement){
        //PointXY normalizedVector = vectorTo(p2).normalize();

        // 一行にいろいろ書いてみる
        return new PointXY(this.minus(this.vectorTo(p2.rotate(this, -90)).normalize().scale(movement)));
        //return new PointXY(this.minus(normalizedVector.scale(movement)));
        //return new PointXY( X-(normalizedVector.getX()*movement), Y-(normalizedVector.getY()*movement) );
    }


    public PointXY vectorTo(PointXY p2)
    {
        return new PointXY(p2.X-X, p2.Y-Y);
    }

    public float lengthTo(PointXY p2){
        return new PointXY(this.X, this.Y).vectorTo(p2).lengthXY();
    }

    public PointXY normalize(){
        return new PointXY(X/ lengthXY(), Y/ lengthXY());
    }

    public float lengthXY(){
        return (float)(Math.pow(Math.pow(X,2)+Math.pow(Y,2), 0.5));
    }

    public float calcDimAngle(PointXY a){
        float angle = (float)(atan2(a.X-X, a.Y-Y) * 180 / Math.PI);
        //if(0 > angle )
        angle = -angle;
        angle += 90f;
        if(90 < angle) angle -= 180;

        if( angle < 0 ) angle += 360;

        return angle;
    }

    public float calcSokAngle(PointXY a, int vector){
        float angle = (float)(atan2(a.X-X, a.Y-Y) * 180 / Math.PI);
        //if(0 > angle )
        angle = -angle;
        angle += 90f;
        if( vector < 0 ) angle -= 180;

        return angle;
    }


    public void scale(PointXY a, float scale){
        X = X*scale + a.X;
        Y = Y*scale + a.Y;
    }

    public PointXY scale(float scale){
        return new PointXY(X*scale, Y*scale);
    }

    public PointXY scale(float scaleX, float scaleY ){
        return new PointXY(X*scaleX, Y*scaleY );
    }

    public PointXY scale(PointXY scale){
        return new PointXY(X*scale.getX(), Y*scale.getY());
    }



    public PointXY scale(PointXY a, float sx, float sy){
        return new PointXY(X*sx + a.X, Y*sy + a.Y);
    }

    public boolean nearBy( PointXY target, float range){
        return this.X > target.X - range && this.X < target.X + range && this.Y > target.Y - range && this.Y < target.Y + range;
    }

    public boolean isCollide(PointXY ab, PointXY bc, PointXY ca){

        boolean b1, b2, b3;

        b1 = sign(this, ab, bc) < 0.0f;
        b2 = sign(this, bc, ca) < 0.0f;
        b3 = sign(this, ca, ab) < 0.0f;

        return ((b1 == b2) && (b2 == b3));
    }

    float sign (PointXY p1, PointXY p2, PointXY p3)
    {
        return (p1.X - p3.X) * (p2.Y - p3.Y) - (p2.X - p3.X) * (p1.Y - p3.Y);
    }


    public boolean isCollide(Triangle tri){
            return isCollide(tri.pointAB_,tri.pointBC_,tri.point[0]); //Inside Triangle
    }

    public boolean isCollide(TriangleK tri){
        return isCollide(tri.point[1],tri.point[2],tri.point[0]); //Inside Triangle
    }


    public PointXY subtract(PointXY point) {
        return new PointXY(this.X - point.X, this.Y - point.Y);
    }

    public double magnitude() {
        return Math.sqrt(X * X + Y * Y);
    }

    public double innerProduct(PointXY point) {
        return this.X * point.X + this.Y * point.Y;
    }

    public double outerProduct(PointXY point) {
        return this.X * point.Y - this.Y * point.X;
    }

    // 座標回転メソッド
    public PointXY rotate( PointXY cp, float degree) {
        float x,y;            //回転後の座標
        double px,py;
        px = X-cp.getX();
        py = Y-cp.getY();
        x= (float)(px*Math.cos(degree/180*3.141592))-(float)(py*Math.sin(degree/180*3.141592))+cp.getX();
        y= (float)(px*Math.sin(degree/180*3.141592))+(float)(py*Math.cos(degree/180*3.141592))+cp.getY();

        return new PointXY(x,y);
    }


}