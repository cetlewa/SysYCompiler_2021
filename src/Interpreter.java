import java.util.ArrayList;
import java.util.HashMap;

public class Interpreter {
    private Node CompUnit;
    private ArrayList<Code> codeList;
    private int normalStrIndex;

    public Interpreter(Node CompUnit) {
        this.CompUnit = CompUnit;
        this.codeList = new ArrayList<>();
        this.normalStrIndex = 0;
        this.analyze();
        System.out.println(">>>>>>>>>> Interpreter End <<<<<<<<<<");
    }

    public ArrayList<Code> getCodeList() {
        return codeList;
    }

    public void analyze() {
        tran_CompUnit(CompUnit);
        for (Code c : codeList) {
            c.print();
        }
    }

    public void tran_CompUnit(Node node) {
        for (Node compUnitCh : node.getChildNode()) {
            switch (compUnitCh.getNodeName()) {
                case "Decl":
                    tran_Decl(compUnitCh);
                    break;
                case "FuncDef":
                    tran_FuncDef(compUnitCh);
                    break;
                case "MainFuncDef":
                    tran_MainFuncDef(compUnitCh);
                    break;
            }
        }
    }

    public void tran_Decl(Node node) {
        for (Node declCh : node.getChildNode()) {
            switch (declCh.getNodeName()) {
                case "ConstDecl":
                    for (Node cDeclCh : declCh.getChildNode()) {
                        if (cDeclCh.getNodeName().equals("ConstDef")) {
                            tran_ConstDef(cDeclCh);
                        }
                    }
                    break;
                case "VarDecl":
                    for (Node vDeclCh : declCh.getChildNode()) {
                        if (vDeclCh.getNodeName().equals("VarDef")) {
                            tran_VarDef(vDeclCh);
                        }
                    }
                    break;
            }
        }
    }

    public void tran_VarDef(Node node) {
        ConVarDef def = new ConVarDef();
        AssignValue assign = new AssignValue();
        def.setType(NodeInfo.VAR);
        for (Node varDefCh : node.getChildNode()) {
            switch (varDefCh.getNodeName()) {
                case "Ident":
                    def.setName(varDefCh.getToken().getContent() + "$" + varDefCh.getApearIndex());
                    assign.setTarget(varDefCh.getToken().getContent() + "$" + varDefCh.getApearIndex());
                    codeList.add(def);
                    break;
                case "InitVal":
                    assign.setValue(varDefCh.getChildNode().get(0).getFinalValue());
                    getArith(varDefCh.getChildNode().get(0));
                    break;
            }
        }
        if (assign.getValue() != null) {
            codeList.add(assign);
        }
    }

    public void tran_ConstDef(Node node) {
        ConVarDef def = new ConVarDef();
        AssignValue assign = new AssignValue();
        def.setType(NodeInfo.CONST);
        for (Node constDefCh : node.getChildNode()) {
            switch (constDefCh.getNodeName()) {
                case "Ident":
                    def.setName(constDefCh.getToken().getContent() + "$" + constDefCh.getApearIndex());
                    assign.setTarget(constDefCh.getToken().getContent() + "$" + constDefCh.getApearIndex());
                    codeList.add(def);
                    break;
                case "ConstInitVal":
                    assign.setValue(constDefCh.getChildNode().get(0).getFinalValue());
                    getArith(constDefCh.getChildNode().get(0));
                    break;
            }
        }
        codeList.add(assign);
    }

    public void tran_FuncDef(Node node) {
        FuncDef funcDef = new FuncDef();
        for (Node funcDefCh : node.getChildNode()) {
            switch (funcDefCh.getNodeName()) {
                case "FuncType":
                    funcDef.setType(funcDefCh.getType());
                    break;
                case "Ident":
                    funcDef.setName(funcDefCh.getToken().getContent());
                    break;
                case "FuncFParams":
                    funcDef.setParaMap(funcDefCh.getParams());
                    break;
                case "Block":
                    codeList.add(funcDef);
                    tran_Block(funcDefCh);
                    break;
            }
        }
    }

    public void tran_Block(Node node) {
        for (Node blockCh : node.getChildNode()) {
            switch (blockCh.getNodeName()) {
                case "BlockItem":
                        tran_BlockItem(blockCh);
                    break;
            }
        }
    }

    public void tran_BlockItem(Node node) {
        for (Node blockItemCh : node.getChildNode()) {
            switch (blockItemCh.getNodeName()) {
                case "Decl":
                    tran_Decl(blockItemCh);
                    break;
                case "Stmt":
                    tran_Stmt(blockItemCh);
                    break;
            }
        }
    }

