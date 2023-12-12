package net.namandixit.java.yamak;

public class Token {

    final TokenType type;
    final String lexeme;
    final Object literal;
    final Position pos;

    Token(TokenType type, String lexeme, Object literal, Position pos) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.pos = pos;
    }

    public String toString() {
        if (literal == null) {
            return type + " \"" + lexeme + "\"";

        } else {
            return type + " \"" + lexeme + "\" " + literal;
        }
    }

}
