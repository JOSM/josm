// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download.overpass;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.gui.download.OverpassQueryWizardDialog;

/**
 * Registers the overpass query wizards.
 * @author Michael Zangl
 * @since 13930
 */
public final class OverpassWizardRegistration {
    /**
     * A list of all reigstered wizards. Needs to be synchronized since plugin registration may happen outside main thread / asynchronously.
     */
    private static List<OverpassQueryWizard> wizards = Collections.synchronizedList(new ArrayList<>());

    /**
     * Registers a wizard to be added to the overpass download dialog
     * <p>
     * To be called by plugins during the JOSM boot process or at least before opening the download dialog for the first time.
     * @param wizard The wizard to register
     * @since 13930
     */
    public static void registerWizard(OverpassQueryWizard wizard) {
        Objects.requireNonNull(wizard, "wizard");
        wizards.add(wizard);
    }

    /**
     * Gets all wizards that are currently registered.
     * @return The list of wizards.
     */
    public static List<OverpassQueryWizard> getWizards() {
        return Collections.unmodifiableList(wizards);
    }

    static {
        // Register the default wizard
        registerWizard(new OverpassQueryWizard() {
            @Override
            public void startWizard(OverpassWizardCallbacks callbacks) {
                new OverpassQueryWizardDialog(callbacks).showDialog();
            }

            @Override
            public Optional<String> getWizardTooltip() {
                return Optional.of(tr("Build an Overpass query using the Overpass Turbo Query Wizard tool"));
            }

            @Override
            public String getWizardName() {
                return tr("Query Wizard");
            }
        });
    }

    private OverpassWizardRegistration() {
        // hidden
    }

    /**
     * Defines a query wizard that generates overpass queries.
     * @author Michael Zangl
     * @since 13930
     */
    public interface OverpassQueryWizard {
        /**
         * Get the name of the wizard
         * @return The name
         */
        String getWizardName();

        /**
         * Get the tooltip text to display when hovering the wizard button.
         * @return The tooltip text or an empty optional to display no tooltip.
         */
        Optional<String> getWizardTooltip();

        /**
         * Start the wizard.
         * @param callbacks The callbacks to use to send back wizard results.
         */
        void startWizard(OverpassWizardCallbacks callbacks);
    }

    /**
     * Wizard callbacks required by {@link OverpassQueryWizard#startWizard(OverpassWizardCallbacks)}
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
