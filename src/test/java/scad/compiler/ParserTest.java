package scad.compiler;

import jay.yydebug.Debug;
import jay.yydebug.yyDebug;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class ParserTest {

    @Test
    public void yyparse() throws IOException, Parser.yyException {
        InputStream is = getClass().getResourceAsStream("/openscad/support");
        InputStreamReader isr = new InputStreamReader(is);
        new BufferedReader(isr);
        Parser p = new Parser();
        p.yyparse(new Lexer(new BufferedReader(isr)));
        //yyDebug debugger = new Debug();
        //p.yyparse(new Lexer(new BufferedReader(isr)), debugger);
    }
}
