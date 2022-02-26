import java.util.ArrayList;

public class Lexer {
    private String code;
    private ArrayList<Token> tokenList;
    private String[] resWords = {"main", "const", "int", "break", "continue", "if", "else", "while", "getint", "printf", "return", "void"};

    public Lexer(String code) {
        this.code = code;
        this.tokenList = new ArrayList<>();
        this.analyze();
    }

    public ArrayList<Token> getTokenList() {
        return tokenList;
    }

    public void analyze() {
        int charPtr = 0;
        int curLine = 1;
        int codeLen = code.length();
        while (charPtr < codeLen) {
            String tokenContent = "";
            SymbolType tokenType;

            // filter space and get current line
            while (charPtr < codeLen && (code.charAt(charPtr) == '\n' || code.charAt(charPtr) == '\t' || code.charAt(charPtr) == '\r' || code.charAt(charPtr) == '\0' || code.charAt(charPtr) == ' ')) {
                if (code.charAt(charPtr) == '\n') curLine++;
                charPtr++;
            }
            if (charPtr >= codeLen) break;

            // identifier and reserved words
            if (Character.isLetter(code.charAt(charPtr)) || code.charAt(charPtr) == '_') {
                while (charPtr < codeLen && (Character.isLetter(code.charAt(charPtr)) || Character.isDigit(code.charAt(charPtr))) || code.charAt(charPtr) == '_' ) {
                    tokenContent += code.charAt(charPtr);
                    charPtr++;
                }
                tokenType = SymbolType.IDENFR;
                // judge for reserved words
                for (int j = 0; j < resWords.length; j++) {
                    if (tokenContent.equals(resWords[j])) {
                        tokenType = SymbolType.values()[j];
                        break;
                    }
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // digit number
            else if (Character.isDigit(code.charAt(charPtr))) {
                tokenContent = "";
                while (charPtr < codeLen && Character.isDigit(code.charAt(charPtr))) {
                    tokenContent += code.charAt(charPtr);
                    charPtr++;
                }
                tokenType = SymbolType.INTCON;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // "xxx"
            else if (code.charAt(charPtr) == '"') {
                tokenContent += code.charAt(charPtr);
                charPtr++;
                while (charPtr < codeLen && code.charAt(charPtr) != '"' && code.charAt(charPtr) != '\n') {
                    tokenContent += code.charAt(charPtr);
                    charPtr++;
                }
                if (charPtr < codeLen && code.charAt(charPtr) == '\"') {
                    tokenContent += code.charAt(charPtr);
                    tokenType = SymbolType.STRCON;
                    tokenList.add(new Token(tokenContent, tokenType, curLine));
                    charPtr++;
                }
            }

            // ! and !=
            else if (code.charAt(charPtr) == '!') {
                tokenContent = "!";
                tokenType = SymbolType.NOT;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "!=";
                    tokenType = SymbolType.NEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // &&
            else if (code.charAt(charPtr) == '&') {
                tokenContent = "&";
                tokenType = SymbolType.AND;
                charPtr++;
                if (code.charAt(charPtr) == '&') {
                    tokenContent = "&&";
                    tokenType = SymbolType.AND;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ||
            else if (code.charAt(charPtr) == '|') {
                tokenContent = "|";
                tokenType = SymbolType.OR;
                charPtr++;
                if (code.charAt(charPtr) == '|') {
                    tokenContent = "||";
                    tokenType = SymbolType.OR;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // +
            else if (code.charAt(charPtr) == '+') {
                tokenContent = "+";
                tokenType = SymbolType.PLUS;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // -
            else if (code.charAt(charPtr) == '-') {
                tokenContent = "-";
                tokenType = SymbolType.MINU;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // *
            else if (code.charAt(charPtr) == '*') {
                tokenContent = "*";
                tokenType = SymbolType.MULT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // / and annotation handle
            else if (code.charAt(charPtr) == '/') {
                tokenContent = "/";
                tokenType = SymbolType.DIV;
                charPtr++;

                // handle annotation
                if (code.charAt(charPtr) == '/') {
                    charPtr++;
                    while (charPtr < codeLen) {
                        if (code.charAt(charPtr) == '\n') {
                            curLine++;
                            charPtr++;
                            break;
                        }
                        charPtr++;
                    }
                }
                else if (code.charAt(charPtr) == '*') {
                    charPtr++;
                    while (charPtr < codeLen - 1 && (code.charAt(charPtr) != '*' || code.charAt(charPtr + 1) != '/')) {
                        if (code.charAt(charPtr) == '\n') {
                            curLine++;
                        }
                        charPtr++;
                    }
                    if (charPtr < codeLen - 1) {
                        charPtr += 2;
                    }
                }

                else {
                    tokenList.add(new Token(tokenContent, tokenType, curLine));
                }
            }

            // %
            else if (code.charAt(charPtr) == '%') {
                tokenContent = "%";
                tokenType = SymbolType.MOD;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // < and <=
            else if (code.charAt(charPtr) == '<') {
                tokenContent = "<";
                tokenType = SymbolType.LSS;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "<=";
                    tokenType = SymbolType.LEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // > and >=
            else if (code.charAt(charPtr) == '>') {
                tokenContent = ">";
                tokenType = SymbolType.GRE;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = ">=";
                    tokenType = SymbolType.GEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // = and ==
            else if (code.charAt(charPtr) == '=') {
                tokenContent = "=";
                tokenType = SymbolType.ASSIGN;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "==";
                    tokenType = SymbolType.EQL;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ;
            else if (code.charAt(charPtr) == ';') {
                tokenContent = ";";
                tokenType = SymbolType.SEMICN;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ,
            else if (code.charAt(charPtr) == ',') {
                tokenContent = ",";
                tokenType = SymbolType.COMMA;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // (
            else if (code.charAt(charPtr) == '(') {
                tokenContent = "(";
                tokenType = SymbolType.LPARENT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // )
            else if (code.charAt(charPtr) == ')') {
                tokenContent = ")";
                tokenType = SymbolType.RPARENT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // [
            else if (code.charAt(charPtr) == '[') {
                tokenContent = "[";
                tokenType = SymbolType.LBRACK;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ]
            else if (code.charAt(charPtr) == ']') {
                tokenContent = "]";
                tokenType = SymbolType.RBRACK;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // {
            else if (code.charAt(charPtr) == '{') {
                tokenContent = "{";
                tokenType = SymbolType.LBRACE;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // }
            else if (code.charAt(charPtr) == '}') {
                tokenContent = "}";
                tokenType = SymbolType.RBRACE;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            else {
                charPtr++;
            }
        }
    }
}
