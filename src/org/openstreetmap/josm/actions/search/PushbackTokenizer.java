// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.Reader;

import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;

public class PushbackTokenizer {
    private final Reader search;

    private Token currentToken;
    private String currentText;
    private long currentNumber;
    private int c;

    public PushbackTokenizer(Reader search) {
        this.search = search;
        getChar();
    }

    public enum Token {
        NOT(marktr("<not>")), OR(marktr("<or>")), LEFT_PARENT(marktr("<left parent>")),
        RIGHT_PARENT(marktr("<right parent>")), COLON(marktr("<colon>")), EQUALS(marktr("<equals>")),
        KEY(marktr("<key>")), QUESTION_MARK(marktr("<question mark>")), NUMBER(marktr("<number>")),
        EOF(marktr("<end-of-file>"));

        private Token(String name) {
            this.name = name;
        }

        private final String name;

        @Override
        public String toString() {
            return tr(name);
        }
    }


    private void getChar() {
        try {
            c = search.read();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private long getNumber() {
        long result = 0;
        while (Character.isDigit(c)) {
            result = result * 10 + (c - '0');
            getChar();
        }
        return result;
    }

    /**
     * The token returned is <code>null</code> or starts with an identifier character:
     * - for an '-'. This will be the only character
     * : for an key. The value is the next token
     * | for "OR"
     * ' ' for anything else.
     * @return The next token in the stream.
     */
    public Token nextToken() {
        if (currentToken != null) {
            Token result = currentToken;
            currentToken = null;
            return result;
        }

        while (Character.isWhitespace(c)) {
            getChar();
        }
        switch (c) {
        case -1:
            getChar();
            return Token.EOF;
        case ':':
            getChar();
            return Token.COLON;
        case '=':
            getChar();
            return Token.EQUALS;
        case '-':
            getChar();
            if (Character.isDigit(c)) {
                currentNumber = -1 * getNumber();
                return Token.NUMBER;
            } else
                return Token.NOT;
        case '(':
            getChar();
            return Token.LEFT_PARENT;
        case ')':
            getChar();
            return Token.RIGHT_PARENT;
        case '|':
            getChar();
            return Token.OR;
        case '?':
            getChar();
            return Token.QUESTION_MARK;
        case '"':
        {
            getChar();
            StringBuilder s = new StringBuilder();
            boolean escape = false;
            while (c != -1 && (c != '"' || escape)) {
                if (c == '\\' && !escape) {
                    escape = true;
                } else {
                    s.append((char)c);
                    escape = false;
                }
                getChar();
            }
            getChar();
            currentText = s.toString();
            return Token.KEY;
        }
        default:
        {
            if (Character.isDigit(c)) {
                currentNumber = getNumber();
                return Token.NUMBER;
            }

            StringBuilder s = new StringBuilder();
            while (!(c == -1 || Character.isWhitespace(c) || c == '"'|| c == ':' || c == '(' || c == ')' || c == '|' || c == '=' || c == '?')) {
                s.append((char)c);
                getChar();
            }
            currentText = s.toString();
            if ("or".equals(currentText))
                return Token.OR;
            else
                return Token.KEY;
        }
        }
    }

    public boolean readIfEqual(Token token) {
        Token nextTok = nextToken();
        if (nextTok == null ? token == null : nextTok == token)
            return true;
        currentToken = nextTok;
        return false;
    }

    public String readText() {
        Token nextTok = nextToken();
        if (nextTok == Token.KEY)
            return currentText;
        currentToken = nextTok;
        return null;
    }

    public String readText(String errorMessage) throws ParseError {
        String text = readText();
        if (text == null)
            throw new ParseError(errorMessage);
        else
            return text;
    }

    public long readNumber(String errorMessage) throws ParseError {
        if (nextToken() == Token.NUMBER)
            return currentNumber;
        else
            throw new ParseError(errorMessage);
    }

    public String getText() {
        return currentText;
    }
}
