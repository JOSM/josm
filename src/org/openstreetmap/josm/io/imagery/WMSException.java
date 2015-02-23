// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.openstreetmap.josm.tools.Utils;

/**
 * WMS Service Exception, as defined by {@code application/vnd.ogc.se_xml} format:<ul>
 * <li><a href="http://schemas.opengis.net/wms/1.1.0/exception_1_1_0.dtd">WMS 1.1.0 DTD</a></li>
 * <li><a href="http://schemas.opengis.net/wms/1.3.0/exception_1_3_0.dtd">WMS 1.3.0 XSD</a></li>
 * </ul>
 * @since 7425
 */
public class WMSException extends Exception {

    private final WMSRequest request;
    private final URL url;
    private final String[] exceptions;

    /**
     * Constructs a new {@code WMSException}.
     * @param request the WMS request that lead to this exception
     * @param url the URL that lead to this exception
     * @param exceptions the exceptions replied by WMS server
     */
    public WMSException(WMSRequest request, URL url, Collection<String> exceptions) {
        super(Utils.join("\n", exceptions));
        this.request = request;
        this.url = url;
        this.exceptions = exceptions.toArray(new String[0]);
    }

    /**
     * Replies the WMS request that lead to this exception.
     * @return the WMS request
     */
    public final WMSRequest getRequest() {
        return request;
    }

    /**
     * Replies the URL that lead to this exception.
     * @return the URL
     */
    public final URL getUrl() {
        return url;
    }

    /**
     * Replies the WMS Service exceptions.
     * @return the exceptions
     */
    public final Collection<String> getExceptions() {
        return Arrays.asList(exceptions);
    }
}
