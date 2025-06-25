package com.jpaver.trianglelist

import com.jpaver.trianglelist.writer.HandleGen
import org.junit.Test

class HandleGenTest {

    @Test
    fun testHandleGenOutput() {
        val handleGen = HandleGen(100)
        
        println("=== HandleGen出力テスト（10進数版） ===")
        for (i in 1..10) {
            val handle = handleGen.new()
            println("$i: $handle")
        }
        
        println("\n=== オリジナル版との比較 ===")
        var entityHandle = 100
        for (i in 1..10) {
            entityHandle += 1
            println("オリジナル版 $i: $entityHandle")
        }
        
        println("\n=== current()メソッドテスト ===")
        val handleGen2 = HandleGen(200)
        println("current() before new(): ${handleGen2.current()}")
        println("new(): ${handleGen2.new()}")
        println("current() after new(): ${handleGen2.current()}")
    }
} 