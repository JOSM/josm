// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.tools.template_engine.Tokenizer.Token;
import org.openstreetmap.josm.tools.template_engine.Tokenizer.TokenType;

/**
 * Exception thrown in case of an error during template parsing.
 *
 * Usually caused by invalid user input.
 */
public class ParseError extends Exception {

    private final transient Token unexpectedToken;

    /**
     * Constructs a new {@code ParseError} for an unexpected token.
     * @param unexpectedToken the unexpected token
     */
    public ParseError(Token unexpectedToken) {
        super(tr("Unexpected token ({0}) on position {1}", unexpectedToken.getType(), unexpectedToken.getPosition()));
        this.unexpectedToken = unexpectedToken;
    }

    /**
     * Constructs a new {@code ParseError} for an unexpected token and an expected token.
     * @param unexpectedToken the unexpected token
     * @param expected the expected token
     */
    public ParseError(Token unexpectedToken, TokenType expected) {
        super(tr("Unexpected token on position {0}. Expected {1}, found {2}",
                unexpectedToken.getPosition(), expected, unexpectedToken.getType()));
        this.unexpectedToken = unexpectedToken;
    }

    /**
     * Constructs a new {@code ParseError} from a {@link SearchParseError}.
     * @param position the position
     * @param e the cause
     */
    public ParseError(int position, SearchParseError e) {
        super(tr("Error while parsing search expression on position {0}", position), e);
        unexpectedToken = null;
    }

    /**
     * Constructs a new {@code ParseError} with a generic message.
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ParseError(String message) {
        super(message);
        unexpectedToken = null;
    }

    /**
     * Returns the unexpected token, if any.
     * @return the unexpected token, or null
     */
    public Token getUnexpectedToken() {
        return unexpectedToken;
    }

    /**
     * Constructs a new {@code ParseError} for an unexpected character.
     * @param expected the expected character
     * @param found the found character
     * @param position the position
     * @return a new {@code ParseError}
     */
    public static ParseError unexpectedChar(char expected, char found, int position) {
        return new ParseError(tr("Unexpected char on {0}. Expected {1} found {2}", position, expected, found));
    }
}
