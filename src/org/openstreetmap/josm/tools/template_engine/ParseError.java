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

    public ParseError(Token unexpectedToken) {
        super(tr("Unexpected token ({0}) on position {1}", unexpectedToken.getType(), unexpectedToken.getPosition()));
        this.unexpectedToken = unexpectedToken;
    }

    public ParseError(Token unexpectedToken, TokenType expected) {
        super(tr("Unexpected token on position {0}. Expected {1}, found {2}",
                unexpectedToken.getPosition(), expected, unexpectedToken.getType()));
        this.unexpectedToken = unexpectedToken;
    }

    public ParseError(int position, SearchParseError e) {
        super(tr("Error while parsing search expression on position {0}", position), e);
        unexpectedToken = null;
    }

    public ParseError(String message) {
        super(message);
        unexpectedToken = null;
    }

    public Token getUnexpectedToken() {
        return unexpectedToken;
    }

    public static ParseError unexpectedChar(char expected, char found, int position) {
        return new ParseError(tr("Unexpected char on {0}. Expected {1} found {2}", position, expected, found));
    }
}
