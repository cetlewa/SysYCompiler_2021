import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> tokenList;
    private ArrayList<String> parseList;
    public int tkPtr;
    public Token curToken;

    public Parser(ArrayList<Token> tokenList) {
        this.tokenList = tokenList;
        tokenList.add(new Token("end", SymbolType.END, 0));
        this.parseList = new ArrayList<>();
        this.tkPtr = 1;
        this.curToken = tokenList.get(0);
        this.analyze();
    }

    public void analyze() {
        CompUnit();
    }

    public void getToken() {
        parseList.add(curToken.getType() + " " + curToken.getContent());
        System.out.println(curToken.getType() + " " + curToken.getContent());
        curToken = tokenList.get(tkPtr);
        tkPtr++;
    }

    public ArrayList<String> getParseList() {
        return parseList;
    }

    public void error() {
        System.out.println("error");
        System.out.println(curToken.getType() + " " + curToken.getContent() + " " + curToken.getLine());
    }

    public void CompUnit() {
        while (curToken.type == SymbolType.CONSTTK ||
                 (curToken.type == SymbolType.INTTK && tokenList.get(tkPtr).type == SymbolType.IDENFR &&
                         (tokenList.get(tkPtr+1).type == SymbolType.LBRACK || tokenList.get(tkPtr+1).type == SymbolType.ASSIGN || tokenList.get(tkPtr+1).type == SymbolType.SEMICN || tokenList.get(tkPtr+1).type == SymbolType.COMMA))) {
            Decl();
        }
        while ((curToken.type == SymbolType.VOIDTK || curToken.type == SymbolType.INTTK) && tokenList.get(tkPtr).type == SymbolType.IDENFR && tokenList.get(tkPtr+1).type == SymbolType.LPARENT) {
            FuncDef();
        }
        if (curToken.type == SymbolType.INTTK && tokenList.get(tkPtr).type == SymbolType.MAINTK) {
            MainFuncDef();
        } else {
            error();
        }
        parseList.add("<CompUnit>");
    }

    public void Decl() {
        if (curToken.type == SymbolType.CONSTTK) {

            ConstDecl();
        }
        else if (curToken.type == SymbolType.INTTK) {
            VarDecl();
        } else {
            error();
        }
    }

    public void FuncDef() {
        FuncType();
        if (curToken.type == SymbolType.IDENFR) {
            getToken();
            if (curToken.type == SymbolType.LPARENT) {
                getToken();
                if (curToken.type == SymbolType.RPARENT) {
                    getToken();
                    Block();
                }
                else {
                    FuncFParams();
                    if (curToken.type == SymbolType.RPARENT) {
                        getToken();
                        Block();
                    } else {
                        error();
                    }
                }
            }
        } else {
            error();
        }

        parseList.add("<FuncDef>");
    }

    public void FuncType() {
        if (curToken.type == SymbolType.VOIDTK || curToken.type == SymbolType.INTTK) {
            getToken();

        } else {
            error();
        }
        parseList.add("<FuncType>");
    }

    public void MainFuncDef() {
        getToken();
        getToken();
        if (curToken.type == SymbolType.LPARENT) {
            getToken();
            if (curToken.type == SymbolType.RPARENT) {
                getToken();
                Block();
            } else {
                error();
            }
        } else {
            error();
        }
        parseList.add("<MainFuncDef>");
    }

    public void Block() {
        if (curToken.type == SymbolType.LBRACE) {
            getToken();
            if (curToken.type == SymbolType.RBRACE) {
                getToken();
            }
            else {
                while (curToken.type != SymbolType.RBRACE) {
                    BlockItem();
                }
                if (curToken.type == SymbolType.RBRACE) {
                    getToken();
                } else {
                    error();
                }
            }
        } else {
            error();
        }
        parseList.add("<Block>");
    }

    public void BlockItem() {
        if (curToken.type == SymbolType.CONSTTK || curToken.type == SymbolType.INTTK) {
            Decl();
        }
        else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.IFTK || curToken.type == SymbolType.WHILETK
                    || curToken.type == SymbolType.BREAKTK || curToken.type == SymbolType.CONTINUETK || curToken.type == SymbolType.RETURNTK
                    || curToken.type == SymbolType.PRINTFTK || curToken.type == SymbolType.SEMICN || curToken.type == SymbolType.LBRACE
                    || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU
                    || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            Stmt();
        }
    }

    public void Stmt() {
        if (curToken.type == SymbolType.IFTK) {
            getToken();
            if (curToken.type == SymbolType.LPARENT) {
                getToken();
                Cond();
                if (curToken.type == SymbolType.RPARENT) {
                    getToken();
                    Stmt();
                } else {
                    error();
                }
                while (curToken.type == SymbolType.ELSETK) {
                    getToken();
                    Stmt();
                }
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.WHILETK) {
            getToken();
            if (curToken.type == SymbolType.LPARENT) {
                getToken();
                Cond();
                if (curToken.type == SymbolType.RPARENT) {
                    getToken();
                    Stmt();
                } else {
                    error();
                }
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.BREAKTK) {
            getToken();
            if (curToken.type == SymbolType.SEMICN) {
                getToken();
            }
        }
        else if (curToken.type == SymbolType.CONTINUETK) {
            getToken();
            if (curToken.type == SymbolType.SEMICN) {
                getToken();
            }
        }
        else if (curToken.type == SymbolType.RETURNTK) {
            getToken();
            if (curToken.type == SymbolType.SEMICN) {
                getToken();
            }
            else if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
                Exp();
                if (curToken.type == SymbolType.SEMICN) {
                    getToken();
                } else {
                    error();
                }
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.PRINTFTK) {
            getToken();
            if (curToken.type == SymbolType.LPARENT) {
                getToken();
                if (curToken.type == SymbolType.STRCON) {
                    getToken();
                    while (curToken.type == SymbolType.COMMA) {
                        getToken();
                        Exp();
                    }
                    if (curToken.type == SymbolType.RPARENT) {
                        getToken();
                        if (curToken.type == SymbolType.SEMICN) {
                            getToken();
                        } else {
                            error();
                        }
                    } else {
                        error();
                    }
                } else {
                    error();
                }
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.LBRACE) {
            Block();
        }
        else if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            Exp();
            if (curToken.type == SymbolType.SEMICN) {
                getToken();
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.IDENFR) {
            int ptr = tkPtr;
            int assignFlag = 0;
            Token temp = tokenList.get(ptr);
            while (temp.type != SymbolType.SEMICN) {
                if (temp.type == SymbolType.ASSIGN) {
                    assignFlag = 1;
                    break;
                }
                ptr++;
                temp = tokenList.get(ptr);
            }
            if (assignFlag == 1) {
                if (tokenList.get(ptr + 1).type == SymbolType.GETINTTK) {
                    LVal();
                    if (curToken.type == SymbolType.ASSIGN) {
                        getToken();
                        if (curToken.type == SymbolType.GETINTTK) {
                            getToken();
                            if (curToken.type == SymbolType.LPARENT) {
                                getToken();
                                if (curToken.type == SymbolType.RPARENT) {
                                    getToken();
                                    if (curToken.type == SymbolType.SEMICN) {
                                        getToken();
                                    }
                                } else {
                                    error();
                                }
                            } else {
                                error();
                            }
                        } else {
                            error();
                        }
                    } else {
                        error();
                    }
                }
                // TODO
                else if (tokenList.get(ptr + 1).type == SymbolType.LPARENT || tokenList.get(ptr + 1).type == SymbolType.IDENFR || tokenList.get(ptr + 1).type == SymbolType.PLUS || tokenList.get(ptr + 1).type == SymbolType.MINU || tokenList.get(ptr + 1).type == SymbolType.NOT || tokenList.get(ptr + 1).type == SymbolType.INTCON) {
                    LVal();
                    if (curToken.type == SymbolType.ASSIGN) {
                        getToken();
                        if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                            Exp();
                            if (curToken.type == SymbolType.SEMICN) {
                                getToken();
                            } else {
                                error();
                            }
                        } else {
                            error();
                        }
                    } else {
                        error();
                    }
                }
                else {
                    error();
                }
            }
            else {
                Exp();
                if (curToken.type == SymbolType.SEMICN) {
                    getToken();
                } else {
                    error();
                }
            }
        }
        else if (curToken.type == SymbolType.SEMICN) {
            getToken();
        }
        else {
            error();
        }
        parseList.add("<Stmt>");
    }

    public void Cond() {
        if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            LOrExp();
        }
        parseList.add("<Cond>");
    }

    public void LOrExp() {
        LAndExp();
        parseList.add("<LOrExp>");
        while (curToken.type == SymbolType.OR) {
            getToken();
            LAndExp();
            parseList.add("<LOrExp>");
        }
    }

    public void LAndExp() {
        EqExp();
        parseList.add("<LAndExp>");
        while (curToken.type == SymbolType.AND) {
            getToken();
            EqExp();
            parseList.add("<LAndExp>");
        }
    }

    public void EqExp() {
        RelExp();
        parseList.add("<EqExp>");
        while (curToken.type == SymbolType.EQL || curToken.type == SymbolType.NEQ) {
            getToken();
            RelExp();
            parseList.add("<EqExp>");
        }
    }

    public void RelExp() {
        AddExp();
        parseList.add("<RelExp>");
        while (curToken.type == SymbolType.LSS || curToken.type == SymbolType.LEQ || curToken.type == SymbolType.GRE || curToken.type == SymbolType.GEQ) {
            getToken();
            AddExp();
            parseList.add("<RelExp>");
        }
    }

    public void ConstDecl() {
        if (curToken.type == SymbolType.CONSTTK) {
            getToken();
            if (curToken.type == SymbolType.INTTK) {
                getToken();
                ConstDef();
                while (curToken.type == SymbolType.COMMA) {
                    getToken();
                    ConstDef();
                }
                if (curToken.type == SymbolType.SEMICN) {
                    getToken();
                    parseList.add("<ConstDecl>");
                } else {
                    error();
                }
            } else {
                error();
            }
        } else {
            error();
        }
    }

    public void ConstDef() {
        if (curToken.type == SymbolType.IDENFR) {
            getToken();
            while (curToken.type == SymbolType.LBRACK) {
                getToken();
                ConstExp();
                if (curToken.type == SymbolType.RBRACK) {
                    getToken();
                } else {
                    error();
                }
            }
            if (curToken.type == SymbolType.ASSIGN) {
                getToken();
                ConstInitVal();
            } else {
                error();
            }
            parseList.add("<ConstDef>");
        } else {
            error();
        }
    }

    public void VarDecl() {
        if (curToken.type == SymbolType.INTTK) {
            getToken();
            VarDef();
            while (curToken.type == SymbolType.COMMA) {
                getToken();
                VarDef();
            }
            if (curToken.type == SymbolType.SEMICN) {
                getToken();
                parseList.add("<VarDecl>");
            } else {
                error();
            }
        } else {
            error();
        }
    }

    public void VarDef() {
        if (curToken.type == SymbolType.IDENFR) {
            getToken();
            while (curToken.type == SymbolType.LBRACK) {
                getToken();
                ConstExp();
                if (curToken.type == SymbolType.RBRACK) {
                    getToken();
                } else {
                    error();
                }
            }
            if (curToken.type == SymbolType.ASSIGN) {
                getToken();
                InitVal();
                if (curToken.type != SymbolType.COMMA && curToken.type != SymbolType.SEMICN) {
                    error();
                }
            }
            parseList.add("<VarDef>");
        } else {
            error();
        }
    }

    // TODO
    public void ConstInitVal() {
        if (curToken.type == SymbolType.LBRACE) {
            getToken();
            if (curToken.type == SymbolType.LBRACE || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                ConstInitVal();
                while (curToken.type == SymbolType.COMMA) {
                    getToken();
                    ConstInitVal();
                }
                if (curToken.type == SymbolType.RBRACE) {
                    getToken();
                }
            }
            else if (curToken.type == SymbolType.RBRACE) {
                getToken();
            }
            else {
                error();
            }
        }
        else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            ConstExp();
        }
        else {
            error();
        }
        parseList.add("<ConstInitVal>");
    }

    public void InitVal() {
        if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            Exp();
            parseList.add("<InitVal>");
        }
        else if (curToken.type == SymbolType.LBRACE) {
            getToken();
            if (curToken.type == SymbolType.RBRACE) {
                getToken();
            }
            else if (curToken.type == SymbolType.LBRACE || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                InitVal();
                while (curToken.type == SymbolType.COMMA) {
                    getToken();
                    InitVal();
                }
                if (curToken.type == SymbolType.RBRACE) {
                    getToken();
                    parseList.add("<InitVal>");
                } else {
                    error();
                }
            }
        }
        else {
            error();
        }
