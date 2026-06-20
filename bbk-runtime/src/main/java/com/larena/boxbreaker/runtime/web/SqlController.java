package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.sql.SqlConsoleService;
import com.larena.boxbreaker.runtime.sql.SqlConsoleService.SqlResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consola SQL interactiva (STRSQL): ejecuta una sentencia contra la base. Sólo
 * exige estar autenticado (la sentencia corre con la conexión del runtime).
 */
@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private final SqlConsoleService sql;

    public SqlController(SqlConsoleService sql) {
        this.sql = sql;
    }

    @PostMapping
    public SqlResult run(@RequestBody SqlRequest request) {
        return sql.run(request.sql());
    }

    public record SqlRequest(String sql) {}
}
