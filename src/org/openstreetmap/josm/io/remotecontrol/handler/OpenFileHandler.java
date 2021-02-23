// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.gui.io.importexport.Options;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Opens a local file
 */
public class OpenFileHandler extends RequestHandler {

    /**
     * The remote control command name used to open a local file.
     */
    public static final String command = "open_file";

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"filename"};
    }

    @Override
    public String getUsage() {
        return "opens a local file in JOSM";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/open_file?filename=/tmp/test.osm"};
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.OPEN_FILES;
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        EnumSet<Options> options = EnumSet.noneOf(Options.class);
        if (PermissionPrefWithDefault.ALLOW_WEB_RESOURCES.isAllowed()) {
            options.add(Options.ALLOW_WEB_RESOURCES);
        }
        GuiHelper.runInEDTAndWait(() ->
            OpenFileAction.openFiles(Arrays.asList(new File(args.get("filename"))), options.toArray(new Options[0])));
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to open a local file.");
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        // Nothing to do
    }
}
