// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoaderJob;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * An abstract imagery panel used to add WMS/TMS imagery sources. See implementations.
 * @see AddTMSLayerPanel
 * @see AddWMSLayerPanel
 * @see AddWMTSLayerPanel
 * @since 5617
 */
public abstract class AddImageryPanel extends JPanel {

    protected final JosmTextArea rawUrl = new JosmTextArea(3, 40).transferFocusOnTab();
    protected final JosmTextField name = new JosmTextField();

    protected final transient Collection<ContentValidationListener> listeners = new ArrayList<>();

    private final JCheckBox validGeoreference = new JCheckBox(tr("Is layer properly georeferenced?"));
    private HeadersTable headersTable;
    private JSpinner minimumCacheExpiry;
    private JComboBox<String> minimumCacheExpiryUnit;
    private TimeUnit currentUnit;

    /**
     * A listener notified when the validation status of this panel change.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface ContentValidationListener {
        /**
         * Called when the validation status of this panel changed
         * @param isValid true if the conditions required to close this panel are met
         */
        void contentChanged(boolean isValid);
    }

    protected AddImageryPanel() {
        this(new GridBagLayout());
        headersTable = new HeadersTable();
        minimumCacheExpiry = new JSpinner(new SpinnerNumberModel(
                (Number) TimeUnit.MILLISECONDS.toSeconds(TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get()),
                0L,
                Long.valueOf(Integer.MAX_VALUE),
                1
                ));
        List<String> units = Arrays.asList(tr("seconds"), tr("minutes"), tr("hours"), tr("days"));
        minimumCacheExpiryUnit = new JComboBox<>(units.toArray(new String[]{}));
        currentUnit = TimeUnit.SECONDS;
        minimumCacheExpiryUnit.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                long newValue = 0;
                switch (units.indexOf(e.getItem())) {
                case 0:
                    newValue = currentUnit.toSeconds((long) minimumCacheExpiry.getValue());
                    currentUnit = TimeUnit.SECONDS;
                    break;
                case 1:
                    newValue = currentUnit.toMinutes((long) minimumCacheExpiry.getValue());
                    currentUnit = TimeUnit.MINUTES;
                    break;
                case 2:
                    newValue = currentUnit.toHours((long) minimumCacheExpiry.getValue());
                    currentUnit = TimeUnit.HOURS;
                    break;
                case 3:
                    newValue = currentUnit.toDays((long) minimumCacheExpiry.getValue());
                    currentUnit = TimeUnit.DAYS;
                    break;
                default:
                    Logging.warn("Unknown unit: " + units.indexOf(e.getItem()));
                }
                minimumCacheExpiry.setValue(newValue);
            }
        });


    }

    protected void addCommonSettings() {
        if (ExpertToggleAction.isExpert()) {
            add(new JLabel(tr("Minimum cache expiry: ")));
            add(minimumCacheExpiry);
            add(minimumCacheExpiryUnit, GBC.eol());
            add(new JLabel(tr("Set custom HTTP headers (if needed):")), GBC.eop());
            add(headersTable, GBC.eol().fill());
            add(validGeoreference, GBC.eop().fill(GBC.HORIZONTAL));
        }
    }

    protected Map<String, String> getCommonHeaders() {
        return headersTable.getHeaders();
    }

    protected boolean getCommonIsValidGeoreference() {
        return validGeoreference.isSelected();
    }

    protected AddImageryPanel(LayoutManager layout) {
        super(layout);
        registerValidableComponent(name);
    }

    protected final void registerValidableComponent(AbstractButton component) {
        component.addChangeListener(e -> notifyListeners());
    }

    protected final void registerValidableComponent(JTextComponent component) {
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyListeners();
            }
        });
    }

    protected abstract ImageryInfo getImageryInfo();

    protected static String sanitize(String s) {
        return s.replaceAll("[\r\n]+", "").trim();
    }

    protected static String sanitize(String s, ImageryType type) {
        String ret = s;
        String imageryType = type.getTypeString() + ':';
        if (ret.startsWith(imageryType)) {
            // remove ImageryType from URL
            ret = ret.substring(imageryType.length());
        }
        return sanitize(ret);
    }

    protected final String getImageryName() {
        return sanitize(name.getText());
    }

    protected final String getImageryRawUrl() {
        return sanitize(rawUrl.getText());
    }

    protected abstract boolean isImageryValid();

    /**
     * Registers a new ContentValidationListener
     * @param l The new ContentValidationListener that will be notified of validation status changes
     */
    public final void addContentValidationListener(ContentValidationListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    private void notifyListeners() {
        for (ContentValidationListener l : listeners) {
            l.contentChanged(isImageryValid());
        }
    }
}
