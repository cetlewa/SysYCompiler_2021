import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Node {
    private String nodeName;
    private Token token;
    private Node parNode;
    private ArrayList<Node> childNode = new ArrayList<>();
    private NodeInfo nodeInfo = new NodeInfo();
    private ArrayList<Integer> paramDim = new ArrayList<>();
    private LinkedHashMap<String, String> params = new LinkedHashMap<>();
    private String finalValue;  //final value of this tree node
    private ArrayList<Code> arithExp = new ArrayList<>();   //tree node's 4-n expr
    private int apearIndex = 0;  //how many times does this idenfr appear

    public Node(Token token) {
        this.token = token;
    }

    public Node(String name) {
        this.nodeName = name;
    }

    public Node(String name, Token token) {
        this.nodeInfo.setName(name);
        this.token = token;
    }

    public int getApearIndex() {
        return apearIndex;
    }

    public void setApearIndex(int apearIndex) {
        this.apearIndex = apearIndex;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void addParamDim(int dim) {
        paramDim.add(dim);
    }

    public void setParNode(Node parNode) {
        this.parNode = parNode;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public void setFinalValue(String finalValue) {
        this.finalValue = finalValue;
    }

    public String getFinalValue() {
        return finalValue;
    }

    public ArrayList<Code> getArithExp() {
        return arithExp;
    }

    public void addArithExp(Code arith) {
        this.arithExp.add(arith);
    }

    public void setType(int type) {
        this.nodeInfo.setType(type);
    }

    public void addChildNode(Node childNode) {
        this.childNode.add(childNode);
    }

    public void setNodeInfo(String name, int type, int level, int dimension, int paramNum) {
        this.nodeInfo.setInfo(name, type, level, dimension, paramNum);
    }

    public void setParamNum(int paramNum) {
        this.nodeInfo.setParamNum(paramNum);
    }

    public Node getParNode() {
        return this.parNode;
    }

    public ArrayList<Node> getChildNode() {
        return this.childNode;
    }

    public String getName() {
        return this.nodeInfo.getName();
    }

    public int getType() {
        return this.nodeInfo.getType();
    }

    public Token getToken() {
        return this.token;
    }

    public int getParamNum() {
        return this.nodeInfo.getParamNum();
    }

    public ArrayList<Integer> getParamDim() {
        return this.paramDim;
    }

    public int getDimension() {
        return this.nodeInfo.getDimension();
    }

    public void setParams(LinkedHashMap<String, String> params) {
        this.params = params;
    }

    public LinkedHashMap<String, String> getParams() {
        return params;
    }
}


