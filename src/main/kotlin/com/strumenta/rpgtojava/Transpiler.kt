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
import kotlin.system.exitProcess

fun transform(rpgCu: CompilationUnit, name: String) : com.github.javaparser.ast.CompilationUnit {
    val gProgram = transformFromRPGtoIntermediate(rpgCu, name)
    val javaAst = transformFromIntermediateToJava(gProgram)
    return javaAst
}

fun generate(javaAst: com.github.javaparser.ast.CompilationUnit) : String {
    return javaAst.toString(PrettyPrinterConfiguration())
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Exactly one argument expected");
        exitProcess(1)
    }
    val inputFile = File(args[0])
    if (!inputFile.isFile || !inputFile.exists()) {
        System.err.println("Path specified does not exist or it is not a file: $inputFile");
        exitProcess(1)
    }
    val facade = RpgParserFacade()
    val rpgCu = facade.parseAndProduceAst(FileInputStream(inputFile))
    rpgCu.resolveAndValidate(DummyDBInterface)
    val javaAst = transform(rpgCu, inputFile.nameWithoutExtension)
    val transpilationRes = generate(javaAst)
    println(transpilationRes)
}