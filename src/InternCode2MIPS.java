import java.util.ArrayList;
import java.util.HashMap;

public class InternCode2MIPS {
    private HashMap<String, Integer> funcOffset;
    private ArrayList<mipsCode> mipsCode;
    private ArrayList<Code> internCode;
    private ArrayList<Code> ic_Decl;
    private ArrayList<ArrayList<Code>> ic_Func;
    private ArrayList<Code> ic_MainFunc;
    private HashMap<String, Integer> globalSymbolTable; // name <-> offset
    private ArrayList<HashMap<String, Integer>> funcSymbolTable;
    private HashMap<String, Integer> mainfuncSymbolTable;

    public InternCode2MIPS(ArrayList<Code> internCode) {
        this.funcOffset = new HashMap<>();
        this.mipsCode = new ArrayList<>();
        this.internCode = internCode;
        this.ic_Decl = new ArrayList<>();
        this.ic_Func = new ArrayList<>();
        this.ic_MainFunc = new ArrayList<>();
        this.globalSymbolTable = new HashMap<>();
        this.funcSymbolTable = new ArrayList<>();
        this.mainfuncSymbolTable = new HashMap<>();
        this.translate();
        this.print();
        System.out.println(">>>>>>>>>> InternCode2MIPS End <<<<<<<<<<");
    }

    public ArrayList<mipsCode> getMipsCode() {
        return mipsCode;
    }

    public void print() {
        for (mipsCode mc : mipsCode) {
            System.out.println(mc.mips());
        }
    }

    public void translate() {
        classify();
        translateData();
        translateText();
    }

    public void translateText() {
        mipsCode.add(new text());
        globalDataInit();
        translateMainFunc();
        translateFunc();
    }

    public void translateFunc() {
        for (ArrayList<Code> list: ic_Func) {
            HashMap<String, Integer> funcMap = new HashMap<>();
            String funcName = ((FuncDef)list.get(0)).getName();
            for (Code ic : list) {
                translateFuncDef(ic, funcMap, "Func", funcName);
                translateConVarDef(ic, funcMap, "Func", funcName);
                translateAssignValue(ic, funcMap, "Func", funcName);
                translateArith(ic, funcMap, "Func", funcName);
                translateFuncUse(ic, funcMap, "Func", funcName);
                translateFuncReturn(ic, funcMap, "Func", funcName);
                translatePrintf(ic, funcMap, "Func", funcName);
            }
            funcSymbolTable.add(funcMap);
        }
    }

