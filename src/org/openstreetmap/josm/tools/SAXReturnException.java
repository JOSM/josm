// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.xml.sax.SAXException;

/** Quit parsing, when a certain condition is met */
class SAXReturnException extends SAXException {
    private final String result;

    SAXReturnException(String result) {
        this.result = result;
    }

    public final String getResult() {
        return result;
    }
}