    public void tran_Stmt(Node node) {
        Node stmtFirst = node.getChildNode().get(0);
        switch (stmtFirst.getNodeName()) {
            case "LVal":
                AssignValue assign = new AssignValue();
                assign.setTarget(node.getChildNode().get(0).getFinalValue());
                if (node.getChildNode().size() == 4) {
                    getArith(node.getChildNode().get(2));
                    assign.setValue(node.getChildNode().get(2).getFinalValue());
                    codeList.add(assign);
                } else {
                    assign.setValue("getint");
                    codeList.add(assign);
                }
                break;
            case "Exp":
                getArith(stmtFirst);
                break;
            case "Block":
                tran_Block(stmtFirst);
                break;
            case "if":
                break;
            case "while":
                break;
            case "break":
                break;
            case "continue":
                break;
            case "return":
                FuncReturn ret = new FuncReturn();
                if (node.getChildNode().size() == 3){
                    getArith(node.getChildNode().get(1));
                    ret.setRetValue(node.getChildNode().get(1).getFinalValue());
                }
                codeList.add(ret);
                break;
            case "printf":
                ArrayList<Code> printf = new ArrayList<>();
                for (Node pf : node.getChildNode()) {
                    switch (pf.getNodeName()) {
                        case "FormatString":
                            String formatStr = pf.getToken().getContent();
                            formatStr = formatStr.split("\"")[1];
                            int ptr = 0;
                            String str = "";
                            while (ptr != formatStr.length()) {
                                if (formatStr.charAt(ptr) == '%' && ptr + 1 != formatStr.length() && formatStr.charAt(ptr + 1) == 'd') {
                                    if (ptr != 0) {
                                        Printf p = new Printf();
                                        p.setStr("str" + "$" + normalStrIndex);
                                        printf.add(p);
                                        NormalString ns = new NormalString();
                                        ns.setName("str" + "$" + normalStrIndex);
                                        ns.setContent(str);
                                        printf.add(ns);
                                        normalStrIndex++;
                                        str = "";
                                    }

                                    Printf pp = new Printf();
                                    pp.setStr("%d");
                                    printf.add(pp);
                                    ptr = ptr + 2;
                                } else {
                                    str += formatStr.charAt(ptr);
                                    ptr++;
                                }
                            }
                            if (!str.equals("")) {
                                Printf p = new Printf();
                                p.setStr("str" + "$" + normalStrIndex);
                                printf.add(p);
                                NormalString ns = new NormalString();
                                ns.setName("str" + "$" + normalStrIndex);
                                ns.setContent(str);
                                printf.add(ns);
                                normalStrIndex++;
                            }
                            break;
                        case "Exp":
                            getArith(pf);
                            for (Code prf : printf) {
                                if (prf.getClass().getName().substring(prf.getClass().getName().lastIndexOf(".") + 1).equals("Printf")) {
                                    if (((Printf)prf).getStr().equals("%d")) {
                                        ((Printf)prf).setStr(pf.getFinalValue());
                                        break;
                                    }
                                }
                            }
                            break;
                    }
                }
                codeList.addAll(printf);
                break;
        }
    }

    public void tran_MainFuncDef(Node node) {
        FuncDef funcDef = new FuncDef();
        funcDef.setType(NodeInfo.INTFUN);
        funcDef.setName("main");
        codeList.add(funcDef);
        tran_Block(node.getChildNode().get(4));
    }

    public void getArith(Node node) {
        for (Node child : node.getChildNode()) {
            getArith(child);
        }
        codeList.addAll(node.getArithExp());
    }
}

interface Code {

    String getTarget();

    void print();

}

class NormalString implements Code {
    private String name;
    private String content;

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append(name + " " + content);
        System.out.println(internCode);
    }
}

class Printf implements Code {
    private String str;

    public void setStr(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("print " + str);
        System.out.println(internCode);
    }
}

class FuncReturn implements Code {
    private String retValue;

    public void setRetValue(String retValue) {
        this.retValue = retValue;
    }

    public String getRetValue() {
        return retValue;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("ret");
        if (retValue != null) {
            internCode.append(" " + retValue);
        }
        System.out.println(internCode);
    }
}

class FuncUse implements Code {
    private String name;
    private String target;
    private ArrayList<String> param = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void addParam(String param) {
        this.param.add(param);
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getParam() {
        return param;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        for (String para : param) {
            internCode.append("push " + para + "\n");
        }
        internCode.append("call " + name + "\n");
        internCode.append(target + " = RET");
        System.out.println(internCode);
    }
}

class Arith implements Code {
    private SymbolType op;
    private String left;
    private String right;
    private String target;

    public Arith(String target, String left, SymbolType op, String right) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.target = target;
    }

    public Arith() {

    }

    public void setOp(SymbolType op) {
        this.op = op;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLeft() {
        return left;
    }

    public SymbolType getOp() {
        return op;
    }

    public String getRight() {
        return right;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        if (left != null) {
            internCode.append(target + " = " + left + " " + op + " " + right);
        } else {
            internCode.append(target + " = " + op + " " + right);
        }
        System.out.println(internCode);
    }
}

class AssignValue implements Code {
    private String target;
    private String value;

    public void setTarget(String target) {
        this.target = target;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append(target + " = " + value);
        System.out.println(internCode);
    }
}

class ConVarDef implements Code {
    private String name;    //const or var name
    private int type;   //const or var

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append((this.type == NodeInfo.CONST) ? "const " : "var ");
        internCode.append("int " + this.name);
        System.out.println(internCode);
    }
}

class FuncDef implements Code {
    private String name;    //func name
    private int type;   //intfun or voidfun
    private HashMap<String, String> paraMap;    // params of func

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setParaMap(HashMap<String, String> paraMap) {
        this.paraMap = paraMap;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, String> getParaMap() {
        return paraMap;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public void print() {
        StringBuffer internCode = new StringBuffer();
        internCode.append((this.type == NodeInfo.INTFUN) ? "int " : "void ");
        internCode.append(this.name + "()" + "\n");
        if (paraMap != null) {
            for (String para : paraMap.keySet()) {
                internCode.append("para " + paraMap.get(para) + " " + para + "\n");
            }
        }
        internCode.deleteCharAt(internCode.length() - 1);
        System.out.println(internCode);
    }
}