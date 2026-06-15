package com.larena.boxbreaker.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the BoxBreaker runtime — a standalone Spring Boot service that
 * emulates the IBM&nbsp;i system surface (jobs, library lists, record-level data
 * access, spool, program calls). Compiled or interpreted BBK programs call it
 * over HTTP/REST; the hot per-value arithmetic (BCD, strings) stays local in the
 * generated code / bbk-core and never crosses the wire.
 */
@SpringBootApplication
public class BbkRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BbkRuntimeApplication.class, args);
    }
}
