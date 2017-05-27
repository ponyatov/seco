grammar g;
repl : | ID* EOF ;   // Read Eval Print Loop
ID   : [a-zA-Z0-9_.+\-]+  ; // tokens
WS   : [ \t\r\n]+ -> skip ; // drop spaces
