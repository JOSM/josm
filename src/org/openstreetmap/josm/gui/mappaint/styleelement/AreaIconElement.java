// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.PartiallyInsideAreaStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.PositionForAreaStrategy;
import org.openstreetmap.josm.tools.RotationAngle;

/**
 * This class defines how an icon is rendered onto the area.
 * @author Michael Zangl
 * @since 11730
 */
public class AreaIconElement extends StyleElement {
    /**
     * The icon that is displayed on the center of the area.
     */
    private final MapImage iconImage;

    /**
     * The rotation of the {@link #iconImageAngle}
     */
    private final RotationAngle iconImageAngle;

    /**
     * The position of the icon inside the area.
     */
    private final PositionForAreaStrategy iconPosition;

    protected AreaIconElement(Cascade c, MapImage iconImage, RotationAngle iconImageAngle, PositionForAreaStrategy iconPosition) {
        super(c, 4.8f);
        this.iconImage = Objects.requireNonNull(iconImage, "iconImage");
        this.iconImageAngle = Objects.requireNonNull(iconImageAngle, "iconImageAngle");
        this.iconPosition = Objects.requireNonNull(iconPosition, "iconPosition");
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        if (painter.isShowIcons()) {
            painter.drawAreaIcon(osm, iconImage, painter.isInactiveMode() || osm.isDisabled(), selected, member,
                    iconImageAngle.getRotationAngle(osm), iconPosition);
        }
    }

    /**
     * Create a new {@link AreaIconElement}
     * @param env The current style definitions
     * @return The area element or <code>null</code> if there is no icon.
     */
    public static AreaIconElement create(final Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);
        MapImage iconImage = NodeElement.createIcon(env);
        if (iconImage != null) {
            RotationAngle rotationAngle = NodeElement.createRotationAngle(env);
            Keyword positionKeyword = c.get(AreaElement.ICON_POSITION, null, Keyword.class);
            PositionForAreaStrategy position = PositionForAreaStrategy.forKeyword(positionKeyword, PartiallyInsideAreaStrategy.INSTANCE);

            return new AreaIconElement(c, iconImage, rotationAngle, position);
        } else {
            return null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((iconImage == null) ? 0 : iconImage.hashCode());
        result = prime * result + ((iconImageAngle == null) ? 0 : iconImageAngle.hashCode());
        result = prime * result + ((iconPosition == null) ? 0 : iconPosition.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AreaIconElement other = (AreaIconElement) obj;
        return Objects.equals(iconImage, other.iconImage) &&
                Objects.equals(iconImageAngle, other.iconImageAngle) &&
                Objects.equals(iconPosition, other.iconPosition);
    }

    @Override
    public String toString() {
        return "AreaIconElement{" + super.toString() + "iconImage=[" + iconImage + "] iconImageAngle=[" + iconImageAngle + "]}";
    }
}
