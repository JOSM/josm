// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Exception for unexpected processing errors during XML stream parsing.
 * It uses proper JOSM i18n system to translate error message, including file location.
 * @since 10235
 */
public class XmlStreamParsingException extends XMLStreamException {

    /**
     * Constructs a new {@code XmlStreamParsingException}.
     * @param msg error message
     * @param location file location
     */
    public XmlStreamParsingException(String msg, Location location) {
        super(msg); /* cannot use super(msg, location) because it messes with the message preventing localization */
        this.location = location;
    }

    /**
     * Constructs a new {@code XmlStreamParsingException}.
     * @param msg error message
     * @param location file location
     * @param th Throwable cause
     */
    public XmlStreamParsingException(String msg, Location location, Throwable th) {
        super(msg, th);
        this.location = location;
    }

    @Override
    public String getMessage() {
        String msg = Optional.ofNullable(super.getMessage()).orElseGet(() -> getClass().getName());
        if (getLocation() == null)
            return msg;
        StringBuilder sb = new StringBuilder(msg).append(' ')
                .append(tr("(at line {0}, column {1})", getLocation().getLineNumber(), getLocation().getColumnNumber()));
        int offset = getLocation().getCharacterOffset();
        if (offset > -1) {
            sb.append(". ").append(tr("{0} bytes have been read", offset));
        }
        return sb.toString();
    }
}
