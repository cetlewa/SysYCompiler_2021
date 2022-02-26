public class NodeInfo {
    private String name;    //标识符名：常量，变量，过程
    private int type;
    private int level;
    private int dimension;
    private int paramNum;

    public static final int CONST=1, VAR=2, INTFUN=3, VOIDFUN=4, PARAM=5, STR=6;

//    public NodeInfo(String name, int type, int level, int dimension, int  paramNum) {
//        this.name = name;
//        this.type = type;
//        this.level = level;
//        this.dimension = dimension;
//        this.paramNum = paramNum;
//    }
    public NodeInfo() {

    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getDimension() {
        return dimension;
    }

    public int getParamNum() {
        return paramNum;
    }

    public void setInfo(String name, int type, int level, int dimension, int paramNum) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.dimension = dimension;
        this.paramNum = paramNum;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public void setParamNum(int paramNum) {
        this.paramNum = paramNum;
    }
}
