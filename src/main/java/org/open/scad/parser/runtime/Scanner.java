package org.open.scad.parser.runtime;

public interface Scanner {
    /** Return the next token, or <code>null</code> on end-of-file. */
    public Symbol next_token() throws java.lang.Exception;
}
