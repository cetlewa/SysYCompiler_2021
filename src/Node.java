import java.util.ArrayList;

public class Node {
    private Token token;
    private Node parNode;
    private ArrayList<Node> childNode = new ArrayList<>();
    private NodeInfo nodeInfo = new NodeInfo();
    private ArrayList<Integer> paramDim = new ArrayList<>();

    public Node(Token token) {
        this.token = token;
    }

    public Node(String name) {
        this.nodeInfo.setName(name);
    }

    public void addParamDim(int dim) {
        paramDim.add(dim);
    }

    public void setParNode(Node parNode) {
        this.parNode = parNode;
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
}

