// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Setting containing a {@link String} value.
 * @since 12881 (moved from package {@code org.openstreetmap.josm.data.preferences})
 */
public class StringSetting extends AbstractSetting<String> {
    /**
     * Constructs a new {@code StringSetting} with the given value
     * @param value The setting value
     */
    public StringSetting(String value) {
        super(value);
    }

    @Override
    public StringSetting copy() {
        return new StringSetting(value);
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public StringSetting getNullInstance() {
        return new StringSetting(null);
    }
}
