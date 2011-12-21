package org.openstreetmap.josm.gui.progress;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;

public class ContributorTermsUpdateRunnable extends PleaseWaitRunnable {

    public ContributorTermsUpdateRunnable() {
        super(tr("Updating CT user information"));
    }

    @Override
    protected void cancel() {
    }

    @Override
    protected void realRun() {
        progressMonitor.indeterminateSubTask(null);
        User.initRelicensingInformation();
    }

    @Override
    protected void finish() {
    }
}
