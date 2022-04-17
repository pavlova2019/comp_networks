package pavlova.alexandra

import org.junit.Assert.*
import org.junit.Test

class TestCounter {
    @Test
    fun testEmpty() {
        val counter = BitCounter(10)
        val list: List<Byte> = listOf()
        assertEquals(65535u, counter.countControlSum(list))
        assertTrue(counter.checkSum(list, 65535u))
        assertFalse(counter.checkSum(list, 0u))
    }

    @Test
    fun testSimple() {
        val counter = BitCounter(10)
        val list: List<Byte> = listOf(1, 2, 3, 4)
        assertEquals(64505u, counter.countControlSum(list))
        assertTrue(counter.checkSum(list, 64505u))
        assertFalse(counter.checkSum(list, 1030u))
    }

    @Test
    fun testEven() {
        val counter = BitCounter(10)
        val list: List<Byte> = listOf(1, 2, 3, 4, 5)
        assertEquals(64500u, counter.countControlSum(list))
        assertTrue(counter.checkSum(list, 64500u))
        assertFalse(counter.checkSum(list, 1035u))
    }

    @Test
    fun testMaxLen() {
        val counter = BitCounter(5)
        val list: List<Byte> = listOf(1, 2, 3, 4, 5, 6)
        assertThrows(IllegalArgumentException::class.java) {counter.countControlSum(list)}
        assertThrows(IllegalArgumentException::class.java) {counter.checkSum(list, 1u)}
    }
}