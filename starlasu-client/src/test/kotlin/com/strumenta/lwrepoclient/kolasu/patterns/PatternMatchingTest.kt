package com.strumenta.lwrepoclient.kolasu.patterns

import com.strumenta.javalangmodule.ast.JClassDeclaration
import com.strumenta.javalangmodule.ast.JCompilationUnit
import com.strumenta.javalangmodule.ast.JFieldDecl
import com.strumenta.javalangmodule.ast.JIntType
import com.strumenta.javalangmodule.ast.JVariableDeclarator
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import kotlin.test.Test
import kotlin.test.assertEquals

class PatternMatchingTest {

    @Test
    fun matchExactSameJavaAST() {
        val ast1 = JavaKolasuParser().parse("""class A {}""").root!!
        val ast2 = JavaKolasuParser().parse("""class B {}""").root!!
        val pattern = Pattern(JCompilationUnit(declarations = mutableListOf(
            JClassDeclaration("A")
        )))
        assertEquals(true, ast1.canMatchPattern(pattern))
        assertEquals(false, ast2.canMatchPattern(pattern))
    }

    @Test
    fun matchJavaASTWithPlaceholder() {
        val ast1 = JavaKolasuParser().parse("""class A {}""").root!!
        val ast2 = JavaKolasuParser().parse("""class B {}""").root!!
        val pattern = Pattern(JCompilationUnit(declarations = mutableListOf(
            JClassDeclaration("X")
        ))).apply {
            withVariable("X")
        }
        assertEquals(true, ast1.canMatchPattern(pattern))
        assertEquals(true, ast2.canMatchPattern(pattern))
        assertEquals("A", ast1.matchPattern(pattern)!!.valueFor("X"))
        assertEquals("B", ast2.matchPattern(pattern)!!.valueFor("X"))
    }

    @Test
    fun matchJavaASTWithMatchingPlaceholder() {
        val ast1 = JavaKolasuParser().parse("""class A { int A;}""").root!!
        val ast2 = JavaKolasuParser().parse("""class B { int B;}""").root!!
        val ast3 = JavaKolasuParser().parse("""class C { int A;}""").root!!
        val pattern = Pattern(JCompilationUnit(declarations = mutableListOf(
            JClassDeclaration("X", members = mutableListOf(
                JFieldDecl(type = JIntType(), declarators = mutableListOf(JVariableDeclarator("X")))
            ))
        ))).apply {
            withVariable("X")
        }
        assertEquals(true, ast1.canMatchPattern(pattern))
        assertEquals(true, ast2.canMatchPattern(pattern))
        assertEquals(false, ast3.canMatchPattern(pattern))
        assertEquals("A", ast1.matchPattern(pattern)!!.valueFor("X"))
        assertEquals("B", ast2.matchPattern(pattern)!!.valueFor("X"))
    }
}