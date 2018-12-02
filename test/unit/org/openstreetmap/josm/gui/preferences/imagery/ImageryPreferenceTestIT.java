// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link ImageryPreference} class.
 */
public class ImageryPreferenceTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().timeout(10000*60);

    private final Map<String, Map<ImageryInfo, List<String>>> errors = Collections.synchronizedMap(new TreeMap<>());
    private final Set<String> workingURLs = Collections.synchronizedSet(new HashSet<>());

    private boolean addError(ImageryInfo info, String error) {
        return errors.computeIfAbsent(info.getCountryCode(), x -> Collections.synchronizedMap(new TreeMap<>()))
                     .computeIfAbsent(info, x -> Collections.synchronizedList(new ArrayList<>()))
                     .add(error);
    }

    private void checkUrl(ImageryInfo info, String url) {
        if (url != null && !workingURLs.contains(url)) {
            try {
                Response response = HttpClient.create(new URL(url)).connect();
                if (response.getResponseCode() >= 400) {
                    addError(info, url + " -> HTTP " + response.getResponseCode());
                } else if (response.getResponseCode() >= 300) {
                    Logging.warn(url + " -> HTTP " + response.getResponseCode());
                } else {
                    workingURLs.add(url);
                }
                response.disconnect();
            } catch (IOException e) {
                addError(info, url + " -> " + e);
            }
        }
    }

    private void checkEntry(ImageryInfo info) {
        Logging.info("Checking "+ info);

        if (info.getAttributionImageRaw() != null && info.getAttributionImage() == null) {
            addError(info, "Can't fetch attribution image: " + info.getAttributionImageRaw());
        }

        checkUrl(info, info.getAttributionImageURL());
        checkUrl(info, info.getAttributionLinkURL());
        String eula = info.getEulaAcceptanceRequired();
        if (eula != null) {
            checkUrl(info, eula.replaceAll("\\{lang\\}", ""));
        }
        checkUrl(info, info.getPermissionReferenceURL());
        checkUrl(info, info.getTermsOfUseURL());

        for (ImageryInfo mirror : info.getMirrors()) {
            checkEntry(mirror);
        }
    }

    /**
     * Test that available imagery entries are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testValidityOfAvailableImageryEntries() throws Exception {
        ImageryLayerInfo.instance.load(false);
        ImageryLayerInfo.instance.getDefaultLayers().parallelStream().forEach(this::checkEntry);
        assertTrue(errors.toString().replaceAll("\\}, ", "\n\\}, ").replaceAll(", ImageryInfo\\{", "\n      ,ImageryInfo\\{"),
                errors.isEmpty());
    }
}
