public class Token {
    public String content;
    public SymbolType type;
    public int line;

    public Token(String symbol, SymbolType type, int line) {
        this.content = symbol;
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
