package pavlova.alexandra

class BitCounter(L: Int) {
    private val maxLen = L
    private val chunkSize = 2
    private val mod = (1 shl (8 * chunkSize)).toUInt()

    private fun checkLen(array: List<Byte>) {
        if (array.size > maxLen) {
            throw IllegalArgumentException("Length of the array should be < $maxLen, but it's ${array.size}")
        }
    }

    private fun UInt.invert(): UInt {
        return mod - 1u - this
    }

    fun checkSum(array: List<Byte>, sum: UInt): Boolean {
        return (countControlSum(array) + sum.invert()) % mod == mod - 1u
    }

    fun countControlSum(array: List<Byte>): UInt {
        checkLen(array)
        var sum = 0u
        for (chunk in array.chunked(chunkSize)) {
            sum += getSum(chunk)
        }
        return (sum % mod).invert()
    }

    private fun getSum(array: List<Byte>): UInt {
        var sum = 0u
        for (byte in array) {
            sum = sum shl 8
            sum += byte.toUInt()
        }
        return sum
    }
}