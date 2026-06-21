package com.larena.boxbreaker.jvmdebug;

import com.larena.boxbreaker.core.backend.jvm.JvmCompiler;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.request.ClassPrepareRequest;

import java.io.IOException;
import java.util.Map;

/**
 * Conexión JDI con el debuggee: se engancha al socket JDWP de un {@link BbkDebuggee}
 * y desde ahí maneja la sesión de debug (por ahora: pedir el {@code ClassPrepare} de
 * {@code bbk.Main}; más adelante breakpoints, frames y step).
 *
 * <p>Usa el {@code SocketAttach} estándar de {@code com.sun.jdi} &mdash; el mismo
 * cliente JDI que usa el debugger de Java de IntelliJ.
 */
public final class BbkDebugSession implements AutoCloseable {

    private final VirtualMachine vm;

    private BbkDebugSession(VirtualMachine vm) {
        this.vm = vm;
    }

    /** Se conecta por JDI al puerto JDWP del debuggee (host:port que publicó {@link BbkDebuggee}). */
    public static BbkDebugSession attach(String host, int port) throws IOException {
        AttachingConnector connector = socketAttachConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("hostname").setValue(host);
        args.get("port").setValue(Integer.toString(port));
        try {
            return new BbkDebugSession(connector.attach(args));
        } catch (IllegalConnectorArgumentsException e) {
            throw new IOException("argumentos inválidos para el conector JDI: " + e.getMessage(), e);
        }
    }

    private static AttachingConnector socketAttachConnector() {
        for (AttachingConnector c : Bootstrap.virtualMachineManager().attachingConnectors()) {
            if ("com.sun.jdi.SocketAttach".equals(c.name())) return c;
        }
        throw new IllegalStateException("no se encontró el conector JDI SocketAttach (¿falta el módulo jdk.jdi?)");
    }

    /** La VM remota. */
    public VirtualMachine vm() {
        return vm;
    }

    /**
     * Pide notificación cuando se prepara {@code bbk.Main} y deja el request habilitado.
     * Hay que pedirlo <b>antes</b> de reanudar el arranque, porque la clase se prepara al
     * entrar a {@code main}.
     */
    public ClassPrepareRequest requestMainClassPrepare() {
        ClassPrepareRequest req = vm.eventRequestManager().createClassPrepareRequest();
        req.addClassFilter(JvmCompiler.CLASS_NAME);     // "bbk.Main"
        req.enable();
        return req;
    }

    /** Reanuda la VM (arrancó suspendida por {@code suspend=y}). */
    public void resume() {
        vm.resume();
    }

    @Override
    public void close() {
        try {
            vm.dispose();
        } catch (Exception ignored) {
            // la VM puede haber muerto ya; el proceso del debuggee se mata por su cuenta
        }
    }
}
