public class Token {
    public String content;
    public TypeTable type;
    public int line;

    public Token(String symbol, TypeTable type, int line) {
        this.content = symbol;
        this.type = type;
        this.line = line;
    }

    public String getContent() {
        return content;
    }

    public TypeTable getType() {
        return type;
    }

    public int getLine() {
        return line;
    }
}
