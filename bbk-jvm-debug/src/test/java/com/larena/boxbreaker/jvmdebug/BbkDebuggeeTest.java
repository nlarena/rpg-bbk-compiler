package com.larena.boxbreaker.jvmdebug;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Forkea una JVM con el agente JDWP suspendido y verifica que queda escuchando para JDI. */
public class BbkDebuggeeTest {

    @Test
    public void forksJvmListeningForJdwp() throws Exception {
        BbkDebuggee debuggee = BbkDebuggee.launch("print(char(12));\n");
        Path root = debuggee.classpathRoot();
        try {
            assertTrue("debería anunciar un puerto JDWP válido: " + debuggee.port(), debuggee.port() > 0);
            assertTrue("la JVM de debug debería quedar viva (suspendida)", debuggee.process().isAlive());

            // el agente JDWP acepta conexiones TCP en ese puerto: prueba de que de verdad escucha
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(debuggee.host(), debuggee.port()), 5000);
                assertTrue("debería poder conectarse al puerto JDWP", socket.isConnected());
            }
        } finally {
            debuggee.close();
        }

        assertFalse("close() debería matar el proceso", debuggee.process().isAlive());
        assertFalse("close() debería borrar el dir temporal", Files.exists(root));
    }
}
