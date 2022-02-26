import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private ArrayList<Token> tokenList;
    private ArrayList<String> parseList;
    private int tkPtr;
    private Token curToken;
    private int level;
    private int tbPtr;
    private ArrayList<Node> curTable;
    private ArrayList<ArrayList<Node>> globalSymbolTable;
    private Error error;
    private Node CompUnit;
    private Node garbage = new Node("garbage");
    private int valueIndex;

    public Parser(ArrayList<Token> tokenList) {
        this.tokenList = tokenList;
        tokenList.add(new Token("end", SymbolType.END, 0));
        this.parseList = new ArrayList<>();
        this.tkPtr = 1;
        this.curToken = tokenList.get(0);
        this.level = 1;
        this.globalSymbolTable = new ArrayList<>();
        globalSymbolTable.add(new ArrayList<>());
        this.tbPtr = 1;
        this.curTable = globalSymbolTable.get(0);
        this.error = new Error();
        this.CompUnit = new Node("CompUnit");
        valueIndex = 0;
        this.analyze();
        System.out.println(">>>>>>>>>> Parser End <<<<<<<<<<");
    }

    public void analyze() {
        CompUnit(CompUnit);
//        recrusiveOutputTree(CompUnit);
    }

    public ArrayList<String> getErrorMessage() {
        return this.error.getErrorMessage();
    }

    public Node getCompUnit() {
        return this.CompUnit;
    }

    private void insertTable(Node node) {
        curTable.add(node);
//        System.out.println("<insertTable> " + node.getName() + " " + node.getType() + " " + level);
    }

    // 0:success 1:未定义的名字 2:函数参数个数不匹配 3:函数参数类型不匹配 4:不能改变常量的值
    private int searchTable(Node node) {
        for (int i = globalSymbolTable.size() - 1; i >= 0; i--) {
            for (Node gnd : globalSymbolTable.get(i)) {
                if (gnd.getName() != null && gnd.getName().equals(node.getName())) {
                    if (node.getType() == NodeInfo.VAR && gnd.getType() == NodeInfo.CONST) {
                        return 4;
                    }
                    if (gnd.getType() == node.getType() && node.getType() == NodeInfo.VAR) {
                        return 0;
                    }
                    if (gnd.getType() == node.getType() /*&& node.getType() == NodeInfo.INTFUN*/) {
                        if (gnd.getParamNum() != node.getParamNum()) {
                            return 2;
                        } else {
                            if (node.getParamNum() == 0) {
                                return 0;
                            } else {
                                for (int j = 0; j < node.getParamNum(); j++) {
                                    if (!Objects.equals(gnd.getParamDim().get(j), node.getParamDim().get(j))) {
                                        return 3;
                                    }
                                }
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        return 1;
    }

    private boolean checkTable(Node node) {
        for (Node curtbNode : curTable) {
            if (curtbNode.getName() != null && curtbNode.getName().equals(node.getName())
                && !(curtbNode.getType() == NodeInfo.CONST && node.getType() == NodeInfo.INTFUN)
                && !(curtbNode.getType() == NodeInfo.INTFUN && node.getType() == NodeInfo.CONST)
                && !(curtbNode.getType() == NodeInfo.CONST && node.getType() == NodeInfo.VOIDFUN)
                && !(curtbNode.getType() == NodeInfo.VOIDFUN && node.getType() == NodeInfo.CONST)
                && !(curtbNode.getType() == NodeInfo.VAR && node.getType() == NodeInfo.INTFUN)
                && !(curtbNode.getType() == NodeInfo.INTFUN && node.getType() == NodeInfo.VAR)
                && !(curtbNode.getType() == NodeInfo.VAR && node.getType() == NodeInfo.VOIDFUN)
                && !(curtbNode.getType() == NodeInfo.VOIDFUN && node.getType() == NodeInfo.VAR)) {
                return false;
            }
        }
        return true;
    }

    private void removeTable() {
        curTable = globalSymbolTable.get(tbPtr - 2);
        globalSymbolTable.remove(tbPtr - 1);
        tbPtr--;
    }

    private void addTable() {
        globalSymbolTable.add(new ArrayList<>());
        curTable = globalSymbolTable.get(tbPtr);
        tbPtr++;
    }

    public void recrusiveOutputTree(Node node) {
        for (Node child : node.getChildNode()) {
            recrusiveOutputTree(child);
        }
        System.out.println(node.getNodeName());
    }

    public Node getToken(Node node) {
        parseList.add(curToken.getType() + " " + curToken.getContent());
        Node tNode = new Node(curToken);
        addRelation(node, tNode);
//        System.out.println(curToken.getType() + " " + curToken.getContent() + " " + curToken.getLine());
        curToken = tokenList.get(tkPtr);
        tkPtr++;
        return tNode;
    }

    public void addRelation(Node parent, Node child) {
        parent.addChildNode(child);
        child.setParNode(parent);
    }

    public ArrayList<String> getParseList() {
        return parseList;
    }

    public void error() {
//        System.out.println("error");
//        System.out.println(curToken.getType() + " " + curToken.getContent() + " " + curToken.getLine());
    }

    public void CompUnit(Node CompUnit) {
//        Node CompUnit = new Node("CompUnit");
        while (curToken.type == SymbolType.CONSTTK ||
                (curToken.type == SymbolType.INTTK && tokenList.get(tkPtr).type == SymbolType.IDENFR &&
                        (tokenList.get(tkPtr + 1).type == SymbolType.LBRACK || tokenList.get(tkPtr + 1).type == SymbolType.ASSIGN || tokenList.get(tkPtr + 1).type == SymbolType.SEMICN || tokenList.get(tkPtr + 1).type == SymbolType.COMMA))) {
            Decl(CompUnit);
        }
        while ((curToken.type == SymbolType.VOIDTK || curToken.type == SymbolType.INTTK) && tokenList.get(tkPtr).type == SymbolType.IDENFR && tokenList.get(tkPtr + 1).type == SymbolType.LPARENT) {
            FuncDef(CompUnit);
        }
        if (curToken.type == SymbolType.INTTK && tokenList.get(tkPtr).type == SymbolType.MAINTK) {
            MainFuncDef(CompUnit);
        } else {
            error();
        }
        parseList.add("<CompUnit>");
    }

    public void Decl(Node node) {
        Node Decl = new Node("Decl");
        addRelation(node, Decl);
        if (curToken.type == SymbolType.CONSTTK) {
            ConstDecl(Decl);
        } else if (curToken.type == SymbolType.INTTK) {
            VarDecl(Decl);
        } else {
            error();
        }
    }

    public void FuncDef(Node node) {
        Node FuncDef = new Node("FuncDef");
        addRelation(node, FuncDef);
        int type = FuncType(FuncDef);
        if (curToken.type == SymbolType.IDENFR) {
            String name = curToken.getContent();
            int paramNum = 0;
            Node idenfr = getToken(FuncDef);
            idenfr.setNodeName("Ident");
            idenfr.setNodeInfo(name, type, level, 0, paramNum);
            if (checkTable(idenfr)) {
                insertTable(idenfr);
            } else {
                error.addErrorMessage(idenfr.getToken().getLine(), 'b');
            }
            addTable();
            if (curToken.type == SymbolType.LPARENT) {
                level++;
                getToken(FuncDef).setNodeName("(");
                if (curToken.type == SymbolType.RPARENT) {
                    level--;
                    paramNum = 0;
                    getToken(FuncDef).setNodeName(")");
                } else if (curToken.type == SymbolType.INTTK) {
                    paramNum = FuncFParams(FuncDef, idenfr);
                    if (curToken.type == SymbolType.RPARENT) {
                        level--;
                        getToken(FuncDef).setNodeName(")");
                    } else {
                        level--;
                        Node tNode = new Node(new Token(")", SymbolType.RPARENT, idenfr.getToken().getLine()));
                        addRelation(FuncDef, tNode);
                        tNode.setNodeName(")");
                        error.addErrorMessage(idenfr.getToken().getLine(), 'j');
                    }
                } else {
                    level--;
                    Node tNode = new Node(new Token(")", SymbolType.RPARENT, idenfr.getToken().getLine()));
                    addRelation(FuncDef, tNode);
                    tNode.setNodeName(")");
                    error.addErrorMessage(idenfr.getToken().getLine(), 'j');
                    paramNum = 0;
                }
                idenfr.setParamNum(paramNum);
                Block(FuncDef, (type == NodeInfo.INTFUN));
            }
        } else {
            error();
        }
        parseList.add("<FuncDef>");
    }

    public int FuncType(Node node) {
        Node FuncType = new Node("FuncType");
        addRelation(node, FuncType);
        int type = 0;
        if (curToken.type == SymbolType.VOIDTK || curToken.type == SymbolType.INTTK) {
            type = (curToken.type == SymbolType.VOIDTK) ? NodeInfo.VOIDFUN : NodeInfo.INTFUN;
            FuncType.setType(type);
            getToken(FuncType).setNodeName("FType");
        } else {
            error();
        }
        parseList.add("<FuncType>");
        return type;
    }

    public void MainFuncDef(Node node) {
        Node MainFuncDef = new Node("MainFuncDef");
        addRelation(node, MainFuncDef);
        getToken(MainFuncDef).setNodeName("FType");
        Node idenfr = getToken(MainFuncDef);
        String name = "main";
        int type = NodeInfo.INTFUN;
        idenfr.setNodeName("main");
        idenfr.setNodeInfo(name, type, level, 0, 0);
        if (checkTable(idenfr)) {
            insertTable(idenfr);
        } else {
            error.addErrorMessage(idenfr.getToken().getLine(), 'b');
        }
        addTable();
        if (curToken.type == SymbolType.LPARENT) {
            getToken(MainFuncDef).setNodeName("(");
            if (curToken.type == SymbolType.RPARENT) {
                getToken(MainFuncDef).setNodeName(")");
                Block(MainFuncDef, true);
            } else {
                error();
            }
        } else {
            error();
        }
        parseList.add("<MainFuncDef>");
    }

    public void Block(Node node, boolean isIntFunc) {
        level++;
        Node Block = new Node("Block");
        addRelation(node, Block);
        if (curToken.type == SymbolType.LBRACE) {
            getToken(Block).setNodeName("{");
            if (!Block.getParNode().getNodeName().equals("FuncDef") && !Block.getParNode().getNodeName().equals("MainFuncDef")) {
                addTable();
            }
            if (curToken.type == SymbolType.RBRACE) {
                Node rbrace = getToken(Block);
                rbrace.setNodeName("}");
                if (isIntFunc) {
                    boolean flag = false;
                    for (Node ctNode : curTable) {
                        if (ctNode.getToken().type == SymbolType.RETURNTK) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        error.addErrorMessage(rbrace.getToken().getLine(), 'g');
                    }
                }
                removeTable();
            } else {
                while (curToken.type != SymbolType.RBRACE) {
                    BlockItem(Block);
                }
                if (curToken.type == SymbolType.RBRACE) {
                    Node rbrace = getToken(Block);
                    rbrace.setNodeName("}");
                    if (isIntFunc) {
                        boolean flag = false;
                        for (Node ctNode : curTable) {
                            if (ctNode.getToken().type == SymbolType.RETURNTK) {
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            error.addErrorMessage(rbrace.getToken().getLine(), 'g');
                        }
                    }
                    removeTable();
                } else {
                    error();
                }
            }
        } else {
            error();
        }
        parseList.add("<Block>");
        level--;
    }

    public void BlockItem(Node node) {
        Node BlockItem = new Node("BlockItem");
        addRelation(node, BlockItem);
        if (curToken.type == SymbolType.CONSTTK || curToken.type == SymbolType.INTTK) {
            Decl(BlockItem);
        } else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.IFTK || curToken.type == SymbolType.WHILETK
                || curToken.type == SymbolType.BREAKTK || curToken.type == SymbolType.CONTINUETK || curToken.type == SymbolType.RETURNTK
                || curToken.type == SymbolType.PRINTFTK || curToken.type == SymbolType.SEMICN || curToken.type == SymbolType.LBRACE
                || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU
                || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            Stmt(BlockItem);
        }
    }

    public void Stmt(Node node) {
        Node Stmt = new Node("Stmt");
        addRelation(node, Stmt);
        if (curToken.type == SymbolType.IFTK) {
            Node ifToken = getToken(Stmt);
            insertTable(ifToken);
            ifToken.setNodeInfo("if", 0, level, 0, 0);
//            addTable();
            if (curToken.type == SymbolType.LPARENT) {
                getToken(Stmt).setNodeName("(");
                Cond(Stmt);
                if (curToken.type == SymbolType.RPARENT) {
                    getToken(Stmt);
                } else {
                    Node tNode = new Node(new Token(")", SymbolType.RPARENT, ifToken.getToken().getLine()));
                    addRelation(Stmt, tNode);
                    error.addErrorMessage(ifToken.getToken().getLine(), 'j');
                }
                boolean notBlock = curToken.type != SymbolType.LBRACE;
                if (notBlock) {
                    addTable();
                    Stmt(Stmt);
                    removeTable();
                } else {
                    Stmt(Stmt);
                }
                while (curToken.type == SymbolType.ELSETK) {
                    getToken(Stmt);
                    notBlock = curToken.type != SymbolType.LBRACE;
                    if (notBlock) {
                        addTable();
                        Stmt(Stmt);
                        removeTable();
                    } else {
                        Stmt(Stmt);
                    }
                }
            } else {
                error();
            }
        } else if (curToken.type == SymbolType.WHILETK) {
            Node whileToken = getToken(Stmt);
            insertTable(whileToken);
            whileToken.setNodeInfo("while", 0, level, 0, 0);
//            addTable();
            if (curToken.type == SymbolType.LPARENT) {
                getToken(Stmt).setNodeName("(");
                Cond(Stmt);
                if (curToken.type == SymbolType.RPARENT) {
                    getToken(Stmt);
                } else {
                    Node tNode = new Node(new Token(")", SymbolType.RPARENT, whileToken.getToken().getLine()));
                    addRelation(Stmt, tNode);
                    error.addErrorMessage(whileToken.getToken().getLine(), 'j');
                }
                boolean notBlock = curToken.type != SymbolType.LBRACE;
                if (notBlock) {
                    addTable();
                    Stmt(Stmt);
                    removeTable();
                } else {
                    Stmt(Stmt);
                }
            } else {
                error();
            }
        } else if (curToken.type == SymbolType.BREAKTK) {
            Node nodePtr = getToken(Stmt);
            if (tbPtr - 2 <= 0) {
                error.addErrorMessage(nodePtr.getToken().getLine(), 'm');
            } else {
                boolean hasWhile = false;
                for (int i = tbPtr - 2; i >= 0; i--) {
                    if (globalSymbolTable.get(i).size() != 0
                           && globalSymbolTable.get(i).get(globalSymbolTable.get(i).size() - 1).getName().equals("while")) {
                        hasWhile = true;
                        break;
                    }
                }
                if (!hasWhile) {
                    error.addErrorMessage(nodePtr.getToken().getLine(), 'm');
                }
            }
            if (curToken.type == SymbolType.SEMICN) {
                getToken(Stmt);
            } else {
                Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                addRelation(Stmt, tNode);
                error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
            }
        } else if (curToken.type == SymbolType.CONTINUETK) {
            Node nodePtr = getToken(Stmt);
            if (tbPtr - 2 <= 0) {
                error.addErrorMessage(nodePtr.getToken().getLine(), 'm');
            } else {
                boolean hasWhile = false;
                for (int i = tbPtr - 2; i >= 0; i--) {
                    if (globalSymbolTable.get(i).get(globalSymbolTable.get(i).size() - 1).getName().equals("while")) {
                        hasWhile = true;
                        break;
                    }
                }
                if (!hasWhile) {
                    error.addErrorMessage(nodePtr.getToken().getLine(), 'm');
                }
            }
            if (curToken.type == SymbolType.SEMICN) {
                getToken(Stmt);
            } else {
                Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                addRelation(Stmt, tNode);
                error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
            }
        } else if (curToken.type == SymbolType.RETURNTK) {
            Node nodePtr = getToken(Stmt);
            nodePtr.setNodeInfo("return", 0, level, 0,0);
            nodePtr.setNodeName("return");
            insertTable(nodePtr);
            boolean hasValue = false;
            if (curToken.type == SymbolType.SEMICN) {
                hasValue = false;
                getToken(Stmt).setNodeName(";");
            } else if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
                hasValue = true;
                Exp(Stmt, garbage);
                if (curToken.type == SymbolType.SEMICN) {
                    getToken(Stmt).setNodeName(";");
                } else {
                    Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                    addRelation(Stmt, tNode);
                    tNode.setNodeName(";");
                    error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
                }
            } else {
                Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                addRelation(Stmt, tNode);
                tNode.setNodeName(";");
                error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
            }
            for (int i = tbPtr - 2; i >= 0; i--) {
                if (globalSymbolTable.get(i).get(globalSymbolTable.get(i).size() - 1).getType() == NodeInfo.INTFUN
                    || globalSymbolTable.get(i).get(globalSymbolTable.get(i).size() - 1).getType() == NodeInfo.VOIDFUN) {
                    if (globalSymbolTable.get(i).get(globalSymbolTable.get(i).size() - 1).getType() == NodeInfo.VOIDFUN
                        && hasValue) {
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'f');
                    }
                    break;
                }
            }
        } else if (curToken.type == SymbolType.PRINTFTK) {
            Node nodePtr = getToken(Stmt);
            nodePtr.setNodeName("printf");
            if (curToken.type == SymbolType.LPARENT) {
                getToken(Stmt).setNodeName("(");
                if (curToken.type == SymbolType.STRCON) {
                    String formatChar = "%d";
                    String NormalChar = "\\s|!|\\(|\\)|\\*|\\+|,|-|\\.|/|\\w|:|;|<|=|>|\\?|@|\\[|]|\\^|_|`|~|\\\\n";
                    String Char = "((" + formatChar + ")|(" + NormalChar + "))";
                    String formatString = "\"" + Char + "*\"";
                    Pattern p1 = Pattern.compile(formatString);
                    Matcher m1 = p1.matcher(curToken.getContent());
                    Pattern p2 = Pattern.compile(Char);
                    Matcher m2 = p2.matcher(curToken.getContent().substring(1, curToken.getContent().length() - 1));
                    int fstrNum = 0;
                    int expNum = 0;
                    if (!m1.matches()) {
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'a');
                    }
                    while (m2.find()) {
                        if (m2.group(0).equals("%d")) {
                            fstrNum++;
                        }
                    }
                    getToken(Stmt).setNodeName("FormatString");
                    while (curToken.type == SymbolType.COMMA) {
                        getToken(Stmt).setNodeName(",");
                        Exp(Stmt, garbage);
                        expNum++;
                    }
                    if (expNum != fstrNum) {
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'l');
                    }
                    if (curToken.type == SymbolType.RPARENT) {
                        getToken(Stmt).setNodeName(")");
                    } else {
                        Node tNode = new Node(new Token(")", SymbolType.RPARENT, nodePtr.getToken().getLine()));
                        addRelation(Stmt, tNode);
                        tNode.setNodeName(")");
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'j');
                    }
                    if (curToken.type == SymbolType.SEMICN) {
                        getToken(Stmt).setNodeName(";");
                    } else {
                        Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                        addRelation(Stmt, tNode);
                        tNode.setNodeName(";");
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
                    }
                } else {
                    error();
                }
            } else {
                error();
            }
        } else if (curToken.type == SymbolType.LBRACE) {
            Block(Stmt, false);
        } else if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            int line = curToken.getLine();
            Exp(Stmt, garbage);
            if (curToken.type == SymbolType.SEMICN) {
                getToken(Stmt).setNodeName(";");
            } else {
                Node tNode = new Node(new Token(";", SymbolType.SEMICN, line));
                addRelation(node, tNode);
                tNode.setNodeName(";");
                error.addErrorMessage(line, 'i');
            }
        } else if (curToken.type == SymbolType.IDENFR) {
            int ptr = tkPtr;
            int assignFlag = 0;
            Token temp = tokenList.get(ptr);
            while (temp.type != SymbolType.SEMICN && ptr < tokenList.size() - 1) {
                if (temp.type == SymbolType.ASSIGN) {
                    assignFlag = 1;
                    break;
                }
                ptr++;
                temp = tokenList.get(ptr);
            }
            if (assignFlag == 1) {
                if (tokenList.get(ptr + 1).type == SymbolType.GETINTTK) {
                    Node nodePtr = LVal(Stmt, garbage);
                    nodePtr.setNodeName("LVal");
                    if (searchTable(nodePtr) == 4) {
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'h');
                    }
                    if (curToken.type == SymbolType.ASSIGN) {
                        getToken(Stmt).setNodeName("=");
                        if (curToken.type == SymbolType.GETINTTK) {
                            getToken(Stmt).setNodeName("getint");
                            if (curToken.type == SymbolType.LPARENT) {
                                getToken(Stmt).setNodeName("(");
                                if (curToken.type == SymbolType.RPARENT) {
                                    getToken(Stmt).setNodeName(")");
                                    if (curToken.type == SymbolType.SEMICN) {
                                        getToken(Stmt).setNodeName(";");
                                    } else {
                                        Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                                        addRelation(Stmt, tNode);
                                        tNode.setNodeName(";");
                                        error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
                                    }
                                } else {
                                    Node tNode = new Node(new Token(")", SymbolType.RPARENT, nodePtr.getToken().getLine()));
                                    addRelation(Stmt, tNode);
                                    tNode.setNodeName(")");
                                    error.addErrorMessage(nodePtr.getToken().getLine(), 'j');
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
                else if (tokenList.get(ptr + 1).type == SymbolType.LPARENT || tokenList.get(ptr + 1).type == SymbolType.IDENFR || tokenList.get(ptr + 1).type == SymbolType.PLUS || tokenList.get(ptr + 1).type == SymbolType.MINU || tokenList.get(ptr + 1).type == SymbolType.NOT || tokenList.get(ptr + 1).type == SymbolType.INTCON) {
                    Node nodePtr = LVal(Stmt, garbage);
                    if (searchTable(nodePtr) == 4) {
                        error.addErrorMessage(nodePtr.getToken().getLine(), 'h');
                    }
                    if (curToken.type == SymbolType.ASSIGN) {
                        getToken(Stmt).setNodeName("=");
                        if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                            Exp(Stmt, garbage);
                            if (curToken.type == SymbolType.SEMICN) {
                                getToken(Stmt).setNodeName(";");
                            } else {
                                Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                                addRelation(Stmt, tNode);
                                tNode.setNodeName(";");
                                error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
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
                int line = curToken.getLine();
                Exp(Stmt, garbage);
                if (curToken.type == SymbolType.SEMICN) {
                    getToken(Stmt).setNodeName(";");
                } else {
                    Node tNode = new Node(new Token(";", SymbolType.SEMICN, line));
                    addRelation(Stmt, tNode);
                    tNode.setNodeName(";");
                    error.addErrorMessage(line, 'i');
                }
            }
        } else if (curToken.type == SymbolType.SEMICN) {
            getToken(Stmt).setNodeName(";");
        } else {
            error();
        }
        parseList.add("<Stmt>");
    }

    public void Cond(Node node) {
        Node Cond = new Node("Cond");
        addRelation(node, Cond);
        if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.INTCON || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            LOrExp(Cond);
        }
        parseList.add("<Cond>");
    }

    public void LOrExp(Node node) {
        Node LOrExp = new Node("LOrExp");
        addRelation(node, LOrExp);
        LAndExp(LOrExp);
        parseList.add("<LOrExp>");
        while (curToken.type == SymbolType.OR) {
            getToken(LOrExp);
            LAndExp(LOrExp);
            parseList.add("<LOrExp>");
        }
    }

    public void LAndExp(Node node) {
        Node LAndExp = new Node("LAndExp");
        addRelation(node, LAndExp);
        EqExp(LAndExp);
        parseList.add("<LAndExp>");
        while (curToken.type == SymbolType.AND) {
            getToken(LAndExp);
            EqExp(LAndExp);
            parseList.add("<LAndExp>");
        }
    }

    public void EqExp(Node node) {
        Node EqExp = new Node("EqExp");
        addRelation(node, EqExp);
        RelExp(EqExp);
        parseList.add("<EqExp>");
        while (curToken.type == SymbolType.EQL || curToken.type == SymbolType.NEQ) {
            getToken(EqExp);
            RelExp(EqExp);
            parseList.add("<EqExp>");
        }
    }

    public void RelExp(Node node) {
        Node RelExp = new Node("RelExp");
        addRelation(node, RelExp);
        AddExp(RelExp, garbage);
        parseList.add("<RelExp>");
        while (curToken.type == SymbolType.LSS || curToken.type == SymbolType.LEQ || curToken.type == SymbolType.GRE || curToken.type == SymbolType.GEQ) {
            getToken(RelExp);
            AddExp(RelExp, garbage);
            parseList.add("<RelExp>");
        }
    }

    public void ConstDecl(Node node) {
        Node ConstDecl = new Node("ConstDecl");
        addRelation(node, ConstDecl);
        if (curToken.type == SymbolType.CONSTTK) {
            Node nodePtr = getToken(ConstDecl);
            nodePtr.setNodeName("const");
            if (curToken.type == SymbolType.INTTK) {
                getToken(ConstDecl).setNodeName("BType");
                ConstDef(ConstDecl);
                while (curToken.type == SymbolType.COMMA) {
                    getToken(ConstDecl).setNodeName(",");
                    ConstDef(ConstDecl);
                }
                if (curToken.type == SymbolType.SEMICN) {
                    getToken(ConstDecl).setNodeName(";");
                    parseList.add("<ConstDecl>");
                } else {
                    Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                    addRelation(ConstDecl, tNode);
                    tNode.setNodeName(";");
                    error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
                }
            } else {
                error();
            }
        } else {
            error();
        }
    }

    public void ConstDef(Node node) {
        Node ConstDef = new Node("ConstDef");
        addRelation(node, ConstDef);
        if (curToken.type == SymbolType.IDENFR) {
            String name = curToken.getContent();
            int type = NodeInfo.CONST;
            int dimension = 0;
            Node idenfr = getToken(ConstDef);
            idenfr.setNodeName("Ident");
            while (curToken.type == SymbolType.LBRACK) {
                getToken(ConstDef);
                ConstExp(ConstDef);
                if (curToken.type == SymbolType.RBRACK) {
                    getToken(ConstDef);
                } else {
                    Node tNode = new Node(new Token("]", SymbolType.RBRACK, idenfr.getToken().getLine()));
                    addRelation(ConstDef, tNode);
                    error.addErrorMessage(idenfr.getToken().getLine(), 'k');
                }
                dimension++;
            }
            if (curToken.type == SymbolType.ASSIGN) {
                getToken(ConstDef).setNodeName("=");
                ConstInitVal(ConstDef);
            } else {
                error();
            }
            idenfr.setNodeInfo(name, type, level, dimension, 0);
            if (checkTable(idenfr)) {
                insertTable(idenfr);
                // intepreter to rename same-named ident
                boolean findFlag = false;
                for (int i = globalSymbolTable.size() - 2; i >= 0; i--) {
                    if (findFlag) break;
                    for (Node gnd : globalSymbolTable.get(i)) {
                        if (idenfr.getName().equals(gnd.getName()) && gnd.getType() != NodeInfo.VOIDFUN && gnd.getType() != NodeInfo.INTFUN) {
                            findFlag = true;
                            idenfr.setApearIndex(gnd.getApearIndex() + 1);
                            break;
                        }
                    }
                }
            } else {
                error.addErrorMessage(idenfr.getToken().getLine(), 'b');
            }
            parseList.add("<ConstDef>");
        } else {
            error();
        }
    }

    public void VarDecl(Node node) {
        Node VarDecl = new Node("VarDecl");
        addRelation(node, VarDecl);
        if (curToken.type == SymbolType.INTTK) {
            Node nodePtr = getToken(VarDecl);
            nodePtr.setNodeName("BType");
            VarDef(VarDecl);
            while (curToken.type == SymbolType.COMMA) {
                getToken(VarDecl).setNodeName(",");
                VarDef(VarDecl);
            }
            if (curToken.type == SymbolType.SEMICN) {
                getToken(VarDecl).setNodeName(";");
                parseList.add("<VarDecl>");
            } else {
                Node tNode = new Node(new Token(";", SymbolType.SEMICN, nodePtr.getToken().getLine()));
                addRelation(VarDecl, tNode);
                tNode.setNodeName(";");
                error.addErrorMessage(nodePtr.getToken().getLine(), 'i');
            }
        } else {
            error();
        }
    }

    public void VarDef(Node node) {
        Node VarDef = new Node("VarDef");
        addRelation(node, VarDef);
        if (curToken.type == SymbolType.IDENFR) {
            String name = curToken.getContent();
            int type = NodeInfo.VAR;
            int dimension = 0;
            Node idenfr = getToken(VarDef);
            idenfr.setNodeName("Ident");
            while (curToken.type == SymbolType.LBRACK) {
                getToken(VarDef);
                ConstExp(VarDef);
                if (curToken.type == SymbolType.RBRACK) {
                    getToken(VarDef);
                } else {
                    Node tNode = new Node(new Token("]", SymbolType.RBRACK, idenfr.getToken().getLine()));
                    addRelation(VarDef, tNode);
                    error.addErrorMessage(idenfr.getToken().getLine(), 'k');
                }
                dimension++;
            }
            if (curToken.type == SymbolType.ASSIGN) {
                getToken(VarDef).setNodeName("=");
                InitVal(VarDef);
                if (curToken.type != SymbolType.COMMA && curToken.type != SymbolType.SEMICN) {
                    error();
                }
            }
            idenfr.setNodeInfo(name, type, level, dimension, 0);
            if (checkTable(idenfr)) {
                insertTable(idenfr);
                // intepreter to rename same-named ident
                boolean findFlag = false;
                for (int i = globalSymbolTable.size() - 2; i >= 0; i--) {
                    if (findFlag) break;
                    for (Node gnd : globalSymbolTable.get(i)) {
                        if (idenfr.getName().equals(gnd.getName()) && gnd.getType() != NodeInfo.VOIDFUN && gnd.getType() != NodeInfo.INTFUN) {
                            findFlag = true;
                            idenfr.setApearIndex(gnd.getApearIndex() + 1);
                            break;
                        }
                    }
                }
            } else {
                error.addErrorMessage(idenfr.getToken().getLine(), 'b');
            }
            parseList.add("<VarDef>");
        } else {
            error();
        }
    }

    public void ConstInitVal(Node node) {
        Node ConstInitVal = new Node("ConstInitVal");
        addRelation(node, ConstInitVal);
        if (curToken.type == SymbolType.LBRACE) {
            getToken(ConstInitVal);
            if (curToken.type == SymbolType.LBRACE || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                ConstInitVal(ConstInitVal);
                while (curToken.type == SymbolType.COMMA) {
                    getToken(ConstInitVal);
                    ConstInitVal(ConstInitVal);
                }
                if (curToken.type == SymbolType.RBRACE) {
                    getToken(ConstInitVal);
                }
            } else if (curToken.type == SymbolType.RBRACE) {
                getToken(ConstInitVal);
            } else {
                error();
            }
        } else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            ConstExp(ConstInitVal);
        } else {
            error();
        }
        parseList.add("<ConstInitVal>");
    }

    public void InitVal(Node node) {
        Node InitVal = new Node("InitVal");
        addRelation(node, InitVal);
        if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
            Exp(InitVal, garbage);
            parseList.add("<InitVal>");
        } else if (curToken.type == SymbolType.LBRACE) {
            getToken(InitVal);
            if (curToken.type == SymbolType.RBRACE) {
                getToken(InitVal);
            } else if (curToken.type == SymbolType.LBRACE || curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                InitVal(InitVal);
                while (curToken.type == SymbolType.COMMA) {
                    getToken(InitVal);
                    InitVal(InitVal);
                }
                if (curToken.type == SymbolType.RBRACE) {
                    getToken(InitVal);
                    parseList.add("<InitVal>");
                } else {
                    error();
                }
            }
        } else {
            error();
        }
    }

    public void ConstExp(Node node) {
        Node ConstExp = new Node("ConstExp");
        addRelation(node, ConstExp);
        ConstExp.setFinalValue(AddExp(ConstExp, garbage));
        parseList.add("<ConstExp>");
    }

    //TODO: arith right?
    public String AddExp(Node node, Node funcName) {
        Node AddExp = new Node("AddExp");
        addRelation(node, AddExp);
        Arith arith = new Arith();
        arith.setLeft(MulExp(AddExp, funcName));
        parseList.add("<AddExp>");
        while (curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU) {
            if (AddExp.getArithExp().size() != 0) {
                arith.setLeft(AddExp.getArithExp().get(AddExp.getArithExp().size()-1).getTarget());
            }
            arith.setOp(curToken.type);
            if (curToken.type == SymbolType.PLUS) {
                getToken(AddExp).setNodeName("+");
            } else {
                getToken(AddExp).setNodeName("-");
            }
            arith.setRight(MulExp(AddExp, funcName));
            arith.setTarget("t" + "$$" + valueIndex);
            AddExp.addArithExp(new Arith(arith.getTarget(), arith.getLeft(), arith.getOp(), arith.getRight()));
            valueIndex++;
            parseList.add("<AddExp>");
        }
        if (AddExp.getArithExp().size() == 0) {
            AddExp.setFinalValue(arith.getLeft());
        } else {
            AddExp.setFinalValue(AddExp.getArithExp().get(AddExp.getArithExp().size() - 1).getTarget());
        }
        return AddExp.getFinalValue();
    }

    public String MulExp(Node node, Node funcName) {
        Node MulExp = new Node("MulExp");
        addRelation(node, MulExp);
        Arith arith = new Arith();
        arith.setLeft(UnaryExp(MulExp, funcName));
        parseList.add("<MulExp>");
        while (curToken.type == SymbolType.MULT || curToken.type == SymbolType.DIV || curToken.type == SymbolType.MOD) {
            if (MulExp.getArithExp().size() != 0) {
                arith.setLeft(MulExp.getArithExp().get(MulExp.getArithExp().size()-1).getTarget());
            }
            arith.setOp(curToken.type);
            if (curToken.type == SymbolType.MULT) {
                getToken(MulExp).setNodeName("*");
            }
            else if (curToken.type == SymbolType.DIV) {
                getToken(MulExp).setNodeName("/");
            }
            else {
                getToken(MulExp).setNodeName("%");
            }
            arith.setRight(UnaryExp(MulExp, funcName));
            arith.setTarget("t" + "$$" +valueIndex);
            MulExp.addArithExp(new Arith(arith.getTarget(), arith.getLeft(), arith.getOp(), arith.getRight()));
            valueIndex++;
            parseList.add("<MulExp>");
        }
        if (MulExp.getArithExp().size() == 0) {
            MulExp.setFinalValue(arith.getLeft());
        } else {
            MulExp.setFinalValue(MulExp.getArithExp().get(MulExp.getArithExp().size()-1).getTarget());
        }
        return MulExp.getFinalValue();
    }

    //TODO not finished
    public String UnaryExp(Node node, Node funcName) {
        Node UnaryExp = new Node("UnaryExp");
        addRelation(node, UnaryExp);
        if (curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.INTCON || (curToken.type == SymbolType.IDENFR && tokenList.get(tkPtr).type != SymbolType.LPARENT)) {
            UnaryExp.setFinalValue(PrimaryExp(UnaryExp, funcName));
        }
        else if (curToken.type == SymbolType.IDENFR && tokenList.get(tkPtr).type == SymbolType.LPARENT) {
            FuncUse funcUse = new FuncUse();
            String name = curToken.getContent();
            funcUse.setName(name);
            int type = NodeInfo.INTFUN;
            int paramNum = 0;
            Node idenfr = getToken(UnaryExp);
            idenfr.setNodeName("Ident");
            for (int i = globalSymbolTable.size() - 1; i >= 0; i--) {
                for (Node gnd : globalSymbolTable.get(i)) {
                    if (gnd.getName().equals(name)) {
                        type = gnd.getType();
                        break;
                    }
                }
            }
            if (type == NodeInfo.INTFUN) {
                funcName.addParamDim(0);
            } else {
                funcName.addParamDim(3);
            }
            getToken(UnaryExp).setNodeName("(");
            if (curToken.type == SymbolType.RPARENT) {
                paramNum = 0;
                getToken(UnaryExp).setNodeName(")");
                funcUse.setTarget("t" + "$$" + valueIndex);
                UnaryExp.addArithExp(funcUse);
                UnaryExp.setFinalValue(funcUse.getTarget());
            } else if (curToken.type == SymbolType.IDENFR || curToken.type == SymbolType.LPARENT || curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT || curToken.type == SymbolType.INTCON) {
                paramNum = FuncRParams(UnaryExp, idenfr, funcUse);
                funcUse.setTarget("t" + "$$" + valueIndex);
                valueIndex++;
                if (curToken.type == SymbolType.RPARENT) {
                    getToken(UnaryExp).setNodeName(")");
                } else {
                    Node tNode = new Node(new Token(")", SymbolType.RPARENT, idenfr.getToken().getLine()));
                    addRelation(UnaryExp, tNode);
                    error.addErrorMessage(idenfr.getToken().getLine(), 'j');
                }
                UnaryExp.addArithExp(funcUse);
                UnaryExp.setFinalValue(funcUse.getTarget());
            } else {
                // TODO right?
                Node tNode = new Node(new Token(")", SymbolType.RPARENT, idenfr.getToken().getLine()));
                addRelation(UnaryExp, tNode);
                error.addErrorMessage(idenfr.getToken().getLine(), 'j');
            }
            idenfr.setNodeInfo(name, type, level, 0, paramNum);
            if (!idenfr.getName().equals("garbage")) {
//                for (Integer i : idenfr.getParamDim()) {
//                    System.out.println(idenfr.getName()+" "+i);
//                }
                switch (searchTable(idenfr)) {
                    case 1:
                        error.addErrorMessage(idenfr.getToken().getLine(), 'c');
                        break;
                    case 2:
                        error.addErrorMessage(idenfr.getToken().getLine(), 'd');
                        break;
                    case 3:
                        error.addErrorMessage(idenfr.getToken().getLine(), 'e');
                        break;
                }
            }
        }
        else if (curToken.type == SymbolType.PLUS || curToken.type == SymbolType.MINU || curToken.type == SymbolType.NOT) {
            Arith arith = new Arith();
            arith.setOp(curToken.type);
            UnaryOp(UnaryExp);
            arith.setRight(UnaryExp(UnaryExp, funcName));
            arith.setTarget("t" + "$$" + valueIndex);
            UnaryExp.addArithExp(arith);
            UnaryExp.setFinalValue("t" + "$$" + valueIndex);
            valueIndex++;
        }
        else {
            error();
        }
        parseList.add("<UnaryExp>");
        return UnaryExp.getFinalValue();
    }

    public void UnaryOp(Node node) {
        Node UnaryOp = new Node("UnaryOp");
        addRelation(node, UnaryOp);
        if (curToken.getType() == SymbolType.PLUS) {
            getToken(UnaryOp).setNodeName("+");
        }
        else if (curToken.getType() == SymbolType.MINU) {
            getToken(UnaryOp).setNodeName("-");
        }
        else {
            getToken(UnaryOp).setNodeName("!");
        }
        parseList.add("<UnaryOp>");
    }

    public String PrimaryExp(Node node, Node funcName) {
        Node PrimaryExp = new Node("PrimaryExp");
        addRelation(node, PrimaryExp);
        if (curToken.type == SymbolType.LPARENT) {
            getToken(PrimaryExp).setNodeName("(");
            PrimaryExp.setFinalValue(Exp(PrimaryExp, funcName));
            if (curToken.type == SymbolType.RPARENT) {
                getToken(PrimaryExp).setNodeName(")");
            } else {
                error();
            }
        } else if (curToken.type == SymbolType.IDENFR) {
            LVal(PrimaryExp, funcName);
        } else if (curToken.type == SymbolType.INTCON) {
            funcName.addParamDim(0);
            PrimaryExp.setFinalValue(Number(PrimaryExp));
        } else {
            error();
        }
        parseList.add("<PrimaryExp>");
        return PrimaryExp.getFinalValue();
    }

    public String Number(Node node) {
        Node Number = new Node("Number");
        addRelation(node, Number);
        Number.setFinalValue(curToken.getContent());
//        System.out.println(Number.getFinalValue());
        getToken(Number).setNodeName("specNumber");
        parseList.add("<Number>");
        return Number.getFinalValue();
    }

    public Node LVal(Node node, Node funcName) {
        Node LVal = new Node("LVal");
        addRelation(node, LVal);
        String name = curToken.getContent();
        int type = NodeInfo.VAR;
        int dimension = 0;
        Node idenfr = getToken(LVal);
        idenfr.setNodeName("Ident");
        while (curToken.type == SymbolType.LBRACK) {
            getToken(LVal);
            Exp(LVal, garbage);
            if (curToken.type == SymbolType.RBRACK) {
                getToken(LVal);
            } else {
                Node tNode = new Node(new Token("]", SymbolType.RBRACK, idenfr.getToken().getLine()));
                addRelation(LVal, tNode);
                error.addErrorMessage(idenfr.getToken().getLine(), 'k');
            }
            dimension++;
        }
        idenfr.setNodeInfo(name, type, level, dimension, 0);
        for (int i = globalSymbolTable.size() - 1; i >= 0; i--) {
            for (Node gnd : globalSymbolTable.get(i)) {
                if (gnd.getName().equals(idenfr.getName()) && gnd.getType() != NodeInfo.VOIDFUN && gnd.getType() != NodeInfo.INTFUN) {
                    funcName.addParamDim(gnd.getDimension() - dimension);
                    break;
                }
            }
        }
        // intepreter to rename same-named ident
        boolean findFlag = false;
        for (int i = globalSymbolTable.size() - 1; i >= 0; i--) {
            if (findFlag) break;
            for (Node gnd : globalSymbolTable.get(i)) {
                if (idenfr.getName().equals(gnd.getName()) && gnd.getType() != NodeInfo.VOIDFUN && gnd.getType() != NodeInfo.INTFUN) {
                    findFlag = true;
                    idenfr.setApearIndex(gnd.getApearIndex());
                    break;
                }
            }
        }
//        System.out.println("LVal " + idenfr.getName() + " " + idenfr.getApearIndex() + " " + idenfr.getToken().getLine());
        if (searchTable(idenfr) == 1) {
            error.addErrorMessage(idenfr.getToken().getLine(), 'c');
        }
        LVal.setFinalValue(name + "$" + idenfr.getApearIndex());
        node.setFinalValue(name + "$" + idenfr.getApearIndex());
        parseList.add("<LVal>");
        return idenfr;
    }

    public int FuncFParams(Node node, Node funcName) {
        Node FuncFParams = new Node("FuncFParams");
        addRelation(node, FuncFParams);
        HashMap<String, String> paraMap = new HashMap<>();
        int paramNum = 0;
        paraMap.putAll(FuncFParam(FuncFParams, funcName));
        paramNum++;
        while (curToken.type == SymbolType.COMMA) {
            getToken(FuncFParams).setNodeName(",");
            paraMap.putAll(FuncFParam(FuncFParams, funcName));
            paramNum++;
        }
        parseList.add("<FuncFParams>");
        FuncFParams.setParams(paraMap);
        return paramNum;
    }

    public HashMap<String, String> FuncFParam(Node node, Node funcName) {
        Node FuncFParam = new Node("FuncFParam");
        addRelation(node, FuncFParam);
        HashMap<String, String> map = new HashMap<>();
        if (curToken.type == SymbolType.INTTK) {
            getToken(FuncFParam).setNodeName("BType");
            if (curToken.type == SymbolType.IDENFR) {
                String name = curToken.getContent();
                int type = NodeInfo.VAR;
                int dimension = 0;
                Node idenfr = getToken(FuncFParam);
                idenfr.setNodeName("Ident");
                if (curToken.type == SymbolType.LBRACK) {
                    dimension++;
                    getToken(FuncFParam);
                    if (curToken.type == SymbolType.RBRACK) {
                        getToken(FuncFParam);
                        while (curToken.type == SymbolType.LBRACK) {
                            dimension++;
                            getToken(FuncFParam);
                            if (curToken.type == SymbolType.RBRACK) {
                                getToken(FuncFParam);
                            } else {
                                Node tNode = new Node(new Token("]", SymbolType.RBRACK, idenfr.getToken().getLine()));
                                addRelation(FuncFParam, tNode);
                                error.addErrorMessage(idenfr.getToken().getLine(), 'k');
                            }
                        }
                        parseList.add("<FuncFParam>");
                    } else {
                        Node tNode = new Node(new Token("]", SymbolType.RBRACK, idenfr.getToken().getLine()));
                        addRelation(FuncFParam, tNode);
                        error.addErrorMessage(idenfr.getToken().getLine(), 'k');
                    }
                } else if (curToken.type == SymbolType.COMMA || curToken.type == SymbolType.RPARENT) {
                    parseList.add("<FuncFParam>");
                } else {
                    error();
                }
                idenfr.setNodeInfo(name, type, level, dimension, 0);
                funcName.addParamDim(dimension);
                if (checkTable(idenfr)) {
                    insertTable(idenfr);
                    // intepreter to rename same-named ident
                    boolean findFlag = false;
                    for (int i = globalSymbolTable.size() - 2; i >= 0; i--) {
                        if (findFlag) break;
                        for (Node gnd : globalSymbolTable.get(i)) {
                            if (idenfr.getName().equals(gnd.getName()) && gnd.getType() != NodeInfo.VOIDFUN && gnd.getType() != NodeInfo.INTFUN) {
                                findFlag = true;
                                idenfr.setApearIndex(gnd.getApearIndex() + 1);
                                break;
                            }
                        }
                    }
                } else {
                    error.addErrorMessage(idenfr.getToken().getLine(), 'b');
                }
                map.put(idenfr.getName() + "$" + idenfr.getApearIndex(), "int");
            } else {
                error();
            }
        } else {
            error();
        }
        return map;
    }

    public int FuncRParams(Node node, Node funcName, FuncUse funcUse) {
        Node FuncRParams = new Node("FuncRParams");
        addRelation(node, FuncRParams);
        int paramNum = 0;
        funcUse.addParam(Exp(FuncRParams, funcName));
        paramNum++;
        while (curToken.type == SymbolType.COMMA) {
            getToken(FuncRParams).setNodeName(",");
            funcUse.addParam(Exp(FuncRParams, funcName));
            paramNum++;
        }
        parseList.add("<FuncRParams>");
        return paramNum;
    }

    public String Exp(Node node, Node funcName) {
        Node Exp = new Node("Exp");
        addRelation(node, Exp);
        Exp.setFinalValue(AddExp(Exp, funcName));
        parseList.add("<Exp>");
        return Exp.getFinalValue();
    }
}
