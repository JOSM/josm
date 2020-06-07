// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.tools.CheckParameterUtil;

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
public abstract class AbstractTextComponentValidator implements ActionListener, FocusListener, DocumentListener, PropertyChangeListener {
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
    private static final Color ERROR_BACKGROUND = new Color(255, 224, 224);

    private JTextComponent tc;
    /** remembers whether the content of the text component is currently valid or not; null means,
     * we don't know yet
     */
    private Boolean valid;
    // remember the message
    private String msg;

    protected void feedbackInvalid(String msg) {
        if (valid == null || valid || !Objects.equals(msg, this.msg)) {
            // only provide feedback if the validity has changed. This avoids unnecessary UI updates.
            tc.setBorder(ERROR_BORDER);
            tc.setBackground(ERROR_BACKGROUND);
            tc.setToolTipText(msg);
            valid = Boolean.FALSE;
            this.msg = msg;
        }
    }

    protected void feedbackDisabled() {
        feedbackValid(null);
    }

    protected void feedbackValid(String msg) {
        if (valid == null || !valid || !Objects.equals(msg, this.msg)) {
            // only provide feedback if the validity has changed. This avoids unnecessary UI updates.
            tc.setBorder(UIManager.getBorder("TextField.border"));
            tc.setBackground(UIManager.getColor("TextField.background"));
            tc.setToolTipText(msg == null ? "" : msg);
            valid = Boolean.TRUE;
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
     * @throws IllegalArgumentException if tc is null
     */
    protected AbstractTextComponentValidator(JTextComponent tc) {
        this(tc, true);
    }

    /**
     * Alternative constructor that allows to turn off the actionListener.
     * This can be useful if the enter key stroke needs to be forwarded to the default button in a dialog.
     * @param tc text component
     * @param addActionListener {@code true} to add the action listener
     */
    protected AbstractTextComponentValidator(JTextComponent tc, boolean addActionListener) {
        this(tc, true, true, addActionListener);
    }

    /**
     * Constructs a new {@code AbstractTextComponentValidator}.
     * @param tc text component
     * @param addFocusListener {@code true} to add the focus listener
     * @param addDocumentListener {@code true} to add the document listener
     * @param addActionListener {@code true} to add the action listener
     */
    protected AbstractTextComponentValidator(
            JTextComponent tc, boolean addFocusListener, boolean addDocumentListener, boolean addActionListener) {
        CheckParameterUtil.ensureParameterNotNull(tc, "tc");
        this.tc = tc;
        if (addFocusListener) {
            tc.addFocusListener(this);
        }
        if (addDocumentListener) {
            tc.getDocument().addDocumentListener(this);
        }
        if (addActionListener && tc instanceof JosmTextField) {
            ((JosmTextField) tc).addActionListener(this);
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
        if ("enabled".equals(evt.getPropertyName())) {
            boolean enabled = (Boolean) evt.getNewValue();
            if (enabled) {
                validate();
            } else {
                feedbackDisabled();
            }
        }
    }
}
