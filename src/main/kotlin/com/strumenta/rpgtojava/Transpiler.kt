package com.strumenta.rpgtojava

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.smeup.rpgparser.interpreter.*
import com.smeup.rpgparser.parsing.ast.*
import com.smeup.rpgparser.parsing.ast.Expression
import com.smeup.rpgparser.parsing.facade.RpgParserFacade
import com.smeup.rpgparser.parsing.parsetreetoast.resolveAndValidate
import com.strumenta.rpgtojava.intermediateast.*
import java.io.File
import java.io.FileInputStream

class RpgToIntermediateContext {
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
    program.mainFunction.body.addAll(rpgCu.main.stmts.map { it.toGStatements(ctx) }.flatten())
    rpgCu.subroutines.forEach {
        val gf = ctx.toGFunction(it)
        gf.body.addAll(it.stmts.map { it.toGStatements(ctx) }.flatten())
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
                elseCase = GSwitchStmt.Else(this.other!!.body.map { it.toGStatements(ctx) }.flatten())
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

private fun Expression.wrappedType() : Type {
    return try {
        this.type()
    } catch (e: NotImplementedError) {
        StringType(-1, true)
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
        val javaField = javaClass.addField(it.type.toJavaType(), it.name, Modifier.Keyword.PRIVATE)
        if (it.initialValue != null) {
            javaField.getVariable(0).setInitializer(it.initialValue!!.toJavaExpression())
        }
    }
    val mainMethod = javaClass.addMethod("executeProgram");
    intermediateAst.mainFunction.parameters.forEach {
        mainMethod.addParameter(it.type.toJavaType(), it.name)
        mainMethod.body.get().addStatement(JavaParser().parseStatement("this.${it.name} = ${it.name};").result.get())
    }
    intermediateAst.mainFunction.body.forEach {
        mainMethod.body.get().addStatement(it.toJavaStatement())
    }

    intermediateAst.otherFunctions.forEach {
        val javaMethod = javaClass.addMethod(it.name)
        it.body.forEach {
            javaMethod.body.get().addStatement(it.toJavaStatement())
        }
    }

    return javaCu;
}

private fun GStatement.toJavaStatement(): com.github.javaparser.ast.stmt.Statement {
    return when (this) {
        is GAssignment -> ExpressionStmt(
                AssignExpr(
                    this.target.toJavaExpression(),
                    this.value.toJavaExpression(),
                    AssignExpr.Operator.ASSIGN
                )
        )
        is GExecuteFunction -> ExpressionStmt(
                MethodCallExpr(this.function.name)
        )
        is GResetStringStmt -> {
            ExpressionStmt(AssignExpr(
                    this.target.toJavaExpression(),
                    StringLiteralExpr(""),
                    AssignExpr.Operator.ASSIGN
            ))
        }
        is GPrintStmt -> {
            val expr = JavaParser().parseExpression<MethodCallExpr>("java.lang.System.out.println()").result.get();
            expr.addArgument(this.value.toJavaExpression())
            ExpressionStmt(expr)
        }
        is GSwitchStmt -> {
            var top : com.github.javaparser.ast.stmt.Statement? = null
            var current : IfStmt? = null
            this.cases.forEach {
                val newIf = IfStmt()
                newIf!!.condition = it.condition.toJavaExpression()
                val block = BlockStmt()
                it.body.map { it.toJavaStatement() }.forEach { block.addStatement(it) }
                newIf!!.thenStmt = block
                if (current == null) {
                    current = newIf
                    top = newIf
                } else {
                    current!!.setElseStmt(newIf)
                    current = newIf
                }
            }
            if (this.elseCase != null) {
                val block = BlockStmt()
                this.elseCase.body.map { it.toJavaStatement() }.forEach { block.addStatement(it) }
                if (current == null) {
                    top = block
                } else {
                    current!!.setElseStmt(block)
                }
            }
            current!!
        }
        is GForStmt -> {
            val forStmt = com.github.javaparser.ast.stmt.ForStmt()
            forStmt.initialization = NodeList(AssignExpr(this.variable.toJavaExpression(), this.minValue.toJavaExpression(), AssignExpr.Operator.ASSIGN))
            forStmt.update = NodeList(UnaryExpr(this.variable.toJavaExpression(), UnaryExpr.Operator.POSTFIX_INCREMENT))
            forStmt.setCompare(BinaryExpr(this.variable.toJavaExpression(), this.maxValue.toJavaExpression(), BinaryExpr.Operator.LESS_EQUALS))
            val block = BlockStmt()
            this.body.map { it.toJavaStatement() }.forEach { block.addStatement(it) }
            forStmt.body = block
            forStmt
        }
        else -> TODO("Not yet implemented $this")
    }
}

private fun GTarget.toJavaExpression(): com.github.javaparser.ast.expr.Expression {
    return when (this) {
        is GGlobalVariableTarget -> JavaParser().parseExpression<com.github.javaparser.ast.expr.Expression>("this.${this.globalVariable.name}").result.get()
        else -> TODO("Not yet implemented $this")
    }
}

private fun GExpression.toJavaExpression(): com.github.javaparser.ast.expr.Expression {
    return when (this) {
        is GIntegerLiteral -> IntegerLiteralExpr(this.value.toString())
        is GDecimalLiteral -> DoubleLiteralExpr(this.value.toString())
        is GStringLiteral -> StringLiteralExpr(this.value)
        is GStringToIntExpr -> {
            val call = JavaParser().parseExpression<com.github.javaparser.ast.expr.MethodCallExpr>("java.lang.Integer.valueOf()").result.get()
            call.addArgument(this.string.toJavaExpression())
            call
        }
        is GGlobalVariableRef -> {
            JavaParser().parseExpression<com.github.javaparser.ast.expr.Expression>("this.${this.globalVariable.name}").result.get()
        }
        is GStringConcatExpr -> {
            BinaryExpr(this.left.toJavaExpression(), this.right.toJavaExpression(), BinaryExpr.Operator.PLUS)
        }
        is GToStringExpr -> {
            BinaryExpr(StringLiteralExpr(""), this.value.toJavaExpression(), BinaryExpr.Operator.PLUS)
        }
        is GEqualityExpr -> {
            BinaryExpr(this.left.toJavaExpression(), this.right.toJavaExpression(), BinaryExpr.Operator.EQUALS)
        }
        is GSumExpr -> {
            BinaryExpr(this.left.toJavaExpression(), this.right.toJavaExpression(), BinaryExpr.Operator.PLUS)
        }
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
    rpgCu.resolveAndValidate(DummyDBInterface)
    val javaAst = transform(rpgCu, "CalcFib")
    val transpilationRes = generate(javaAst)
    println(transpilationRes)
}