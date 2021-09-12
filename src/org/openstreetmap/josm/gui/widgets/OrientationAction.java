// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * An action that toggles text orientation.
 * @since 18221
 */
public class OrientationAction extends AbstractAction implements PropertyChangeListener {
    /** Default for {@link #RTL_LANGUAGES} */
    public static final List<String> DEFAULT_RTL_LANGUAGES = Arrays.asList("ar", "he", "fa", "iw", "ur", "lld");

    /** Default for {@link #LOCALIZED_KEYS} */
    public static final List<String> DEFAULT_LOCALIZED_KEYS = Arrays.asList(
        "(\\p{Alnum}+_)?name", "addr", "description", "fixme", "note", "source", "strapline", "operator");

    /**
     * Language codes of languages that are right-to-left
     *
     * @see #getValueOrientation
     */
    public static final ListProperty RTL_LANGUAGES = new ListProperty("properties.rtl-languages", DEFAULT_RTL_LANGUAGES);
    /**
     * Keys whose values are localized
     *
     * Regex fractions are allowed. The items will be merged into a regular expression.
     *
     * @see #getValueOrientation
     */
    public static final ListProperty LOCALIZED_KEYS = new ListProperty("properties.localized-keys", DEFAULT_LOCALIZED_KEYS);

    private static final Pattern LANG_PATTERN = Pattern.compile(":([a-z]{2,3})$");

    private Component component;
    private ImageIcon iconRTL;
    private ImageIcon iconLTR;
    protected static Set<String> RTLLanguages = new HashSet<>(RTL_LANGUAGES.get());
    protected static Pattern localizedKeys = compile_localized_keys();

    /**
     * Constructs a new {@code OrientationAction}.
     *
     * @param component The component to toggle
     */
    public OrientationAction(Component component) {
        super(null);
        this.component = component;
        setEnabled(true);
        if (Config.getPref().getBoolean("text.popupmenu.useicons", true)) {
            iconLTR = new ImageProvider("dialogs/next").setSize(ImageProvider.ImageSizes.SMALLICON).get();
            iconRTL = new ImageProvider("dialogs/previous").setSize(ImageProvider.ImageSizes.SMALLICON).get();
        }
        component.addPropertyChangeListener(this);
        putValue(Action.ACCELERATOR_KEY, getShortcutKey());
        updateState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        firePropertyChange("orientationAction", null, getValue("newState"));
    }

    /**
     * Updates the text and the icon.
     */
    public void updateState() {
        if (component.getComponentOrientation().isLeftToRight()) {
            putValue(Action.NAME, tr("Right to Left"));
            putValue(Action.SMALL_ICON, iconRTL);
            putValue(Action.SHORT_DESCRIPTION, tr("Switch the text orientation to Right-to-Left."));
            putValue("newState", ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            putValue(Action.NAME, tr("Left to Right"));
            putValue(Action.SMALL_ICON, iconLTR);
            putValue(Action.SHORT_DESCRIPTION, tr("Switch the text orientation to Left-to-Right."));
            putValue("newState", ComponentOrientation.LEFT_TO_RIGHT);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("componentOrientation".equals(evt.getPropertyName())) {
            updateState();
        }
    }

    /**
     * Returns the shortcut key to assign to this action.
     *
     * @return the shortcut key
     */
    public static KeyStroke getShortcutKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, PlatformManager.getPlatform().getMenuShortcutKeyMaskEx());
    }

    /**
     * Returns the default component orientation by the user's locale
     *
     * @return the default component orientation
     */
    public static ComponentOrientation getDefaultComponentOrientation() {
        Component main = MainApplication.getMainFrame();
        // is null while testing
        return main != null ? main.getComponentOrientation() : ComponentOrientation.LEFT_TO_RIGHT;
    }

    /**
     * Returns the text orientation of the value for the given key.
     *
     * This is intended for Preset Dialog comboboxes. The choices in the dropdown list are
     * typically translated. Ideally the user needs not see the English value.
     *
     * The algorithm is as follows:
     * <ul>
     * <li>If the key has an explicit language suffix, return the text orientation for that
     * language.
     * <li>Else return the text orientation of the user's locale.
     * </ul>
     *
     * You can configure which languages are RTL with the list property: {@code rtl-languages}.
     *
     * @param key the key
     * @return the text orientation of the value
     */
    public static ComponentOrientation getValueOrientation(String key) {
        if (key == null || key.isEmpty())
            return ComponentOrientation.LEFT_TO_RIGHT;

        // if the key has an explicit language suffix, use it
        Matcher m = LANG_PATTERN.matcher(key);
        if (m.find()) {
            if (RTLLanguages.contains(m.group(1))) {
                return ComponentOrientation.RIGHT_TO_LEFT;
            }
            return ComponentOrientation.LEFT_TO_RIGHT;
        }
        // return the user's locale
        return ComponentOrientation.getOrientation(Locale.getDefault());
    }

    /**
     * Returns the text orientation of the value for the given key.
     *
     * This expansion of {@link #getValueOrientation} is intended for Preset Dialog textfields and
     * for the Add Tag and Edit Tag dialog comboboxes.
     *
     * The algorithm is as follows:
     * <ul>
     * <li>If the key has an explicit language suffix, return the text orientation for that
     * language.
     * <li>If the key is usually localized, return the text orientation of the user's locale.
     * <li>Else return left to right.
     * </ul>
     *
     * You can configure which keys are localized with the list property: {@code localized-keys}.
     * You can configure which languages are RTL with the list property: {@code rtl-languages}.
     *
     * @param key the key
     * @return the text orientation of the value
     */
    public static ComponentOrientation getNamelikeOrientation(String key) {
        if (key == null || key.isEmpty())
            return ComponentOrientation.LEFT_TO_RIGHT;

        // if the key has an explicit language suffix, use it
        Matcher m = LANG_PATTERN.matcher(key);
        if (m.find()) {
            if (RTLLanguages.contains(m.group(1))) {
                return ComponentOrientation.RIGHT_TO_LEFT;
            }
            return ComponentOrientation.LEFT_TO_RIGHT;
        }
        // if the key is usually localized, use the user's locale
        m = localizedKeys.matcher(key);
        if (m.find()) {
            return ComponentOrientation.getOrientation(Locale.getDefault());
        }
        // all other keys are LTR
        return ComponentOrientation.LEFT_TO_RIGHT;
    }

    private static Pattern compile_localized_keys() {
        return Pattern.compile("^(" + String.join("|", LOCALIZED_KEYS.get()) + ")$");
    }
}
