// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is an abstract class for a validator on a text component.
 *
 * Subclasses implement {@link #validate()}. {@link #validate()} is invoked whenever
 * <ul>
 *   <li>the content of the text component changes (the validator is a {@link DocumentListener})</li>
 *   <li>the text component loses focus (the validator is a {@link FocusListener})</li>
 *   <li>the text component is a {@link JosmTextField} and an {@link ActionEvent} is detected</li>
 * </ul>
 *
 *
 */
public abstract class AbstractTextComponentValidator implements ActionListener, FocusListener, DocumentListener, PropertyChangeListener{
    static final private Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
    static final private Color ERROR_BACKGROUND =  new Color(255,224,224);

    private JTextComponent tc;
    /** remembers whether the content of the text component is currently valid or not; null means,
     * we don't know yet
     */
    private Boolean valid = null;
    // remember the message
    private String msg;

    protected void feedbackInvalid(String msg) {
        if (valid == null || valid || !Utils.equal(msg, this.msg)) {
            // only provide feedback if the validity has changed. This avoids
            // unnecessary UI updates.
            tc.setBorder(ERROR_BORDER);
            tc.setBackground(ERROR_BACKGROUND);
            tc.setToolTipText(msg);
            valid = false;
            this.msg = msg;
        }
    }

    protected void feedbackDisabled() {
        feedbackValid(null);
    }

    protected void feedbackValid(String msg) {
        if (valid == null || !valid || !Utils.equal(msg, this.msg)) {
            // only provide feedback if the validity has changed. This avoids
            // unnecessary UI updates.
            tc.setBorder(UIManager.getBorder("TextField.border"));
            tc.setBackground(UIManager.getColor("TextField.background"));
            tc.setToolTipText(msg == null ? "" : msg);
            valid = true;
            this.msg = msg;
        }
    }

    /**
     * Replies the decorated text component
     *
     * @return the decorated text component
     */
    public JTextComponent getComponent() {
        return tc;
    }

    /**
     * Creates the validator and weires it to the text component <code>tc</code>.
     *
     * @param tc the text component. Must not be null.
     * @throws IllegalArgumentException thrown if tc is null
     */
    public AbstractTextComponentValidator(JTextComponent tc) throws IllegalArgumentException {
        this(tc, true);
    }

    /**
     * Alternative constructor that allows to turn off the actionListener.
     * This can be useful if the enter key stroke needs to be forwarded to the default button in a dialog.
     */
    public AbstractTextComponentValidator(JTextComponent tc, boolean addActionListener) throws IllegalArgumentException {
        this(tc, true, true, addActionListener);
    }

    public AbstractTextComponentValidator(JTextComponent tc, boolean addFocusListener, boolean addDocumentListener, boolean addActionListener) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(tc, "tc");
        this.tc = tc;
        if (addFocusListener) {
            tc.addFocusListener(this);
        }
        if (addDocumentListener) {
            tc.getDocument().addDocumentListener(this);
        }
        if (addActionListener) {
            if (tc instanceof JosmTextField) {
                JosmTextField tf = (JosmTextField)tc;
                tf.addActionListener(this);
            }
        }
        tc.addPropertyChangeListener("enabled", this);
    }

    /**
     * Implement in subclasses to validate the content of the text component.
     *
     */
    public abstract void validate();

    /**
     * Replies true if the current content of the decorated text component is valid;
     * false otherwise
     *
     * @return true if the current content of the decorated text component is valid
     */
    public abstract boolean isValid();

    /* -------------------------------------------------------------------------------- */
    /* interface FocusListener                                                          */
    /* -------------------------------------------------------------------------------- */
    @Override
    public void focusGained(FocusEvent arg0) {}

    @Override
    public void focusLost(FocusEvent arg0) {
        validate();
    }

    /* -------------------------------------------------------------------------------- */
    /* interface ActionListener                                                         */
    /* -------------------------------------------------------------------------------- */
    @Override
    public void actionPerformed(ActionEvent arg0) {
        validate();
    }

    /* -------------------------------------------------------------------------------- */
    /* interface DocumentListener                                                       */
    /* -------------------------------------------------------------------------------- */
    @Override
    public void changedUpdate(DocumentEvent arg0) {
        validate();
    }

    @Override
    public void insertUpdate(DocumentEvent arg0) {
        validate();
    }

    @Override
    public void removeUpdate(DocumentEvent arg0) {
        validate();
    }

    /* -------------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                                 */
    /* -------------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("enabled")) {
            boolean enabled = (Boolean)evt.getNewValue();
            if (enabled) {
                validate();
            } else {
                feedbackDisabled();
            }
        }
    }
}
