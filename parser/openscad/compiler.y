%token TOK_ERROR

%token TOK_EOT

%token TOK_MODULE
%token TOK_FUNCTION
%token TOK_IF
%token TOK_ELSE
%token TOK_FOR
%token TOK_LET
%token TOK_ASSERT
%token TOK_ECHO
%token TOK_EACH

%token <String> TOK_ID
%token <String> TOK_STRING
%token <String> TOK_USE
%token <Number> TOK_NUMBER

%token TOK_TRUE
%token TOK_FALSE
%token TOK_UNDEF

%token LE GE EQ NE AND OR

%nonassoc NO_ELSE
%nonassoc TOK_ELSE

%type <expr> expr
%type <expr> call
%type <expr> logic_or
%type <expr> logic_and
%type <expr> equality
%type <expr> comparison
%type <expr> addition
%type <expr> multiplication
%type <expr> exponent
%type <expr> unary
%type <expr> primary
%type <vec> vector_expr
%type <expr> list_comprehension_elements
%type <expr> list_comprehension_elements_p
%type <expr> list_comprehension_elements_or_expr
%type <expr> expr_or_empty

%type <inst> module_instantiation
%type <ifelse> if_statement
%type <ifelse> ifelse_statement
%type <inst> single_module_instantiation

%type <args> arguments_call
%type <args> arguments_decl

%type <arg> argument_call
%type <arg> argument_decl
%type <String> module_id

%%

input
 :
 | input
 TOK_USE
 {
    System.out.println("use " + $2);
 }
 | input statement
 ;

statement
 : ';'
 | '{' inner_input '}'
 | module_instantiation
 {
     System.out.println("module_instantiation");
 }
 | assignment
 | TOK_MODULE TOK_ID '(' arguments_decl optional_commas ')'
 {
     System.out.println("TOK_MODULE TOK_ID ( arguments_decl optional_commas )");
 }
 statement
 {
     System.out.println("statement");
 }
 | TOK_FUNCTION TOK_ID '(' arguments_decl optional_commas ')' '=' expr ';'
 {
     System.out.println("TOK_FUNCTION TOK_ID ( arguments_decl optional_commas ) = expr");
 }
 | TOK_EOT
 {
    System.out.println("TOK_EOT");
 }
 ;

inner_input
 : /* empty */
 | statement inner_input
 ;

assignment
 : TOK_ID '=' expr ';'
 {
     System.out.println("assignment");
 }
 ;

module_instantiation
 : '!' module_instantiation
 {
     System.out.println("!module_instantiation");
 }
 | '#' module_instantiation
 {
     System.out.println("#module_instantiation");
 }
 | '%' module_instantiation
 {
     System.out.println("%module_instantiation");
 }
 | '*' module_instantiation
 {
     System.out.println("*module_instantiation");
 }
 | single_module_instantiation
 {
     System.out.println("single_module_instantiation");
 }
 child_statement
 {
     System.out.println("child_statement");
 }
 | ifelse_statement
 {
     System.out.println("ifelse_statement");
 }
 ;

ifelse_statement
 : if_statement %prec NO_ELSE
 {
     System.out.println("ifelse_statement NO_ELSE");
 }
 | if_statement TOK_ELSE
 {
     System.out.println("ifelse_statement TOK_ELSE");
 }
 child_statement
 {
     System.out.println("child_statement");
 }
 ;

if_statement
 : TOK_IF '(' expr ')'
 {
     System.out.println("if_statement");
 }
 child_statement
 {
     System.out.println("child_statement");
 }
 ;

child_statements
 : /* empty */
 | child_statements child_statement
 | child_statements assignment
 ;

child_statement
 : ';'
 | '{' child_statements '}'
 | module_instantiation
 {
    System.out.println("child_statement");
 }
 ;

// "for", "let" and "each" are valid module identifiers
module_id
 : TOK_ID { System.out.println("TOK_ID"); }
 | TOK_FOR { System.out.println("TOK_FOR");}
 | TOK_LET { System.out.println("TOK_LET");}
 | TOK_ASSERT { System.out.println("TOK_ASSERT"); }
 | TOK_ECHO { System.out.println("TOK_ECHO"); }
 | TOK_EACH { System.out.println("TOK_EACH"); }
 ;

single_module_instantiation
 : module_id '(' arguments_call ')'
 {
    System.out.println("single_module_instantiation, module_id = " + $1 + " argument = " + $3);
 }
 ;

expr
 : logic_or
 | TOK_FUNCTION '(' arguments_decl optional_commas ')' expr %prec NO_ELSE
 {
    System.out.println("254");
 }
 | logic_or '?' expr ':' expr
 {
    System.out.println("258");
 }
 | TOK_LET '(' arguments_call ')' expr
 {
    System.out.println("262");
 }
 | TOK_ASSERT '(' arguments_call ')' expr_or_empty
 {
    System.out.println("266");
 }
 | TOK_ECHO '(' arguments_call ')' expr_or_empty
 {
    System.out.println("270");
 }
 ;

