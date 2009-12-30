// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.ProxyPreferences;
import org.openstreetmap.josm.gui.preferences.ProxyPreferences.ProxyPolicy;

/**
 * This is the default proxy selector used in JOSM.
 * 
 */
public class DefaultProxySelector extends ProxySelector {
    static private final Logger logger = Logger.getLogger(DefaultProxySelector.class.getName());

    /**
     * The {@see ProxySelector} provided by the JDK will retrieve proxy information
     * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
     * is defined <strong>at startup</strong>. It has no effect if the property is set
     * later by the application.
     *
     * We therefore read the property at class loading time and remember it's value.
     */
    private static boolean JVM_WILL_USE_SYSTEM_PROXIES = false;
    {
        String v = System.getProperty("java.net.useSystemProxies");
        if (v != null && v.equals(Boolean.TRUE.toString())) {
            JVM_WILL_USE_SYSTEM_PROXIES = true;
        }
    }

    /**
     * The {@see ProxySelector} provided by the JDK will retrieve proxy information
     * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
     * is defined <strong>at startup</strong>. If the property is set later by the application,
     * this has no effect.
     * 
     * @return true, if <tt>java.net.useSystemProxies</tt> was set to true at class initialization time
     * 
     */
    public static boolean willJvmRetrieveSystemProxies() {
        return JVM_WILL_USE_SYSTEM_PROXIES;
    }

    private ProxyPolicy proxyPolicy;
    private InetSocketAddress httpProxySocketAddress;
    private InetSocketAddress socksProxySocketAddress;
    private ProxySelector delegate;

    /**
     * A typical example is:
     * <pre>
     *    PropertySelector delegate = PropertySelector.getDefault();
     *    PropertySelector.setDefault(new DefaultPropertySelector(delegate));
     * </pre>
     * 
     * @param delegate the proxy selector to delegate to if system settings are used. Usually
     * this is the proxy selector found by ProxySelector.getDefault() before this proxy
     * selector is installed
     */
    public DefaultProxySelector(ProxySelector delegate) {
        this.delegate = delegate;
        initFromPreferences();
    }

    protected int parseProxyPortValue(String property, String value) {
        if (value == null) return 0;
        int port = 0;
        try {
            port = Integer.parseInt(value);
        } catch(NumberFormatException e){
            System.err.println(tr("Unexpected format for port number in in preference ''{0}''. Got ''{1}''. Proxy won't be used.", property, value));
            return 0;
        }
        if (port <= 0 || port >  65535) {
            System.err.println(tr("Illegal port number in preference ''{0}''. Got {1}. Proxy won't be used.", property, port));
            return 0;
        }
        return port;
    }

    /**
     * Initializes the proxy selector from the setting in the preferences.
     * 
     */
    public void initFromPreferences() {
        String value = Main.pref.get(ProxyPreferences.PROXY_POLICY);
        if (value.length() == 0) {
            System.err.println(tr("Warning: no preference ''{0}'' found. Will use no proxy.", ProxyPreferences.PROXY_POLICY));
            proxyPolicy = ProxyPolicy.NO_PROXY;
        } else {
            proxyPolicy= ProxyPolicy.fromName(value);
            if (proxyPolicy == null) {
                System.err.println(tr("Warning: unexpected value for preference ''{0}'' found. Got ''{1}''. Will use no proxy.", ProxyPreferences.PROXY_POLICY, value));
                proxyPolicy = ProxyPolicy.NO_PROXY;
            }
        }
        String host = Main.pref.get(ProxyPreferences.PROXY_HTTP_HOST, null);
        int port = parseProxyPortValue(ProxyPreferences.PROXY_HTTP_PORT, Main.pref.get(ProxyPreferences.PROXY_HTTP_PORT, null));
        if (host != null && ! host.trim().equals("") && port > 0) {
            httpProxySocketAddress = new InetSocketAddress(host,port);
        } else {
            httpProxySocketAddress = null;
            if (proxyPolicy.equals(ProxyPolicy.USE_HTTP_PROXY)) {
                System.err.println(tr("Warning: Unexpected parameters for HTTP proxy. Got host ''{0}'' and port ''{1}''. Proxy won't be used", host, port));
            }
        }

        host = Main.pref.get(ProxyPreferences.PROXY_SOCKS_HOST, null);
        port = parseProxyPortValue(ProxyPreferences.PROXY_SOCKS_PORT, Main.pref.get(ProxyPreferences.PROXY_SOCKS_PORT, null));
        if (host != null && ! host.trim().equals("") && port > 0) {
            socksProxySocketAddress = new InetSocketAddress(host,port);
        } else {
            socksProxySocketAddress = null;
            if (proxyPolicy.equals(ProxyPolicy.USE_SOCKS_PROXY)) {
                System.err.println(tr("Warning: Unexpected parameters for SOCKS proxy. Got host ''{0}'' and port ''{1}''. Proxy won't be used", host, port));
            }
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Just log something. The network stack will also throw an exception which will be caught
        // somewhere else
        //
        System.out.println(tr("Error: Connection to proxy ''{0}'' for URI ''{1}'' failed. Exception was: {2}", sa.toString(), uri.toString(), ioe.toString()));
    }

    @Override
    public List<Proxy> select(URI uri) {
        Proxy proxy;
        switch(proxyPolicy) {
        case USE_SYSTEM_SETTINGS:
            if (!JVM_WILL_USE_SYSTEM_PROXIES) {
                System.err.println(tr("Warning: the JVM is not configured to lookup proxies from the system settings. The property ''java.net.useSystemProxies'' was missing at startup time. Won't use a proxy."));
                return Collections.singletonList(Proxy.NO_PROXY);
            }
            // delegate to the former proxy selector
            List<Proxy> ret = delegate.select(uri);
            return ret;
        case NO_PROXY:
            return Collections.singletonList(Proxy.NO_PROXY);
        case USE_HTTP_PROXY:
            if (httpProxySocketAddress == null)
                return Collections.singletonList(Proxy.NO_PROXY);
            proxy = new Proxy(Type.HTTP, httpProxySocketAddress);
            return Collections.singletonList(proxy);
        case USE_SOCKS_PROXY:
            if (socksProxySocketAddress == null)
                return Collections.singletonList(Proxy.NO_PROXY);
            proxy = new Proxy(Type.SOCKS, socksProxySocketAddress);
            return Collections.singletonList(proxy);
        }
        // should not happen
        return null;
    }
}
