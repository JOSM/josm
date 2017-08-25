// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.search.PushbackTokenizer.Token;

/**
 * Search compiler parsing error.
 * @since 12656 (extracted from {@link SearchCompiler}).
 */
public class SearchParseError extends Exception {

    /**
     * Constructs a new generic {@code ParseError}.
     * @param msg the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public SearchParseError(String msg) {
        super(msg);
    }

    /**
     * Constructs a new generic {@code ParseError}.
     * @param msg the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public SearchParseError(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a new detailed {@code ParseError}.
     * @param expected expected token
     * @param found actual token
     */
    public SearchParseError(Token expected, Token found) {
        this(tr("Unexpected token. Expected {0}, found {1}", expected, found));
    }
}