    public void translateFuncDef(Code ic, HashMap<String, Integer> funcMap, String location, String funcName) {
        if (!ic.getClass().getName().equals("FuncDef")) return;
        FuncDef fd = (FuncDef) ic;
        mipsCode.add(new label("$" + fd.getName() +"$"));
        funcOffset.put(funcName, 0);
        funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        sw sw0 = new sw("ra", 0, "sp");
        mipsCode.add(sw0);
        // init funcMap
        if (fd.getParaMap() != null) {
//            System.out.println(fd.getParaMap().keySet());
            ArrayList<String> param = new ArrayList<>();
            for (String para : fd.getParaMap().keySet()) {
                param.add(para);
            }
            for (int i = param.size() - 1; i >= 0; i--) {
//                System.out.println(para);
                funcMap.put(param.get(i), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
        }
        //TODO
    }

    public void translateMainFunc() {
        mipsCode.add(new label("$MainFunc$"));
        funcOffset.put("main", 0);
        for (Code ic : ic_MainFunc) {
            translateArith(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateAssignValue(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateConVarDef(ic, mainfuncSymbolTable, "MainFunc", "main");
            translatePrintf(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateFuncUse(ic, mainfuncSymbolTable, "MainFunc", "main");
            //translateFuncReturn(ic, mainfuncSymbolTable, "MainFunc");
        }
        li li = new li("v0", 10);
        mipsCode.add(li);
        syscall syscall = new syscall();
        mipsCode.add(syscall);
    }

    public void globalDataInit() {
        mipsCode.add(new label("$init$"));
        funcOffset.put("init", 0);
        for (Code ic : ic_Decl) {
            translateArith(ic, globalSymbolTable, "Global", "init");
            translateAssignValue(ic, globalSymbolTable, "Global", "init");
        }
    }

    public void translateFuncUse(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("FuncUse")) return;
        FuncUse fu = (FuncUse) ic;
        int nowOffset = funcOffset.get(funcName);
        //sw sw0 = new sw("ra", nowOffset, "sp");
        //mipsCode.add(sw0);
        nowOffset -= 4;
        for (int i = 0; i < fu.getParam().size(); i++) {
            if (fu.getParam().get(i).contains("t$$")) {
                int offset = symbolTable.get(fu.getParam().get(i));
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else if (isDigital(fu.getParam().get(i))) {
                li li = new li("t0", Integer.parseInt(fu.getParam().get(i)));
                mipsCode.add(li);
            }
            else {
                if (symbolTable.get(fu.getParam().get(i)) == null) {
                    la la = new la("t0", fu.getParam().get(i));
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else {
                    int offset = symbolTable.get(fu.getParam().get(i));
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            sw sw = new sw("t0", nowOffset, "sp");
            mipsCode.add(sw);
            nowOffset -= 4;
        }
        addiu addiu0 = new addiu("sp", "sp", funcOffset.get(funcName));
        mipsCode.add(addiu0);
        jal jal = new jal("$" + fu.getName() + "$");
        mipsCode.add(jal);
        addiu addiu1 = new addiu("sp", "sp", -1 * funcOffset.get(funcName));
        mipsCode.add(addiu1);
        sw sw = new sw("v0", funcOffset.get(funcName), "sp");
        mipsCode.add(sw);
        if (fu.getTarget() != null) {
            symbolTable.put(fu.getTarget(), funcOffset.get(funcName));
        }
        funcOffset.put(funcName, funcOffset.get(funcName) - 4);
    }

    public void translateFuncReturn(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("FuncReturn")) return;
        FuncReturn fr = (FuncReturn) ic;
        // set v0 -> return value
        if (fr.getRetValue() != null) {
            if (isDigital(fr.getRetValue())) {
                li li = new li("v0", Integer.parseInt(fr.getRetValue()));
                mipsCode.add(li);
            } else if (symbolTable.get(fr.getRetValue()) == null) {
                la la = new la("v0", fr.getRetValue());
                mipsCode.add(la);
                lw lw = new lw("v0", 0, "v0");
                mipsCode.add(lw);
            } else {
                int offset = symbolTable.get(fr.getRetValue());
                lw lw = new lw("v0", offset, "sp");
                mipsCode.add(lw);
            }
        }
        // set ra -> return address
        lw lw = new lw("ra", 0, "sp");
        mipsCode.add(lw);
        // jump to ra
        jr jr = new jr("ra");
        mipsCode.add(jr);
    }

    public void translatePrintf(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("Printf")) return;
        Printf print = (Printf) ic;
        if (print.getStr().contains("str$")) {
            la la = new la("a0", print.getStr());
            mipsCode.add(la);
            li li = new li("v0", 4);
            mipsCode.add(li);
            syscall syscall = new syscall();
            mipsCode.add(syscall);
        }
        else if (isDigital(print.getStr())) {
            li li0 = new li("a0", Integer.parseInt(print.getStr()));
            mipsCode.add(li0);
            li li1 = new li("v0", 1);
            mipsCode.add(li1);
            syscall syscall = new syscall();
            mipsCode.add(syscall);
        }
        else {
            if (symbolTable.get(print.getStr()) == null) {
                la la = new la("t0", print.getStr());
                mipsCode.add(la);
                lw lw = new lw("a0", 0, "t0");
                mipsCode.add(lw);
            } else {
                int offset = symbolTable.get(print.getStr());
                lw lw = new lw("a0", offset, "sp");
                mipsCode.add(lw);
            }
            li li = new li("v0", 1);
            mipsCode.add(li);
            syscall syscall = new syscall();
            mipsCode.add(syscall);
        }

    }

    public void translateConVarDef(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("ConVarDef")) return;
        ConVarDef cvd = (ConVarDef) ic;
        symbolTable.put(cvd.getName(), funcOffset.get(funcName));
        funcOffset.put(funcName, funcOffset.get(funcName) - 4);
    }

    public void translateArith(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("Arith")) return;
        Arith arith = (Arith) ic;
        if (arith.getOp() == SymbolType.PLUS) {
            if (arith.getLeft() == null) {
                li li = new li("t0", 0);
                mipsCode.add(li);
            }
            else if (isDigital(arith.getLeft())) {
                li li = new li("t0", Integer.parseInt(arith.getLeft()));
                mipsCode.add(li);
            }
            else if (arith.getLeft().contains("$$")) {
                int offset = symbolTable.get(arith.getLeft());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getLeft()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getLeft()) == null)) {
                    la la = new la("t0", arith.getLeft());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getLeft());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            if (isDigital(arith.getRight())) {
                li li = new li("t1", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t1", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t1", 0, "t1");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t1", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            addu addu = new addu("t0", "t0", "t1");
            mipsCode.add(addu);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.MINU) {
            if (arith.getLeft() == null) {
                li li = new li("t0", 0);
                mipsCode.add(li);
            }
            else if (isDigital(arith.getLeft())) {
                li li = new li("t0", Integer.parseInt(arith.getLeft()));
                mipsCode.add(li);
            }
            else if (arith.getLeft().contains("$$")) {
                int offset = symbolTable.get(arith.getLeft());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getLeft()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getLeft()) == null)) {
                    la la = new la("t0", arith.getLeft());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getLeft());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            if (isDigital(arith.getRight())) {
                li li = new li("t1", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t1", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t1", 0, "t1");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t1", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            subu subu = new subu("t0", "t0", "t1");
            mipsCode.add(subu);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.MULT) {
            if (isDigital(arith.getLeft())) {
                li li = new li("t0", Integer.parseInt(arith.getLeft()));
                mipsCode.add(li);
            }
            else if (arith.getLeft().contains("$$")) {
                int offset = symbolTable.get(arith.getLeft());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getLeft()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getLeft()) == null)) {
                    la la = new la("t0", arith.getLeft());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getLeft());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            if (isDigital(arith.getRight())) {
                li li = new li("t1", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t1", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t1", 0, "t1");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t1", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            mul mul = new mul("t0", "t0", "t1");
            mipsCode.add(mul);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.DIV) {
            if (isDigital(arith.getLeft())) {
                li li = new li("t0", Integer.parseInt(arith.getLeft()));
                mipsCode.add(li);
            }
            else if (arith.getLeft().contains("$$")) {
                int offset = symbolTable.get(arith.getLeft());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getLeft()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getLeft()) == null)) {
                    la la = new la("t0", arith.getLeft());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getLeft());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            if (isDigital(arith.getRight())) {
                li li = new li("t1", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t1", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t1", 0, "t1");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t1", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            divu divu = new divu("t0", "t1");
            mipsCode.add(divu);
            mflo mflo = new mflo("t0");
            mipsCode.add(mflo);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.MOD) {
            if (isDigital(arith.getLeft())) {
                li li = new li("t0", Integer.parseInt(arith.getLeft()));
                mipsCode.add(li);
            }
            else if (arith.getLeft().contains("$$")) {
                int offset = symbolTable.get(arith.getLeft());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getLeft()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getLeft()) == null)) {
                    la la = new la("t0", arith.getLeft());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getLeft());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            if (isDigital(arith.getRight())) {
                li li = new li("t1", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t1", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t1", 0, "t1");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t1", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            divu divu = new divu("t0", "t1");
            mipsCode.add(divu);
            mfhi mfhi = new mfhi("t0");
            mipsCode.add(mfhi);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.NOT) {
            // TODO
        }
    }

    public void translateAssignValue(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("AssignValue")) return;
        AssignValue assign = (AssignValue) ic;
        if (assign.getValue().equals("getint")) {
            li li = new li("v0", 5);
            mipsCode.add(li);
            syscall syscall = new syscall();
            mipsCode.add(syscall);
            if (location.equals("Global")
                    || (location.equals("MainFunc") && symbolTable.get(assign.getTarget()) == null)
                    || (location.equals("Func") && symbolTable.get(assign.getTarget()) == null)) {
                la la = new la("t0", assign.getTarget());
                mipsCode.add(la);
                sw sw = new sw("v0", 0, "t0");
                mipsCode.add(sw);
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset = symbolTable.get(assign.getTarget());
                sw sw = new sw("v0", offset, "sp");
                mipsCode.add(sw);
            }
        }
        else if (isDigital(assign.getValue())) {
            li li = new li("t0", Integer.parseInt(assign.getValue()));
            mipsCode.add(li);
            if (location.equals("Global")
                    || (location.equals("MainFunc") && symbolTable.get(assign.getTarget()) == null)
                    || (location.equals("Func") && symbolTable.get(assign.getTarget()) == null)) {
                la la = new la("t1", assign.getTarget());
                mipsCode.add(la);
                sw sw = new sw("t0", 0, "t1");
                mipsCode.add(sw);
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset = symbolTable.get(assign.getTarget());
                sw sw = new sw("t0", offset, "sp");
                mipsCode.add(sw);
            }
        }
        else if (assign.getValue().contains("$$")) {
            int offset0 = symbolTable.get(assign.getValue());
            lw lw = new lw("t0", offset0, "sp");
            mipsCode.add(lw);
            if (location.equals("Global")
                    || (location.equals("MainFunc") && symbolTable.get(assign.getTarget()) == null)
                    || (location.equals("Func") && symbolTable.get(assign.getTarget()) == null)) {
                la la = new la("t1", assign.getTarget());
                mipsCode.add(la);
                sw sw = new sw("t0", 0, "t1");
                mipsCode.add(sw);
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset1 = symbolTable.get(assign.getTarget());
                sw sw = new sw("t0", offset1, "sp");
                mipsCode.add(sw);
            }
        }
        else {
            if (location.equals("Global")) {
                la la0 = new la("t0", assign.getValue());
                mipsCode.add(la0);
                lw lw = new lw("t0", 0, "t0");
                mipsCode.add(lw);
                la la1 = new la("t1", assign.getTarget());
                mipsCode.add(la1);
                sw sw = new sw("t0", 0, "t1");
                mipsCode.add(sw);
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                if (symbolTable.get(assign.getValue()) == null) {
                    la la = new la("t0", assign.getValue());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                } else {
                    int offset0 = symbolTable.get(assign.getValue());
                    lw lw = new lw("t0", offset0, "sp");
                    mipsCode.add(lw);
                }
                if (symbolTable.get(assign.getTarget()) == null) {
                    la la = new la("t1", assign.getTarget());
                    mipsCode.add(la);
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                } else {
                    int offset1 = symbolTable.get(assign.getTarget());
                    sw sw = new sw("t0", offset1, "sp");
                    mipsCode.add(sw);
                }
            }
        }
    }

    public void translateData() {
        mipsCode.add(new data());
        for (Code ic : ic_Decl) {
            if (ic.getClass().getName().substring(ic.getClass().getName().lastIndexOf(".") + 1).equals("ConVarDef")) {
                declGlobalInt dgi = new declGlobalInt(((ConVarDef)ic).getName());
                mipsCode.add(dgi);
            }
        }
        for (ArrayList<Code> list: ic_Func) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getClass().getName().substring(list.get(i).getClass().getName().lastIndexOf(".") + 1).equals("Printf")) {
                    if (((Printf)list.get(i)).getStr().contains("str$")) {
                        declGlobalStr dgs = new declGlobalStr(((Printf)list.get(i)).getStr(), ((NormalString)list.get(i + 1)).getContent());
                        mipsCode.add(dgs);
                    }
                }
            }
        }
        for (int i = 0; i < ic_MainFunc.size(); i++) {
            if (ic_MainFunc.get(i).getClass().getName().substring(ic_MainFunc.get(i).getClass().getName().lastIndexOf(".") + 1).equals("Printf")) {
                if (((Printf)ic_MainFunc.get(i)).getStr().contains("str$")) {
                    declGlobalStr dgs = new declGlobalStr(((Printf)ic_MainFunc.get(i)).getStr(), ((NormalString)ic_MainFunc.get(i + 1)).getContent());
                    mipsCode.add(dgs);
                }
            }
        }
    }

    public void classify() {
        int ptr = 0;
        boolean isDeclEnd = false;
        while(ptr != internCode.size()) {
            if (internCode.get(ptr).getClass().getName().substring(internCode.get(ptr).getClass().getName().lastIndexOf(".") + 1).equals("FuncDef")) {
                isDeclEnd = true;
                if (((FuncDef) internCode.get(ptr)).getName().equals("main")) {
                    while (ptr != internCode.size()) {
                        ic_MainFunc.add(internCode.get(ptr));
                        ptr++;
                    }
                    break;
                }
                ArrayList<Code> thisFunc = new ArrayList<>();
                thisFunc.add(internCode.get(ptr));
                ptr++;
                while (!internCode.get(ptr).getClass().getName().substring(internCode.get(ptr).getClass().getName().lastIndexOf(".") + 1).equals("FuncDef")) {
                    thisFunc.add(internCode.get(ptr));
                    ptr++;
                }
                ptr--;
                ic_Func.add(thisFunc);
            }
            if (!isDeclEnd) {
                ic_Decl.add(internCode.get(ptr));
            }
            ptr++;
        }
    }

    public boolean isDigital(String str) {
        int ptr = 0;
        while (ptr != str.length()) {
            if (ptr == 0) {
                if (str.charAt(ptr) != '-' && !Character.isDigit(str.charAt(ptr))) {
                    return false;
                }
            } else {
                if (!Character.isDigit(str.charAt(ptr))) {
                    return false;
                }
            }
            ptr++;
        }
        return true;
    }
}

