// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This panel displays the settings that can be changed before submitting a bug report to the web page.
 * @author Michael Zangl
 * @since 10585
 */
public class BugReportSettingsPanel extends JPanel {
    /**
     * Creates the new settings panel.
     * @param report The report this panel should influence.
     */
    public BugReportSettingsPanel(BugReport report) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JCheckBox statusReport = new JCheckBox(tr("Include the system status report."));
        statusReport.setSelected(report.isIncludeStatusReport());
        statusReport.addChangeListener(e -> report.setIncludeStatusReport(statusReport.isSelected()));
        add(statusReport);

        JCheckBox data = new JCheckBox(tr("Include information about the data you were working on."));
        data.setSelected(report.isIncludeData());
        data.addChangeListener(e -> report.setIncludeData(data.isSelected()));
        add(data);

        JCheckBox allStackTraces = new JCheckBox(tr("Include all stack traces."));
        allStackTraces.setSelected(report.isIncludeAllStackTraces());
        allStackTraces.addChangeListener(e -> report.setIncludeAllStackTraces(allStackTraces.isSelected()));
        add(allStackTraces);
    }
}
