package com.strumenta.rpgtojava

import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.smeup.rpgparser.interpreter.*
import com.smeup.rpgparser.parsing.ast.*
import com.smeup.rpgparser.parsing.facade.RpgParserFacade
import com.smeup.rpgparser.parsing.parsetreetoast.resolveAndValidate
import com.strumenta.rpgtojava.intermediateast.GProgram
import com.strumenta.rpgtojava.transformations.transformFromIntermediateToJava
import com.strumenta.rpgtojava.transformations.transformFromRPGtoIntermediate
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.system.exitProcess

fun transform(rpgAst: CompilationUnit, name: String) : com.github.javaparser.ast.CompilationUnit {
    val intermediateAst = transformFromRPGtoIntermediate(rpgAst, name)
    val javaAst = transformFromIntermediateToJava(intermediateAst)
    return javaAst
}

fun generate(javaAst: com.github.javaparser.ast.CompilationUnit) : String {
    return javaAst.toString(PrettyPrinterConfiguration())
}

fun parseRpgCode(source: InputStream) : CompilationUnit {
    val facade = RpgParserFacade()
    facade.muteSupport = false
    val rpgAst = facade.parseAndProduceAst(source)
    rpgAst.resolveAndValidate(DummyDBInterface)
    return rpgAst
}

fun transformRpgToIntermediate(source: InputStream, name: String) : GProgram {
    val rpgAst = parseRpgCode(source)
    return transformFromRPGtoIntermediate(rpgAst, name)
}

fun transpileRpgToJava(source: InputStream, name: String) : String {
    val rpgAst = parseRpgCode(source)
    val javaAst = transform(rpgAst, name)
    return generate(javaAst)
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
    val transpilationRes = transpileRpgToJava(FileInputStream(inputFile), inputFile.nameWithoutExtension)
    println(transpilationRes)
}
