package org.open.scad.compiler;
import static org.open.scad.compiler.Parser.*;


public class Lexer implements Parser.yyInput {
	private final int YY_BUFFER_SIZE = 512;
	private final int YY_F = -1;
	private final int YY_NO_STATE = -1;
	private final int YY_NOT_ACCEPT = 0;
	private final int YY_START = 1;
	private final int YY_END = 2;
	private final int YY_NO_ANCHOR = 4;
	private final int YY_BOL = 128;
	private final int YY_EOF = 129;

  public boolean advance() throws java.io.IOException {
      Token t = yylex();
      if (t == null) {
          return false;
      }
      value = t.getValue();
      token = t.getToken();
      return (token != YY_EOF);
  }
  /**
   * returned by {@link #token()}.
   */
  protected int token;
  /**
   * returned by {@link #value()}.
   */
  protected Object value;
  /** current input symbol.
    */
  public int token() {
      return token;
  }
  public String tokenName() {
      return yyNames[token];
  }
  /** null or string associated with current input symbol.
    */
  public Object value() {
      return value;
  }
  /** position for error message.
    */
  public String toString() { return "Error line: " + (yyline + 1); }
  /*
   Rules for include <path/file>
   1) include <sourcepath/path/file>
   2) include <librarydir/path/file>
   Globals used: filepath, sourcefile, filename
   */
  //"use"[ \t\r\n]*"<" [^\t\r\n>]+ ">"          { return new Token(TOK_USE, yytext()); }
  //"include"[ \t\r\n]*"<" [^\t\r\n>]*"/" [^\t\r\n>/]+ ">"   { }
  //. { System.out.println(yytext().charAt(0)); }
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private int yyline;
	private boolean yy_at_bol;
	private int yy_lexical_state;

	public Lexer (java.io.Reader reader) {
		this ();
		if (null == reader) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(reader);
	}

	public Lexer (java.io.InputStream instream) {
		this ();
		if (null == instream) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
	}

	private Lexer () {
		yy_buffer = new char[YY_BUFFER_SIZE];
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yyline = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;
	}

