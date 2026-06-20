package com.larena.boxbreaker.runtime.sql;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Consola SQL interactiva (análoga a STRSQL del IBM&nbsp;i): ejecuta una sentencia
 * <b>directamente contra la base</b> y devuelve, según el caso, un conjunto de
 * resultados (SELECT/SHOW/DESCRIBE) o la cantidad de filas afectadas
 * (INSERT/UPDATE/DELETE/DDL). Es una herramienta de poder: corre cualquier
 * sentencia que la sesión autenticada quiera ejecutar.
 */
@Service
public class SqlConsoleService {

    /** Tope de filas que se devuelven (para no traer result sets gigantes). */
    public static final int MAX_ROWS = 500;

    private final JdbcTemplate jdbc;

    public SqlConsoleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Resultado de una sentencia: o un result set (RESULT) o un conteo (UPDATE). */
    public record SqlResult(String kind, List<String> columns, List<List<String>> rows,
                            Integer updateCount, boolean truncated) {

        static SqlResult ofResultSet(List<String> columns, List<List<String>> rows, boolean truncated) {
            return new SqlResult("RESULT", columns, rows, null, truncated);
        }

        static SqlResult ofUpdate(int count) {
            return new SqlResult("UPDATE", null, null, count, false);
        }
    }

    public SqlResult run(String rawSql) {
        String sql = rawSql == null ? "" : rawSql.trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
        if (sql.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "escribí una sentencia SQL");
        }

        final String statement = sql;
        try {
            return jdbc.execute((StatementCallback<SqlResult>) stmt -> {
                boolean hasResultSet = stmt.execute(statement);
                if (!hasResultSet) {
                    return SqlResult.ofUpdate(stmt.getUpdateCount());
                }
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();

                    List<String> columns = new ArrayList<>(cols);
                    for (int i = 1; i <= cols; i++) columns.add(md.getColumnLabel(i));

                    List<List<String>> rows = new ArrayList<>();
                    boolean truncated = false;
                    while (rs.next()) {
                        if (rows.size() >= MAX_ROWS) { truncated = true; break; }
                        List<String> row = new ArrayList<>(cols);
                        for (int i = 1; i <= cols; i++) {
                            Object v = rs.getObject(i);
                            row.add(v == null ? null : String.valueOf(v));
                        }
                        rows.add(row);
                    }
                    return SqlResult.ofResultSet(columns, rows, truncated);
                }
            });
        } catch (DataAccessException e) {
            // error de SQL (sintaxis, tabla inexistente, etc.): mostrar el motivo real
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        String msg = cur.getMessage();
        return (msg == null || msg.isBlank()) ? cur.getClass().getSimpleName() : msg;
    }
}
