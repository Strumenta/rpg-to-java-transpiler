package com.strumenta.rpgtojava.intermediateast

import com.github.javaparser.ast.expr.Expression
import com.strumenta.kolasu.model.Link
import com.strumenta.kolasu.model.Node

interface GNamed {
    val name: String
}

abstract class GType : Node()

object GStringType : GType()
object GIntegerType : GType()
object GDecimalType : GType()

interface GTyped {
    val type: GType
}

abstract class GStatement : Node()

abstract class GExpression : Node()

data class GGlobalVariable(
        override val name: String,
        override val type: GType,
        var initialValue: GExpression? = null) : Node(), GNamed, GTyped

data class GFunction(
        override val name: String,
        val parameters: MutableList<Parameter> = mutableListOf(),
        val body: MutableList<GStatement> = mutableListOf()
) : Node(), GNamed {
    data class Parameter(val name: String, val type: GType)
}

abstract class GTarget : Node()

data class GGlobalVariableTarget(
        @Link
        val globalVariable: GGlobalVariable) : GTarget()

data class GAssignment(
        val target: GTarget,
        val value: GExpression
) : GStatement()

data class GIntegerLiteral(val value: Long) : GExpression()
data class GDecimalLiteral(val value: Double) : GExpression()
data class GStringLiteral(val value: String) : GExpression()
data class GStringToIntExpr(val string: GExpression) : GExpression()
data class GGlobalVariableRef(
        @Link
        val globalVariable: GGlobalVariable) : GExpression()

data class GExecuteFunction(
        @Link
        val function: GFunction) : GStatement()

data class GResetStringStmt(val target: GTarget) : GStatement()

data class GStringConcatExpr(val left: GExpression, val right: GExpression) : GExpression()
data class GSumExpr(val left: GExpression, val right: GExpression) : GExpression()

data class GToStringExpr(val value: GExpression) : GExpression()

data class GPrintStmt(val value: GExpression) : GStatement()

data class GEqualityExpr(val left: GExpression, val right: GExpression) : GExpression()

data class GSwitchStmt(val cases: List<Case>, val elseCase: Else?) : GStatement() {
    data class Case(val condition: GExpression, val body: List<GStatement>)
    data class Else(val body: List<GStatement>)
}

data class GForStmt(val variable: GTarget,
                    val minValue: GExpression,
                    val maxValue: GExpression, val body: List<GStatement>) : GStatement()

data class GProgram(
        override val name: String,
        val globalVariables: MutableList<GGlobalVariable> = mutableListOf(),
        val mainFunction: GFunction = GFunction("main"),
        val otherFunctions: MutableList<GFunction> = mutableListOf()
) : Node(), GNamed
