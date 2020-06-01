package com.strumenta.rpgtojava.transformations

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.type.PrimitiveType
import com.strumenta.rpgtojava.intermediateast.*

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