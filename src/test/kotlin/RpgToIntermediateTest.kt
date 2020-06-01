import com.strumenta.kolasu.parsing.toStream
import com.strumenta.rpgtojava.intermediateast.GGlobalVariable
import com.strumenta.rpgtojava.intermediateast.GIntegerType
import com.strumenta.rpgtojava.intermediateast.GProgram
import com.strumenta.rpgtojava.intermediateast.GStringType
import com.strumenta.rpgtojava.transformRpgToIntermediate
import com.strumenta.rpgtojava.transpileRpgToJava
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RpgToIntermediateTest {

    @Test
    fun simpleGlobalVariable() {
        val program = transformRpgToIntermediate("""     D NBR             S              8  0""".toStream(), "Test")
        assertEquals(GProgram("Test", globalVariables = mutableListOf(GGlobalVariable("NBR", GIntegerType))), program)
    }

}