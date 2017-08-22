// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit.HTMLFactory;

import org.openstreetmap.josm.tools.Logging;

/**
 * Specialized HTML Factory allowing to display SVG images.
 * @since 8933
 */
public class JosmHTMLFactory extends HTMLFactory {

    @Override
    public View create(Element elem) {
        AttributeSet attrs = elem.getAttributes();
        Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
        Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            HTML.Tag kind = (HTML.Tag) o;
            if (kind == HTML.Tag.IMG) {
                try {
                    return new JosmImageView(elem);
                } catch (NoSuchFieldException | SecurityException e) {
                    Logging.error(e);
                }
            }
        }
        return super.create(elem);
    }
}
