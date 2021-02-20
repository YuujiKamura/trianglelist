package com.jpaver.trianglelist;

public class TriangleParent {
    protected int myParentNumber;
    protected String myParentBC;
    protected int myConnection;

    TriangleParent(int myprNum, String myprBC, int myconnection){
        this.myParentNumber = myprNum;
        this.myParentBC = myprBC;
        this.myConnection = myconnection;
    }

    public void set(int myprNum, String myprBC, int myconnection){
        this.myParentNumber = myprNum;
        this.myParentBC = myprBC;
        this.myConnection = myconnection;
    }
}
