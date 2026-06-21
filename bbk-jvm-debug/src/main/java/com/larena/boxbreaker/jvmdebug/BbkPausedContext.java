package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;

import java.util.ArrayList;
import java.util.List;

/**
 * El estado del programa cuando frena: la pila de frames BBK del thread suspendido.
 * Se lee mientras la VM está suspendida (entre {@code onPaused} y el próximo
 * resume/step); leer después de reanudar devuelve vacío.
 */
public final class BbkPausedContext {

    private final ThreadReference thread;

    BbkPausedContext(ThreadReference thread) {
        this.thread = thread;
    }

    /** Posición donde frenó (frame tope), o {@code null} si el thread ya se reanudó. */
    public BbkPosition position() {
        try {
            return BbkLineMap.positionOf(thread.frame(0).location());
        } catch (IncompatibleThreadStateException e) {
            return null;
        }
    }

    /** La pila de llamadas, del frame actual (arriba) hacia el mainline (abajo). Solo posiciones (barato); las variables se leen perezosas. */
    public List<BbkFrame> frames() {
        List<BbkFrame> out = new ArrayList<>();
        try {
            List<StackFrame> frames = thread.frames();
            for (int i = 0; i < frames.size(); i++) {
                out.add(new BbkFrame(BbkLineMap.positionOf(frames.get(i).location()), thread, i));
            }
        } catch (IncompatibleThreadStateException e) {
            // el thread se reanudó mientras leíamos
        }
        return out;
    }

    /** Atajo: las variables del frame actual (lo que muestra el panel al frenar). */
    public List<BbkVariable> variables() {
        List<BbkFrame> frames = frames();
        return frames.isEmpty() ? List.of() : frames.get(0).variables();
    }

    /** Evalúa una expresión BBK en el frame actual y devuelve el valor formateado (Evaluate/watches). */
    public String evaluate(String expression) {
        return evaluator().evaluateText(expression);
    }

    /** Evalúa una expresión como condición booleana en el frame actual. */
    public boolean evaluateCondition(String expression) {
        return evaluator().evaluateCondition(expression);
    }

    private BbkExprEval evaluator() {
        try {
            return BbkExprEval.forFrame(thread, thread.frame(0));
        } catch (IncompatibleThreadStateException e) {
            throw new BbkExprEval.EvalException("el programa no está suspendido");
        }
    }
}
