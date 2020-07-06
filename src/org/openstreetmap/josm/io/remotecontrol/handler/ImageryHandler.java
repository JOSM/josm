// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;

import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Adds an imagery (WMS/TMS) layer. For instance, {@code /imagery?title=...&type=...&url=...}.
 * @since 3715
 */
public class ImageryHandler extends RequestHandler.RawURLParseRequestHandler {

    /**
     * The remote control command name used to add an imagery layer.
     */
    public static final String command = "imagery";

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load an imagery layer from the following URL:")
                + "<br>" + args.get("url");
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"url"};
    }

    @Override
    public String[] getOptionalParams() {
        return StructUtils.serializeStruct(new ImageryPreferenceEntry(), ImageryPreferenceEntry.class,
                StructUtils.SerializeOptions.INCLUDE_NULL, StructUtils.SerializeOptions.INCLUDE_DEFAULT
        ).keySet().toArray(new String[0]);
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_IMAGERY;
    }

    protected ImageryInfo buildImageryInfo() {
        args.computeIfAbsent("type", ignore -> ImageryType.WMS.getDefault().getTypeString());
        args.computeIfAbsent("name", ignore -> args.getOrDefault("title", tr("Remote imagery")));
        ImageryPreferenceEntry imageryPreferenceEntry = StructUtils.deserializeStruct(args, ImageryPreferenceEntry.class);
        return new ImageryInfo(imageryPreferenceEntry);
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        final ImageryInfo imgInfo = buildImageryInfo();
        if (MainApplication.isDisplayingMapView()) {
            for (ImageryLayer layer : MainApplication.getLayerManager().getLayersOfType(ImageryLayer.class)) {
                if (layer.getInfo().equals(imgInfo)) {
                    Logging.info("Imagery layer already exists: "+imgInfo);
                    return;
                }
            }
        }
        GuiHelper.runInEDT(() -> {
            try {
                MainApplication.getLayerManager().addLayer(ImageryLayer.create(imgInfo));
            } catch (IllegalArgumentException e) {
                Logging.log(Logging.LEVEL_ERROR, e);
            }
        });
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        try {
            CheckParameterUtil.ensureParameterNotNull(args);
            CheckParameterUtil.ensureParameterNotNull(args.get("url"));
            ImageryLayer.create(buildImageryInfo());
        } catch (IllegalArgumentException e) {
            throw new RequestHandlerBadRequestException(e.getMessage(), e);
        }
    }

    @Override
    public String getUsage() {
        return "adds an imagery layer (e.g. WMS, TMS)";
    }

    @Override
    public String[] getUsageExamples() {
        final String types = String.join("|", Utils.transform(Arrays.asList(ImageryInfo.ImageryType.values()),
                ImageryType::getTypeString));
        return new String[] {
            "/imagery?title=osm&type=tms&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png",
            "/imagery?title=landsat&type=wms&url=http://irs.gis-lab.info/?" +
                    "layers=landsat&SRS=%7Bproj%7D&WIDTH=%7Bwidth%7D&HEIGHT=%7Bheight%7D&BBOX=%7Bbbox%7D",
            "/imagery?title=...&type={"+types+"}&url=....[&cookies=...][&min_zoom=...][&max_zoom=...]"
            };
    }
}
