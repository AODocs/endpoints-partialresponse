grammar FieldsExpression;

//valid field names are anything but the syntax chars and whitespaces
//TODO actual valid key names might not allow such a broad range of chars
//TODO syntax chars might be escaped, to be checked a real API
//see https://github.com/antlr/grammars-v4/blob/master/json/JSON.g4
FIELDNAME: ~(',' | '/' | ')' | '(' | [ \t\r\n])+;

expression  : selection (',' selection)*;
selection : FIELDNAME ('/' FIELDNAME)* ('(' expression ')')?;
WS : [ \t\r\n]+ -> skip ;
