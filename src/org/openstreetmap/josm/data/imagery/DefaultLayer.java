// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;

/**
 *
 * Simple class representing default layer that might be set in imagery information
 *
 * This simple class is needed - as for WMS there is different information needed to specify layer than for WMTS
 *
 * @author Wiktor Niesiobedzki
 *
 */
public class DefaultLayer {
    private final String layerName;
    private final String tileMatrixSet;
    private final String style;

    /**
     * Constructor
     * @param imageryType for which this layer is defined
     * @param layerName as returned by getIdentifier for WMTS and getName for WMS
     * @param style of the layer
     * @param tileMatrixSet only for WMTS - tileMatrixSet to use
     */
    public DefaultLayer(ImageryType imageryType, String layerName, String style, String tileMatrixSet) {
        this.layerName = layerName == null ? "" : layerName;
        this.style = style == null ? "" : style;
        if (imageryType != ImageryType.WMTS && !(tileMatrixSet == null || "".equals(tileMatrixSet))) {
            throw new IllegalArgumentException(tr("{0} imagery has tileMatrixSet defined to: {1}", imageryType, tileMatrixSet));
        }
        this.tileMatrixSet = tileMatrixSet == null ? "" : tileMatrixSet;
    }

    /**
     * Returns layer name of the default layer.
     * @return layer name of the default layer
     */
    public String getLayerName() {
        return layerName;
    }

    /**
     * Returns default tileMatrixSet. Only usable for WMTS
     * @return default tileMatrixSet. Only usable for WMTS
     */
    public String getTileMatrixSet() {
        return tileMatrixSet;
    }

    /**
     * Returns style for this WMS / WMTS layer to use.
     * @return style for this WMS / WMTS layer to use
     */
    public String getStyle() {
        return style;
    }

    /**
     * Returns JSON representation of the default layer object.
     * @return JSON representation of the default layer object
     */
    public JsonObject toJson() {
        JsonObjectBuilder ret = Json.createObjectBuilder();
        ret.add("layerName", layerName);
        ret.add("style", style);
        ret.add("tileMatrixSet", tileMatrixSet);
        return ret.build();
    }

    /**
     * Factory method creating DefaultLayer from JSON objects
     * @param o serialized DefaultLayer object
     * @param type of ImageryType serialized
     * @return DefaultLayer instance based on JSON object
     */
    public static DefaultLayer fromJson(JsonObject o, ImageryType type) {
        return new DefaultLayer(type, o.getString("layerName"), o.getString("style"), o.getString("tileMatrixSet"));
    }
}
