package com.strumenta.rpgtojava.intermediateast

import com.strumenta.kolasu.model.Link
import com.strumenta.kolasu.model.Node

// Common interfaces

interface GNamed {
    val name: String
}

interface GTyped {
    val type: GType
}

// Targets

abstract class GTarget : Node()

data class GGlobalVariableTarget(
        @Link
        val globalVariable: GGlobalVariable) : GTarget()


// Others

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

data class GProgram(
        override val name: String,
        val globalVariables: MutableList<GGlobalVariable> = mutableListOf(),
        val mainFunction: GFunction = GFunction("executeProgram"),
        val otherFunctions: MutableList<GFunction> = mutableListOf()
) : Node(), GNamed
