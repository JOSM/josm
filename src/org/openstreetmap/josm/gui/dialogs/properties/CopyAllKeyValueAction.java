// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Copy the key and value of all the tags to clipboard.
 * @since 13521
 */
public class CopyAllKeyValueAction extends AbstractCopyAction {

    /**
     * Constructs a new {@code CopyAllKeyValueAction}.
     * @param tagTable the tag table
     * @param keyFn a function which returns the selected key for a given row index
     * @param objectSp a supplier which returns the selected tagged object(s)
     */
    public CopyAllKeyValueAction(JTable tagTable, IntFunction<String> keyFn, Supplier<Collection<? extends Tagged>> objectSp) {
        super(tagTable, keyFn, objectSp);
        putValue(NAME, tr("Copy all Keys/Values"));
        putValue(SHORT_DESCRIPTION, tr("Copy the key and value of all the tags to clipboard"));
        new ImageProvider("copy").getResource().attachImageIcon(this, true);
    }

    /**
     * Registers this action shortcut
     * @return this instance, for easy chaining
     */
    CopyAllKeyValueAction registerShortcut() {
        Shortcut sc = Shortcut.registerShortcut("system:copytags", tr("Edit: {0}", tr("Copy Tags")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        MainApplication.registerActionShortcut(this, sc);
        sc.setAccelerator(this);
        return this;
    }

    @Override
    protected Collection<String> getString(Tagged p, String key) {
        return p.getKeys().entrySet().stream()
                .map(kv -> new Tag(kv.getKey(), kv.getValue()).toString())
                .collect(Collectors.toList());
    }
}
