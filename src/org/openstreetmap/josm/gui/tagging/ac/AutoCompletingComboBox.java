// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;

/**
 * An auto-completing ComboBox.
 *
 * @author guilhem.bonnefille@gmail.com
 * @since 272
 * @deprecated Use the generic type {@link AutoCompComboBox} instead.  Eg.
 *             {@code AutoCompComboBox<AutoCompletionItem>} or {@code AutoCompComboBox<String>}.
 */
@Deprecated
public class AutoCompletingComboBox extends AutoCompComboBox<AutoCompletionItem> {
}
