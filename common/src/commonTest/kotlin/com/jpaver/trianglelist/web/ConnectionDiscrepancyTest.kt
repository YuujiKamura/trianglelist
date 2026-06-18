package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Line
import com.jpaver.trianglelist.editmodel.ConnParam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionDiscrepancyTest {

    @Test
    fun test_setOn_vs_CycleShapeConstructor_SideB() {
        val parent = Triangle(10f, 8f, 6f) 
        // Parent Root (180 deg): p[0]=(0,0), pAB=(-10,0)
        // alpha at AB: (100+64-36)/(2*10*8) = 0.8 -> 36.87 deg
        // pBC = (-10,0) + vector(8, 36.87 deg) = (-10+6.4, 4.8) = (-3.6, 4.8)
        
        // --- Pure Path (setOn) ---
        // Side B (1) has length 8. New triangle side A=8, B=7, C=5.
        // setOn(1) -> Start=BC=(-3.6, 4.8), Angle=BC->AB.
        val triPure = Triangle(parent, 1, 7f, 5f) 
        
        // --- Mixed Path (CycleShape Constructor) ---
        // Side B(1), type(0), lcr(2) (Default for code 1)
        val cp = ConnParam(1, 0, 2, 8f)
        val triMixed = Triangle(parent as CycleShape, cp, 7f, 5f)
        
        println("Pure p[0]: ${triPure.point[0]}, pAB: ${triPure.pointAB}, pBC: ${triPure.pointBC}")
        println("Mixed p[0]: ${triMixed.point[0]}, pAB: ${triMixed.pointAB}, pBC: ${triMixed.pointBC}")
        
        // After fix, they should be IDENTICAL
        assertEquals(triPure.point[0].x, triMixed.point[0].x, "p[0].x mismatch")
        assertEquals(triPure.point[0].y, triMixed.point[0].y, "p[0].y mismatch")
        assertEquals(triPure.pointAB.x, triMixed.pointAB.x, "pAB.x mismatch")
        assertEquals(triPure.pointAB.y, triMixed.pointAB.y, "pAB.y mismatch")
    }

    @Test
    fun test_setOn_vs_CycleShapeConstructor_SideC() {
        val parent = Triangle(10f, 8f, 6f) 
        // Side C is BC -> CA = (-3.6, 4.8) -> (0,0). Length 6.
        
        // --- Pure Path (setOn) ---
        val triPure = Triangle(parent, 2, 5f, 4f) 
        
        // --- Mixed Path (CycleShape Constructor) ---
        // Side C(2), type(0), lcr(2) (Default for code 2)
        val cp = ConnParam(2, 0, 2, 6f)
        val triMixed = Triangle(parent as CycleShape, cp, 5f, 4f)
        
        println("Pure p[0]: ${triPure.point[0]}, pAB: ${triPure.pointAB}")
        println("Mixed p[0]: ${triMixed.point[0]}, pAB: ${triMixed.pointAB}")
        
        assertEquals(triPure.point[0].x, triMixed.point[0].x, "p[0].x mismatch")
        assertEquals(triPure.point[0].y, triMixed.point[0].y, "p[0].y mismatch")
    }
}
