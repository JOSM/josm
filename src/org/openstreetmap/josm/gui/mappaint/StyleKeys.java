// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

/**
 * Interface defining string constants (MapCSS property keys).
 *
 * For the implementation of the <code>@supports</code> feature, the list of
 * supported keys is loaded from this interface using reflection.
 * @see org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource#evalSupportsDeclCondition(java.lang.String, java.lang.Object)
 */
public interface StyleKeys {

    /**
     * MapCSS color property key
     */
    String COLOR = "color";
    /**
     * MapCSS dashes property key
     */
    String DASHES = "dashes";
    /**
     * MapCSS dashes-background-color property key
     */
    String DASHES_BACKGROUND_COLOR = "dashes-background-color";
    /**
     * MapCSS dashes-background-opacity property key
     */
    String DASHES_BACKGROUND_OPACITY = "dashes-background-opacity";
    /**
     * MapCSS dashes-offset property key
     */
    String DASHES_OFFSET = "dashes-offset";
    /**
     * MapCSS fill-color property key
     */
    String FILL_COLOR = "fill-color";
    /**
     * MapCSS fill-extent property key
     */
    String FILL_EXTENT = "fill-extent";
    /**
     * MapCSS fill-extent-threshold property key
     */
    String FILL_EXTENT_THRESHOLD = "fill-extent-threshold";
    /**
     * MapCSS fill-image property key
     */
    String FILL_IMAGE = "fill-image";
    /**
     * MapCSS fill-opacity property key
     */
    String FILL_OPACITY = "fill-opacity";
    /**
     * MapCSS font-family property key
     */
    String FONT_FAMILY = "font-family";
    /**
     * MapCSS font-size property key
     */
    String FONT_SIZE = "font-size";
    /**
     * MapCSS font-style property key
     */
    String FONT_STYLE = "font-style";
    /**
     * MapCSS font-weight property key
     */
    String FONT_WEIGHT = "font-weight";
    /**
     * MapCSS icon-image property key
     */
    String ICON_IMAGE = "icon-image";
    /**
     * MapCSS icon-height property key
     */
    String ICON_HEIGHT = "icon-height";
    /**
     * MapCSS icon-offset-x property key
     */
    String ICON_OFFSET_X = "icon-offset-x";
    /**
     * MapCSS icon-offset-y property key
     */
    String ICON_OFFSET_Y = "icon-offset-y";
    /**
     * MapCSS icon-opacity property key
     */
    String ICON_OPACITY = "icon-opacity";
    /**
     * MapCSS icon-rotation property key
     */
    String ICON_ROTATION = "icon-rotation";
    /**
     * MapCSS text-rotation property key
     */
    String TEXT_ROTATION = "text-rotation";
    /**
     * MapCSS icon-width property key
     */
    String ICON_WIDTH = "icon-width";
    /**
     * Position of icons on area.
     */
    String ICON_POSITION = "icon-position";
    /**
     * MapCSS linecap property key
     */
    String LINECAP = "linecap";
    /**
     * MapCSS linejoin property key
     */
    String LINEJOIN = "linejoin";
    /**
     * MapCSS major-z-index property key
     */
    String MAJOR_Z_INDEX = "major-z-index";
    /**
     * MapCSS miterlimit property key
     */
    String MITERLIMIT = "miterlimit";
    /**
     * MapCSS modifier property key
     */
    String MODIFIER = "modifier";
    /**
     * MapCSS object-z-index property key
     */
    String OBJECT_Z_INDEX = "object-z-index";
    /**
     * MapCSS offset property key
     */
    String OFFSET = "offset";
    /**
     * MapCSS opacity property key
     */
    String OPACITY = "opacity";
    /**
     * MapCSS real-width property key
     */
    String REAL_WIDTH = "real-width";
    /**
     * MapCSS repeat-image property key
     */
    String REPEAT_IMAGE = "repeat-image";
    /**
     * MapCSS repeat-image-align property key
     */
    String REPEAT_IMAGE_ALIGN = "repeat-image-align";
    /**
     * MapCSS repeat-image-height property key
     */
    String REPEAT_IMAGE_HEIGHT = "repeat-image-height";
    /**
     * MapCSS repeat-image-offset property key
     */
    String REPEAT_IMAGE_OFFSET = "repeat-image-offset";
    /**
     * MapCSS repeat-image-opacity property key
     */
    String REPEAT_IMAGE_OPACITY = "repeat-image-opacity";
    /**
     * MapCSS repeat-image-phase property key
     */
    String REPEAT_IMAGE_PHASE = "repeat-image-phase";
    /**
     * MapCSS repeat-image-spacing property key
     */
    String REPEAT_IMAGE_SPACING = "repeat-image-spacing";
    /**
     * MapCSS repeat-image-width property key
     */
    String REPEAT_IMAGE_WIDTH = "repeat-image-width";
    /**
     * MapCSS text property key
     */
    String TEXT = "text";
    /**
     * MapCSS text-anchor-horizontal property key
     */
    String TEXT_ANCHOR_HORIZONTAL = "text-anchor-horizontal";
    /**
     * MapCSS text-anchor-vertical property key
     */
    String TEXT_ANCHOR_VERTICAL = "text-anchor-vertical";
    /**
     * MapCSS text-color property key
     */
    String TEXT_COLOR = "text-color";
    /**
     * MapCSS text-halo-color property key
     */
    String TEXT_HALO_COLOR = "text-halo-color";
    /**
     * MapCSS text-halo-opacity property key
     */
    String TEXT_HALO_OPACITY = "text-halo-opacity";
    /**
     * MapCSS text-halo-radius property key
     */
    String TEXT_HALO_RADIUS = "text-halo-radius";
    /**
     * MapCSS text-offset property key
     */
    String TEXT_OFFSET = "text-offset";
    /**
     * MapCSS text-offset-x property key
     */
    String TEXT_OFFSET_X = "text-offset-x";
    /**
     * MapCSS text-offset-y property key
     */
    String TEXT_OFFSET_Y = "text-offset-y";
    /**
     * MapCSS text-opacity property key
     */
    String TEXT_OPACITY = "text-opacity";
    /**
     * MapCSS text-position property key
     */
    String TEXT_POSITION = "text-position";
    /**
     * MapCSS way-direction-arrows property key
     */
    String WAY_DIRECTION_ARROWS = "way-direction-arrows";
    /**
     * MapCSS width property key
     */
    String WIDTH = "width";
    /**
     * MapCSS z-index property key
     */
    String Z_INDEX = "z-index";

}
