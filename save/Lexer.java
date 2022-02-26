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
            TypeTable tokenType;

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
                tokenType = TypeTable.IDENFR;
                // judge for reserved words
                for (int j = 0; j < resWords.length; j++) {
                    if (tokenContent.equals(resWords[j])) {
                        tokenType = TypeTable.values()[j];
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
                // TODO error:标识符数字开头 数字前导0
                tokenType = TypeTable.INTCON;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // "xxx"
            else if (code.charAt(charPtr) == '"') {
                tokenContent += code.charAt(charPtr);
                charPtr++;
                while (charPtr < codeLen && code.charAt(charPtr) != '"' && code.charAt(charPtr) != '\n') {
                    if (code.charAt(charPtr) == '\\') {
                        tokenContent += code.charAt(charPtr);
                        charPtr++;
                        if (charPtr >= codeLen) {
                            break;
                        }
                        else {
                            tokenContent += code.charAt(charPtr);
                            charPtr++;
                        }
                    }
                    else {
                        tokenContent += code.charAt(charPtr);
                        charPtr++;
                    }
                }
                if (charPtr < codeLen && code.charAt(charPtr) == '\"') {
                    tokenContent += code.charAt(charPtr);
                    tokenType = TypeTable.STRCON;
                    tokenList.add(new Token(tokenContent, tokenType, curLine));
                    charPtr++;
                }
                else {
                    // TODO
                }
            }

            // ! and !=
            else if (code.charAt(charPtr) == '!') {
                tokenContent = "!";
                tokenType = TypeTable.NOT;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "!=";
                    tokenType = TypeTable.NEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // &&
            else if (code.charAt(charPtr) == '&') {
                tokenContent = "&";
                tokenType = TypeTable.AND;
                charPtr++;
                if (code.charAt(charPtr) == '&') {
                    tokenContent = "&&";
                    tokenType = TypeTable.AND;
                    charPtr++;
                }
                else {
                    // TODO Error and
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }
            // ||
            else if (code.charAt(charPtr) == '|') {
                tokenContent = "|";
                tokenType = TypeTable.OR;
                charPtr++;
                if (code.charAt(charPtr) == '|') {
                    tokenContent = "||";
                    tokenType = TypeTable.OR;
                    charPtr++;
                }
                else {
                    // TODO Error or
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // +
            else if (code.charAt(charPtr) == '+') {
                tokenContent = "+";
                tokenType = TypeTable.PLUS;
                charPtr++;
                // TODO 带符号整数
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // -
            else if (code.charAt(charPtr) == '-') {
                tokenContent = "-";
                tokenType = TypeTable.MINU;
                charPtr++;
                // TODO 带符号整数
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // *
            else if (code.charAt(charPtr) == '*') {
                tokenContent = "*";
                tokenType = TypeTable.MULT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // / and annotation handle
            else if (code.charAt(charPtr) == '/') {
                tokenContent = "/";
                tokenType = TypeTable.DIV;
                charPtr++;
                //handle annotation
                if (code.charAt(charPtr) == '*') {
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
                    else {
                        // TODO
                    }
                }
                else if (code.charAt(charPtr) == '/') {
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
                else {
                    tokenList.add(new Token(tokenContent, tokenType, curLine));
                }
            }

            // %
            else if (code.charAt(charPtr) == '%') {
                tokenContent = "%";
                tokenType = TypeTable.MOD;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // < and <=
            else if (code.charAt(charPtr) == '<') {
                tokenContent = "<";
                tokenType = TypeTable.LSS;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "<=";
                    tokenType = TypeTable.LEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // > and >=
            else if (code.charAt(charPtr) == '>') {
                tokenContent = ">";
                tokenType = TypeTable.GRE;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = ">=";
                    tokenType = TypeTable.GEQ;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // = and ==
            else if (code.charAt(charPtr) == '=') {
                tokenContent = "=";
                tokenType = TypeTable.ASSIGN;
                charPtr++;
                if (code.charAt(charPtr) == '=') {
                    tokenContent = "==";
                    tokenType = TypeTable.EQL;
                    charPtr++;
                }
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ;
            else if (code.charAt(charPtr) == ';') {
                tokenContent = ";";
                tokenType = TypeTable.SEMICN;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ,
            else if (code.charAt(charPtr) == ',') {
                tokenContent = ",";
                tokenType = TypeTable.COMMA;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // (
            else if (code.charAt(charPtr) == '(') {
                tokenContent = "(";
                tokenType = TypeTable.LPARENT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // )
            else if (code.charAt(charPtr) == ')') {
                tokenContent = ")";
                tokenType = TypeTable.RPARENT;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // [
            else if (code.charAt(charPtr) == '[') {
                tokenContent = "[";
                tokenType = TypeTable.LBRACK;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // ]
            else if (code.charAt(charPtr) == ']') {
                tokenContent = "]";
                tokenType = TypeTable.RBRACK;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // {
            else if (code.charAt(charPtr) == '{') {
                tokenContent = "{";
                tokenType = TypeTable.LBRACE;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }

            // )
            else if (code.charAt(charPtr) == '}') {
                tokenContent = "}";
                tokenType = TypeTable.RBRACE;
                charPtr++;
                tokenList.add(new Token(tokenContent, tokenType, curLine));
            }








            else {
                charPtr++;
            }
        }
    }
}
