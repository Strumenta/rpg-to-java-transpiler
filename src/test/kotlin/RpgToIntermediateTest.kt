import com.strumenta.kolasu.parsing.toStream
import com.strumenta.rpgtojava.intermediateast.*
import com.strumenta.rpgtojava.transformRpgToIntermediate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RpgToIntermediateTest {

    @Test
    fun simpleGlobalVariable() {
        val actualGProgram = transformRpgToIntermediate("""     D NBR             S              8  0""".toStream(), "Test")
        assertEquals(GProgram("Test", globalVariables = mutableListOf(GGlobalVariable("NBR", GIntegerType))), actualGProgram)
    }

    @Test
    fun simpleStatement() {
        val actualGProgram = transformRpgToIntermediate("""     D NBR             S              8  0
     C                   EVAL      NBR = 123""".toStream(), "Test")
        val expectedGProgram = GProgram("Test",
                globalVariables = mutableListOf(GGlobalVariable("NBR", GIntegerType)))
        expectedGProgram.mainFunction.body.add(GAssignment(GGlobalVariableTarget(expectedGProgram.globalVariables[0]), GIntegerLiteral(123L)))
        assertEquals(expectedGProgram, actualGProgram)
    }

    @Test
    fun emptySupportFunction() {
        val actualGProgram = transformRpgToIntermediate("""     C     FIB           BEGSR
     C                   ENDSR""".toStream(), "Test")
        assertEquals(GProgram("Test", otherFunctions = mutableListOf(GFunction("FIB"))), actualGProgram)
    }

}