interface mipsCode {
    String mips();
}

class label implements mipsCode {
    private String name;

    public label(String name) {
        this.name = name;
    }

    @Override
    public String mips() {
        return name + ":";
    }
}

class text implements mipsCode {
    @Override
    public String mips() {
        return ".text";
    }
}

class data implements mipsCode {
    @Override
    public String mips() {
        return ".data";
    }
}

class declGlobalStr implements mipsCode {
    private String name;
    private String content;

    public declGlobalStr(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String mips() {
        return "    " + name + ": .asciiz \"" + content + "\"";
    }
}

class declGlobalInt implements mipsCode {
    private String name;

    public declGlobalInt(String name) {
        this.name = name;
    }

    @Override
    public String mips() {
        return "    " + name + ": .word 1";
    }
}

class mfhi implements mipsCode {
    public String reg;

    public mfhi(String reg) {
        this.reg = reg;
    }

    @Override
    public String mips() {
        return "    mfhi $" + reg;
    }
}

class mflo implements mipsCode {
    public String reg;

    public mflo(String reg) {
        this.reg = reg;
    }

    @Override
    public String mips() {
        return "    mflo $" + reg;
    }
}

class divu implements mipsCode {
    public String regSrc0;
    public String regSrc1;

    public divu(String regSrc0, String regSrc1) {
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String mips() {
        return "    divu $" + regSrc0 + ", $" + regSrc1;
    }
}

class mul implements mipsCode {
    public String regDst;
    public String regSrc0;
    public String regSrc1;

