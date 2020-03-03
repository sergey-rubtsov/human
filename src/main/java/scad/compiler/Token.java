package scad.compiler;

public class Token {

    private int token;

    private Object value;

    public Token(int token, Object value) {
        this.token = token;
        this.value = value;
    }

    public int getToken() {
        return token;
    }

    public Object getValue() {
        return value;
    }
}
