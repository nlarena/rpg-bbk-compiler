package com.larena.boxbreaker.core.backend.jvm;

import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.parser.ParsedProgram;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.ASM9;

/** El backend JVM emite info de debug BBK (LineNumberTable + SourceFile) — base del debug por bytecode. */
public class JvmDebugInfoTest {

    @Test
    public void emitsBbkLineNumbers() {
        String src = "DCL-S x INT(10) INZ(0);\nx = 5;\nprint(char(x));\n";
        ParsedProgram parsed = BbkParser.parseWithPositions(src);
        byte[] bytes = JvmCompiler.compile(parsed.program(), parsed.positions());

        Set<Integer> lines = new TreeSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                return new MethodVisitor(ASM9) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        lines.add(line);
                    }
                };
            }
        }, 0);

        // línea 2 (x = 5) y línea 3 (print) deben quedar en el LineNumberTable
        assertTrue("debería emitir números de línea BBK: " + lines, lines.contains(2) && lines.contains(3));
    }

    @Test
    public void emitsSourceFileAttribute() {
        byte[] bytes = JvmCompiler.compile(BbkParser.parse("DCL-S x INT(10) INZ(0);\nx = 5;\n"));

        AtomicReference<String> source = new AtomicReference<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM9) {
            @Override
            public void visitSource(String src, String debug) {
                source.set(src);
            }
        }, 0);

        // el atributo SourceFile permite que el debugger sepa de qué .bbk viene la clase
        assertEquals("Main.bbk", source.get());
    }

    @Test
    public void emitsLocalVariableTable() {
        String src = "DCL-PROC add(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) {\n"
            + "  DCL-S sum INT(10);\n"
            + "  sum = a + b;\n"
            + "  return sum;\n"
            + "}\n"
            + "print(char(add(5, 7)));\n";
        ParsedProgram parsed = BbkParser.parseWithPositions(src);
        byte[] bytes = JvmCompiler.compile(parsed.program(), parsed.positions());

        Set<String> locals = new TreeSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                return new MethodVisitor(ASM9) {
                    @Override
                    public void visitLocalVariable(String n, String d, String s, Label st, Label en, int index) {
                        locals.add(n);
                    }
                };
            }
        }, 0);

        // los parámetros y el local del proc 'add' quedan nombrados en el LocalVariableTable
        assertTrue("debería nombrar los locales BBK: " + locals,
            locals.contains("a") && locals.contains("b") && locals.contains("sum"));
    }
}
