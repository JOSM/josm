// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * This class is used to parse a search string and split it into tokens.
 * It provides methods to parse numbers and extract strings.
 * @since 12656 (moved from actions.search package)
 */
public class PushbackTokenizer {

    /**
     * A range of long numbers. Immutable
     */
    public static class Range {
        private final long start;
        private final long end;

        /**
         * Create a new range
         * @param start The start
         * @param end The end (inclusive)
         */
        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        /**
         * @return The start
         */
        public long getStart() {
            return start;
        }

        /**
         * @return The end (inclusive)
         */
        public long getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "Range [start=" + start + ", end=" + end + ']';
        }
    }

    private final Reader search;

    private Token currentToken;
    private String currentText;
    private Long currentNumber;
    private Long currentRange;
    private int c;
    private boolean isRange;

    /**
     * Creates a new {@link PushbackTokenizer}
     * @param search The search string reader to read the tokens from
     */
    public PushbackTokenizer(Reader search) {
        this.search = search;
        getChar();
    }

    /**
     * The token types that may be read
     */
    public enum Token {
        /**
         * Not token (-)
         */
        NOT(marktr("<not>")),
        /**
         * Or token (or) (|)
         */
        OR(marktr("<or>")),
        /**
         * Xor token (xor) (^)
         */
        XOR(marktr("<xor>")),
        /**
         * opening parentheses token (
         */
        LEFT_PARENT(marktr("<left parent>")),
        /**
         * closing parentheses token )
         */
        RIGHT_PARENT(marktr("<right parent>")),
        /**
         * Colon :
         */
        COLON(marktr("<colon>")),
        /**
         * The equals sign (=)
         */
        EQUALS(marktr("<equals>")),
        /**
         * A text
         */
        KEY(marktr("<key>")),
        /**
         * A question mark (?)
         */
        QUESTION_MARK(marktr("<question mark>")),
        /**
         * Marks the end of the input
         */
        EOF(marktr("<end-of-file>")),
        /**
         * Less than sign (&lt;)
         */
        LESS_THAN("<less-than>"),
        /**
         * Greater than sign (&gt;)
         */
        GREATER_THAN("<greater-than>");

        Token(String name) {
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
            throw new JosmRuntimeException(e.getMessage(), e);
        }
    }

    private static final List<Character> SPECIAL_CHARS = Arrays.asList('"', ':', '(', ')', '|', '^', '=', '?', '<', '>');
    private static final List<Character> SPECIAL_CHARS_QUOTED = Arrays.asList('"');

    private String getString(boolean quoted) {
        List<Character> sChars = quoted ? SPECIAL_CHARS_QUOTED : SPECIAL_CHARS;
        StringBuilder s = new StringBuilder();
        boolean escape = false;
        while (c != -1 && (escape || (!sChars.contains((char) c) && (quoted || !Character.isWhitespace(c))))) {
            if (c == '\\' && !escape) {
                escape = true;
            } else {
                s.append((char) c);
                escape = false;
            }
            getChar();
        }
        return s.toString();
    }

    private String getString() {
        return getString(false);
    }

    /**
     * The token returned is <code>null</code> or starts with an identifier character:
     * - for an '-'. This will be the only character
     * : for an key. The value is the next token
     * | for "OR"
     * ^ for "XOR"
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
        case '<':
            getChar();
            return Token.LESS_THAN;
        case '>':
            getChar();
            return Token.GREATER_THAN;
        case '(':
            getChar();
            return Token.LEFT_PARENT;
        case ')':
            getChar();
            return Token.RIGHT_PARENT;
        case '|':
            getChar();
            return Token.OR;
        case '^':
            getChar();
            return Token.XOR;
        case '&':
            getChar();
            return nextToken();
        case '?':
            getChar();
            return Token.QUESTION_MARK;
        case '"':
            getChar();
            currentText = getString(true);
            getChar();
            return Token.KEY;
        default:
            String prefix = "";
            if (c == '-') {
                getChar();
                if (!Character.isDigit(c))
                    return Token.NOT;
                prefix = "-";
            }
            currentText = prefix + getString();
            if ("or".equalsIgnoreCase(currentText))
                return Token.OR;
            else if ("xor".equalsIgnoreCase(currentText))
                return Token.XOR;
            else if ("and".equalsIgnoreCase(currentText))
                return nextToken();
            // try parsing number
            try {
                currentNumber = Long.valueOf(currentText);
            } catch (NumberFormatException e) {
                currentNumber = null;
            }
            // if text contains "-", try parsing a range
            int pos = currentText.indexOf('-', 1);
            isRange = pos > 0;
            if (isRange) {
                try {
                    currentNumber = Long.valueOf(currentText.substring(0, pos));
                } catch (NumberFormatException e) {
                    currentNumber = null;
                }
                try {
                    currentRange = Long.valueOf(currentText.substring(pos + 1));
                } catch (NumberFormatException e) {
                    currentRange = null;
                    }
                } else {
                    currentRange = null;
                }
            return Token.KEY;
        }
    }

    /**
     * Reads the next token if it is equal to the given, suggested token
     * @param token The token the next one should be equal to
     * @return <code>true</code> if it has been read
     */
    public boolean readIfEqual(Token token) {
        Token nextTok = nextToken();
        if (Objects.equals(nextTok, token))
            return true;
        currentToken = nextTok;
        return false;
    }

    /**
     * Reads the next token. If it is a text, return that text. If not, advance
     * @return the text or <code>null</code> if the reader was advanced
     */
    public String readTextOrNumber() {
        Token nextTok = nextToken();
        if (nextTok == Token.KEY)
            return currentText;
        currentToken = nextTok;
        return null;
    }

    /**
     * Reads a number
     * @param errorMessage The error if the number cannot be read
     * @return The number that was found
     * @throws SearchParseError if there is no number
     */
    public long readNumber(String errorMessage) throws SearchParseError {
        if ((nextToken() == Token.KEY) && (currentNumber != null))
            return currentNumber;
        else
            throw new SearchParseError(errorMessage);
    }

    /**
     * Gets the last number that was read
     * @return The last number
     */
    public long getReadNumber() {
        return (currentNumber != null) ? currentNumber : 0;
    }

    /**
     * Reads a range of numbers
     * @param errorMessage The error if the input is malformed
     * @return The range that was found
     * @throws SearchParseError If the input is not as expected for a range
     */
    public Range readRange(String errorMessage) throws SearchParseError {
        if (nextToken() != Token.KEY || (currentNumber == null && currentRange == null)) {
            throw new SearchParseError(errorMessage);
        } else if (!isRange && currentNumber != null) {
            if (currentNumber >= 0) {
                return new Range(currentNumber, currentNumber);
            } else {
                return new Range(0, Math.abs(currentNumber));
            }
        } else if (isRange && currentRange == null) {
            return new Range(currentNumber, Long.MAX_VALUE);
        } else if (currentNumber != null && currentRange != null) {
            return new Range(currentNumber, currentRange);
        } else {
            throw new SearchParseError(errorMessage);
        }
    }

    /**
     * Gets the last text that was found
     * @return The text
     */
    public String getText() {
        return currentText;
    }
}
