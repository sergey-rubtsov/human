package org.open.scad.lexer;

public class LexerBuilder {

    public static void main(String[] arg) throws java.io.IOException {
        CLexGen lg;
        //"parser/openscad/lexer.lex"
        try {
            lg = new CLexGen("src\\main\\java\\org\\open\\scad\\lexer\\", "lexer");
            lg.generate();
        } catch (Error e) {
            System.out.println(e.getMessage());
        }
    }
}
