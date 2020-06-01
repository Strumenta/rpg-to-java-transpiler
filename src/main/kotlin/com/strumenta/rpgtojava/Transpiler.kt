package com.strumenta.rpgtojava

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.smeup.rpgparser.interpreter.DataDefinition
import com.smeup.rpgparser.interpreter.NumberType
import com.smeup.rpgparser.interpreter.StringType
import com.smeup.rpgparser.interpreter.Type
import com.smeup.rpgparser.parsing.ast.*
import com.smeup.rpgparser.parsing.facade.RpgParserFacade
import com.smeup.rpgparser.parsing.parsetreetoast.toAst
import com.strumenta.rpgtojava.intermediateast.*
import java.io.File
import java.io.FileInputStream

fun transformFromRPGtoIntermediate(rpgCu: CompilationUnit, name: String) : GProgram {
    val program = GProgram(name)
    rpgCu.allDataDefinitions.forEach {
        val gv = GGlobalVariable(it.name, it.type.toGType())
        if (it is DataDefinition) {
            if (it.initializationValue != null) {
                gv.initialValue = (it.initializationValue as Expression).toGExpression()
            }
        }
        program.globalVariables.add(gv)
    }
    return program
}

private fun Expression.toGExpression(): GExpression {
    return when (this) {
        is IntLiteral -> GIntegerLiteral(this.value)
        is RealLiteral -> GDecimalLiteral(this.value.toDouble())
        is StringLiteral -> GStringLiteral(this.value)
        else -> TODO("Not yet implemented $this")
    }
}

private fun Type.toGType(): GType {
    return when (this) {
        is StringType -> GStringType
        is NumberType -> {
            if (this.integer) {
                GIntegerType
            } else {
                GDecimalType
            }
        }
        else -> TODO("Not yet implemented $this")
    }
}

fun transformFromIntermediateToJava(intermediateAst: GProgram) : com.github.javaparser.ast.CompilationUnit {
    val javaCu = com.github.javaparser.ast.CompilationUnit()
    val javaClass = javaCu.addClass(intermediateAst.name)
    intermediateAst.globalVariables.forEach {
        val javaField = javaClass.addField(it.type.toJavaType(), it.name, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC)
        if (it.initialValue != null) {
            javaField.getVariable(0).setInitializer(it.initialValue!!.toJavaExpression())
        }
    }
    return javaCu;
}

private fun GExpression.toJavaExpression(): com.github.javaparser.ast.expr.Expression {
    return when (this) {
        is GIntegerLiteral -> IntegerLiteralExpr(this.value.toString())
        is GDecimalLiteral -> DoubleLiteralExpr(this.value.toString())
        is GStringLiteral -> StringLiteralExpr(this.value)
        else -> TODO("Not yet implemented $this")
    }
}

private fun GType.toJavaType(): com.github.javaparser.ast.type.Type {
    return when (this) {
        is GStringType -> JavaParser().parseClassOrInterfaceType("java.lang.String").result.get()
        is GIntegerType -> PrimitiveType.longType()
        is GDecimalType -> PrimitiveType.doubleType()
        else -> TODO("Not yet implemented $this")
    }
}

fun transform(rpgCu: CompilationUnit, name: String) : com.github.javaparser.ast.CompilationUnit {
    val gProgram = transformFromRPGtoIntermediate(rpgCu, name)
    val javaAst = transformFromIntermediateToJava(gProgram)
    return javaAst
}

fun generate(javaAst: com.github.javaparser.ast.CompilationUnit) : String {
    return javaAst.toString(PrettyPrinterConfiguration())
}

fun main(args: Array<String>) {
    val facade = RpgParserFacade()
    val rpgCu = facade.parseAndProduceAst(FileInputStream(File("src/test/resources/CALCFIB.rpgle")))
    val javaAst = transform(rpgCu, "CalcFib")
    val transpilationRes = generate(javaAst)
    println(transpilationRes)
}