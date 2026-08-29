
import com.jpaver.trianglelist.agent.AgentEdit
import com.jpaver.trianglelist.agent.AgentLayers
import com.jpaver.trianglelist.agent.Parts
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.dxf.DxfLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentEditTest {

    private fun reset() { AgentLayers.clearAll() }

    @Test fun `テキストを掴んで動かせる`() {
        reset()
        AgentLayers.claim("L", "a")
        AgentLayers.add("L", Parts.build("text", mapOf("x" to "100", "y" to "50", "t" to "作業帯", "h" to "3.5"), "L")!!)
        val i = AgentEdit.hitTest(101.0, 51.0, 5.0)
        assertNotNull(i, "テキストの上をクリックしたら当たること")
        AgentEdit.move(i, 10.0, -4.0)
        val t = AgentLayers.flat()[i].second as DxfText
        assertEquals(110.0, t.x, 1e-9)
        assertEquals(46.0, t.y, 1e-9)
    }

    @Test fun `テキストを書き換えられる`() {
        reset()
        AgentLayers.claim("L", "a")
        AgentLayers.add("L", Parts.build("text", mapOf("x" to "0", "y" to "0", "t" to "旧"), "L")!!)
        assertEquals("旧", AgentEdit.textOf(0))
        AgentEdit.setText(0, "新しい文字")
        assertEquals("新しい文字", AgentEdit.textOf(0))
    }

    @Test fun `線を掴んで動かせる`() {
        reset()
        AgentLayers.claim("L", "a")
        AgentLayers.add("L", Parts.build("line", mapOf("x1" to "0", "y1" to "0", "x2" to "100", "y2" to "0"), "L")!!)
        val i = AgentEdit.hitTest(50.0, 1.0, 3.0)
        assertNotNull(i, "線の近傍をクリックしたら当たること")
        AgentEdit.move(i, 0.0, 20.0)
        val l = AgentLayers.flat()[i].second as DxfLine
        assertEquals(20.0, l.y1, 1e-9)
        assertEquals(20.0, l.y2, 1e-9)
    }

    @Test fun `離れた位置をクリックしても掴まない`() {
        reset()
        AgentLayers.claim("L", "a")
        AgentLayers.add("L", Parts.build("line", mapOf("x1" to "0", "y1" to "0", "x2" to "100", "y2" to "0"), "L")!!)
        assertNull(AgentEdit.hitTest(50.0, 999.0, 3.0))
    }

    @Test fun `削除するとflatから消える`() {
        reset()
        AgentLayers.claim("L", "a")
        AgentLayers.add("L", Parts.build("line", mapOf("x1" to "0", "y1" to "0", "x2" to "10", "y2" to "0"), "L")!!)
        AgentLayers.add("L", Parts.build("line", mapOf("x1" to "0", "y1" to "5", "x2" to "10", "y2" to "5"), "L")!!)
        assertEquals(2, AgentLayers.flat().size)
        AgentEdit.deleteAt(0)
        assertEquals(1, AgentLayers.flat().size)
        assertEquals(5.0, (AgentLayers.flat()[0].second as DxfLine).y1, 1e-9)
    }

    @Test fun `他エージェントのレイヤは掴めるが所有権は奪えない`() {
        reset()
        assertTrue(AgentLayers.claim("X", "a1"))
        assertTrue(!AgentLayers.claim("X", "a2"), "別エージェントは同じレイヤを取れない")
        assertTrue(AgentLayers.claim("X", "a1"), "同一エージェントは再取得できる")
    }
}
