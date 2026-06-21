package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;

import java.util.List;

/**
 * Un frame de la pila: dónde está parado (posición BBK) y, <b>en forma perezosa</b>,
 * sus variables. La lectura de variables se hace al llamar {@link #variables()}, no al
 * pausar: hay que invocarla con la VM suspendida y <b>fuera del hilo del event loop</b>
 * (leer un valor puede invocar métodos en la VM remota, p.ej. para formatear decimales,
 * y hacerlo en el hilo de eventos cuelga la sesión).
 */
public final class BbkFrame {

    private final BbkPosition position;
    private final ThreadReference thread;
    private final int index;

    BbkFrame(BbkPosition position, ThreadReference thread, int index) {
        this.position = position;
        this.thread = thread;
        this.index = index;
    }

    public BbkPosition position() {
        return position;
    }

    /** Lee las variables del frame al momento de llamar (VM suspendida; no en el hilo de eventos). */
    public List<BbkVariable> variables() {
        try {
            return BbkVariables.read(thread, thread.frame(index));
        } catch (IncompatibleThreadStateException e) {
            return List.of();
        }
    }
}
