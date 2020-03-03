package scad.compiler;
import static scad.compiler.Parser.*;
import java.text.NumberFormat;
import java.text.ParseException;


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
	  Token t = null;
	  try {
		  t = yylex();
	  } catch (ParseException e) {
		  throw new RuntimeException(e);
	  }
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
{START}	  	{yymore(); BEGIN C;}
<C>{SIMPLE}	{yymore();}
<C>{COMPLEX}	{yymore();}
<C>{END}  	{BEGIN 0;}
*/
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
		/* 26 */ YY_NO_ANCHOR,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NO_ANCHOR,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NOT_ACCEPT,
		/* 33 */ YY_NO_ANCHOR,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NO_ANCHOR,
		/* 37 */ YY_NOT_ACCEPT,
		/* 38 */ YY_NO_ANCHOR,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NO_ANCHOR,
		/* 41 */ YY_NOT_ACCEPT,
		/* 42 */ YY_NO_ANCHOR,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NO_ANCHOR,
		/* 45 */ YY_NOT_ACCEPT,
		/* 46 */ YY_NO_ANCHOR,
		/* 47 */ YY_NO_ANCHOR,
		/* 48 */ YY_NO_ANCHOR,
		/* 49 */ YY_NOT_ACCEPT,
		/* 50 */ YY_NO_ANCHOR,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NOT_ACCEPT,
		/* 53 */ YY_NO_ANCHOR,
		/* 54 */ YY_NO_ANCHOR,
		/* 55 */ YY_NOT_ACCEPT,
		/* 56 */ YY_NO_ANCHOR,
		/* 57 */ YY_NO_ANCHOR,
		/* 58 */ YY_NOT_ACCEPT,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NO_ANCHOR,
		/* 61 */ YY_NOT_ACCEPT,
		/* 62 */ YY_NO_ANCHOR,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NOT_ACCEPT,
		/* 65 */ YY_NO_ANCHOR,
		/* 66 */ YY_NO_ANCHOR,
		/* 67 */ YY_NOT_ACCEPT,
		/* 68 */ YY_NO_ANCHOR,
		/* 69 */ YY_NO_ANCHOR,
		/* 70 */ YY_NOT_ACCEPT,
		/* 71 */ YY_NO_ANCHOR,
		/* 72 */ YY_NO_ANCHOR,
		/* 73 */ YY_NOT_ACCEPT,
		/* 74 */ YY_NO_ANCHOR,
		/* 75 */ YY_NOT_ACCEPT,
		/* 76 */ YY_NO_ANCHOR,
		/* 77 */ YY_NOT_ACCEPT,
		/* 78 */ YY_NOT_ACCEPT,
		/* 79 */ YY_NOT_ACCEPT,
		/* 80 */ YY_NOT_ACCEPT,
		/* 81 */ YY_NOT_ACCEPT,
		/* 82 */ YY_NO_ANCHOR,
		/* 83 */ YY_NOT_ACCEPT,
		/* 84 */ YY_NO_ANCHOR,
		/* 85 */ YY_NOT_ACCEPT,
		/* 86 */ YY_NO_ANCHOR,
		/* 87 */ YY_NO_ANCHOR,
		/* 88 */ YY_NO_ANCHOR,
		/* 89 */ YY_NO_ANCHOR,
		/* 90 */ YY_NO_ANCHOR,
		/* 91 */ YY_NO_ANCHOR,
		/* 92 */ YY_NO_ANCHOR,
		/* 93 */ YY_NO_ANCHOR,
		/* 94 */ YY_NO_ANCHOR,
		/* 95 */ YY_NO_ANCHOR,
		/* 96 */ YY_NO_ANCHOR,
		/* 97 */ YY_NO_ANCHOR,
		/* 98 */ YY_NO_ANCHOR,
		/* 99 */ YY_NO_ANCHOR,
		/* 100 */ YY_NO_ANCHOR,
		/* 101 */ YY_NO_ANCHOR,
		/* 102 */ YY_NO_ANCHOR,
		/* 103 */ YY_NO_ANCHOR,
		/* 104 */ YY_NO_ANCHOR,
		/* 105 */ YY_NO_ANCHOR,
		/* 106 */ YY_NO_ANCHOR,
		/* 107 */ YY_NO_ANCHOR,
		/* 108 */ YY_NO_ANCHOR,
		/* 109 */ YY_NO_ANCHOR,
		/* 110 */ YY_NO_ANCHOR,
		/* 111 */ YY_NO_ANCHOR,
		/* 112 */ YY_NO_ANCHOR,
		/* 113 */ YY_NO_ANCHOR,
		/* 114 */ YY_NO_ANCHOR,
		/* 115 */ YY_NO_ANCHOR,
		/* 116 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,130,
"27:9,31,22,27,28,22,27:18,32,4,24,27,25,27,6,27:3,43,38,27,34,36,33,41,35:2" +
",42,35:6,27:2,1,2,3,27:2,39:2,23:2,37,39:3,23,39:4,23:2,39:5,23,39:5,27,30," +
"27:2,39,27,19,39,15,9,12,13,39,21,17,39:2,11,7,14,8,39:2,20,18,16,10,39:2,4" +
"0,39:2,26,5,29,27:2,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,117,
"0,1,2,3,1,4,5,1:6,6,1,7,8,6:6,1:2,6:3,1,6:2,1,9,10,11,12,13,14,15,16,17,18," +
"19,20,21,22,23,24,25,26,27,28,29,9,30,8,6,31,32,1,33,34,35,36,25,37,38,39,4" +
"0,41,42,22,43,44,45,46,47,48,13,49,50,51,52,53,54,55,56,57,58,59,60,61,62,6" +
"3,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,6,84,85")[0];

	private int yy_nxt[][] = unpackFromString(86,44,
"1,2,33,38,42,46,50,3,114:2,98,82,99,86,114:2,100,34,114,116,114:2,4,5,53,56" +
",59:2,4,59,62,4:2,65,68,6,71,5,59,114:2,6:2,59,-1:46,7,-1:48,114,107,114:13" +
",-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:15,-1,5,-1:11,114,-1,5,-1,114:4," +
"-1:8,114:5,47,114:9,-1,114,-1:11,6,45,47,-1,114:2,6:2,-1:8,114:15,-1,114,-1" +
":11,114,-1,114,-1,114:4,-1:2,15:21,-1,15:21,-1,55:42,58,-1,32:23,14,83,32:4" +
",37,32:13,-1:2,8,-1:48,114:6,13,109,114:7,-1,114,-1:11,114,-1,114,-1,114:4," +
"-1:13,61,-1:22,35,45,61,-1:3,35:2,-1:2,78:21,-1,78:6,49,78:14,-1,32:21,-1,3" +
"2:21,-1:2,9,-1:48,114:9,17,114:5,-1,114,-1:11,114,-1,114,-1,114:4,-1:13,61," +
"-1:22,40,-1,61,-1:3,40:2,-1:42,52,-1:4,10,-1:48,114:13,18,114,-1,114,-1:11," +
"114,-1,114,-1,114:4,-1:8,114:15,-1,114,-1:11,44,-1,114,-1,114:2,44:2,-1:36," +
"40,-1:5,40:2,-1:6,11,-1:45,114:15,-1,114,-1:10,64,44,-1,114,64,114:2,44:2,-" +
"1:36,48,-1:5,48:2,-1:2,49:21,32,49,36,49:5,73,49:13,-1:6,12,-1:38,67,-1:5,1" +
"14:15,70,114,-1:7,70:2,-1:2,114,-1,114,-1,114:4,-1:43,23,-1:8,114:5,19,114:" +
"9,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114,20,114:13,-1,114,-1:11,114,-1,1" +
"14,-1,114:4,-1:2,75:32,24,75:9,58,-1:7,114:14,21,-1,114,-1:11,114,-1,114,-1" +
",114:4,-1:35,64,48,-1:2,64,-1:2,48:2,-1:41,41,-1:10,114:5,22,114:9,-1,114,-" +
"1:11,114,-1,114,-1,114:4,-1:34,15,-1:9,16,-1:7,114:6,25,114:8,-1,114,-1:11," +
"114,-1,114,-1,114:4,-1:2,77:2,-1,77:18,-1,77:8,-1,77:12,-1:35,35,45,-1:4,35" +
":2,-1:8,114:5,26,114:9,-1,114,-1:11,114,-1,114,-1,114:4,-1:2,67,-1:20,70,-1" +
":8,70:2,-1:18,114:5,27,114:9,-1,114,-1:11,114,-1,114,-1,114:4,-1:2,49:21,-1" +
",49:21,-1:7,114:9,29,114:5,-1,114,-1:11,114,-1,114,-1,114:4,-1:2,75:42,58,-" +
"1:7,114:7,30,114:7,-1,114,-1:11,114,-1,114,-1,114:4,-1:2,77:2,28,77:18,-1,7" +
"7:8,-1,77:12,-1,79:2,-1,79:18,-1,79:8,-1,79,85,79:10,-1,79,-1:20,80,-1:8,80" +
":2,-1:12,81:2,31,81:18,-1,81:8,-1,81,85,81:10,-1:7,114:5,39,114:9,-1,114,-1" +
":11,114,-1,114,-1,114:4,-1:2,32:23,14,83,49,32:3,37,32:13,-1,79,-1:5,114:15" +
",80,114,-1:7,80:2,-1:2,114,-1,114,-1,114:4,-1:2,81:2,-1,81:18,-1,81:8,-1,81" +
",85,81:10,-1:7,114,43,114,108,114:8,102,114:2,-1,114,-1:11,114,-1,114,-1,11" +
"4:4,-1:8,114:5,51,114:9,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:11,54,114" +
":3,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:14,57,-1,114,-1:11,114,-1,114," +
"-1,114:4,-1:8,114:8,60,114:6,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:3,63" +
",114:11,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:5,66,114:9,-1,114,-1:11,1" +
"14,-1,114,-1,114:4,-1:8,114:11,69,114:3,-1,114,-1:11,114,-1,114,-1,114:4,-1" +
":8,114:4,72,114:10,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:13,74,114,-1,1" +
"14,-1:11,114,-1,114,-1,114:4,-1:8,114,76,114:13,-1,114,-1:11,114,-1,114,-1," +
"114:4,-1:8,114:5,84,114:9,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:7,101,1" +
"14:3,87,114:3,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:4,88,114:3,89,114:3" +
",90,114:2,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:13,91,114,-1,114,-1:11," +
"114,-1,114,-1,114:4,-1:8,114:2,92,114:12,-1,114,-1:11,114,-1,114,-1,114:4,-" +
"1:8,114:4,93,114:10,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:3,94,114:11,-" +
"1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:5,95,114:9,-1,114,-1:11,114,-1,114" +
",-1,114:4,-1:8,114:10,96,114:4,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:2," +
"97,114:12,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:2,103,114:12,-1,114,-1:" +
"11,114,-1,114,-1,114:4,-1:8,114:7,115,114:7,-1,114,-1:11,114,-1,114,-1,114:" +
"4,-1:8,114:8,111,114:6,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:11,104,114" +
":3,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:4,113,114:10,-1,114,-1:11,114," +
"-1,114,-1,114:4,-1:8,114:9,105,114:5,-1,114,-1:11,114,-1,114,-1,114:4,-1:8," +
"114:3,106,114:11,-1,114,-1:11,114,-1,114,-1,114:4,-1:8,114:8,112,114:6,-1,1" +
"14,-1:11,114,-1,114,-1,114:4,-1:8,114:11,110,114:3,-1,114,-1:11,114,-1,114," +
"-1,114:4,-1");

	public Token yylex ()
		throws java.io.IOException, 
ParseException

		{
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
						{ return new Token(yytext().charAt(0), yytext()); }
					case -3:
						break;
					case 3:
						{ return new Token(TOK_ID, yytext()); }
					case -4:
						break;
					case 4:
						{ /* ignore white space. */ }
					case -5:
						break;
					case 5:
						{ return new Token(TOK_ERROR, yytext()); }
					case -6:
						break;
					case 6:
						{ return new Token(TOK_NUMBER, NumberFormat.getInstance().parse(yytext())); }
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
						{ return new Token(OR, yytext()); }
					case -12:
						break;
					case 12:
						{ return new Token(AND, yytext()); }
					case -13:
						break;
					case 13:
						{ return new Token(TOK_IF, yytext()); }
					case -14:
						break;
					case 14:
						{ return new Token(TOK_STRING, yytext()); }
					case -15:
						break;
					case 15:
						{ /* skip line comment */ }
					case -16:
						break;
					case 16:
						{ throw new ParseException("Unterminated comment", yyline); }
					case -17:
						break;
					case 17:
						{ return new Token(TOK_LET, yytext()); }
					case -18:
						break;
					case 18:
						{ return new Token(TOK_FOR, yytext()); }
					case -19:
						break;
					case 19:
						{ return new Token(TOK_ELSE, yytext()); }
					case -20:
						break;
					case 20:
						{ return new Token(TOK_ECHO, yytext()); }
					case -21:
						break;
					case 21:
						{ return new Token(TOK_EACH, yytext()); }
					case -22:
						break;
					case 22:
						{ return new Token(TOK_TRUE, yytext()); }
					case -23:
						break;
					case 23:
						{ return new Token(TOK_EOT, yytext()); }
					case -24:
						break;
					case 24:
						{ /* skip multiline comment */ }
					case -25:
						break;
					case 25:
						{ return new Token(TOK_UNDEF, yytext()); }
					case -26:
						break;
					case 26:
						{ return new Token(TOK_FALSE, yytext()); }
					case -27:
						break;
					case 27:
						{ return new Token(TOK_MODULE, yytext()); }
					case -28:
						break;
					case 28:
						{ return new Token(TOK_USE, yytext()); }
					case -29:
						break;
					case 29:
						{ return new Token(TOK_ASSERT, yytext()); }
					case -30:
						break;
					case 30:
						{ return new Token(TOK_FUNCTION, yytext()); }
					case -31:
						break;
					case 31:
						{ }
					case -32:
						break;
					case 33:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -33:
						break;
					case 34:
						{ return new Token(TOK_ID, yytext()); }
					case -34:
						break;
					case 35:
						{ return new Token(TOK_NUMBER, NumberFormat.getInstance().parse(yytext())); }
					case -35:
						break;
					case 36:
						{ return new Token(TOK_STRING, yytext()); }
					case -36:
						break;
					case 38:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -37:
						break;
					case 39:
						{ return new Token(TOK_ID, yytext()); }
					case -38:
						break;
					case 40:
						{ return new Token(TOK_NUMBER, NumberFormat.getInstance().parse(yytext())); }
					case -39:
						break;
					case 42:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -40:
						break;
					case 43:
						{ return new Token(TOK_ID, yytext()); }
					case -41:
						break;
					case 44:
						{ return new Token(TOK_NUMBER, NumberFormat.getInstance().parse(yytext())); }
					case -42:
						break;
					case 46:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -43:
						break;
					case 47:
						{ return new Token(TOK_ID, yytext()); }
					case -44:
						break;
					case 48:
						{ return new Token(TOK_NUMBER, NumberFormat.getInstance().parse(yytext())); }
					case -45:
						break;
					case 50:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -46:
						break;
					case 51:
						{ return new Token(TOK_ID, yytext()); }
					case -47:
						break;
					case 53:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -48:
						break;
					case 54:
						{ return new Token(TOK_ID, yytext()); }
					case -49:
						break;
					case 56:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -50:
						break;
					case 57:
						{ return new Token(TOK_ID, yytext()); }
					case -51:
						break;
					case 59:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -52:
						break;
					case 60:
						{ return new Token(TOK_ID, yytext()); }
					case -53:
						break;
					case 62:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -54:
						break;
					case 63:
						{ return new Token(TOK_ID, yytext()); }
					case -55:
						break;
					case 65:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -56:
						break;
					case 66:
						{ return new Token(TOK_ID, yytext()); }
					case -57:
						break;
					case 68:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -58:
						break;
					case 69:
						{ return new Token(TOK_ID, yytext()); }
					case -59:
						break;
					case 71:
						{ return new Token(yytext().charAt(0), yytext()); }
					case -60:
						break;
					case 72:
						{ return new Token(TOK_ID, yytext()); }
					case -61:
						break;
					case 74:
						{ return new Token(TOK_ID, yytext()); }
					case -62:
						break;
					case 76:
						{ return new Token(TOK_ID, yytext()); }
					case -63:
						break;
					case 82:
						{ return new Token(TOK_ID, yytext()); }
					case -64:
						break;
					case 84:
						{ return new Token(TOK_ID, yytext()); }
					case -65:
						break;
					case 86:
						{ return new Token(TOK_ID, yytext()); }
					case -66:
						break;
					case 87:
						{ return new Token(TOK_ID, yytext()); }
					case -67:
						break;
					case 88:
						{ return new Token(TOK_ID, yytext()); }
					case -68:
						break;
					case 89:
						{ return new Token(TOK_ID, yytext()); }
					case -69:
						break;
					case 90:
						{ return new Token(TOK_ID, yytext()); }
					case -70:
						break;
					case 91:
						{ return new Token(TOK_ID, yytext()); }
					case -71:
						break;
					case 92:
						{ return new Token(TOK_ID, yytext()); }
					case -72:
						break;
					case 93:
						{ return new Token(TOK_ID, yytext()); }
					case -73:
						break;
					case 94:
						{ return new Token(TOK_ID, yytext()); }
					case -74:
						break;
					case 95:
						{ return new Token(TOK_ID, yytext()); }
					case -75:
						break;
					case 96:
						{ return new Token(TOK_ID, yytext()); }
					case -76:
						break;
					case 97:
						{ return new Token(TOK_ID, yytext()); }
					case -77:
						break;
					case 98:
						{ return new Token(TOK_ID, yytext()); }
					case -78:
						break;
					case 99:
						{ return new Token(TOK_ID, yytext()); }
					case -79:
						break;
					case 100:
						{ return new Token(TOK_ID, yytext()); }
					case -80:
						break;
					case 101:
						{ return new Token(TOK_ID, yytext()); }
					case -81:
						break;
					case 102:
						{ return new Token(TOK_ID, yytext()); }
					case -82:
						break;
					case 103:
						{ return new Token(TOK_ID, yytext()); }
					case -83:
						break;
					case 104:
						{ return new Token(TOK_ID, yytext()); }
					case -84:
						break;
					case 105:
						{ return new Token(TOK_ID, yytext()); }
					case -85:
						break;
					case 106:
						{ return new Token(TOK_ID, yytext()); }
					case -86:
						break;
					case 107:
						{ return new Token(TOK_ID, yytext()); }
					case -87:
						break;
					case 108:
						{ return new Token(TOK_ID, yytext()); }
					case -88:
						break;
					case 109:
						{ return new Token(TOK_ID, yytext()); }
					case -89:
						break;
					case 110:
						{ return new Token(TOK_ID, yytext()); }
					case -90:
						break;
					case 111:
						{ return new Token(TOK_ID, yytext()); }
					case -91:
						break;
					case 112:
						{ return new Token(TOK_ID, yytext()); }
					case -92:
						break;
					case 113:
						{ return new Token(TOK_ID, yytext()); }
					case -93:
						break;
					case 114:
						{ return new Token(TOK_ID, yytext()); }
					case -94:
						break;
					case 115:
						{ return new Token(TOK_ID, yytext()); }
					case -95:
						break;
					case 116:
						{ return new Token(TOK_ID, yytext()); }
					case -96:
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
