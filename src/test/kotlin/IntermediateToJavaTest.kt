import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.type.PrimitiveType
import com.strumenta.rpgtojava.intermediateast.*
import com.strumenta.rpgtojava.transformations.transformFromIntermediateToJava
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.github.javaparser.ast.CompilationUnit as JavaCU

class IntermediateToJavaTest {

    @Test
    fun simpleGlobalVariable() {
        val intermediateProgram = GProgram("Test", globalVariables = mutableListOf(GGlobalVariable("NBR", GIntegerType)))
        val actualJavaCu = transformFromIntermediateToJava(intermediateProgram)
        val expectedJavaCu = JavaCU()
        val expectedJavaClass = expectedJavaCu.addClass("Test")
        expectedJavaClass.addField(PrimitiveType.longType(), "NBR").setPrivate(true)
        expectedJavaClass.addMethod("executeProgram")
        assertEquals(expectedJavaCu, actualJavaCu)
    }

    @Test
    fun simpleStatement() {
        val intermediateProgram = GProgram("Test",
                globalVariables = mutableListOf(GGlobalVariable("NBR", GIntegerType)))
        intermediateProgram.mainFunction.body.add(GAssignment(GGlobalVariableTarget(intermediateProgram.globalVariables[0]), GIntegerLiteral(123L)))
        val actualJavaCu = transformFromIntermediateToJava(intermediateProgram)
        val expectedJavaCu = JavaCU()
        val expectedJavaClass = expectedJavaCu.addClass("Test")
        expectedJavaClass.addField(PrimitiveType.longType(), "NBR").setPrivate(true)
        val executeProgram = expectedJavaClass.addMethod("executeProgram")
        executeProgram.body.get().addStatement(AssignExpr(FieldAccessExpr(ThisExpr(), "NBR"), IntegerLiteralExpr("123"), AssignExpr.Operator.ASSIGN))
        assertEquals(expectedJavaCu, actualJavaCu)
    }

    @Test
    fun emptySupportFunction() {
        val intermediateProgram = GProgram("Test", otherFunctions = mutableListOf(GFunction("FIB")))
        val actualJavaCu = transformFromIntermediateToJava(intermediateProgram)
        val expectedJavaCu = JavaCU()
        val expectedJavaClass = expectedJavaCu.addClass("Test")
        expectedJavaClass.addMethod("executeProgram")
        expectedJavaClass.addMethod("FIB")
        assertEquals(expectedJavaCu, actualJavaCu)
    }

}