// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import java.util.Collection;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Validation error easily fixable right at its detection. The fix can be given when constructing the error.
 * @since 6377
 */
public class FixableTestError extends TestError {
    protected final Command fix;

    /**
     * Constructs a new {@code FixableTestError} for a single primitive.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param code The test error reference code
     * @param primitive The affected primitive
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, int code, OsmPrimitive primitive, Command fix) {
        super(tester, severity, message, code, primitive);
        this.fix = fix;
    }

    /**
     * Constructs a new {@code FixableTestError} for multiple primitives.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param code The test error reference code
     * @param primitives The affected primitives
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives,
            Command fix) {
        super(tester, severity, message, code, primitives);
        this.fix = fix;
    }

    /**
     * Constructs a new {@code FixableTestError} for multiple primitives.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param code The test error reference code
     * @param primitives The affected primitives
     * @param highlighted OSM primitives to highlight
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives,
            Collection<?> highlighted, Command fix) {
        super(tester, severity, message, code, primitives, highlighted);
        this.fix = fix;
    }

    /**
     * Constructs a new {@code FixableTestError} for a single primitive.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param description The translated description
     * @param descriptionEn The English description
     * @param code The test error reference code
     * @param primitive The affected primitive
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, String description, String descriptionEn, int code,
            OsmPrimitive primitive, Command fix) {
        super(tester, severity, message, description, descriptionEn, code, primitive);
        this.fix = fix;
    }

    /**
     * Constructs a new {@code FixableTestError} for multiple primitives.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param description The translated description
     * @param descriptionEn The English description
     * @param code The test error reference code
     * @param primitives The affected primitives
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, String description, String descriptionEn, int code,
            Collection<? extends OsmPrimitive> primitives, Command fix) {
        super(tester, severity, message, description, descriptionEn, code, primitives);
        this.fix = fix;
    }

    /**
     * Constructs a new {@code FixableTestError} for multiple primitives.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param description The translated description
     * @param descriptionEn The English description
     * @param code The test error reference code
     * @param primitives The affected primitives
     * @param highlighted OSM primitives to highlight
     * @param fix The command used to fix the error
     */
    public FixableTestError(Test tester, Severity severity, String message, String description, String descriptionEn, int code,
            Collection<? extends OsmPrimitive> primitives, Collection<?> highlighted, Command fix) {
        super(tester, severity, message, description, descriptionEn, code, primitives, highlighted);
        this.fix = fix;
    }

    @Override
    public Command getFix() {
        return fix;
    }

    @Override
    public final boolean isFixable() {
        return true;
    }
}
