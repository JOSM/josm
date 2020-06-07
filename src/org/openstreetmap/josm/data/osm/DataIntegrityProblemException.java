// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;

/**
 * Exception thrown when a primitive or data set does not pass its integrity checks.
 * @since 2399
 */
public class DataIntegrityProblemException extends RuntimeException {

    private final String htmlMessage;

    /**
     * Constructs a new {@code DataIntegrityProblemException}.
     * @param message the detail message
     */
    public DataIntegrityProblemException(String message) {
        this(message, null);
    }

    /**
     * Constructs a new {@code DataIntegrityProblemException}.
     * @param message the detail message
     * @param htmlMessage HTML-formatted error message. Can be null
     * @param p the primitive involved in this integrity problem (used for constructing a detailed message)
     */
    public DataIntegrityProblemException(String message, String htmlMessage, OsmPrimitive... p) {
        super(message + relevantCommands(p));
        this.htmlMessage = htmlMessage;
    }

    /**
     * Returns the HTML-formatted error message.
     * @return the HTML-formatted error message, or null
     */
    public String getHtmlMessage() {
        return htmlMessage;
    }

    private static String relevantCommands(OsmPrimitive... p) {
        if (p == null || p.length == 0) {
            return "";
        }
        Predicate<Command> isParticipating = c -> Arrays.stream(p).anyMatch(c.getParticipatingPrimitives()::contains);
        Stream<String> undo = UndoRedoHandler.getInstance().getUndoCommands().stream()
                .filter(isParticipating)
                .map(c -> "[" + c.getDescriptionText() + "]");
        Stream<String> redo = UndoRedoHandler.getInstance().getRedoCommands().stream()
                .filter(isParticipating)
                .map(c -> "[" + c.getDescriptionText() + " (undone)]");
        return Stream.concat(undo, redo)
                .collect(Collectors.joining(", ", " (changed by the following commands: ", ")"));
    }
}