logic_or
 : logic_and
 | logic_or OR logic_and
 {
    System.out.println("278");
 }
 ;

logic_and
 : equality
 | logic_and AND equality
 {
    System.out.println("286");
 }
 ;

equality
 : comparison
 | equality EQ comparison
 {
    System.out.println("294");
 }
 | equality NE comparison
 {
    System.out.println("298");
 }
 ;

comparison
 : addition
 | comparison '>' addition
 {
    System.out.println("306");
 }
 | comparison GE addition
 {
    System.out.println("310");
 }
 | comparison '<' addition
 {
    System.out.println("314");
 }
 | comparison LE addition
 {
    System.out.println("318");
 }
 ;

addition
 : multiplication
 | addition '+' multiplication
 {
    System.out.println("326");
 }
 | addition '-' multiplication
 {
    System.out.println("330");
 }
 ;

multiplication
 : unary
 | multiplication '*' unary
 {
    System.out.println("338");
 }
 | multiplication '/' unary
 {
    System.out.println("342");
 }
 | multiplication '%' unary
 {
    System.out.println("346");
 }
 ;


unary
 : exponent
 | '+' unary
 {
    System.out.println("355");
 }
 | '-' unary
 {
     System.out.println("359");
 }
 | '!' unary
 {
     System.out.println("363");
 }
 ;

exponent
 : call
 | call '^' unary
 {
    System.out.println("371");
 }
 ;

call
 : primary
 | call '(' arguments_call ')'
 {
    System.out.println("379");
 }
 | call '[' expr ']'
 {
    System.out.println("383");
 }
 | call '.' TOK_ID
 {
    System.out.println("387");
 }
 ;

primary
 : TOK_TRUE
 {
    System.out.println("TOK_TRUE");
 }
 | TOK_FALSE
 {
    System.out.println("TOK_FALSE");
 }
 | TOK_UNDEF
 {
    System.out.println("TOK_UNDEF");
 }
 | TOK_NUMBER
 {
    System.out.println("TOK_NUMBER");
 }
 | TOK_STRING
 {
    System.out.println("String");
 }
 | TOK_ID
 {
    System.out.println("TOK_ID");
 }
 | '(' expr ')'
 {
    System.out.println("418");
 }
 | '[' expr ':' expr ']'
 {
    System.out.println("422");
 }
 | '[' expr ':' expr ':' expr ']'
 {
    System.out.println("426");
 }
 | '[' optional_commas ']'
 {
    System.out.println("430");
 }
 | '[' vector_expr optional_commas ']'
 {
    System.out.println("434");
 }
 ;

expr_or_empty
 : /* empty */
 {
 $$ = null;
 }
 | expr
 {
 //$$ = $1;
 }
 ;

/* The last set element may not be a "let" (as that would instead
 be parsed as an expression) */
list_comprehension_elements
 : TOK_LET '(' arguments_call ')' list_comprehension_elements_p
 {
    System.out.println("454");
 }
 | TOK_EACH list_comprehension_elements_or_expr
 {
    System.out.println("458");
 }
 | TOK_FOR '(' arguments_call ')' list_comprehension_elements_or_expr
 {
    System.out.println("462");
 }
 | TOK_FOR '(' arguments_call ';' expr ';' arguments_call ')' list_comprehension_elements_or_expr
 {
    System.out.println("466");
 }
 | TOK_IF '(' expr ')' list_comprehension_elements_or_expr %prec NO_ELSE
 {
    System.out.println("470");
 }
 | TOK_IF '(' expr ')' list_comprehension_elements_or_expr TOK_ELSE list_comprehension_elements_or_expr
 {
    System.out.println("474");
 }
 ;

// list_comprehension_elements with optional parenthesis
list_comprehension_elements_p
 : list_comprehension_elements
 | '(' list_comprehension_elements ')'
 {
 //$$ = $2;
 }
 ;

list_comprehension_elements_or_expr
 : list_comprehension_elements_p
 | expr
 ;

optional_commas
 : /* empty */
 | ',' optional_commas
 ;

vector_expr
 : expr
 {
    System.out.println("500");
 }
 | list_comprehension_elements
 {
    System.out.println("504");
 }
 | vector_expr ',' optional_commas list_comprehension_elements_or_expr
 {
    System.out.println("509");
 }
 ;

arguments_decl
 : /* empty */
 {
    System.out.println("516");
 }
 | argument_decl
 {

    System.out.println("520");
 }
 | arguments_decl ',' optional_commas argument_decl
 {

    System.out.println("525");
 }
 ;

argument_decl
 : TOK_ID
 {
    System.out.println("532");
 }
 | TOK_ID '=' expr
 {
    System.out.println("536");
 }
 ;

arguments_call
 : /* empty */
 {
    System.out.println("543");
 }
 | argument_call
 {
    System.out.println("547");
 }
 | arguments_call ',' optional_commas argument_call
 {
    System.out.println("552");
 }
 ;

argument_call
 : expr
 {
    System.out.println("559");
 }
 | TOK_ID '=' expr
 {
     System.out.println("563");
 }
 ;

%%
