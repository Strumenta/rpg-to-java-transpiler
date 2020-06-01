package com.strumenta.rpgtojava.intermediateast

import com.strumenta.kolasu.model.Link
import com.strumenta.kolasu.model.Node

abstract class GStatement : Node()

data class GAssignment(
        val target: GTarget,
        val value: GExpression
) : GStatement()

data class GResetStringStmt(val target: GTarget) : GStatement()

data class GExecuteFunction(
        @Link
        val function: GFunction) : GStatement()

data class GPrintStmt(val value: GExpression) : GStatement()


data class GSwitchStmt(val cases: List<Case>, val elseCase: Else?) : GStatement() {
    data class Case(val condition: GExpression, val body: List<GStatement>)
    data class Else(val body: List<GStatement>)
}

data class GForStmt(val variable: GTarget,
                    val minValue: GExpression,
                    val maxValue: GExpression, val body: List<GStatement>) : GStatement()
