// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * An abstract class for ID text fields.
 *
 * @param <T> The ID validator class
 * @since 5765
 */
public abstract class AbstractIdTextField<T extends AbstractTextComponentValidator> extends JosmTextField {

    protected final transient T validator;

    /**
     * Constructs a new {@link AbstractIdTextField}
     * @param klass The validator class
     */
    public AbstractIdTextField(Class<T> klass) {
        this(klass, 0);
    }

    /**
     * Constructs a new {@link AbstractIdTextField}
     * @param klass The validator class
     * @param columns The number of columns to use to calculate the preferred width
     * @see JosmTextField#JosmTextField(int)
     */
    public AbstractIdTextField(Class<T> klass, int columns) {
        super(columns);
        T validator = null;
        try {
            if (klass != null) {
                validator = klass.getConstructor(JTextComponent.class).newInstance(this);
            }
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
        } finally {
            this.validator = validator;
        }
    }

    /**
     * Performs the field validation
     */
    public final void performValidation() {
        validator.validate();
    }

    /**
     * Clears field if content is invalid
     */
    public final void clearTextIfInvalid() {
        if (!validator.isValid())
            setText("");
        validator.validate();
    }

    /**
     * Reads the id(s).
     * @return true if at least a valid id has been successfully read, false otherwise
     */
    public abstract boolean readIds();

    /**
     * Tries to set text from clipboard (no effect with invalid or empty clipboard)
     */
    public void tryToPasteFromClipboard() {
        tryToPasteFrom(ClipboardUtils.getClipboardStringContent());
    }

    /**
     * Tries to set text from given contents (no effect with invalid or empty contents)
     * @param contents The text to interprete as ID(s)
     * @return true if text has been pasted and valid ids have been read
     */
    public boolean tryToPasteFrom(String contents) {
        if (contents != null && !contents.trim().isEmpty()) {
            setText(contents.trim());
            clearTextIfInvalid();
            return readIds();
        }
        return false;
    }
}
