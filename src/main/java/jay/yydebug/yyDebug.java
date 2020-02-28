package jay.yydebug;

public interface yyDebug {
    /** just pushed the state/value stack.
     @param state current state.
     @param value current value.
     */
    void push (int state, Object value);
    /** just called the scanner.
     @param state current state.
     @param token just obtained from scanner.
     @param name of token in grammar.
     @param value will be obtained from scanner.
     */
    void lex (int state, int token, String name, Object value);
    /** moving to a new state because of input or error.
     @param from current state.
     @param to next state.
     @param errorFlag value in next state.
     */
    void shift (int from, int to, int errorFlag);
    /** discarding a state during error recovery.
     @param state discarded.
     */
    void pop (int state);
    /** error recovery failed, about to throw <tt>yyException</tt>.
     */
    void reject ();
    /** discarding token during error recovery.
     @param state current state.
     @param token discarded.
     @param name of token in grammar.
     @param value will be obtained from scanner.
     */
    void discard (int state, int token, String name, Object value);
    /** rule completed, calling action.
     @param from current state.
     @param to state to be uncovered.
     @param rule number of completed right-hand side.
     @param text text of rule.
     @param len number of symbols in rule.
     */
    void reduce (int from, int to, int rule, String text, int len);
    /** moving to a new state following an action.
     @param from current state.
     @param to next state.
     */
    void shift (int from, int to);
    /** parse is successful.
     @param value to be returned by <tt>yyparse()</tt>.
     */
    void accept (Object value);
    /** syntax error.
     */
    void error (String message);
}
