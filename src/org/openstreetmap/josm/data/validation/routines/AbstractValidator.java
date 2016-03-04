// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.routines;

/**
 * Abstract validator superclass to extend Apache Validator routines.
 * @since 7489
 */
public abstract class AbstractValidator {

    private String errorMessage;

    /**
     * Tests validity of a given value.
     * @param value Value to test
     * @return {@code true} if value is valid, {@code false} otherwise
     */
    public abstract boolean isValid(String value);

    /**
     * Returns the name of this validator
     * @return the name of this validator
     */
    public abstract String getValidatorName();

    /**
     * Replies the error message.
     * @return the errorMessage
     */
    public final String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     * @param errorMessage the errorMessage
     */
    protected final void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
