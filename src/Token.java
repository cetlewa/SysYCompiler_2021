public class Token {
    public String content;
    public SymbolType type;
    public int line;

    public Token(String content, SymbolType type, int line) {
        this.content = content;
        this.type = type;
        this.line = line;
    }

    public String getContent() {
        return content;
    }

    public SymbolType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }
}
