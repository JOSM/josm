// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import java.io.IOException;
import java.io.Reader;

import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;

public class PushbackTokenizer {
    private final Reader search;

    private Token currentToken;
    private String currentText;
    private int c;

    public PushbackTokenizer(Reader search) {
        this.search = search;
        getChar();
    }

    public enum Token {NOT, OR, LEFT_PARENT, RIGHT_PARENT, COLON, EQUALS, KEY, EOF}

    private void getChar() {
        try {
            c = search.read();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
            StringBuilder s = new StringBuilder();
            while (!(c == -1 || Character.isWhitespace(c) || c == '"'|| c == ':' || c == '(' || c == ')' || c == '|' || c == '=')) {
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

    public String getText() {
        return currentText;
    }
}
