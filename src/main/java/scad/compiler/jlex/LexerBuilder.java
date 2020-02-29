package scad.compiler.jlex;

public class LexerBuilder {

    public static void main(String[] arg) throws java.io.IOException {
        try {
            new CLexGen("src/main/java/scad/compiler/",
                    "lexer")
                    .generate();
        } catch (Error e) {
            System.out.println(e.getMessage());
        }
    }
}
