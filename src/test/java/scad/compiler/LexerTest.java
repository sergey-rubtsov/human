package scad.compiler;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class LexerTest {

    @Test
    public void complete() throws IOException {
        InputStream is1 = getClass().getResourceAsStream("/openscad/support");
        InputStreamReader is = new InputStreamReader(is1);
        Lexer lexer = new Lexer(is);
        while(lexer.advance()) {
            System.out.print(" " + lexer.token() + " " + lexer.tokenName());
            System.out.println(" " + lexer.value());
        }
    }

    @Test
    public void token() {
    }

    @Test
    public void value() {
    }

    @Test
    public void advance() {
    }
}
