package org.open.scad.compiler.jlex;

public class LexerBuilder {

    public static void main(String[] arg) throws java.io.IOException {
        try {
            new CLexGen("src\\main\\java\\org\\open\\scad\\compiler\\",
                    "lexer")
                    .generate();
        } catch (Error e) {
            System.out.println(e.getMessage());
        }
    }
}
