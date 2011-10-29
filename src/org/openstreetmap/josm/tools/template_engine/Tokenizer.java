// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Arrays;
import java.util.List;

public class Tokenizer {

    public static class Token {
        private final TokenType type;
        private final int position;
        private final String text;

        public Token(TokenType type, int position) {
            this(type, position, null);
        }

        public Token(TokenType type, int position, String text) {
            this.type = type;
            this.position = position;
            this.text = text;
        }

        public TokenType getType() {
            return type;
        }

        public int getPosition() {
            return position;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return type + (text != null?" " + text:"");
        }
    }

    public enum TokenType { CONDITION_START, VARIABLE_START, CONTEXT_SWITCH_START, END, PIPE, APOSTROPHE, TEXT, EOF }

    private final List<Character> specialCharaters = Arrays.asList(new Character[] {'$', '?', '{', '}', '|', '\'', '!'});

    private final String template;

    private int c;
    private int index;
    private Token currentToken;
    private StringBuilder text = new StringBuilder();

    public Tokenizer(String template) {
        this.template = template;
        getChar();
    }

    private void getChar() {
        if (index >= template.length()) {
            c = -1;
        } else {
            c = template.charAt(index++);
        }
    }

    public Token nextToken() throws ParseError {
        if (currentToken != null) {
            Token result = currentToken;
            currentToken = null;
            return result;
        }
        int position = index;

        text.setLength(0);
        switch (c) {
        case -1:
            return new Token(TokenType.EOF, position);
        case '{':
            getChar();
            return new Token(TokenType.VARIABLE_START, position);
        case '?':
            getChar();
            if (c == '{') {
                getChar();
                return new Token(TokenType.CONDITION_START, position);
            } else
                throw ParseError.unexpectedChar('{', (char)c, position);
        case '!':
            getChar();
            if (c == '{') {
                getChar();
                return new Token(TokenType.CONTEXT_SWITCH_START, position);
            } else
                throw ParseError.unexpectedChar('{', (char)c, position);
        case '}':
            getChar();
            return new Token(TokenType.END, position);
        case '|':
            getChar();
            return new Token(TokenType.PIPE, position);
        case '\'':
            getChar();
            return new Token(TokenType.APOSTROPHE, position);
        default:
            while (c != -1 && !specialCharaters.contains((char)c)) {
                if (c == '\\') {
                    getChar();
                    if (c == 'n') {
                        c = '\n';
                    }
                }
                text.append((char)c);
                getChar();
            }
            return new Token(TokenType.TEXT, position, text.toString());
        }
    }

    public Token lookAhead() throws ParseError {
        if (currentToken == null) {
            currentToken = nextToken();
        }
        return currentToken;
    }

    public Token skip(char lastChar) {
        currentToken = null;
        int position = index;
        StringBuilder result = new StringBuilder();
        while (c != lastChar && c != -1) {
            if (c == '\\') {
                getChar();
            }
            result.append((char)c);
            getChar();
        }
        return new Token(TokenType.TEXT, position, result.toString());
    }

}
