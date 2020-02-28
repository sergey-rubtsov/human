package org.open.scad.compiler;

public class Token {

    private int token;

    private String value;

    public Token(int token, String value) {
        this.token = token;
        this.value = value;
    }

    public int getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }
}
