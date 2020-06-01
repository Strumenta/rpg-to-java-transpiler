package com.strumenta.rpgtojava.intermediateast

import com.strumenta.kolasu.model.Link
import com.strumenta.kolasu.model.Node

abstract class GExpression : Node()

data class GIntegerLiteral(val value: Long) : GExpression()
data class GDecimalLiteral(val value: Double) : GExpression()
data class GStringLiteral(val value: String) : GExpression()
data class GStringToIntExpr(val string: GExpression) : GExpression()
data class GGlobalVariableRef(
        @Link
        val globalVariable: GGlobalVariable) : GExpression()

data class GStringConcatExpr(val left: GExpression, val right: GExpression) : GExpression()
data class GSumExpr(val left: GExpression, val right: GExpression) : GExpression()

data class GToStringExpr(val value: GExpression) : GExpression()

data class GEqualityExpr(val left: GExpression, val right: GExpression) : GExpression()