//        parseList.add("<InitVal>");
    }

    public void ConstExp() {
        AddExp();
        parseList.add("<ConstExp>");
    }

    public void AddExp() {
        MulExp();
        parseList.add("<AddExp>");
        while (curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU) {
            getToken();
            MulExp();
            parseList.add("<AddExp>");
        }
    }

    public void MulExp() {
        UnaryExp();
        parseList.add("<MulExp>");
        while (curToken.type == SymbolType.MULT || curToken.type == SymbolType.DIV || curToken.type == SymbolType.MOD) {
            getToken();
            UnaryExp();
            parseList.add("<MulExp>");
        }
    }

    public void UnaryExp() {
        if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.INTCON || (curToken.type == SymbolType.IDENFR && tokenList.get(tkPtr).type != SymbolType.LPARENT)) {
            PrimaryExp();
        }
        else if (curToken.type == SymbolType.IDENFR && tokenList.get(tkPtr).type == SymbolType.LPARENT) {
            getToken();
            getToken();
            if (curToken.type == SymbolType.RPARENT) {
                getToken();
            }
            else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                FuncRParams();
                if (curToken.type == SymbolType.RPARENT) {
                    getToken();
                } else {
                    error();
                }
            }
            else {
                error();
            }
        }
        else if (curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            UnaryOp();
            UnaryExp();
        }
        else {
            error();
        }
        parseList.add("<UnaryExp>");
    }

    public void UnaryOp() {
        getToken();
        parseList.add("<UnaryOp>");
    }

    public void PrimaryExp() {
        if (curToken.type == SymbolType.LPARENT) {
            getToken();
            Exp();
            if (curToken.type == SymbolType.RPARENT) {
                getToken();
            } else {
                error();
            }
        }
        else if (curToken.type == SymbolType.IDENFR) {
            LVal();
        }
        else if (curToken.type == SymbolType.INTCON) {
            Number();
        }
        else {
            error();
        }
        parseList.add("<PrimaryExp>");
    }

    public void Number() {
        getToken();
        parseList.add("<Number>");
    }

    public void LVal() {
        getToken();
        while (curToken.type == SymbolType.LBRACK) {
            getToken();
            Exp();
            if (curToken.type == SymbolType.RBRACK) {
                getToken();
            } else {
                error();
            }
        }
        parseList.add("<LVal>");
    }

    public void FuncFParams() {
        FuncFParam();
        while (curToken.type == SymbolType.COMMA) {
            getToken();
            FuncFParam();
        }
        parseList.add("<FuncFParams>");
    }

    public void FuncFParam() {
        if (curToken.type == SymbolType.INTTK) {
            getToken();
            if (curToken.type == SymbolType.IDENFR) {
                getToken();
                if (curToken.type == SymbolType.LBRACK) {
                    getToken();
                    if (curToken.type == SymbolType.RBRACK) {
                        getToken();
                        while (curToken.type == SymbolType.LBRACK) {
                            getToken();
                            ConstExp();
                            if (curToken.type == SymbolType.RBRACK) {
                                getToken();
                            } else {
                                error();
                            }
                        }
                        parseList.add("<FuncFParam>");
                    } else {
                        error();
                    }
                }
                else if (curToken.type == SymbolType.COMMA || curToken.type == SymbolType.RPARENT) {
                    parseList.add("<FuncFParam>");
                }
                else {
                    error();
                }
            } else {
                error();
            }
        } else {
            error();
        }
    }

    public void FuncRParams() {
        Exp();
        while (curToken.type == SymbolType.COMMA) {
            getToken();
            Exp();
        }
        parseList.add("<FuncRParams>");
    }

    public void Exp() {
        AddExp();
        parseList.add("<Exp>");
    }

}
