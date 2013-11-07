package org.openstreetmap.josm.data.validation;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

import java.util.Collection;

public class FixableTestError extends TestError {
    protected final Command fix;

    public FixableTestError(Test tester, Severity severity, String message, int code, OsmPrimitive primitive, Command fix) {
        super(tester, severity, message, code, primitive);
        this.fix = fix;
    }

    public FixableTestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives, Command fix) {
        super(tester, severity, message, code, primitives);
        this.fix = fix;
    }

    public FixableTestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives, Collection<?> highlighted, Command fix) {
        super(tester, severity, message, code, primitives, highlighted);
        this.fix = fix;
    }

    public FixableTestError(Test tester, Severity severity, String message, String description, String description_en, int code, OsmPrimitive primitive, Command fix) {
        super(tester, severity, message, description, description_en, code, primitive);
        this.fix = fix;
    }

    public FixableTestError(Test tester, Severity severity, String message, String description, String description_en, int code, Collection<? extends OsmPrimitive> primitives, Command fix) {
        super(tester, severity, message, description, description_en, code, primitives);
        this.fix = fix;
    }

    public FixableTestError(Test tester, Severity severity, String message, String description, String description_en, int code, Collection<? extends OsmPrimitive> primitives, Collection<?> highlighted, Command fix) {
        super(tester, severity, message, description, description_en, code, primitives, highlighted);
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
