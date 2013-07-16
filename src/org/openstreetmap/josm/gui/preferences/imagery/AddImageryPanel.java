// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;

/**
 * An abstract imagery panel used to add WMS/TMS imagery sources. See implementations.
 * @see AddTMSLayerPanel
 * @see AddWMSLayerPanel
 * @since 5617
 */
public abstract class AddImageryPanel extends JPanel {

    protected final JosmTextArea rawUrl = new JosmTextArea(3, 40);
    protected final JosmTextField name = new JosmTextField();

    protected final Collection<ContentValidationListener> listeners = new ArrayList<ContentValidationListener>();

    /**
     * A listener notified when the validation status of this panel change.
     */
    public interface ContentValidationListener {
        /**
         * Called when the validation status of this panel changed
         * @param isValid true if the conditions required to close this panel are met
         */
        public void contentChanged(boolean isValid);
    }

    protected AddImageryPanel() {
        this(new GridBagLayout());
    }

    protected AddImageryPanel(LayoutManager layout) {
        super(layout);
        registerValidableComponent(name);
    }

    protected final void registerValidableComponent(AbstractButton component) {
        component.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { notifyListeners(); }
        });
    }

    protected final void registerValidableComponent(JTextComponent component) {
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void removeUpdate(DocumentEvent e) { notifyListeners(); }
            @Override public void insertUpdate(DocumentEvent e) { notifyListeners(); }
            @Override public void changedUpdate(DocumentEvent e) { notifyListeners(); }
        });
    }

    protected abstract ImageryInfo getImageryInfo();

    protected static String sanitize(String s) {
        return s.replaceAll("[\r\n]+", "").trim();
    }

    protected final String getImageryName() {
        return sanitize(name.getText());
    }

    protected final String getImageryRawUrl() {
        return sanitize(rawUrl.getText());
    }

    protected abstract boolean isImageryValid();

    /**
     * Registers a new ContentValidationListener
     * @param l The new ContentValidationListener that will be notified of validation status changes
     */
    public final void addContentValidationListener(ContentValidationListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    private final void notifyListeners() {
        for (ContentValidationListener l : listeners) {
            l.contentChanged(isImageryValid());
        }
    }
}
