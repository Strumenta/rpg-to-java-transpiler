package com.strumenta.rpgtojava.intermediateast

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
        val body: MutableList<GStatement> = mutableListOf()
) : Node(), GNamed

data class GIntegerLiteral(val value: Long) : GExpression()
data class GDecimalLiteral(val value: Double) : GExpression()
data class GStringLiteral(val value: String) : GExpression()

data class GProgram(
        override val name: String,
        val globalVariables: MutableList<GGlobalVariable> = mutableListOf(),
        val mainFunction: GFunction = GFunction("main"),
        val otherFunctions: MutableList<GFunction> = mutableListOf()
) : Node(), GNamed
