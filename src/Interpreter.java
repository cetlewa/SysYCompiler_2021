import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Interpreter {
    private Node CompUnit;
    private ArrayList<Code> codeList;
    private LinkedHashMap<String, Integer> constValues; // const name <-> value
    private ArrayList<recArray> arrayValues;
    private ArrayList<String> identNames;
    private int normalStrIndex;
    private int condIndex;

    public Interpreter(Node CompUnit) {
        this.CompUnit = CompUnit;
        this.codeList = new ArrayList<>();
        this.constValues = new LinkedHashMap<>();
        this.arrayValues = new ArrayList<>();
        this.identNames = new ArrayList<>();
        this.normalStrIndex = 0;
        this.condIndex = 0;
        this.analyze();
        this.dealFuncUseArr();
        this.updateIdentNames();
        //this.print();
        System.out.println(">>>>>>>>>> Interpreter End <<<<<<<<<<");
    }

    public ArrayList<Code> getCodeList() {
        return codeList;
    }

    public void dealFuncUseArr() {
        // 区分函数中的数组与变量
        for (Code findfu : this.codeList) {
            if (!findfu.getClass().getName().equals("FuncUse")) continue;
            FuncUse fu = (FuncUse) findfu;
            FuncDef matchfd = new FuncDef();
            for (Code findfd : this.codeList) {
                if (!findfd.getClass().getName().equals("FuncDef")) continue;
                FuncDef fd = (FuncDef) findfd;
                if (fd.getName().equals(fu.getName())) {
                    matchfd = fd;
                }
            }
            int paraPtr = 0;
            if (matchfd.getParaMap() != null) {
                for (String fpara : matchfd.getParaMap().keySet()) {
                    if (fpara.contains("$arr")) {
                        if (!fu.getParam().get(paraPtr).contains("t$$") && !fu.getParam().get(paraPtr).contains("$arr") && !isDigital(fu.getParam().get(paraPtr))) {
                            fu.getParam().set(paraPtr, fu.getParam().get(paraPtr) + "$arr");
                        }
                    }
                    paraPtr++;
                }
            }
        }
    }

    public void updateIdentNames() {
        for (Code ic : codeList) {
            if (ic.getClass().getName().equals("AssignValue")) {
                AssignValue av = (AssignValue) ic;
                if (av.getTarget().contains("$arr")) {
                    for (String name : identNames) {
                        if (name.startsWith(av.getTarget())) {
                            av.setTarget(name);
                            break;
                        }
                    }
                } else if (av.getValue().contains("$arr")) {
                    for (String name : identNames) {
                        if (name.startsWith(av.getValue())) {
                            av.setValue(name);
                            break;
                        }
                    }
                }
            }
            else if (ic.getClass().getName().equals("FuncUse")) {
                FuncUse fu = (FuncUse) ic;
                for (String para : fu.getParam()) {
                    for (String name : identNames) {
                        if (name.startsWith(para) && para.contains("$arr") && name.contains("$arr")) {
                            fu.getParam().set(fu.getParam().indexOf(para), name);
                            break;
                        }
//                        if (name.startsWith(para) && para.contains("$arr") && name.contains("$arr") && !name.contains("$func")) {
//                            fu.getParam().set(fu.getParam().indexOf(para), name);
//                            break;
//                        }
                    }
                }
            }
        }
    }

    public void print() {
        for (Code code : codeList) {
            System.out.println(code.toString());
        }
    }

    public void analyze() {
        tran_CompUnit(CompUnit);
        for (Code c : codeList) {
            c.toString();
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
        HashMap<String, Integer> arrayRec = new HashMap<>();
        def.setType(NodeInfo.VAR);
        // get the dimension of array
        int arrDim = getArrayDimension(node);
        String target = "";
        int dimPtr = 1;
        for (Node varDefCh : node.getChildNode()) {
            switch (varDefCh.getNodeName()) {
                case "Ident":
                    if (arrDim == 0) {
                        target = varDefCh.getToken().getContent() + "$" + varDefCh.getApearIndex();
                        def.setName(target);
                        assign.setTarget(target);
                        codeList.add(def);
                    } else {
                        // a[][] a$arr$dim$2$0
                        target = varDefCh.getToken().getContent() + "$" + varDefCh.getApearIndex() + "$arr";
                        def.setName(target);
                        codeList.add(def);
                    }
                    break;
                case "ConstExp":
                    getArith(varDefCh, codeList);
                    if (!isDigital(varDefCh.getFinalValue())) {
                        calArith(varDefCh);
                        def.addDimSize("dim"+dimPtr, constValues.get(varDefCh.getFinalValue()));
                        def.setName(def.getName()+"$dim"+dimPtr+"_"+constValues.get(varDefCh.getFinalValue()));
                        target += "$dim"+dimPtr+"_"+constValues.get(varDefCh.getFinalValue());
                    } else {
                        def.addDimSize("dim"+dimPtr, Integer.parseInt(varDefCh.getFinalValue()));
                        def.setName(def.getName()+"$dim"+dimPtr+"_"+Integer.parseInt(varDefCh.getFinalValue()));
                        target += "$dim"+dimPtr+"_"+Integer.parseInt(varDefCh.getFinalValue());
                    }
                    dimPtr++;
                    break;
                case "InitVal":
                    if (arrDim == 0) {
                        assign.setValue(varDefCh.getChildNode().get(0).getFinalValue());
                        getArith(varDefCh.getChildNode().get(0), codeList);
                    }
                    else if (arrDim == 1) {
                        int dim1cnt = 0;
                        for (Node value : varDefCh.getChildNode()) {
                            if (value.getNodeName().equals("{") || value.getNodeName().equals("}")) continue;
                            if (value.getNodeName().equals(",")) {
                                dim1cnt++;
                                continue;
                            }
                            getArith(value.getChildNode().get(0), codeList);
                            calArith(value.getChildNode().get(0));
                            if (isDigital(value.getChildNode().get(0).getFinalValue())) {
                                arrayRec.put(String.valueOf(dim1cnt), Integer.parseInt(value.getChildNode().get(0).getFinalValue()));
                            } else {
                                arrayRec.put(String.valueOf(dim1cnt), constValues.get(value.getChildNode().get(0).getFinalValue()));
                            }
                            AssignValue av = new AssignValue();
                            av.setValue(value.getChildNode().get(0).getFinalValue());
                            av.setTarget(target);
                            av.putTargetDimIndex("dim1", String.valueOf(dim1cnt));
                            codeList.add(av);
                        }
                    }
                    else if (arrDim == 2) {
                        int dim1cnt = 0;
                        for (Node arrVal : varDefCh.getChildNode()) {
                            if (arrVal.getNodeName().equals("{") || arrVal.getNodeName().equals("}")) continue;
                            if (arrVal.getNodeName().equals(",")) {
                                dim1cnt++;
                                continue;
                            }
                            int dim2cnt = 0;
                            for (Node value : arrVal.getChildNode()) {
                                if (value.getNodeName().equals("{") || value.getNodeName().equals("}")) continue;
                                if (value.getNodeName().equals(",")) {
                                    dim2cnt++;
                                    continue;
                                }
                                getArith(value.getChildNode().get(0), codeList);
                                calArith(value.getChildNode().get(0));
                                if (isDigital(value.getChildNode().get(0).getFinalValue())) {
                                    arrayRec.put(dim1cnt+"+"+dim2cnt, Integer.parseInt(value.getChildNode().get(0).getFinalValue()));
                                } else {
                                    arrayRec.put(dim1cnt+"+"+dim2cnt, constValues.get(value.getChildNode().get(0).getFinalValue()));
                                }
                                AssignValue av = new AssignValue();
                                av.setValue(value.getChildNode().get(0).getFinalValue());
                                av.setTarget(target);
                                av.putTargetDimIndex("dim1", String.valueOf(dim1cnt));
                                av.putTargetDimIndex("dim2", String.valueOf(dim2cnt));
                                codeList.add(av);
                            }
                        }
                    }
                    break;
            }
        }
        if (arrayRec.size() != 0) {
            recArray ra = new recArray(target, arrayRec);
            arrayValues.add(ra);
        }
        if (assign.getValue() != null) codeList.add(assign);
        identNames.add(target);
    }

    public void tran_ConstDef(Node node) {
        ConVarDef def = new ConVarDef();
        AssignValue assign = new AssignValue();
        HashMap<String, Integer> arrayRec = new HashMap<>();
        def.setType(NodeInfo.CONST);
        // get the dimension of array
        int arrDim = getArrayDimension(node);
        String target = "";
        int dimPtr = 1;
        for (Node constDefCh : node.getChildNode()) {
            switch (constDefCh.getNodeName()) {
                case "Ident":
                    if (arrDim == 0) {
                        target = constDefCh.getToken().getContent() + "$" + constDefCh.getApearIndex();
                        def.setName(target);
                        assign.setTarget(target);
                        codeList.add(def);
                    } else {
                        // a[][] a$arr$dim$2$0
                        target = constDefCh.getToken().getContent() + "$" + constDefCh.getApearIndex() + "$arr";
                        def.setName(target);
                        codeList.add(def);
                    }
                    break;
                case "ConstExp":
                    getArith(constDefCh, codeList);
                    if (!isDigital(constDefCh.getFinalValue())) {
                        calArith(constDefCh);
                        def.addDimSize("dim"+dimPtr, constValues.get(constDefCh.getFinalValue()));
                        def.setName(def.getName()+"$dim"+dimPtr+"_"+constValues.get(constDefCh.getFinalValue()));
                        target += "$dim"+dimPtr+"_"+constValues.get(constDefCh.getFinalValue());
                    } else {
                        def.addDimSize("dim"+dimPtr, Integer.parseInt(constDefCh.getFinalValue()));
                        def.setName(def.getName()+"$dim"+dimPtr+"_"+Integer.parseInt(constDefCh.getFinalValue()));
                        target += "$dim"+dimPtr+"_"+Integer.parseInt(constDefCh.getFinalValue());
                    }
                    dimPtr++;
                    break;
                case "ConstInitVal":
                    if (arrDim == 0) {
                        assign.setValue(constDefCh.getChildNode().get(0).getFinalValue());
                        getArith(constDefCh.getChildNode().get(0), codeList);
                        if (!isDigital(constDefCh.getChildNode().get(0).getFinalValue())) {
                            calArith(constDefCh.getChildNode().get(0));
                            constValues.put(target, constValues.get(constDefCh.getChildNode().get(0).getFinalValue()));
                        } else {
                            constValues.put(target, Integer.parseInt(constDefCh.getChildNode().get(0).getFinalValue()));
                        }
                    }
                    else if (arrDim == 1) {
                        // ConstInitVal → '{' ConstInitVal ',' ConstInitVal ',' ... ConstInitVal '}'
                        int dim1cnt = 0;
                        for (Node value : constDefCh.getChildNode()) {
                            if (value.getNodeName().equals("{") || value.getNodeName().equals("}")) continue;
                            if (value.getNodeName().equals(",")) {
                                dim1cnt++;
                                continue;
                            }
                            getArith(value.getChildNode().get(0), codeList);
                            calArith(value.getChildNode().get(0));
                            if (isDigital(value.getChildNode().get(0).getFinalValue())) {
                                arrayRec.put(String.valueOf(dim1cnt), Integer.parseInt(value.getChildNode().get(0).getFinalValue()));
                            } else {
                                arrayRec.put(String.valueOf(dim1cnt), constValues.get(value.getChildNode().get(0).getFinalValue()));
                            }
                            AssignValue av = new AssignValue();
                            av.setValue(value.getChildNode().get(0).getFinalValue());
                            av.setTarget(target);
                            av.putTargetDimIndex("dim1", String.valueOf(dim1cnt));
                            codeList.add(av);
                        }
                    }
                    else if (arrDim == 2) {
                        // ConstInitVal → {{ConstInitVal, ...},...{ConstInitVal, ...}}
                        int dim1cnt = 0;
                        for (Node arrVal : constDefCh.getChildNode()) {
                            if (arrVal.getNodeName().equals("{") || arrVal.getNodeName().equals("}")) continue;
                            if (arrVal.getNodeName().equals(",")) {
                                dim1cnt++;
                                continue;
                            }
                            int dim2cnt = 0;
                            for (Node value : arrVal.getChildNode()) {
                                if (value.getNodeName().equals("{") || value.getNodeName().equals("}")) continue;
                                if (value.getNodeName().equals(",")) {
                                    dim2cnt++;
                                    continue;
                                }
                                getArith(value.getChildNode().get(0), codeList);
                                calArith(value.getChildNode().get(0));
                                if (isDigital(value.getChildNode().get(0).getFinalValue())) {
                                    arrayRec.put(dim1cnt+"+"+dim2cnt, Integer.parseInt(value.getChildNode().get(0).getFinalValue()));
                                } else {
                                    arrayRec.put(dim1cnt+"+"+dim2cnt, constValues.get(value.getChildNode().get(0).getFinalValue()));
                                }
                                AssignValue av = new AssignValue();
                                av.setValue(value.getChildNode().get(0).getFinalValue());
                                av.setTarget(target);
                                av.putTargetDimIndex("dim1", String.valueOf(dim1cnt));
                                av.putTargetDimIndex("dim2", String.valueOf(dim2cnt));
                                codeList.add(av);
                            }
                        }
                    }
                    break;
            }
        }
        if (arrayRec.size() != 0) {
            recArray ra = new recArray(target, arrayRec);
            arrayValues.add(ra);
        }
        if (arrDim == 0) codeList.add(assign);
        identNames.add(target);
    }

    public void tran_FuncDef(Node node) {
        FuncDef funcDef = new FuncDef();
        // manually add return at the bottom of every void function
        if (node.getChildNode().get(0).getType() == NodeInfo.VOIDFUN) {
            Node returnNode = new Node("return");
            Node stmtNode = new Node("Stmt");
            Node blockitemNode = new Node("BlockItem");
            returnNode.setParNode(stmtNode);
            stmtNode.addChildNode(returnNode);
            stmtNode.setParNode(blockitemNode);
            blockitemNode.addChildNode(stmtNode);
            node.getChildNode().get(node.getChildNode().size() - 1).addChildNode(blockitemNode);
        }
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
                    for (Node FuncFParam : funcDefCh.getChildNode()) {
                        if (FuncFParam.getNodeName().equals(",")) {
                            continue;
                        }
                        if (FuncFParam.getChildNode().size() != 7) continue;
                        Node constExp = FuncFParam.getChildNode().get(5);
                        String name = FuncFParam.getChildNode().get(1).getName() + "$" + FuncFParam.getChildNode().get(1).getApearIndex() + "$arr";
                        if (!isDigital(constExp.getFinalValue())) {
                            calArith(constExp);
                            ArrayList<String> order = new ArrayList<>(funcDef.getParaMap().keySet());
                            String newName = name+"$func$dim2_"+constValues.get(constExp.getFinalValue());
                            LinkedHashMap<String, String> temp = new LinkedHashMap<>();
                            int orderPtr = 0;
                            while (orderPtr != order.size()) {
                                for (String key : funcDef.getParaMap().keySet()) {
                                    if (key.equals(order.get(orderPtr))) {
                                        if (newName.startsWith(key)) {
                                            temp.put(newName, funcDef.getParaMap().get(key));
                                        } else {
                                            temp.put(key, funcDef.getParaMap().get(key));
                                        }
                                        break;
                                    }
                                }
                                orderPtr++;
                            }
                            funcDef.setParaMap(temp);
                            identNames.add(name+"$func$dim2_"+constValues.get(constExp.getFinalValue()));
                        } else {
                            ArrayList<String> order = new ArrayList<>(funcDef.getParaMap().keySet());
                            String newName = name+"$func$dim2_"+Integer.parseInt(constExp.getFinalValue());
                            LinkedHashMap<String, String> temp = new LinkedHashMap<>();
                            int orderPtr = 0;
                            while (orderPtr != order.size()) {
                                for (String key : funcDef.getParaMap().keySet()) {
                                    if (key.equals(order.get(orderPtr))) {
                                        if (newName.startsWith(key)) {
                                            temp.put(newName, funcDef.getParaMap().get(key));
                                        } else {
                                            temp.put(key, funcDef.getParaMap().get(key));
                                        }
                                        break;
                                    }
                                }
                                orderPtr++;
                            }
                            funcDef.setParaMap(temp);
                            identNames.add(newName);
                        }
                    }
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

    public void tran_Cond(Node node, String label) {
        // tran_LOrExp
        Node LOrExp = node.getChildNode().get(0);
        int cntLAndExp = 0;
        for (int i = 0; i < LOrExp.getChildNode().size(); i++) {
            Node LAndExp = LOrExp.getChildNode().get(i);
            if (LAndExp.getNodeName().equals("||")) {
                LabelToGo label0 = new LabelToGo(label + "$" + node.getParNode().getChildNode().get(0).getApearIndex() + "_case" + cntLAndExp);
                codeList.add(label0);
                continue;
            }
            if (i != LOrExp.getChildNode().size() - 1) {
                tran_LAndExp(LAndExp, label + "$" + node.getParNode().getChildNode().get(0).getApearIndex() + "_case" + (cntLAndExp + 1));
            } else {
                tran_LAndExp(LAndExp, label + "$" + node.getParNode().getChildNode().get(0).getApearIndex() + "_end");
            }
            if (label.equals("if")) {
                Jump jump = new Jump(label + "$" + node.getParNode().getChildNode().get(0).getApearIndex() + "_begin");
                codeList.add(jump);
            }
            else if (label.equals("while")) {
                Jump jump = new Jump(label + "$" + node.getParNode().getChildNode().get(0).getApearIndex() + "_stmt");
                codeList.add(jump);
            }
            cntLAndExp++;
        }
        codeList.remove(codeList.size() - 1);
    }

    public void tran_LAndExp(Node node, String label) {
        for (Node EqExp : node.getChildNode()) {
            if (EqExp.getNodeName().equals("&&")) continue;
            tran_EqExp(EqExp, label);
            BranchEqualZero bez = new BranchEqualZero(EqExp.getFinalValue(), label);
            codeList.add(bez);
        }
    }

    public void tran_EqExp(Node node, String label) {
        ArrayList<Code> condList = new ArrayList<>();
        for (Node RelExp : node.getChildNode()) {
            if (RelExp.getNodeName().equals("RelExp")) {
                tran_RelExp(RelExp, label);
            }
        }
        for (int i = 0; i < node.getChildNode().size(); i++) {
            if (node.getChildNode().get(i).getNodeName().equals("RelExp")) continue;
            Condition cond = new Condition();
            if (condList.size() == 0) {
                cond.setLeft(node.getChildNode().get(i - 1).getFinalValue());
            } else {
                cond.setLeft(condList.get(condList.size() - 1).getTarget());
            }
            cond.setOp(node.getChildNode().get(i).getToken().getType());
            cond.setRight(node.getChildNode().get(i + 1).getFinalValue());
            cond.setTarget("cond$" + condIndex);
            cond.setLabel(label);
            condList.add(cond);
            condIndex++;
        }
        if (condList.size() != 0) {
            node.setFinalValue(condList.get(condList.size() - 1).getTarget());
        } else {
            node.setFinalValue(node.getChildNode().get(0).getFinalValue());
        }
        codeList.addAll(condList);
    }

    public void tran_RelExp(Node node, String label) {
        ArrayList<Code> condList = new ArrayList<>();
        for (Node AddExp : node.getChildNode()) {
            if (AddExp.getNodeName().equals("AddExp")) getArith(AddExp, codeList);
        }
        for (int i = 0; i < node.getChildNode().size(); i++) {
            if (node.getChildNode().get(i).getNodeName().equals("AddExp")) continue;
            Condition cond = new Condition();
            if (condList.size() == 0) {
                cond.setLeft(node.getChildNode().get(i - 1).getFinalValue());
            } else {
                cond.setLeft(condList.get(condList.size() - 1).getTarget());
            }
            cond.setOp(node.getChildNode().get(i).getToken().getType());
            cond.setRight(node.getChildNode().get(i + 1).getFinalValue());
            cond.setTarget("cond$" + condIndex);
            cond.setLabel(label);
            condList.add(cond);
            condIndex++;
        }
        if (node.getChildNode().size() == 1) {
//            Condition cond = new Condition();
//            cond.setLeft(node.getChildNode().get(0).getFinalValue());
//            cond.setTarget("cond$" + condIndex);
//            cond.setLabel(label);
//            condList.add(cond);
//            condIndex++;
            // TODO
            if (!node.getChildNode().get(0).getFinalValue().contains("t$$")) {
                Arith arith = new Arith();
                arith.setTarget("t" + "$$" + Parser.valueIndex);
                arith.setLeft(node.getChildNode().get(0).getFinalValue());
                condList.add(arith);
                Parser.valueIndex++;
            }
        }
        if (condList.size() != 0) {
            node.setFinalValue(condList.get(condList.size() - 1).getTarget());
        }
        else {
            node.setFinalValue(node.getChildNode().get(0).getFinalValue());
        }
        codeList.addAll(condList);
    }

    public void tran_Stmt(Node node) {
        Node stmtFirst = node.getChildNode().get(0);
        switch (stmtFirst.getNodeName()) {
            case "LVal":
                if (stmtFirst.getChildNode().size() == 1) {
                    AssignValue assign = new AssignValue();
                    assign.setTarget(node.getChildNode().get(0).getFinalValue());
                    if (node.getChildNode().size() == 4) {
                        getArith(node.getChildNode().get(2), codeList);
                        assign.setValue(node.getChildNode().get(2).getFinalValue());
                        codeList.add(assign);
                    } else {
                        assign.setValue("getint");
                        codeList.add(assign);
                    }
                } else {
                    // LVal是数组
                    AssignValue assign = new AssignValue();
                    assign.setTarget(stmtFirst.getChildNode().get(0).getToken().getContent() + "$" + stmtFirst.getChildNode().get(0).getApearIndex() + "$arr");
                    if (stmtFirst.getChildNode().size() == 4) {
                        // 一维数组
                        getArith(stmtFirst.getChildNode().get(2), codeList);
                        assign.putTargetDimIndex("dim1", stmtFirst.getChildNode().get(2).getFinalValue());
                    } else {
                        // 二维数组
                        getArith(stmtFirst.getChildNode().get(2), codeList);
                        assign.putTargetDimIndex("dim1", stmtFirst.getChildNode().get(2).getFinalValue());
                        getArith(stmtFirst.getChildNode().get(5), codeList);
                        assign.putTargetDimIndex("dim2", stmtFirst.getChildNode().get(5).getFinalValue());
                    }
                    if (node.getChildNode().size() == 4) {
                        getArith(node.getChildNode().get(2), codeList);
                        assign.setValue(node.getChildNode().get(2).getFinalValue());
                        codeList.add(assign);
                    } else {
                        assign.setValue("getint");
                        codeList.add(assign);
                    }
                }
                break;
            case "Exp":
                getArith(stmtFirst, codeList);
                break;
            case "Block":
                tran_Block(stmtFirst);
                break;
            case "if":
                tran_Cond(node.getChildNode().get(2), "if");
                LabelToGo label0 = new LabelToGo("if$" + stmtFirst.getApearIndex() + "_begin");
                codeList.add(label0);
                tran_Stmt(node.getChildNode().get(4));
                if (node.getChildNode().size() == 7) {
                    Jump jump = new Jump("else$" + node.getChildNode().get(5).getApearIndex() + "_end");
                    codeList.add(jump);
                }
                LabelToGo label1 = new LabelToGo("if$" + stmtFirst.getApearIndex() + "_end");
                codeList.add(label1);
                if (node.getChildNode().size() == 7) {
                    tran_Stmt(node.getChildNode().get(6));
                    LabelToGo label2 = new LabelToGo("else$" + node.getChildNode().get(5).getApearIndex() + "_end");
                    codeList.add(label2);
                }
                break;
            case "while":
                LabelToGo label3 = new LabelToGo("while$" + stmtFirst.getApearIndex() + "_begin");
                codeList.add(label3);
                tran_Cond(node.getChildNode().get(2), "while");
                LabelToGo label4 = new LabelToGo("while$" + stmtFirst.getApearIndex() + "_stmt");
                codeList.add(label4);
                tran_Stmt(node.getChildNode().get(4));
                Jump jump0 = new Jump("while$" + stmtFirst.getApearIndex() + "_begin");
                codeList.add(jump0);
                LabelToGo label5 = new LabelToGo("while$" + stmtFirst.getApearIndex() + "_end");
                codeList.add(label5);
                break;
            case "break":
                int whileIndex0 = findWhile(stmtFirst);
                Jump jump1 = new Jump("while$" + whileIndex0 + "_end");
                codeList.add(jump1);
                break;
            case "continue":
                int whileIndex1 = findWhile(stmtFirst);
                Jump jump2 = new Jump("while$" + whileIndex1 + "_begin");
                codeList.add(jump2);
                break;
            case "return":
                FuncReturn ret = new FuncReturn();
                if (node.getChildNode().size() == 3){
                    getArith(node.getChildNode().get(1), codeList);
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
                            if (formatStr.split("\"").length != 0) {
                                formatStr = formatStr.split("\"")[1];
                            }
                            else {
                                formatStr = "";
                            }
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
                            getArith(pf, codeList);
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

    public static void getArith(Node node, ArrayList<Code> codeList) {
        for (Node child : node.getChildNode()) {
            getArith(child, codeList);
        }
        codeList.addAll(node.getArithExp());
    }

    public void calArith(Node node) {
        ArrayList<Code> ariths = new ArrayList<>();
        getArith(node, ariths);
        for (Code code : ariths) {
            if (code.getClass().getName().equals("Arith")) {
                Arith arith = (Arith) code;
                // 取得左操作数
                int left = 0;
                if (arith.getLeft() != null) {
                    if (isDigital(arith.getLeft())) {
                        left = Integer.parseInt(arith.getLeft());
                    } else {
                        left = constValues.get(arith.getLeft());
                    }
                }
                // 取得右操作数
                int right = 0;
                if (isDigital(arith.getRight())) {
                    right = Integer.parseInt(arith.getRight());
                } else {
                    right = constValues.get(arith.getRight());
                }
                // 判断op进行操作
                int ans = 0;
                if (arith.getOp() == SymbolType.PLUS) {
                    ans = left + right;
                } else if (arith.getOp() == SymbolType.MINU) {
                    ans = left - right;
                } else if (arith.getOp() == SymbolType.MULT) {
                    ans = left * right;
                } else if (arith.getOp() == SymbolType.DIV) {
                    ans = left / right;
                } else if (arith.getOp() == SymbolType.MOD) {
                    ans = left % right;
                }
                // 写入hashmap
                constValues.put(arith.getTarget(), ans);
            }
            else if (code.getClass().getName().equals("AssignValue")) {
                // t = constArray[0]
                AssignValue av = (AssignValue) code;
                String arrName = av.getValue();
                int value = 0;
                for (recArray ra : arrayValues) {
                    if (ra.getArrName().startsWith(arrName)) {
                        if (av.getValueDimIndex().size() == 1) {
                            String key = av.getValueDimIndex().get("dim1");
                            if (ra.getValues().get(key) != null) {
                                value = ra.getValues().get(key);
                            }
                        }
                        else if (av.getValueDimIndex().size() == 2) {
                            String key = av.getValueDimIndex().get("dim1") + "+" + av.getValueDimIndex().get("dim2");
                            if (ra.getValues().get(key) != null) {
                                value = ra.getValues().get(key);
                            }
                        }
                        break;
                    }
                }
                constValues.put(av.getTarget(), value);
            }
        }
    }

    public int findWhile(Node node) {
        Node nodePtr = node;
        while (!nodePtr.getNodeName().equals("CompUnit")) {
            nodePtr = nodePtr.getParNode();
            for (Node ch : nodePtr.getChildNode()) {
                if (ch.getNodeName().equals("while")) {
                    return ch.getApearIndex();
                }
            }
        }
        return -1;
    }

    public int getArrayDimension(Node node) {
        int dimCnt = 0;
        for (Node ch : node.getChildNode()) {
            if (ch.getNodeName().equals("[")) dimCnt++;
        }
        return dimCnt;
    }

    public static boolean isDigital(String str) {
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

interface Code {

    String getTarget();

    @Override
    String toString();

}

class recArray implements Code {
    private String arrName;
    private HashMap<String, Integer> values;

    public recArray(String arrName, HashMap<String, Integer> values) {
        this.arrName = arrName;
        this.values = values;
    }

    public String getArrName() {
        return arrName;
    }

    public HashMap<String, Integer> getValues() {
        return values;
    }

    @Override
    public String getTarget() {
        return null;
    }
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
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append(name + " " + content);
        return internCode.toString();
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
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("print " + str);
        return internCode.toString();
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
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("ret");
        if (retValue != null) {
            internCode.append(" " + retValue);
        }
        return internCode.toString();
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
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        for (String para : param) {
            internCode.append("push " + para + "\n");
        }
        internCode.append("call " + name + "\n");
        internCode.append(target + " = RET");
        return internCode.toString();
    }
}

class LabelToGo implements Code {
    private String label;

    public LabelToGo(String label) {
        this.label = label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append(label + ":");
        return internCode.toString();
    }
}

class BranchEqualZero implements Code {
    private String judgeObj;
    private String label;

    public BranchEqualZero(String judgeObj, String label) {
        this.judgeObj = judgeObj;
        this.label = label;
    }

    public String getJudgeObj() {
        return judgeObj;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("beq " + judgeObj + ", 0, " + label);
        return internCode.toString();
    }
}

class Condition implements Code {
    private SymbolType op;
    private String left;
    private String right;
    private String target;
    private String label;

    public Condition(String target, String left, SymbolType op, String right, String label) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.target = target;
        this.label = label;
    }

    public Condition() {

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

    public void setLabel(String label) {
        this.label = label;
    }

    public SymbolType getOp() {
        return op;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        if (op != null && right != null) {
            internCode.append(target + " = " + left + " " + op + " " + right);
        }
        else {
            internCode.append(target + " = " + left);
        }
        return internCode.toString();
    }
}

class Jump implements Code {
    private String label;

    public Jump(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append("goto " + label);
        return internCode.toString();
    }
}

class Arith implements Code {
    private SymbolType op;
    private String left;
    private String right;
    private String target;
//    private HashMap<String, Integer> leftDim = new HashMap<>();
//    private HashMap<String, Integer> rightDim = new HashMap<>();

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

//    public void setLeftDim(HashMap<String, Integer> dimMap) {
//        this.leftDim = dimMap;
//    }

    public void setRight(String right) {
        this.right = right;
    }

//    public void setRighttDim(HashMap<String, Integer> dimMap) {
//        this.rightDim = dimMap;
//    }

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
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        if (left != null) {
            internCode.append(target + " = " + left + " " + op + " " + right);
        } else {
            internCode.append(target + " = " + op + " " + right);
        }
        return internCode.toString();
    }
}

class AssignValue implements Code {
    private String target;
    private String value;
    private HashMap<String, String> targetDimIndex = new HashMap<>();
    private HashMap<String, String> valueDimIndex = new HashMap<>();

    public void setTarget(String target) {
        this.target = target;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void putTargetDimIndex(String dim, String index) {
        this.targetDimIndex.put(dim, index);
    }

    public void putValueDimIndex(String dim, String index) {
        this.valueDimIndex.put(dim, index);
    }

    public String getValue() {
        return value;
    }

    public HashMap<String, String> getTargetDimIndex() {
        return targetDimIndex;
    }

    public HashMap<String, String> getValueDimIndex() {
        return valueDimIndex;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        if (targetDimIndex.size() == 0) {
            if (valueDimIndex.size() == 0) {
                internCode.append(target + " = " + value);
            }
            else if (valueDimIndex.size() == 1) {
                internCode.append(target + " = " + value + " [" + valueDimIndex.get("dim1") + "]");
            }
            else if (valueDimIndex.size() == 2) {
                internCode.append(target + " = " + value + " [" + valueDimIndex.get("dim1") + "] [" + valueDimIndex.get("dim2") + "]");
            }
        }
        else if (targetDimIndex.size() == 1) {
            if (valueDimIndex.size() == 0) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] = " + value);
            }
            else if (valueDimIndex.size() == 1) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] = " + value + " [" + valueDimIndex.get("dim1") + "]");
            }
            else if (valueDimIndex.size() == 2) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] = " + value + " [" + valueDimIndex.get("dim1") + "] [" + valueDimIndex.get("dim2") + "]");
            }
        }
        else if (targetDimIndex.size() == 2) {
            if (valueDimIndex.size() == 0) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] [" + targetDimIndex.get("dim2") + "] = " + value);
            }
            else if (valueDimIndex.size() == 1) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] [" + targetDimIndex.get("dim2") + "] = " + value + " [" + valueDimIndex.get("dim1") + "]");
            }
            else if (valueDimIndex.size() == 2) {
                internCode.append(target + " [" + targetDimIndex.get("dim1") + "] [" + targetDimIndex.get("dim2") + "] = " + value + " [" + valueDimIndex.get("dim1") + "] [" + valueDimIndex.get("dim2") + "]");
            }
        }
        return internCode.toString();
    }
}

class ConVarDef implements Code {
    private String name;    //const or var name
    private int type;   //const or var
    private HashMap<String, Integer> dimSize = new HashMap<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void addDimSize(String dim, int size) {
        dimSize.put(dim, size);
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Integer> getDimSize() {
        return dimSize;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append((this.type == NodeInfo.CONST) ? "const " : "var ");
        internCode.append("int " + this.name);
        return internCode.toString();
    }
}

class FuncDef implements Code {
    private String name;    //func name
    private int type;   //intfun or voidfun
    private LinkedHashMap<String, String> paraMap;    // params of func

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setParaMap(LinkedHashMap<String, String> paraMap) {
        this.paraMap = paraMap;
    }

    public String getName() {
        return name;
    }

    public LinkedHashMap<String, String> getParaMap() {
        return paraMap;
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer internCode = new StringBuffer();
        internCode.append((this.type == NodeInfo.INTFUN) ? "int " : "void ");
        internCode.append(this.name + "()" + "\n");
        if (paraMap != null) {
            for (String para : paraMap.keySet()) {
                internCode.append("para " + paraMap.get(para) + " " + para + "\n");
            }
        }
        internCode.deleteCharAt(internCode.length() - 1);
        return internCode.toString();
    }
}