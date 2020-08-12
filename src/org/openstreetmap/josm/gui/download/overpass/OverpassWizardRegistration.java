// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download.overpass;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.josm.gui.download.OverpassQueryWizardDialog;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Registers the overpass query wizards.
 * @author Michael Zangl
 * @since 13930
 */
public final class OverpassWizardRegistration {
    /**
     * A list of all registered wizards. Needs to be synchronized since plugin registration may happen outside main thread / asynchronously.
     */
    private static final List<Function<OverpassWizardCallbacks, Action>> wizards = Collections.synchronizedList(new ArrayList<>());

    /**
     * Registers a wizard to be added to the overpass download dialog
     * <p>
     * To be called by plugins during the JOSM boot process or at least before opening the download dialog for the first time.
     * @param wizard The wizard to register
     * @since 13930, 16355 (signature)
     */
    public static void registerWizard(Function<OverpassWizardCallbacks, Action> wizard) {
        Objects.requireNonNull(wizard, "wizard");
        wizards.add(wizard);
    }

    /**
     * Gets all wizards that are currently registered.
     * @param callbacks wizard callbacks
     * @return The list of wizards.
     */
    public static List<Action> getWizards(OverpassWizardCallbacks callbacks) {
        return wizards.stream()
                .map(x -> x.apply(callbacks))
                .collect(Collectors.toList());
    }

    static {
        // Register the default wizard
        registerWizard(callbacks -> new AbstractAction(tr("Query Wizard")) {
            {
                putValue(SHORT_DESCRIPTION, tr("Build an Overpass query using the query wizard"));
                new ImageProvider("dialogs/magic-wand").getResource().attachImageIcon(this, true);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                new OverpassQueryWizardDialog(callbacks).showDialog();
            }
        });
    }

    private OverpassWizardRegistration() {
        // hidden
    }

    /**
     * Wizard callbacks required by {@link #registerWizard}
     * @author Michael Zangl
     * @since 13930
     */
    public interface OverpassWizardCallbacks {
        /**
         * Send the resulting query
         * @param resultingQuery The query that is used by the wizard
         */
        void submitWizardResult(String resultingQuery);

        /**
         * Get the parent component to use when opening the wizard dialog.
         * @return The component.
         */
        Component getParent();
    }
}
