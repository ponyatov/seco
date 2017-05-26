grammar g;
ex : | ex ID ; // REPL
ID : [a-zA-Z0-9.+\-]+ ID ; // tokens
SPACES : [ \t\r\n]+ -> skip ;