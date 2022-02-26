import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        //this.print();
        System.out.println(">>>>>>>>>> InternCode2MIPS End <<<<<<<<<<");
    }

    public ArrayList<mipsCode> getMipsCode() {
        return mipsCode;
    }

    public void print() {
        for (mipsCode mc : mipsCode) {
            System.out.println(mc.toString());
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
                translateBranchEqualZero(ic, funcMap, "Func", funcName);
                translateCondition(ic, funcMap, "Func", funcName);
                transLateJump(ic, funcMap, "Func", funcName);
                translateLabelToGo(ic, funcMap, "Func", funcName);
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
//            ArrayList<String> param = new ArrayList<>();
//            for (String para : fd.getParaMap().keySet()) {
//                param.add(para);
//                System.out.println(para);
//            }
//            for (int i = param.size() - 1; i >= 0; i--) {
//                funcMap.put(param.get(i), funcOffset.get(funcName));
//                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
//            }
            for (String param : fd.getParaMap().keySet()) {
                funcMap.put(param, funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
        }
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
            translateBranchEqualZero(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateCondition(ic, mainfuncSymbolTable, "MainFunc", "main");
            transLateJump(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateLabelToGo(ic, mainfuncSymbolTable, "MainFunc", "main");
            translateFuncReturn(ic, mainfuncSymbolTable, "MainFunc", "main");
        }
        li li = new li("v0", 10);
        mipsCode.add(li);
        syscall syscall = new syscall();
        mipsCode.add(syscall);
    }

    public void globalDataInit() {
        mipsCode.add(new label("$initCETLEWAinit$"));
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
                if (!fu.getParam().get(i).contains("$arr")) {
                    if (symbolTable.get(fu.getParam().get(i)) == null) {
                        la la = new la("t0", fu.getParam().get(i));
                        mipsCode.add(la);
                        lw lw = new lw("t0", 0, "t0");
                        mipsCode.add(lw);
                    } else {
                        int offset = symbolTable.get(fu.getParam().get(i));
                        lw lw = new lw("t0", offset, "sp");
                        mipsCode.add(lw);
                    }
                } else {
                    // 保存传入的一维/二维数组首地址
                    if (symbolTable.get(fu.getParam().get(i)) == null) {
                        la la = new la("t0", fu.getParam().get(i));
                        mipsCode.add(la);
                    } else {
                        // 判断函数多重调用时，传入内层调用的参数是否是本函数的形式参数，如果是则不加ra，否则加ra
                        if (Integer.parseInt(fu.getParam().get(i).split("\\$")[1]) >= 10000) {
                            int offset = symbolTable.get(fu.getParam().get(i));
                            lw lw = new lw("t0", offset, "sp");
                            mipsCode.add(lw);
                        }
                        else {
                            int offset = symbolTable.get(fu.getParam().get(i));
                            li li = new li("t0", offset);
                            mipsCode.add(li);
                            addu addu = new addu("t0", "t0", "sp");
                            mipsCode.add(addu);
                        }
                    }
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
        if (funcName.equals("main")) {
            li li = new li("v0", 10);
            mipsCode.add(li);
            syscall syscall = new syscall();
            mipsCode.add(syscall);
            return;
        }
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

    public void translateLabelToGo(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("LabelToGo")) return;
        LabelToGo ltg = (LabelToGo) ic;
        label label = new label(ltg.getLabel());
        mipsCode.add(label);
    }

    public void transLateJump(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("Jump")) return;
        Jump jp = (Jump) ic;
        j j = new j(jp.getLabel());
        mipsCode.add(j);
    }

    public void translateBranchEqualZero(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("BranchEqualZero")) return;
        BranchEqualZero bez = (BranchEqualZero) ic;
        if (symbolTable.get(bez.getJudgeObj()) == null) {
            la la = new la("t0", bez.getJudgeObj());
            mipsCode.add(la);
            lw lw = new lw("t0", 0, "t0");
            mipsCode.add(lw);
        } else {
            int offset = symbolTable.get(bez.getJudgeObj());
            lw lw = new lw("t0", offset, "sp");
            mipsCode.add(lw);
        }
        beq beq = new beq("t0", "0", bez.getLabel());
        mipsCode.add(beq);
    }

    public void translateCondition(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("Condition")) return;
        Condition cond = (Condition) ic;
        // 获取左值
        String left = cond.getLeft();
        if (isDigital(left)) {
            li li = new li("t0", Integer.parseInt(left));
            mipsCode.add(li);
        }
        else if (symbolTable.get(left) != null) {
            int offset = symbolTable.get(left);
            lw lw = new lw("t0", offset, "sp");
            mipsCode.add(lw);
        }
        else {
            la la = new la("t0", left);
            mipsCode.add(la);
            lw lw = new lw("t0", 0, "t0");
            mipsCode.add(lw);
        }
        // 判断是不是表达式
        if (cond.getOp() == null) {
            sne sne = new sne("t0", "t0", "0");
            mipsCode.add(sne);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        } else {
            // 获取右值
            String right = cond.getRight();
            if (isDigital(right)) {
                li li = new li("t1", Integer.parseInt(right));
                mipsCode.add(li);
            }
            else if (symbolTable.get(right) != null) {
                int offset = symbolTable.get(right);
                lw lw = new lw("t1", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                la la = new la("t1", right);
                mipsCode.add(la);
                lw lw = new lw("t1", 0, "t1");
                mipsCode.add(lw);
            }
            // 获取符号
            SymbolType op = cond.getOp();
            if (op == SymbolType.LSS) {
                slt slt = new slt("t0", "t0", "t1");
                mipsCode.add(slt);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
            else if (op == SymbolType.LEQ) {
                sle sle = new sle("t0", "t0", "t1");
                mipsCode.add(sle);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
            else if (op == SymbolType.GRE) {
                sgt sgt = new sgt("t0", "t0", "t1");
                mipsCode.add(sgt);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
            else if (op == SymbolType.GEQ) {
                sge sge = new sge("t0", "t0", "t1");
                mipsCode.add(sge);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
            else if (op == SymbolType.EQL) {
                seq seq = new seq("t0", "t0", "t1");
                mipsCode.add(seq);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
            else if (op == SymbolType.NEQ) {
                sne sne = new sne("t0", "t0", "t1");
                mipsCode.add(sne);
                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                mipsCode.add(sw);
                symbolTable.put(cond.getTarget(), funcOffset.get(funcName));
                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
            }
        }
    }

    public void translateConVarDef(Code ic, HashMap<String, Integer> symbolTable, String location, String funcName) {
        if (!ic.getClass().getName().equals("ConVarDef")) return;
        ConVarDef cvd = (ConVarDef) ic;
        if (!cvd.getName().contains("$arr")) {
            symbolTable.put(cvd.getName(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        } else {
            // 数组
            int arrSize = 0;
            if (!cvd.getName().contains("$dim2")) {
                // 一维数组
                arrSize = cvd.getDimSize().get("dim1") * 4;
            } else {
                // 二维数组
                arrSize = cvd.getDimSize().get("dim1") * cvd.getDimSize().get("dim2") * 4;
            }
            symbolTable.put(cvd.getName(), funcOffset.get(funcName) - arrSize + 4);
            funcOffset.put(funcName, funcOffset.get(funcName) - arrSize);
        }
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
            div div = new div("t0", "t1");
            mipsCode.add(div);
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
            div div = new div("t0", "t1");
            mipsCode.add(div);
            mfhi mfhi = new mfhi("t0");
            mipsCode.add(mfhi);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == SymbolType.NOT) {
            if (isDigital(arith.getRight())) {
                li li = new li("t0", Integer.parseInt(arith.getRight()));
                mipsCode.add(li);
            }
            else if (arith.getRight().contains("$$")) {
                int offset = symbolTable.get(arith.getRight());
                lw lw = new lw("t0", offset, "sp");
                mipsCode.add(lw);
            }
            else {
                if (location.equals("Global")
                        || (location.equals("MainFunc") && symbolTable.get(arith.getRight()) == null)
                        || (location.equals("Func") && symbolTable.get(arith.getRight()) == null)) {
                    la la = new la("t0", arith.getRight());
                    mipsCode.add(la);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    int offset = symbolTable.get(arith.getRight());
                    lw lw = new lw("t0", offset, "sp");
                    mipsCode.add(lw);
                }
            }
            seq seq = new seq("t0", "t0", "0");
            mipsCode.add(seq);
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
        }
        else if (arith.getOp() == null) {
            // Cond
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
            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
            mipsCode.add(sw);
            symbolTable.put(arith.getTarget(), funcOffset.get(funcName));
            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
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
                if (!assign.getTarget().contains("$arr")) {
                    // 普通变量
                    sw sw = new sw("v0", 0, "t0");
                    mipsCode.add(sw);
                } else {
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t1", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                // a[const] const 为全局变量
                                la la1 = new la("t1", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li1 = new li("t1", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li1);
                            li li2 = new li("t2", dim1size);
                            mipsCode.add(li2);
                            mul mul = new mul("t1", "t1", "t2");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la1 = new la("t1", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw1 = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t1", "t1", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la1 = new la("t2", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la1);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                addu addu = new addu("t1", "t1", "t2");
                                mipsCode.add(addu);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                addu addu = new addu("t1", "t1", "t2");
                                mipsCode.add(addu);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    addu addu = new addu("t0", "t0", "t1");
                    mipsCode.add(addu);
                    sw sw = new sw("v0", 0, "t0");
                    mipsCode.add(sw);
                }
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset = symbolTable.get(assign.getTarget());
                if (!assign.getTarget().contains("$arr")) {
                    sw sw = new sw("v0", offset, "sp");
                    mipsCode.add(sw);
                } else {
                    boolean flag0 = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag0 = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag0) {
                        lw lw0 = new lw("t0", offset, "sp");
                        mipsCode.add(lw0);
                    }
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t1", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                // a[const] const 为全局变量
                                la la = new la("t1", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                // a[local] local 为局部变量
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li1 = new li("t1", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li1);
                            li li2 = new li("t2", dim1size);
                            mipsCode.add(li2);
                            mul mul = new mul("t1", "t1", "t2");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la = new la("t1", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw2 = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw2);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t1", "t1", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la = new la("t2", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                addu addu = new addu("t1", "t1", "t2");
                                mipsCode.add(addu);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                addu addu = new addu("t1", "t1", "t2");
                                mipsCode.add(addu);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    // 判断是否为数组形参
                    boolean flag = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag) {
                        addu addu = new addu("t0", "t0", "t1");
                        mipsCode.add(addu);
                    } else {
                        addiu addiu = new addiu("t0", "t1", offset);
                        mipsCode.add(addiu);
                        addu addu = new addu("t0", "t0", "sp");
                        mipsCode.add(addu);
                    }
                    sw sw = new sw("v0", 0, "t0");
                    mipsCode.add(sw);
                }
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
                if (!assign.getTarget().contains("$arr")) {
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                } else {
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la1 = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li1 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li1);
                            li li2 = new li("t3", dim1size);
                            mipsCode.add(li2);
                            mul mul = new mul("t2", "t2", "t3");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la1 = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la1 = new la("t3", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la1);
                                lw lw = new lw("t3", 0, "t3");
                                mipsCode.add(lw);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw = new lw("t3", tempValOffset, "sp");
                                mipsCode.add(lw);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    addu addu = new addu("t1", "t1", "t2");
                    mipsCode.add(addu);
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                }
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset = symbolTable.get(assign.getTarget());
                if (!assign.getTarget().contains("$arr")) {
                    sw sw = new sw("t0", offset, "sp");
                    mipsCode.add(sw);
                } else {
                    boolean flag0 = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag0 = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag0) {
                        lw lw0 = new lw("t1", offset, "sp");
                        mipsCode.add(lw0);
                    }
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li1 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li1);
                            li li2 = new li("t3", dim1size);
                            mipsCode.add(li2);
                            mul mul = new mul("t2", "t2", "t3");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw1 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la = new la("t3", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la);
                                lw lw = new lw("t3", 0, "t3");
                                mipsCode.add(lw);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw = new lw("t3", tempValOffset, "sp");
                                mipsCode.add(lw);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    // 判断是否为数组形参
                    boolean flag = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag) {
                        addu addu = new addu("t1", "t1", "t2");
                        mipsCode.add(addu);
                    } else {
                        addiu addiu = new addiu("t1", "t2", offset);
                        mipsCode.add(addiu);
                        addu addu = new addu("t1", "t1", "sp");
                        mipsCode.add(addu);
                    }
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                }
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
                if (!assign.getTarget().contains("$arr")) {
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                } else {
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la1 = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw1 = new lw("t2", 0, "t2");
                                mipsCode.add(lw1);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw0 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw0);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            li li1 = new li("t3", dim1size);
                            mipsCode.add(li1);
                            mul mul = new mul("t2", "t2", "t3");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la1 = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la1);
                                lw lw1 = new lw("t2", 0, "t2");
                                mipsCode.add(lw1);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw1 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la1 = new la("t3", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la1);
                                lw lw1 = new lw("t3", 0, "t3");
                                mipsCode.add(lw1);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw0 = new lw("t3", tempValOffset, "sp");
                                mipsCode.add(lw0);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    addu addu = new addu("t1", "t1", "t2");
                    mipsCode.add(addu);
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                }
            }
            else if (location.equals("MainFunc") || location.equals("Func")) {
                int offset1 = symbolTable.get(assign.getTarget());
                if (!assign.getTarget().contains("$arr")) {
                    sw sw = new sw("t0", offset1, "sp");
                    mipsCode.add(sw);
                } else {
                    boolean flag0 = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag0 = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag0) {
                        lw lw0 = new lw("t1", offset1, "sp");
                        mipsCode.add(lw0);
                    }
                    // 数组
                    if (!assign.getTarget().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw1 = new lw("t2", 0, "t2");
                                mipsCode.add(lw1);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw1 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                            li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                            mipsCode.add(li0);
                            li li1 = new li("t3", dim1size);
                            mipsCode.add(li1);
                            mul mul = new mul("t2", "t2", "t3");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw1 = new lw("t2", 0, "t2");
                                mipsCode.add(lw1);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw2 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw2);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t2", "t2", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                la la = new la("t3", assign.getTargetDimIndex().get("dim2"));
                                mipsCode.add(la);
                                lw lw1 = new lw("t3", 0, "t3");
                                mipsCode.add(lw1);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw1 = new lw("t3", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                    }
                    // 判断是否为数组形参
                    boolean flag = false;
                    for (Code code : this.internCode) {
                        if (code.getClass().getName().equals("FuncDef")) {
                            FuncDef temp = (FuncDef) code;
                            if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                    flag = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag) {
                        addu addu = new addu("t1", "t1", "t2");
                        mipsCode.add(addu);
                    } else {
                        addiu addiu = new addiu("t1", "t2", offset1);
                        mipsCode.add(addiu);
                        addu addu = new addu("t1", "t1", "sp");
                        mipsCode.add(addu);
                    }
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                }
            }
        }
        else {
            if (assign.getValueDimIndex().size() == 0) {
                // value的值为 a$0 类型
                if (location.equals("Global")) {
                    la la0 = new la("t0", assign.getValue());
                    mipsCode.add(la0);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                    if (!assign.getTarget().contains("$arr")) {
                        la la1 = new la("t1", assign.getTarget());
                        mipsCode.add(la1);
                    } else {
                        la la1 = new la("t1", assign.getTarget());
                        mipsCode.add(la1);
                        if (!assign.getTarget().contains("$dim2_")) {
                            // 一维数组
                            if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                                li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                                mipsCode.add(li0);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw1 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        } else {
                            // 二维数组
                            int dim1size = 0;
                            Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                            while (matcher.find()) {
                                dim1size = Integer.parseInt(matcher.group().substring(5));
                            }
                            // 第一维
                            if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                                li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                                mipsCode.add(li0);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                lw lw2 = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw2);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            }
                            // 第二维
                            if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                                addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                                mipsCode.add(addiu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                lw lw1 = new lw("t3", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                addu addu = new addu("t2", "t2", "t3");
                                mipsCode.add(addu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            }
                        }
                        addu addu = new addu("t1", "t1", "t2");
                        mipsCode.add(addu);
                    }
                    sw sw = new sw("t0", 0, "t1");
                    mipsCode.add(sw);
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    // value
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
                    // target
                    if (!assign.getTarget().contains("$arr")){
                        // a = c
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
                    else {
                        // 数组 a[][] = c
                        if (symbolTable.get(assign.getTarget()) == null) {
                            la la = new la("t1", assign.getTarget());
                            mipsCode.add(la);
                        }
                        else {
                            int offset1 = symbolTable.get(assign.getTarget());
                            // 判断是否为数组形参
                            boolean flag = false;
                            for (Code code : this.internCode) {
                                if (code.getClass().getName().equals("FuncDef")) {
                                    FuncDef temp = (FuncDef) code;
                                    if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                        if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getTarget())) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                lw lw = new lw("t1", offset1, "sp");
                                mipsCode.add(lw);
                            }
                            else {
                                li li = new li("t1", offset1);
                                mipsCode.add(li);
                                addu addu = new addu("t1", "t1", "sp");
                                mipsCode.add(addu);
                            }
                        }

                        if (!assign.getTarget().contains("$dim2_")) {
                            // 一维数组
                            if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                                li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                                mipsCode.add(li0);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                    la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                    mipsCode.add(la);
                                    lw lw = new lw("t2", 0, "t2");
                                    mipsCode.add(lw);
                                    sll sll = new sll("t2", "t2", 2);
                                    mipsCode.add(sll);
                                } else {
                                    int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                    lw lw1 = new lw("t2", tempValOffset, "sp");
                                    mipsCode.add(lw1);
                                    sll sll = new sll("t2", "t2", 2);
                                    mipsCode.add(sll);
                                }
                            }
                        } else {
                            // 二维数组
                            int dim1size = 0;
                            Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getTarget());
                            while (matcher.find()) {
                                dim1size = Integer.parseInt(matcher.group().substring(5));
                            }
                            // 第一维
                            if (isDigital(assign.getTargetDimIndex().get("dim1"))) {
                                li li0 = new li("t2", Integer.parseInt(assign.getTargetDimIndex().get("dim1")));
                                mipsCode.add(li0);
                                li li1 = new li("t3", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t2", "t2", "t3");
                                mipsCode.add(mul);
                            } else {
                                if (symbolTable.get(assign.getTargetDimIndex().get("dim1")) == null) {
                                    la la = new la("t2", assign.getTargetDimIndex().get("dim1"));
                                    mipsCode.add(la);
                                    lw lw = new lw("t2", 0, "t2");
                                    mipsCode.add(lw);
                                    li li1 = new li("t3", dim1size);
                                    mipsCode.add(li1);
                                    mul mul = new mul("t2", "t2", "t3");
                                    mipsCode.add(mul);
                                } else {
                                    int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim1"));
                                    lw lw2 = new lw("t2", tempValOffset, "sp");
                                    mipsCode.add(lw2);
                                    li li1 = new li("t3", dim1size);
                                    mipsCode.add(li1);
                                    mul mul = new mul("t2", "t2", "t3");
                                    mipsCode.add(mul);
                                }
                            }
                            // 第二维
                            if (isDigital(assign.getTargetDimIndex().get("dim2"))) {
                                addiu addiu = new addiu("t2", "t2", Integer.parseInt(assign.getTargetDimIndex().get("dim2")));
                                mipsCode.add(addiu);
                                sll sll = new sll("t2", "t2", 2);
                                mipsCode.add(sll);
                            } else {
                                if (symbolTable.get(assign.getTargetDimIndex().get("dim2")) == null) {
                                    la la = new la("t3", assign.getTargetDimIndex().get("dim2"));
                                    mipsCode.add(la);
                                    lw lw = new lw("t3", 0, "t3");
                                    mipsCode.add(lw);
                                    addu addu = new addu("t2", "t2", "t3");
                                    mipsCode.add(addu);
                                    sll sll = new sll("t2", "t2", 2);
                                    mipsCode.add(sll);
                                } else {
                                    int tempValOffset = symbolTable.get(assign.getTargetDimIndex().get("dim2"));
                                    lw lw1 = new lw("t3", tempValOffset, "sp");
                                    mipsCode.add(lw1);
                                    addu addu = new addu("t2", "t2", "t3");
                                    mipsCode.add(addu);
                                    sll sll = new sll("t2", "t2", 2);
                                    mipsCode.add(sll);
                                }
                            }
                        }
                        addu addu = new addu("t1", "t1", "t2");
                        mipsCode.add(addu);
                        sw sw = new sw("t0", 0, "t1");
                        mipsCode.add(sw);
                    }
                }
            }
            else {
                // value 为数组的情况 只可能是一种情况 t$$0 = a$0$arr$dim1_2$dim2_3 [1] [1]
                if (location.equals("Global")) {
                    // 数组基址
                    la la0 = new la("t0", assign.getValue());
                    mipsCode.add(la0);
//                    int offset0 = 0;
                    if (!assign.getValue().contains("$dim2_")) {
                        // 一维数组
                        if (isDigital(assign.getValueDimIndex().get("dim1"))) {
                            li li = new li("t1", Integer.parseInt(assign.getValueDimIndex().get("dim1")));
                            mipsCode.add(li);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
//                            offset0 = 4 * Integer.parseInt(assign.getValueDimIndex().get("dim1"));
                        } else {
                            if (symbolTable.get(assign.getValueDimIndex().get("dim1")) == null) {
                                // t = a[const] const 为全局变量
                                la la = new la("t1", assign.getValueDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                // t = a[local] local 为局部变量
                                int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim1"));
                                lw lw = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
                    } else {
                        // 二维数组
                        int dim1size = 0;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getValue());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getValueDimIndex().get("dim1"))) {
                            li li = new li("t1", Integer.parseInt(assign.getValueDimIndex().get("dim1")));
                            mipsCode.add(li);
                            li li1 = new li("t2", dim1size);
                            mipsCode.add(li1);
                            mul mul = new mul("t1", "t1", "t2");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getValueDimIndex().get("dim1")) == null) {
                                la la = new la("t1", assign.getValueDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim1"));
                                lw lw1 = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (isDigital(assign.getValueDimIndex().get("dim2"))) {
                            addiu addiu = new addiu("t1", "t1", Integer.parseInt(assign.getValueDimIndex().get("dim2")));
                            mipsCode.add(addiu);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getValueDimIndex().get("dim2")) == null) {
                                la la = new la("t2", assign.getValueDimIndex().get("dim2"));
                                mipsCode.add(la);
                                lw lw = new lw("t2", 0, "t2");
                                mipsCode.add(lw);
                                addu addu1 = new addu("t1", "t1", "t2");
                                mipsCode.add(addu1);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim2"));
                                lw lw = new lw("t2", tempValOffset, "sp");
                                mipsCode.add(lw);
                                addu addu1 = new addu("t1", "t1", "t2");
                                mipsCode.add(addu1);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
//                        offset0 = 4 * (Integer.parseInt(assign.getValueDimIndex().get("dim1")) * dim1size + Integer.parseInt(assign.getValueDimIndex().get("dim2")));
                    }
//                    addiu addiu0 = new addiu("t0", "t0", offset0);
//                    mipsCode.add(addiu0);
                    addu addu = new addu("t0", "t0", "t1");
                    mipsCode.add(addu);
                    lw lw = new lw("t0", 0, "t0");
                    mipsCode.add(lw);
                    // 存入target
                    if (!assign.getTarget().contains("$arr")) {
                        // 普通变量
                        if (assign.getTarget().contains("t$$")) {
                            sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                            mipsCode.add(sw);
                            symbolTable.put(assign.getTarget(), funcOffset.get(funcName));
                            funcOffset.put(funcName, funcOffset.get(funcName) - 4);
                        } else {
                            la la1 = new la("t1", assign.getTarget());
                            mipsCode.add(la1);
                            sw sw0 = new sw("t0", 0, "t1");
                            mipsCode.add(sw0);
                        }
                    }
                }
                else if (location.equals("MainFunc") || location.equals("Func")) {
                    // value
                    boolean islocal = false;
                    boolean isArrParam = false;
                    int localoffset = 0;
                    if (symbolTable.get(assign.getValue()) == null) {
                        // 数组基址
                        la la0 = new la("t0", assign.getValue());
                        mipsCode.add(la0);
                    } else {
                        // 数组基址
                        islocal = true;
                        localoffset = symbolTable.get(assign.getValue());
                        boolean flag = false;
                        for (Code code : this.internCode) {
                            if (code.getClass().getName().equals("FuncDef")) {
                                FuncDef temp = (FuncDef) code;
                                if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                    if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getValue())) {
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (flag) {
                            lw lw0 = new lw("t0", localoffset, "sp");
                            mipsCode.add(lw0);
                        } else {
                            li li = new li("t0", localoffset);
                            mipsCode.add(li);
                        }
                    }
//                    int offset0 = 0;
                    if (!assign.getValue().contains("$dim2_") && assign.getValueDimIndex().size() == 1) {
                        // 一维数组
                        if (isDigital(assign.getValueDimIndex().get("dim1"))) {
                            li li = new li("t1", Integer.parseInt(assign.getValueDimIndex().get("dim1")));
                            mipsCode.add(li);
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        } else {
                            if (symbolTable.get(assign.getValueDimIndex().get("dim1")) == null) {
                                la la = new la("t1", assign.getValueDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim1"));
                                lw lw = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            }
                        }
//                        offset0 = 4 * Integer.parseInt(assign.getValueDimIndex().get("dim1"));
                    } else {
                        // 二维数组
                        int dim1size = -1;
                        Matcher matcher = Pattern.compile("dim2_\\d+").matcher(assign.getValue());
                        while (matcher.find()) {
                            dim1size = Integer.parseInt(matcher.group().substring(5));
                        }
                        // 第一维
                        if (isDigital(assign.getValueDimIndex().get("dim1"))) {
                            li li = new li("t1", Integer.parseInt(assign.getValueDimIndex().get("dim1")));
                            mipsCode.add(li);
                            li li1 = new li("t2", dim1size);
                            mipsCode.add(li1);
                            mul mul = new mul("t1", "t1", "t2");
                            mipsCode.add(mul);
                        } else {
                            if (symbolTable.get(assign.getValueDimIndex().get("dim1")) == null) {
                                la la = new la("t1", assign.getValueDimIndex().get("dim1"));
                                mipsCode.add(la);
                                lw lw = new lw("t1", 0, "t1");
                                mipsCode.add(lw);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            } else {
                                int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim1"));
                                lw lw1 = new lw("t1", tempValOffset, "sp");
                                mipsCode.add(lw1);
                                li li1 = new li("t2", dim1size);
                                mipsCode.add(li1);
                                mul mul = new mul("t1", "t1", "t2");
                                mipsCode.add(mul);
                            }
                        }
                        // 第二维
                        if (assign.getValueDimIndex().get("dim2") != null) {
                            if (isDigital(assign.getValueDimIndex().get("dim2"))) {
                                addiu addiu = new addiu("t1", "t1", Integer.parseInt(assign.getValueDimIndex().get("dim2")));
                                mipsCode.add(addiu);
                                sll sll = new sll("t1", "t1", 2);
                                mipsCode.add(sll);
                            } else {
                                if (symbolTable.get(assign.getValueDimIndex().get("dim2")) == null) {
                                    la la = new la("t2", assign.getValueDimIndex().get("dim2"));
                                    mipsCode.add(la);
                                    lw lw = new lw("t2", 0, "t2");
                                    mipsCode.add(lw);
                                    addu addu = new addu("t1", "t1", "t2");
                                    mipsCode.add(addu);
                                    sll sll = new sll("t1", "t1", 2);
                                    mipsCode.add(sll);
                                } else {
                                    int tempValOffset = symbolTable.get(assign.getValueDimIndex().get("dim2"));
                                    lw lw = new lw("t2", tempValOffset, "sp");
                                    mipsCode.add(lw);
                                    addu addu = new addu("t1", "t1", "t2");
                                    mipsCode.add(addu);
                                    sll sll = new sll("t1", "t1", 2);
                                    mipsCode.add(sll);
                                }
                            }
                        } else {
                            // 数组传参 传入二维数组的一个维度
                            isArrParam = true;
                            sll sll = new sll("t1", "t1", 2);
                            mipsCode.add(sll);
                        }
                    }
                    if (!islocal) {
                        // 数组是全局的
                        addu addu = new addu("t0", "t0", "t1");
                        mipsCode.add(addu);
//                    addiu addiu0 = new addiu("t0", "t0", offset0);
//                    mipsCode.add(addiu0);
                    } else {
                        boolean flag = false;
                        for (Code code : this.internCode) {
                            if (code.getClass().getName().equals("FuncDef")) {
                                FuncDef temp = (FuncDef) code;
                                if (!temp.getName().equals("main") && temp.getName().equals(funcName)) {
                                    if (temp.getParaMap() != null && temp.getParaMap().containsKey(assign.getValue())) {
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                        }
                        //数组是局部的
                        if (flag) {
                            addu addu = new addu("t0", "t0", "t1");
                            mipsCode.add(addu);
                        } else {
                            // TODO : 如果当前数组是函数参数取值，那么不要加sp
                            addiu addiu = new addiu("t0", "t1", localoffset);
                            mipsCode.add(addiu);
                            addu addu = new addu("t0", "t0", "sp");
                            mipsCode.add(addu);
                        }
                    }
                    // 如果不是数组传参，就取值，不然就保留地址
                    if (!isArrParam) {
                        //不是数组传参
                        lw lw = new lw("t0", 0, "t0");
                        mipsCode.add(lw);
                    }
                    // target
                    if (!assign.getTarget().contains("$arr")) {
                        // 普通变量
                        if (symbolTable.get(assign.getTarget()) == null) {
                            if (assign.getTarget().contains("t$$")) {
                                sw sw = new sw("t0", funcOffset.get(funcName), "sp");
                                mipsCode.add(sw);
                                symbolTable.put(assign.getTarget(), funcOffset.get(funcName));
                                funcOffset.put(funcName, funcOffset.get(funcName) - 4);
                            } else {
                                la la1 = new la("t1", assign.getTarget());
                                mipsCode.add(la1);
                                sw sw0 = new sw("t0", 0, "t1");
                                mipsCode.add(sw0);
                            }
                        } else {
                            int offset = symbolTable.get(assign.getTarget());
                            sw sw = new sw("t0", offset, "sp");
                            mipsCode.add(sw);
                        }
                    }
                }
            }
        }
    }

    public void translateData() {
        mipsCode.add(new data());
        for (Code ic : ic_Decl) {
            if (ic.getClass().getName().substring(ic.getClass().getName().lastIndexOf(".") + 1).equals("ConVarDef")) {
                if (((ConVarDef)ic).getDimSize().size() == 0) {
                    declGlobalInt dgi = new declGlobalInt(((ConVarDef)ic).getName());
                    mipsCode.add(dgi);
                } else {
                    declGlobalInt dgi = new declGlobalInt(((ConVarDef)ic).getName(), ((ConVarDef)ic).getDimSize());
                    mipsCode.add(dgi);
                }
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
                    if (ptr == internCode.size()) break;
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
    @Override
    String toString();
}

class label implements mipsCode {
    private String name;

    public label(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ":";
    }
}

class text implements mipsCode {
    @Override
    public String toString() {
        return ".text";
    }
}

class data implements mipsCode {
    @Override
    public String toString() {
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
    public String toString() {
        return "    " + name + ": .asciiz \"" + content + "\"";
    }
}

class declGlobalInt implements mipsCode {
    private String name;
    private HashMap<String, Integer> sizeMap;

    public declGlobalInt(String name) {
        this.name = name;
    }

    public declGlobalInt(String name, HashMap<String, Integer> sizeMap) {
        this.name = name;
        this.sizeMap = sizeMap;
    }

    @Override
    public String toString() {
        if (sizeMap == null) {
            return "    " + name + ": .space 4";
        }
        else {
            int size = 4;
            for (String dim : sizeMap.keySet()) {
                size *= sizeMap.get(dim);
            }
            return "    " + name + ": .space " + size;
        }
    }
}

class move implements mipsCode {
    private String regDst;
    private String regSrc;

    public move(String regDst, String regSrc) {
        this.regDst = regDst;
        this.regSrc = regSrc;
    }

    @Override
    public String toString() {
        return "    move $" + regDst + ", $" + regSrc;
    }
}

class sne implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public sne(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    sne $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class slt implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public slt(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    slt $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class sle implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public sle(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    sle $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class sgt implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public sgt(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    sgt $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class sge implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public sge(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    sge $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class seq implements mipsCode {
    private String regDst;
    private String regSrc0;
    private String regSrc1;

    public seq(String regDst, String regSrc0, String regSrc1) {
        this.regDst = regDst;
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    seq $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class mfhi implements mipsCode {
    public String reg;

    public mfhi(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return "    mfhi $" + reg;
    }
}

class mflo implements mipsCode {
    public String reg;

    public mflo(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return "    mflo $" + reg;
    }
}

class div implements mipsCode {
    public String regSrc0;
    public String regSrc1;

    public div(String regSrc0, String regSrc1) {
        this.regSrc0 = regSrc0;
        this.regSrc1 = regSrc1;
    }

    @Override
    public String toString() {
        return "    div $" + regSrc0 + ", $" + regSrc1;
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
    public String toString() {
        return "    mul $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class sll implements mipsCode {
    public String regDst;
    public String reg;
    public int width;

    public sll(String regDst, String reg, int width) {
        this.regDst = regDst;
        this.reg = reg;
        this.width = width;
    }

    @Override
    public String toString() {
        return "    sll $" + regDst + ", $" + reg + ", " + width;
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
    public String toString() {
        return "    subu $" + regDst + ", $" + regSrc0 + ", $" + regSrc1;
    }
}

class beq implements mipsCode {
    public String reg0;
    public String reg1;
    public String label;

    public beq(String reg0, String reg1, String label) {
        this.reg0 = reg0;
        this.reg1 = reg1;
        this.label = label;
    }

    @Override
    public String toString() {
        return "    beq $" + reg0 + ", $" + reg1 + ", " + label;
    }
}

class j implements mipsCode {
    public String label;

    public j(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "    j " + label;
    }
}

class jal implements mipsCode {
    public String label;

    public jal(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "    jal " + label;
    }
}

class jr implements mipsCode {
    public String reg;

    public jr(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
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
    public String toString() {
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
    public String toString() {
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

    public String getRegDst() {
        return regDst;
    }

    public int getOffset() {
        return offset;
    }

    public String getRegBase() {
        return regBase;
    }

    @Override
    public String toString() {
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

    public String getRegDst() {
        return regDst;
    }

    public int getOffset() {
        return offset;
    }

    public String getRegBase() {
        return regBase;
    }

    @Override
    public String toString() {
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
    public String toString() {
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
    public String toString() {
        return "    li $" + regDst + ", " + code;
    }
}

class syscall implements mipsCode {
    @Override
    public String toString() {
        return "    syscall";
    }
}
