package jay.yydebug;

public class Debug implements yyDebug {

    @Override
    public void push(int state, Object value) {
        System.out.println("push: " + state + " " + value);
    }

    @Override
    public void lex(int state, int token, String name, Object value) {
        System.out.println("push: " + state + " " + value);
    }

    @Override
    public void shift(int from, int to, int errorFlag) {
        System.out.println("shift from " + from + " to " + to + " error " + errorFlag);
    }

    @Override
    public void pop(int state) {
        System.out.println("pop: " + state);
    }

    @Override
    public void reject() {
        System.out.println("reject");
    }

    @Override
    public void discard(int state, int token, String name, Object value) {
        System.out.println("discard: state " + state + " token " + token + " name " + name + " value " + value);
    }

    @Override
    public void reduce(int from, int to, int rule, String text, int len) {
        System.out.println("reduce: from " + from + " to " + to + " rule " + rule + " text " + text + " len " + len);
    }

    @Override
    public void shift(int from, int to) {
        System.out.println("shift from " + from + " to " + to);
    }

    @Override
    public void accept(Object value) {
        System.out.println("accept " + value);
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }
}
