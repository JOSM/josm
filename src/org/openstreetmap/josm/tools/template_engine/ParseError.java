// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.tools.template_engine.Tokenizer.Token;
import org.openstreetmap.josm.tools.template_engine.Tokenizer.TokenType;

public class ParseError extends Exception {

    private final Token unexpectedToken;

    public ParseError(Token unexpectedToken) {
        super(tr("Unexpected token ({0}) on position {1}", unexpectedToken.getType(), unexpectedToken.getPosition()));
        this.unexpectedToken = unexpectedToken;
    }

    public ParseError(Token unexpectedToken, TokenType expected) {
        super(tr("Unexpected token on position {0}. Expected {1}, found {2}", unexpectedToken.getPosition(), expected, unexpectedToken.getType()));
        this.unexpectedToken = unexpectedToken;
    }

    public ParseError(org.openstreetmap.josm.actions.search.SearchCompiler.ParseError e) {
        super(tr("Error while parsing search expression"), e);
        unexpectedToken = null;
    }

    public Token getUnexpectedToken() {
        return unexpectedToken;
    }
}