    public mul(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String mips() {
        return "    mul $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class subu implements mipsCode {
    public String regDst;
    public String regSrc0;
    public String regSrc1;

    public subu(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String mips() {
        return "    subu $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class jal implements mipsCode {
    public String label;

    public jal(String label) {
        this.label = label;
    }

    @Override
    public String mips() {
        return "    jal " + label;
    }
}

class jr implements mipsCode {
    public String reg;

    public jr(String reg) {
        this.reg = reg;
    }

    @Override
    public String mips() {
        return "    jr $" + reg;
    }
}

class addiu implements mipsCode {
    public String regDst;
    public String regSrc0;
    public int immediate;

    public addiu(String regDst, String regSrc0, int immediate) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.immediate = immediate;
    }

    @Override
    public String mips() {
        return "    addiu $" + regDst + ", $" + regSrc0 + ", " + immediate;
    }
}

class addu implements mipsCode {
    public String regDst;
    public String regSrc0;
    public String regSrc1;

    public addu(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String mips() {
        return "    addu $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class lw implements mipsCode {
    private String regDst;
    private int offset;
    private String regBase;

    public lw(String regDst, int offset, String regBase) {
        this.regDst = regDst;
        this.offset = offset;
        this.regBase = regBase;
    }

    @Override
    public String mips() {
        return "    lw $" + regDst + ", " + offset + "($" + regBase + ")";
    }
}

class sw implements mipsCode {
    private String regDst;
    private int offset;
    private String regBase;

    public sw(String regDst, int offset, String regBase) {
        this.regDst = regDst;
        this.offset = offset;
        this.regBase = regBase;
    }

    @Override
    public String mips() {
        return "    sw $" + regDst + ", " + offset + "($" + regBase + ")";
    }
}

class la implements mipsCode {
    private String regDst;
//    private int immediate;
    private String label;

    public la(String regDst, String label) {
        this.regDst = regDst;
        this.label = label;
    }

    @Override
    public String mips() {
        return "    la $" + regDst + ", " + label;
    }
}

class li implements mipsCode {
    private String regDst;
    private int code;

    public li(String regDst, int code) {
        this.regDst = regDst;
        this.code = code;
    }

    @Override
    public String mips() {
        return "    li $" + regDst + ", " + code;
    }
}

class syscall implements mipsCode {
    @Override
    public String mips() {
        return "    syscall";
    }
}
