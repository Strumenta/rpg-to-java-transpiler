package com.strumenta.rpgtojava.transformations

import com.smeup.rpgparser.interpreter.*
import com.smeup.rpgparser.parsing.ast.*
import com.strumenta.rpgtojava.intermediateast.*

private class RpgToIntermediateContext {
    private val dataCorrespondences = HashMap<AbstractDataDefinition, GGlobalVariable>()
    private val functionCorrespondences = HashMap<Subroutine, GFunction>()

    fun registerCorrespondence(dataDefinition: AbstractDataDefinition, gGlobalVariable: GGlobalVariable) {
        dataCorrespondences[dataDefinition] = gGlobalVariable
    }

    fun registerCorrespondence(subroutine: Subroutine, gFunction: GFunction) {
        functionCorrespondences[subroutine] = gFunction
    }

    fun toGGlobalVariable(dataDefinition: AbstractDataDefinition): GGlobalVariable {
        return dataCorrespondences[dataDefinition] ?: throw RuntimeException("No mapping for ${dataDefinition.name}")
    }

    fun toGFunction(subroutine: Subroutine): GFunction {
        return functionCorrespondences[subroutine] ?: throw RuntimeException("No mapping for ${subroutine.name}")
    }
}

fun transformFromRPGtoIntermediate(rpgCu: CompilationUnit, name: String) : GProgram {
    val ctx = RpgToIntermediateContext()
    val program = GProgram(name)
    // Translate data declarations
    rpgCu.allDataDefinitions.forEach {
        val gv = GGlobalVariable(it.name, it.type.toGType())
        if (it is DataDefinition) {
            if (it.initializationValue != null) {
                gv.initialValue = (it.initializationValue as Expression).toGExpression(ctx)
            }
        }
        program.globalVariables.add(gv)
        ctx.registerCorrespondence(it, gv)
    }
    // Create the mapping for the functions, as they could be called
    rpgCu.subroutines.forEach {
        val gf = GFunction(it.name)
        program.otherFunctions.add(gf)
        ctx.registerCorrespondence(it, gf)
    }
    // Translate main function
    rpgCu.entryPlist?.params?.forEach {
        program.mainFunction.parameters.add(GFunction.Parameter(it.param.referred!!.name, it.param.referred!!.type.toGType()))
    }
    program.mainFunction.body.addAll(rpgCu.main.stmts.toGStatements(ctx))
    // Translate subroutines
    rpgCu.subroutines.forEach {
        val gf = ctx.toGFunction(it)
        gf.body.addAll(it.stmts.toGStatements(ctx))
    }
    return program
}

private fun List<Statement>.toGStatements(ctx: RpgToIntermediateContext) = this.map { it.toGStatements(ctx) }.flatten()

private fun Statement.toGStatements(ctx: RpgToIntermediateContext) : List<GStatement> {
    return when (this) {
        is PlistStmt -> emptyList() // we can ignore this
        is EvalStmt -> listOf(GAssignment(this.target.toGTarget(ctx), this.expression.toGExpression(ctx)))
        is ExecuteSubroutine -> listOf(GExecuteFunction(ctx.toGFunction(this.subroutine.referred ?: throw RuntimeException("Unresolved subroutine"))))
        is ClearStmt -> listOf(GResetStringStmt(this.value.toGTarget(ctx)))
        is DisplayStmt -> listOf(GPrintStmt(this.response!!.toGExpression(ctx)))
        is SetStmt -> emptyList() // we can ignore this
        is SelectStmt -> {
            var elseCase : GSwitchStmt.Else? = null
            val cases = this.cases.map {
                GSwitchStmt.Case(it.condition.toGExpression(ctx), it.body.toGStatements(ctx))
            }

            if (this.other != null) {
                elseCase = GSwitchStmt.Else(this.other!!.body.toGStatements(ctx))
            }

            listOf(GSwitchStmt(cases, elseCase))
        }
        is ForStmt -> {
            val target = (this.init as AssignmentExpr).target.toGTarget(ctx)
            val initValue = (this.init as AssignmentExpr).value.toGExpression(ctx)
            listOf(GForStmt(target, initValue, this.endValue.toGExpression(ctx), this.body.toGStatements(ctx)))
        }
        else -> TODO("Not yet implemented $this")
    }
}

private fun Expression.toGTarget(ctx: RpgToIntermediateContext): GTarget {
    return when (this) {
        is DataRefExpr -> {
            GGlobalVariableTarget(ctx.toGGlobalVariable(this.variable.referred ?: throw RuntimeException("Unresolved variable")))
        }
        else -> TODO("Not yet implemented $this")
    }
}

private fun Expression.toGExpression(ctx: RpgToIntermediateContext): GExpression {
    return when (this) {
        is IntLiteral -> GIntegerLiteral(this.value)
        is RealLiteral -> GDecimalLiteral(this.value.toDouble())
        is StringLiteral -> GStringLiteral(this.value)
        is DecExpr -> {
            // FIXME We assume that decimal digits are 0
            //       in reality we should use the interpreter to evaluate that
            GStringToIntExpr(this.value.toGExpression(ctx))
        }
        is DataRefExpr -> {
            GGlobalVariableRef(ctx.toGGlobalVariable(this.variable.referred ?: throw RuntimeException("Unresolved variable")))
        }
        is PlusExpr -> {
            if (this.left.wrappedType() is StringType) {
                GStringConcatExpr(this.left.toGExpression(ctx), this.right.toGExpression(ctx))
            } else {
                GSumExpr(this.left.toGExpression(ctx), this.right.toGExpression(ctx))
            }
        }
        is CharExpr -> {
            GToStringExpr(this.value.toGExpression(ctx))
        }
        is EqualityExpr -> {
            GEqualityExpr(this.left.toGExpression(ctx), this.right.toGExpression(ctx))
        }
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

private fun Expression.wrappedType() : Type {
    return try {
        this.type()
    } catch (e: NotImplementedError) {
        StringType(-1, true)
    }
}