package net.namandixit.java.yamak;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.namandixit.java.yamak.TokenType.*;

public class Lexer {

    private boolean hadError;

    private final String file;
    private final String src;

    private final List<Token> tokens = new ArrayList<>();
    private final List<Error> errors = new ArrayList<>();
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("with", WITH);
        keywords.put("return", RETURN);
        keywords.put("defer", DEFER);
        keywords.put("if", IF);
        keywords.put("else", ELSE);
        keywords.put("match", MATCH);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
        keywords.put("label", LABEL);
        keywords.put("type", TYPE);
        keywords.put("func", FUNC);
        keywords.put("var", VAR);
        keywords.put("let", LET);
        keywords.put("struct", STRUCT);
        keywords.put("union", UNION);
        keywords.put("tagged", TAGGED);
        keywords.put("enum", ENUM);
        keywords.put("collect", COLLECT);

        keywords.put("true", TRUE);
        keywords.put("false", FALSE);

        keywords.put("and", AND);
        keywords.put("or", OR);
        keywords.put("not", NOT);
    }

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int cursor = 1; // Location inside the line

    // Beginning of token
    private int from_line = 1;
    private int from_char = 1;

    Lexer(String file, String src) {
        this.file = file;
        this.src = src;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            while (true) {
                if (Character.isWhitespace(peek())) {
                    advance();
                } else {
                    break;
                }
            }

            start = current;

            from_char = cursor;
            from_line = line;

            scanToken();
        }

        start = current;
        addToken(EOF, null);
        return tokens;
    }

    boolean hadError() {
        return hadError;
    }

    List<Error> getErrors() {
        return errors;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' ->
                addToken(LPAREN);
            case ')' ->
                addToken(RPAREN);
            case '{' ->
                addToken(LBRACE);
            case '}' ->
                addToken(RBRACE);
            case ',' ->
                addToken(COMMA);
            case '.' ->
                addToken(DOT);
            case ';' ->
                addToken(SEMICOLON);
            case '-' ->
                addToken(MINUS);
            case '+' ->
                addToken(PLUS);
            case '/' ->
                addToken(SLASH);
            case '*' ->
                addToken(ASTERISK);
            case '=' ->
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' ->
                addToken(match('=') ? LBRACKET_EQUAL
                        : match('<') ? LBRACKET_LBRACKET
                        : match('>') ? LBRACKET_RBRACKET
                        : LBRACKET);
            case '>' ->
                addToken(match('=') ? RBRACKET_EQUAL
                        : match('>') ? RBRACKET_RBRACKET
                        : match('<') ? RBRACKET_LBRACKET
                        : RBRACKET);
            case ':' ->
                addToken(match('=') ? COLON_EQUAL
                        : match(':') ? COLON_COLON
                        : COLON);
            case '"' -> {
                PARSE_STRING:
                while (!isAtEnd()) {
                    switch (peek()) {
                        case '\\' -> {
                            advance();
                            advance();
                        }
                        case '"' -> {
                            advance();
                            break PARSE_STRING;
                        }
                        default ->
                            advance();
                    }
                }

                if (isAtEnd()) {
                    addError("Unterminated string.");
                    return;
                }

                String value = src.substring(start + 1, current - 1);
                addToken(STRING, value);
            }
            case '#' -> {
                if (match('{')) {
                    int level = 1;
                    while (level > 0) {
                        if (isAtEnd()) {
                            addError("Block comment reached end of file");
                            return;
                        } else if (peek() == '#' && peekNext() == '}') {
                            level--;
                            advance();
                            advance();
                        } else if (peek() == '#' && peekNext() == '{') {
                            level++;
                            advance();
                            advance();
                        } else {
                            advance();
                        }
                    }
                    addToken(COMMENT_BLOCK);
                } else {
                    while (!isAtEnd()) {
                        if (peek() == '\n') {
                            break;
                        } else {
                            advance();
                        }
                    }
                    addToken(COMMENT_LINE);
                }
            }
            default -> {
                if (isDigit(c, 10)) {
                    int base = 10;

                    int start_integral = start;

                    if (c == '0' && !isDigit(peek(), base)) {
                        // The character might be an operator or period ("0+1", "0.1", etc.)
                        switch (peek()) {
                            case 'B', 'b' -> {
                                base = 2;
                                start_integral += 2;
                                advance();
                            }
                            case 'O', 'o' -> {
                                base = 8;
                                start_integral += 2;
                                advance();
                            }
                            case 'X', 'x' -> {
                                base = 16;
                                start_integral += 2;
                                advance();
                            }
                        }
                    }

                    while (!isAtEnd() && isDigit(peek(), base)) {
                        advance();
                    }

                    var integral = new BigInteger(src.substring(start_integral, current), base);
                    var value = new BigDecimal(integral);

                    if (peek() == '.') {
                        advance();

                        if (isDigit(peek(), base)) {
                            int start_decimal = current;

                            while (!isAtEnd() && isDigit(peek(), base)) {
                                advance();
                            }

                            var decimal_integral = new BigInteger(src.substring(start_decimal, current), base);
                            var decimal = new BigDecimal(decimal_integral);

                            int power_of_base = getDigitCount(decimal_integral, base);
                            var multiplicand = new BigDecimal(Integer.toString(base));
                            multiplicand = multiplicand.pow(power_of_base);

                            value = value.multiply(multiplicand);
                            value = value.add(decimal);
                            value = value.divide(multiplicand);
                        }
                    }

                    if (peek() == 'p' || peek() == 'P') {
                        advance();

                        boolean negative = (peek() == '-');
                        if (negative) {
                            advance();
                        }

                        if (isDigit(peek(), base)) {
                            int start_exp = current;

                            while (!isAtEnd() && isDigit(peek(), base)) {
                                advance();
                            }

                            try {
                                var exp_bi = new BigInteger(src.substring(start_exp, current), base);
                                int exp = exp_bi.intValueExact();

                                int exp_base = 2;
                                if (base == 10) {
                                    exp_base = 10;
                                }
                                var multiplicand = new BigDecimal(Integer.toString(exp_base));
                                multiplicand = multiplicand.pow(exp);

                                if (negative) {
                                    value = value.divide(multiplicand);
                                } else {
                                    value = value.multiply(multiplicand);
                                }
                            } catch (ArithmeticException e) {
                                addError("Exponent is too big to fit into a Java int");
                                return;
                            }
                        }
                    }

                    addToken(NUMBER, value);
                } else if (isAlpha(c)) {
                    while (isAlphaNumeric(peek())) {
                        advance();
                    }

                    String text = src.substring(start, current);
                    TokenType type = keywords.get(text);
                    if (type == null) {
                        type = IDENTIFIER;
                    }
                    addToken(type);
                } else {
                    addError("Unexpected character");
                }
            }
        }
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = src.substring(start, current);
        tokens.add(new Token(type, text, literal, getPosition()));
    }

    private void addError(String msg) {
        errors.add(new Error(file, getPosition(), msg));
    }

    private boolean isAtEnd() {
        return current >= src.length();
    }

    private char advance() {
        var c = src.charAt(current);

        current++;
        if (c == '\n') {
            line++;
            cursor = 1;
        } else {
            cursor++;
        }

        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (src.charAt(current) != expected) {
            return false;
        }

        current++;
        cursor++;

        return true;
    }

    private Position getPosition() {
        var pos = new Position(file, from_line, from_char, line, cursor);
        return pos;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return src.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= src.length()) {
            return '\0';
        }
        return src.charAt(current + 1);
    }

    private boolean isDigit(char c, int base) {
        return switch (base) {
            case 2 ->
                c == '0' || c == '1';
            case 8 ->
                c >= '0' && c <= '7';
            case 16 ->
                (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
            default ->
                c >= '0' && c <= '9';
        };
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c, 10);
    }

    public static int getDigitCount(BigInteger number, int base) {
        double factor = Math.log(2) / Math.log(base);
        int digitCount = (int) (factor * number.bitLength() + 1);
        if (BigInteger.TEN.pow(digitCount - 1).compareTo(number) > 0) {
            return digitCount - 1;
        }
        return digitCount;
    }
}
