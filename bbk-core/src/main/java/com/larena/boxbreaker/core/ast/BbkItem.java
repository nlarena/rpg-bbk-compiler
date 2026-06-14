package com.larena.boxbreaker.core.ast;

/**
 * A top-level / block element of a BBK program. BBK interleaves declarations
 * and statements in blocks, so both are {@code BbkItem}s. Sealed so the
 * compiler back-ends can {@code switch} over every item kind exhaustively.
 */
public sealed interface BbkItem permits BbkDeclaration, BbkStatement {}
