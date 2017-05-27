grammar g;
ex : | ID* EOF ; // REPL
ID : [a-zA-Z0-9_.+\-]+ ID ; // tokens
WS : [ \t\r\n]+ -> skip ; // drop spaces