	private boolean yy_eof_done = false;
	private final int YYINITIAL = 0;
	private final int yy_state_dtrans[] = {
		0
	};
	private void yybegin (int state) {
		yy_lexical_state = state;
	}
	private int yy_advance ()
		throws java.io.IOException {
		int next_read;
		int i;
		int j;

		if (yy_buffer_index < yy_buffer_read) {
			return yy_buffer[yy_buffer_index++];
		}

		if (0 != yy_buffer_start) {
			i = yy_buffer_start;
			j = 0;
			while (i < yy_buffer_read) {
				yy_buffer[j] = yy_buffer[i];
				++i;
				++j;
			}
			yy_buffer_end = yy_buffer_end - yy_buffer_start;
			yy_buffer_start = 0;
			yy_buffer_read = j;
			yy_buffer_index = j;
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}

		while (yy_buffer_index >= yy_buffer_read) {
			if (yy_buffer_index >= yy_buffer.length) {
				yy_buffer = yy_double(yy_buffer);
			}
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}
		return yy_buffer[yy_buffer_index++];
	}
	private void yy_move_end () {
		if (yy_buffer_end > yy_buffer_start &&
		    '\n' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
		if (yy_buffer_end > yy_buffer_start &&
		    '\r' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
	}
	private boolean yy_last_was_cr=false;
	private void yy_mark_start () {
		int i;
		for (i = yy_buffer_start; i < yy_buffer_index; ++i) {
			if ('\n' == yy_buffer[i] && !yy_last_was_cr) {
				++yyline;
			}
			if ('\r' == yy_buffer[i]) {
				++yyline;
				yy_last_was_cr=true;
			} else yy_last_was_cr=false;
		}
		yy_buffer_start = yy_buffer_index;
	}
	private void yy_mark_end () {
		yy_buffer_end = yy_buffer_index;
	}
	private void yy_to_mark () {
		yy_buffer_index = yy_buffer_end;
		yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
		            ('\r' == yy_buffer[yy_buffer_end-1] ||
		             '\n' == yy_buffer[yy_buffer_end-1] ||
		             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
		             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
	}
	private java.lang.String yytext () {
		return (new java.lang.String(yy_buffer,
			yy_buffer_start,
			yy_buffer_end - yy_buffer_start));
	}
	private int yylength () {
		return yy_buffer_end - yy_buffer_start;
	}
	private char[] yy_double (char buf[]) {
		int i;
		char newbuf[];
		newbuf = new char[2*buf.length];
		for (i = 0; i < buf.length; ++i) {
			newbuf[i] = buf[i];
		}
		return newbuf;
	}
	private final int YY_E_INTERNAL = 0;
	private final int YY_E_MATCH = 1;
	private java.lang.String yy_error_string[] = {
		"Error: Internal error.\n",
		"Error: Unmatched input.\n"
	};
	private void yy_error (int code,boolean fatal) {
		java.lang.System.out.print(yy_error_string[code]);
		java.lang.System.out.flush();
		if (fatal) {
			throw new Error("Fatal Error.\n");
		}
	}
	private int[][] unpackFromString(int size1, int size2, String st) {
		int colonIndex = -1;
		String lengthString;
		int sequenceLength = 0;
		int sequenceInteger = 0;

		int commaIndex;
		String workString;

		int res[][] = new int[size1][size2];
		for (int i= 0; i < size1; i++) {
			for (int j= 0; j < size2; j++) {
				if (sequenceLength != 0) {
					res[i][j] = sequenceInteger;
					sequenceLength--;
					continue;
				}
				commaIndex = st.indexOf(',');
				workString = (commaIndex==-1) ? st :
					st.substring(0, commaIndex);
				st = st.substring(commaIndex+1);
				colonIndex = workString.indexOf(':');
				if (colonIndex == -1) {
					res[i][j]=Integer.parseInt(workString);
					continue;
				}
				lengthString =
					workString.substring(colonIndex+1);
				sequenceLength=Integer.parseInt(lengthString);
				workString=workString.substring(0,colonIndex);
				sequenceInteger=Integer.parseInt(workString);
				res[i][j] = sequenceInteger;
				sequenceLength--;
			}
		}
		return res;
	}
	private int yy_acpt[] = {
		/* 0 */ YY_NOT_ACCEPT,
		/* 1 */ YY_NO_ANCHOR,
		/* 2 */ YY_NO_ANCHOR,
		/* 3 */ YY_NO_ANCHOR,
		/* 4 */ YY_NO_ANCHOR,
		/* 5 */ YY_NO_ANCHOR,
		/* 6 */ YY_NO_ANCHOR,
		/* 7 */ YY_NO_ANCHOR,
		/* 8 */ YY_NO_ANCHOR,
		/* 9 */ YY_NO_ANCHOR,
		/* 10 */ YY_NO_ANCHOR,
		/* 11 */ YY_NO_ANCHOR,
		/* 12 */ YY_NO_ANCHOR,
		/* 13 */ YY_NO_ANCHOR,
		/* 14 */ YY_NO_ANCHOR,
		/* 15 */ YY_NO_ANCHOR,
		/* 16 */ YY_NO_ANCHOR,
		/* 17 */ YY_NO_ANCHOR,
		/* 18 */ YY_NO_ANCHOR,
		/* 19 */ YY_NO_ANCHOR,
		/* 20 */ YY_NO_ANCHOR,
		/* 21 */ YY_NO_ANCHOR,
		/* 22 */ YY_NO_ANCHOR,
		/* 23 */ YY_NO_ANCHOR,
		/* 24 */ YY_NO_ANCHOR,
		/* 25 */ YY_NO_ANCHOR,
		/* 26 */ YY_NOT_ACCEPT,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NOT_ACCEPT,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NO_ANCHOR,
		/* 33 */ YY_NOT_ACCEPT,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NOT_ACCEPT,
		/* 37 */ YY_NO_ANCHOR,
		/* 38 */ YY_NOT_ACCEPT,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NOT_ACCEPT,
		/* 41 */ YY_NO_ANCHOR,
		/* 42 */ YY_NOT_ACCEPT,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NOT_ACCEPT,
		/* 45 */ YY_NO_ANCHOR,
		/* 46 */ YY_NOT_ACCEPT,
		/* 47 */ YY_NO_ANCHOR,
		/* 48 */ YY_NOT_ACCEPT,
		/* 49 */ YY_NO_ANCHOR,
		/* 50 */ YY_NOT_ACCEPT,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NOT_ACCEPT,
		/* 53 */ YY_NOT_ACCEPT,
		/* 54 */ YY_NOT_ACCEPT,
		/* 55 */ YY_NOT_ACCEPT,
		/* 56 */ YY_NOT_ACCEPT,
		/* 57 */ YY_NOT_ACCEPT,
		/* 58 */ YY_NOT_ACCEPT,
		/* 59 */ YY_NOT_ACCEPT,
		/* 60 */ YY_NOT_ACCEPT,
		/* 61 */ YY_NOT_ACCEPT,
		/* 62 */ YY_NOT_ACCEPT,
		/* 63 */ YY_NOT_ACCEPT,
		/* 64 */ YY_NOT_ACCEPT,
		/* 65 */ YY_NOT_ACCEPT,
		/* 66 */ YY_NOT_ACCEPT,
		/* 67 */ YY_NOT_ACCEPT,
		/* 68 */ YY_NOT_ACCEPT,
		/* 69 */ YY_NOT_ACCEPT,
		/* 70 */ YY_NOT_ACCEPT,
		/* 71 */ YY_NOT_ACCEPT,
		/* 72 */ YY_NOT_ACCEPT
	};
	private int yy_cmap[] = unpackFromString(1,130,
"29:9,26:2,29,26:2,29:18,26,4,29:4,5,29:9,9,29:2,10,29:8,1,2,3,29:4,27:3,29:" +
"3,27,29:4,27:2,29:5,27,29:6,7,29:4,23,28,19,13,16,17,29,25,21,29:2,15,11,18" +
",12,29:2,24,22,20,14,29:2,8,29:3,6,29:3,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,73,
"0,1,2,3,1:2,4,1:19,5,6,7,8,9,10,11,12,13,1,14,15,16,17,18,19,20,1,21,22,23," +
"24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48," +
"49")[0];

	private int yy_nxt[][] = unpackFromString(50,30,
"1,2,27,31,34,37,39,41,43:3,3,43,4,28,45,32,47,35,43,49,29,43,51,35,43,5,6,3" +
"5,43,-1:32,7,-1:39,30,-1:44,6,-1:11,50,-1:22,8,-1:45,69,-1:28,13,-1:25,71,-" +
"1:18,9,-1:42,36,-1:3,38,-1:3,40,-1:26,14,-1:11,10,-1:49,53,-1:12,11,-1:49,5" +
"4,-1:10,12,-1:42,55,-1:18,26,-1:45,15,-1:23,56,-1:27,33,-1:28,57,-1:26,42,-" +
"1,44,-1:8,46,-1:20,58,-1:39,48,-1:15,16,-1:41,70,-1:23,60,-1:29,17,-1:25,18" +
",-1:42,19,-1:23,61,-1:32,62,-1:23,20,-1:28,64,-1:31,21,-1:32,65,-1:25,22,-1" +
":37,66,-1:21,23,-1:34,67,-1:28,24,-1:21,68,-1:35,25,-1:24,52,-1:38,72,-1:21" +
",59,-1:31,63,-1:13");

	public Token yylex ()
		throws java.io.IOException {
		int yy_lookahead;
		int yy_anchor = YY_NO_ANCHOR;
		int yy_state = yy_state_dtrans[yy_lexical_state];
		int yy_next_state = YY_NO_STATE;
		int yy_last_accept_state = YY_NO_STATE;
		boolean yy_initial = true;
		int yy_this_accept;

		yy_mark_start();
		yy_this_accept = yy_acpt[yy_state];
		if (YY_NOT_ACCEPT != yy_this_accept) {
			yy_last_accept_state = yy_state;
			yy_mark_end();
		}
		while (true) {
			if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
			else yy_lookahead = yy_advance();
			yy_next_state = YY_F;
			yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
			if (YY_EOF == yy_lookahead && true == yy_initial) {

  return new Token(YY_EOF, "EOF");
			}
			if (YY_F != yy_next_state) {
				yy_state = yy_next_state;
				yy_initial = false;
				yy_this_accept = yy_acpt[yy_state];
				if (YY_NOT_ACCEPT != yy_this_accept) {
					yy_last_accept_state = yy_state;
					yy_mark_end();
				}
			}
			else {
				if (YY_NO_STATE == yy_last_accept_state) {
					throw (new Error("Lexical Error: Unmatched Input."));
				}
				else {
					yy_anchor = yy_acpt[yy_last_accept_state];
					if (0 != (YY_END & yy_anchor)) {
						yy_move_end();
					}
					yy_to_mark();
					switch (yy_last_accept_state) {
					case 1:
						
					case -2:
						break;
					case 2:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -3:
						break;
					case 3:
						{ return new Token(TOK_NUMBER, yytext()); }
					case -4:
						break;
					case 4:
						{ return new Token(TOK_ID, yytext()); }
					case -5:
						break;
					case 5:
						{ /* ignore white space. */ }
					case -6:
						break;
					case 6:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -7:
						break;
					case 7:
						{ return new Token(LE, yytext()); }
					case -8:
						break;
					case 8:
						{ return new Token(EQ, yytext()); }
					case -9:
						break;
					case 9:
						{ return new Token(GE, yytext()); }
					case -10:
						break;
					case 10:
						{ return new Token(NE, yytext()); }
					case -11:
						break;
					case 11:
						{ return new Token(AND, yytext()); }
					case -12:
						break;
					case 12:
						{ return new Token(OR, yytext()); }
					case -13:
						break;
					case 13:
						{ return new Token(TOK_IF, yytext()); }
					case -14:
						break;
					case 14:
						{ return new Token(TOK_LET, yytext()); }
					case -15:
						break;
					case 15:
						{ return new Token(TOK_FOR, yytext()); }
					case -16:
						break;
					case 16:
						{ return new Token(TOK_EOT, yytext()); }
					case -17:
						break;
					case 17:
						{ return new Token(TOK_ELSE, yytext()); }
					case -18:
						break;
					case 18:
						{ return new Token(TOK_ECHO, yytext()); }
					case -19:
						break;
					case 19:
						{ return new Token(TOK_EACH, yytext()); }
					case -20:
						break;
					case 20:
						{ return new Token(TOK_TRUE, yytext()); }
					case -21:
						break;
					case 21:
						{ return new Token(TOK_UNDEF, yytext()); }
					case -22:
						break;
					case 22:
						{ return new Token(TOK_FALSE, yytext()); }
					case -23:
						break;
					case 23:
						{ return new Token(TOK_MODULE, yytext()); }
					case -24:
						break;
					case 24:
						{ return new Token(TOK_ASSERT, yytext()); }
					case -25:
						break;
					case 25:
						{ return new Token(TOK_FUNCTION, yytext()); }
					case -26:
						break;
					case 27:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -27:
						break;
					case 28:
						{ return new Token(TOK_NUMBER, yytext()); }
					case -28:
						break;
					case 29:
						{ return new Token(TOK_ID, yytext()); }
					case -29:
						break;
					case 31:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -30:
						break;
					case 32:
						{ return new Token(TOK_NUMBER, yytext()); }
					case -31:
						break;
					case 34:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -32:
						break;
					case 35:
						{ return new Token(TOK_NUMBER, yytext()); }
					case -33:
						break;
					case 37:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -34:
						break;
					case 39:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -35:
						break;
					case 41:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -36:
						break;
					case 43:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -37:
						break;
					case 45:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -38:
						break;
					case 47:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -39:
						break;
					case 49:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -40:
						break;
					case 51:
						{ return new Token(YY_NOT_ACCEPT, yytext()); }
					case -41:
						break;
					default:
						yy_error(YY_E_INTERNAL,false);
					case -1:
					}
					yy_initial = true;
					yy_state = yy_state_dtrans[yy_lexical_state];
					yy_next_state = YY_NO_STATE;
					yy_last_accept_state = YY_NO_STATE;
					yy_mark_start();
					yy_this_accept = yy_acpt[yy_state];
					if (YY_NOT_ACCEPT != yy_this_accept) {
						yy_last_accept_state = yy_state;
						yy_mark_end();
					}
				}
			}
		}
	}
}
