package com.kshitijpatil.elementaryeditor.domain

/*
class CombineAdjacentStrategyTest {
    private val combineAdjacentStrategy = CombineAdjacentStrategy()


    @Test
    fun combineAdjacentCrop() {
        val inputPayloads = listOf(
            EditPayload.Crop(Bound(284,0,722,755),0,0),
            EditPayload.Crop(Bound(263,50,459,300),0,0),
            EditPayload.Crop(Bound(0,159,459,596),0,0),
        )
        val actual = Bound(
            offsetX = 284, // max
            offsetY = 159, // max
            width = 459,   // min
            height = 300   // min
        )
        val combinedList = combineAdjacentStrategy.combine(inputPayloads)
        assertThat(combinedList).isNotEmpty()
        val result = combinedList[0]
        assertThat(result).isInstanceOf(EditPayload.Crop::class.java)
        result as EditPayload.Crop
        assertThat(result.cropBounds).isEqualTo(actual) // max
    }

    @Test
    fun combineAdjacentRotate() {
        val rotateTestSuite = mapOf(
            listOf(90f,90f,-90f) to 90f,
            listOf(90f) to 90f,
            listOf(90f, 90f, 90f, 90f) to 0f,
            listOf(-90f,-90f,-180f) to -0f
        )
        rotateTestSuite.forEach { (rotateDegrees, actual) ->
            val inputPayloads = rotateDegrees.map { EditPayload.Rotate(it) }
            val combinedList = combineAdjacentStrategy.combine(inputPayloads)
            assertThat(combinedList).isNotEmpty()
            val result = combinedList[0]
            assertThat(result).isInstanceOf(EditPayload.Rotate::class.java)
            result as EditPayload.Rotate
            assertThat(result.degrees).isEqualTo(actual)
        }
    }
}*/
