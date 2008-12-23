// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.LinkedList;

public class PushbackTokenizer {
    private PushbackReader search;

    private LinkedList<String> pushBackBuf = new LinkedList<String>();

    public PushbackTokenizer(PushbackReader search) {
        this.search = search;
    }

    /**
     * The token returned is <code>null</code> or starts with an identifier character:
     * - for an '-'. This will be the only character
     * : for an key. The value is the next token
     * | for "OR"
     * ' ' for anything else.
     * @return The next token in the stream.
     */
    public String nextToken() {
        if (!pushBackBuf.isEmpty()) {
            return pushBackBuf.removeLast();
        }

        try {
            int next;
            char c = ' ';
            while (c == ' ' || c == '\t' || c == '\n') {
                next = search.read();
                if (next == -1)
                    return null;
                c = (char)next;
            }
            StringBuilder s;
            switch (c) {
            case ':':
                next = search.read();
                c = (char) next;
                if (next == -1 || c == ' ' || c == '\t') {
                    pushBack(" ");
                } else {
                    search.unread(next);
                }
                return ":";
            case '-':
                return "-";
            case '(':
                return "(";
            case ')':
                return ")";
            case '|':
                return "|";
            case '"':
                s = new StringBuilder(" ");
                for (int nc = search.read(); nc != -1 && nc != '"'; nc = search.read())
                    s.append((char)nc);
                return s.toString();
            default:
                s = new StringBuilder();
            for (;;) {
                s.append(c);
                next = search.read();
                if (next == -1) {
                    if (s.toString().equals("OR"))
                        return "|";
                    return " "+s.toString();
                }
                c = (char)next;
                if (c == ' ' || c == '\t' || c == '"' || c == ':' || c == '(' || c == ')' || c == '|') {
                    search.unread(next);
                    if (s.toString().equals("OR"))
                        return "|";
                    return " "+s.toString();
                }
            }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean readIfEqual(String tok) {
        String nextTok = nextToken();
        if (nextTok == null ? tok == null : nextTok.equals(tok))
            return true;
        pushBack(nextTok);
        return false;
    }

    public String readText() {
        String nextTok = nextToken();
        if (nextTok != null && nextTok.startsWith(" "))
            return nextTok.substring(1);
        pushBack(nextTok);
        return null;
    }

    public void pushBack(String tok) {
        pushBackBuf.addLast(tok);
    }
}
