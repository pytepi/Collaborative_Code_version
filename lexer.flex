import java_cup.runtime.*;

%%

%class Lexer

%line
%column

%cup

%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]

propositional_symbol = [a-z]
nominal = \d+

diamond = "<>"
box = "[]"
implies = "->"
bi_implies = "<->"

%%

<YYINITIAL> {

/* Symbols */

	"("				{return symbol(sym.LPARAN);}
	")"				{return symbol(sym.RPARAN);}

    "@"             {return symbol(sym.SATIS);}
    "$"             {return symbol(sym.E);}

    {diamond}       {return symbol(sym.DIA);}
    {box}           {return symbol(sym.BOX);}

    {implies}       {return symbol(sym.IMP);}
    {bi_implies}    {return symbol(sym.BIIMP);} 

    "!"             {return symbol(sym.NOT);}
    "*"             {return symbol(sym.AND);}
    "+"             {return symbol(sym.OR);}

/* "Variables" */

    {propositional_symbol}  {return symbol(sym.PROP, yytext());}
    {nominal}               {return symbol(sym.NOM, yytext());}
    

/* Whitespace */
	{WhiteSpace}	{/* just skip what was found, do nothing */}
}

[^]                    {throw new Error("Illegal character \""+yytext()+"\" at column: " + yycolumn + " line: " + yyline);}
