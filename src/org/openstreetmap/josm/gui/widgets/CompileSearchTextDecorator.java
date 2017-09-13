// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.tools.Logging;

/**
 * Decorates a text component with an execution to the search compiler. Afterwards, a {@code "filter"} property change
 * will be fired and the compiled search can be accessed with {@link #getMatch()}.
 */
public final class CompileSearchTextDecorator implements DocumentListener {

    private final JTextComponent textComponent;
    private final String originalToolTipText;
    private SearchCompiler.Match filter;

    private CompileSearchTextDecorator(JTextComponent textComponent) {
        this.textComponent = textComponent;
        this.originalToolTipText = textComponent.getToolTipText();
        textComponent.getDocument().addDocumentListener(this);
    }

    /**
     * Decorates a text component with an execution to the search compiler. Afterwards, a {@code "filter"} property change
     * will be fired and the compiled search can be accessed with {@link #getMatch()}.
     * @param f the text component to decorate
     * @return an instance of the decorator in order to access the compiled search via {@link #getMatch()}
     */
    public static CompileSearchTextDecorator decorate(JTextComponent f) {
        return new CompileSearchTextDecorator(f);
    }

    private void setFilter() {
        try {
            textComponent.setBackground(UIManager.getColor("TextField.background"));
            textComponent.setToolTipText(originalToolTipText);
            filter = SearchCompiler.compile(textComponent.getText());
        } catch (SearchParseError ex) {
            textComponent.setBackground(new Color(255, 224, 224));
            textComponent.setToolTipText(ex.getMessage());
            filter = SearchCompiler.Always.INSTANCE;
            Logging.debug(ex);
        }
        textComponent.firePropertyChange("filter", 0, 1);
    }

    /**
     * Returns the compiled search
     * @return the compiled search
     */
    public SearchCompiler.Match getMatch() {
        return filter;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        setFilter();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        setFilter();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        setFilter();
    }
}
