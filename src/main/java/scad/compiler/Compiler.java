package scad.compiler;

import java.io.IOException;

public class Compiler {

    public static void main(String[] args) throws IOException, Parser.yyException {
        Parser p = new Parser();
        //p.yyparse(new Lexer(System.in));
    }

}
