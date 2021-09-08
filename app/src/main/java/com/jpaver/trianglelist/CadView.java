package com.jpaver.trianglelist;

public class CadView {
    PointXY basePoint;
    TriangleList myTriangleList;
    
    CadView(){
        this.basePoint = new PointXY(0.0f, 0.0f);
    }

    public void moveTo(PointXY basepoint){
        myTriangleList.move(basepoint);
    }

    public void scale(PointXY basepoint, float scale){
        //myTriangleList.cloneByScale(basepoint, scale);
    }

    public void setTriangleList(TriangleList trilist){
        this.myTriangleList = trilist;
    }

    public Triangle getTriangle(int index){
        return myTriangleList.getTriangle(index);
    }
}
