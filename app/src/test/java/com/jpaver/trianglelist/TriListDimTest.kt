import org.junit.Assert.*
import org.junit.Test
import com.jpaver.trianglelist.Triangle
import com.jpaver.trianglelist.TriangleList

class TriangleListAndDimsTest {

    @Test
    fun `adding a long thin Triangle to TriangleList retains side lengths`() {
        // A=100, B=1, C=100 (細長い三角形)
        val longThin = Triangle(100f, 1f, 100f)
        val list = TriangleList(longThin)

        // リストにひとつだけ追加されている
        assertEquals(1, list.size())

        val t = list.getByNumber(1)
        assertEquals(100f, t.lengthA_, 0.001f)
        assertEquals(1f,   t.lengthB_, 0.001f)
        assertEquals(100f, t.lengthC_, 0.001f)
    }

    @Test
    fun `autoDimHorizontal triggers exactly one horizontal align on thin triangle`() {
        // A=100, B=1, C=100 の細長い三角形でテスト
        val tri = Triangle(100f, 1f, 100f)
        val dims = tri.dim

        // 改めて水平自動配置を実行（コンストラクタ内にも一度走っているので、再呼び出しで確実にフラグが立つ側を確認）
        dims.autoDimHorizontal()

        // B 辺・C 辺のうち、ちょうど一方だけフラグが立っていること
        val bAligned = dims.flag[1].isAutoAligned
        val cAligned = dims.flag[2].isAutoAligned
        assertTrue(bAligned.xor(cAligned))

        // 立った側の horizontal が 3 または 4 になっていること
        if (bAligned) {
            assertTrue(dims.horizontal.b == 3 || dims.horizontal.b == 4)
            assertEquals(0, dims.horizontal.c)
        } else {
            assertTrue(dims.horizontal.c == 3 || dims.horizontal.c == 4)
            assertEquals(0, dims.horizontal.b)
        }
    }
}
