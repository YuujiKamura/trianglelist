package com.jpaver.trianglelist;

import java.util.ArrayList;

public class DeductionList extends EditList implements Cloneable {

    ArrayList<Deduction> dedlist_ = new ArrayList<Deduction>();
    ArrayList<Deduction> myDedListAtView = new ArrayList<Deduction>();
    int current = 0;
    int lastTapIndex_ = -1;
    float myAngle = 0f;
    PointXY myCenter = new PointXY(0f,0f);

    DeductionList(){
    }

    public void copy(){
        myDedListAtView.clear();
        for (int i = 0; i < dedlist_.size(); i++ ) {
            myDedListAtView.add(dedlist_.get(i).clone());
        }
    }

    public void scale(PointXY basepoint, float sx, float sy){
        for (int i = 0; i < dedlist_.size(); i++ ) {
            dedlist_.get(i).scale(basepoint, sx, sy);
        }
    }


    public void setScale( float s ){
        for (int i = 0; i < dedlist_.size(); i++ ) {
            dedlist_.get(i).setScale_( s );
        }
    }

    public void scale(PointXY basepoint, float s){
        for (int i = 0; i < dedlist_.size(); i++ ) {
            dedlist_.get(i).scale(basepoint, s, s);
        }
    }

    public void move(PointXY to){
        for (int i = 0; i < dedlist_.size(); i++ ) {
            dedlist_.get(i).move(to);
        }
    }

    public int getTapIndex(PointXY tapP ){
        int i;
        for(i=0; i < dedlist_.size(); i++) {
            if( dedlist_.get(i).getTap(tapP) == true ) return lastTapIndex_ = i;
        }
        return lastTapIndex_ = -11;
    }

    @Override
    public DeductionList clone(){
        DeductionList b = new DeductionList();
        for (int i = 0; i < dedlist_.size(); i++ ) {
            b.add(dedlist_.get(i).clone());
        }
        b.lastTapIndex_ = this.lastTapIndex_;
        return b;
    }

    public void rotate(PointXY bp, float angle){
        myAngle += angle;
        for (int i = 0; i < dedlist_.size(); i++ ) {
            dedlist_.get(i).rotate(bp, angle);
        }
    }

    public float roundByUnderTwo(float fp) {
        float ip = fp * 100f;
        ip = Math.round(ip);
        float rfp = (float)ip / 100;
        return rfp;
    }

    @Override
    public float getArea(){
        float area = 0f;
        for (int i = 0; i < dedlist_.size(); i++ ) {
            area += dedlist_.get(i).getArea();
        }
        return (float)(Math.round( area * 100.0) / 100.0); //roundByUnderTwo(area);
    }

    @Override
    public int addCurrent(int i){
        current = current + i;
        return current;
    }

    @Override
    public int getCurrent(){
        return current;
    }

    @Override
    public void setCurrent(int i){
        current = i;
    }

    public void clear(){
        dedlist_.clear();}

    public void add(Deduction dd){
        dd.setSameDedcount( searchSameDed( dd.getParams() ) );
        dedlist_.add(dd);
        current = dedlist_.size();
    }

    public void add(DeductionParams ddp){
        dedlist_.add(new Deduction(ddp));
        current = dedlist_.size();
    }

    public void add(Params dp){
        int sameCount = searchSameDed( dp );
        dedlist_.add(new Deduction(dp));
        if( sameCount > 1 ) dedlist_.get(dedlist_.size()-1).setSameDedcount( sameCount );
        current = dedlist_.size();
    }

    public int searchSameDed(Params dp){
        int count = 1;
        for (int i = 0; i < dedlist_.size(); i++ ) {
            if( dedlist_.get(i).verify(dp) ) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Deduction get(int num){
        if( num < 1 || num > dedlist_.size() ) return new Deduction();
        return getDeduction(num);
    }

    @Override
    public Params getParams(int num){
        return dedlist_.get(num-1).getParams();
    }

    public Deduction getDeduction(int num){

        if( num < 1 || num > dedlist_.size() ) return null;
        return dedlist_.get(num-1);
    }

    @Override
    public int size(){
        return dedlist_.size();
    }

    @Override
    public void remove(int index){
        if(index < 1) return;
        dedlist_.remove(index-1);    // 0 1 (2) 3  to 0 1 3
        while(index-1 < dedlist_.size()){    // index 2 size 3
            dedlist_.get(index-1).setNum(index); // get [2] renum to 3
            index++;
        }
        current = dedlist_.size();
    }

    public void replace(int index, Params dp){
        if(index < 1) return;
        dedlist_.get(index-1).setParam(dp);
    }

    public DeductionList reverse(){
        DeductionList rev = new DeductionList();

        int iBackward = dedlist_.size()-1;

        for( int i = 0; i < dedlist_.size(); i++ ) {

            dedlist_.get( iBackward ).setNum( i+1 );

            rev.add( dedlist_.get( iBackward ) );

            iBackward--;
        }
        dedlist_ = rev.dedlist_;
        return rev;
    }
}
