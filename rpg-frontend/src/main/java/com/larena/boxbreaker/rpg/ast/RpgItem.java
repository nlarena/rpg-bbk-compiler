package com.larena.boxbreaker.rpg.ast;

/**
 * A top-level program element. The RPG free-form grammar interleaves
 * declarations and statements in module bodies and procedure bodies
 * ({@code { <declaration> | <statement> }*}), so both are {@code RpgItem}s.
 *
 * <p>Sealed across {@link RpgDeclaration} and {@link RpgStatement} so the
 * emitter can {@code switch} over every item kind exhaustively.
 */
public sealed interface RpgItem permits RpgDeclaration, RpgStatement {}
