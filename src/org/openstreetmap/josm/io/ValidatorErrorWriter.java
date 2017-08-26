// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Class to write a collection of validator errors out to XML.
 * The format is inspired by the
 * <a href="https://wiki.openstreetmap.org/wiki/Osmose#Issues_file_format">Osmose API issues file format</a>
 * @since 12667
 */
public class ValidatorErrorWriter extends XmlWriter {

    /**
     * Constructs a new {@code ValidatorErrorWriter} that will write to the given {@link PrintWriter}.
     * @param out PrintWriter to write XML to
     */
    public ValidatorErrorWriter(PrintWriter out) {
        super(out);
    }

    /**
     * Constructs a new {@code ValidatorErrorWriter} that will write to a given {@link OutputStream}.
     * @param out OutputStream to write XML to
     */
    public ValidatorErrorWriter(OutputStream out) {
        super(new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))));
    }

    /**
     * Write validator errors to designated output target
     * @param validationErrors Test error collection to write
     */
    public void write(Collection<TestError> validationErrors) {
        Set<Test> analysers = validationErrors.stream().map(TestError::getTester).collect(Collectors.toCollection(TreeSet::new));
        String timestamp = DateUtils.fromDate(new Date());

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<analysers generator='JOSM' timestamp=\""+timestamp+"\">");

        OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(out, true, OsmChangeBuilder.DEFAULT_API_VERSION);
        String lang = LanguageInfo.getJOSMLocaleCode();

        for (Test test : analysers) {
            out.println("  <analyser timestamp=\""+timestamp+"\" name=\""+test.getName()+"\">");
            // Build map of test error classes for the current test
            Map<ErrorClass, List<TestError>> map = new HashMap<>();
            for (Entry<Severity, Map<String, Map<String, List<TestError>>>> e1 :
                OsmValidator.getErrorsBySeverityMessageDescription(validationErrors, e -> e.getTester() == test).entrySet()) {
                for (Entry<String, Map<String, List<TestError>>> e2 : e1.getValue().entrySet()) {
                    ErrorClass errorClass = new ErrorClass(e1.getKey(), e2.getKey());
                    List<TestError> list = map.get(errorClass);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(errorClass, list);
                    }
                    e2.getValue().values().stream().forEach(list::addAll);
                }
            }
            // Write classes
            for (ErrorClass ec : map.keySet()) {
                out.println("    <class id=\""+ec.id+"\" level=\""+ec.severity.getLevel()+"\">");
                out.println("      <classtext lang=\""+lang+"\" title=\""+ec.message+"\"/>");
                out.println("    </class>");
            }

            // Write errors
            for (Entry<ErrorClass, List<TestError>> entry : map.entrySet()) {
                for (TestError error : entry.getValue()) {
                    LatLon ll = error.getPrimitives().iterator().next().getBBox().getCenter();
                    out.println("    <error class=\""+entry.getKey().id+"\">");
                    out.println("      <location lat=\""+ll.lat()+"\" lon=\""+ll.lon()+"\">");
                    for (OsmPrimitive p : error.getPrimitives()) {
                        p.accept(osmWriter);
                    }
                    out.println("      <text lang=\""+lang+"\" value=\""+error.getDescription()+"\">");
                    if (error.isFixable()) {
                        out.println("      <fixes>");
                        Command fix = error.getFix();
                        if (fix instanceof AddPrimitivesCommand) {
                            Logging.info("TODO: {0}", fix);
                        } else if (fix instanceof DeleteCommand) {
                            Logging.info("TODO: {0}", fix);
                        } else if (fix instanceof ChangePropertyCommand) {
                            Logging.info("TODO: {0}", fix);
                        } else if (fix instanceof ChangePropertyKeyCommand) {
                            Logging.info("TODO: {0}", fix);
                        } else {
                            Logging.warn("Unsupported command type: {0}", fix);
                        }
                        out.println("      </fixes>");
                    }
                    out.println("    </error>");
                }
            }

            out.println("  </analyser>");
        }

        out.println("</analysers>");
        out.flush();
    }

    private static class ErrorClass {
        static int idCounter;
        final Severity severity;
        final String message;
        final int id;

        ErrorClass(Severity severity, String message) {
            this.severity = severity;
            this.message = message;
            this.id = ++idCounter;
        }
    }
}
