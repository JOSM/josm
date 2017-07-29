// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel.ProxyPolicy;

/**
 * This is the default proxy selector used in JOSM.
 *
 */
public class DefaultProxySelector extends ProxySelector {

    private static final List<Proxy> NO_PROXY_LIST = Collections.singletonList(Proxy.NO_PROXY);

    private static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String IPV6_LOOPBACK = "::1";

    /**
     * The {@link ProxySelector} provided by the JDK will retrieve proxy information
     * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
     * is defined <strong>at startup</strong>. It has no effect if the property is set
     * later by the application.
     *
     * We therefore read the property at class loading time and remember it's value.
     */
    private static boolean jvmWillUseSystemProxies;
    static {
        String v = System.getProperty("java.net.useSystemProxies");
        if (v != null && v.equals(Boolean.TRUE.toString())) {
            jvmWillUseSystemProxies = true;
        }
    }

    /**
     * The {@link ProxySelector} provided by the JDK will retrieve proxy information
     * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
     * is defined <strong>at startup</strong>. If the property is set later by the application,
     * this has no effect.
     *
     * @return true, if <tt>java.net.useSystemProxies</tt> was set to true at class initialization time
     *
     */
    public static boolean willJvmRetrieveSystemProxies() {
        return jvmWillUseSystemProxies;
    }

    private ProxyPolicy proxyPolicy;
    private InetSocketAddress httpProxySocketAddress;
    private InetSocketAddress socksProxySocketAddress;
    private final ProxySelector delegate;

    private final Set<String> errorResources = new HashSet<>();
    private final Set<String> errorMessages = new HashSet<>();
    private Set<String> proxyExceptions;

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
        } catch (NumberFormatException e) {
            Main.error(tr("Unexpected format for port number in preference ''{0}''. Got ''{1}''.", property, value));
            Main.error(tr("The proxy will not be used."));
            return 0;
        }
        if (port <= 0 || port > 65_535) {
            Main.error(tr("Illegal port number in preference ''{0}''. Got {1}.", property, port));
            Main.error(tr("The proxy will not be used."));
            return 0;
        }
        return port;
    }

    /**
     * Initializes the proxy selector from the setting in the preferences.
     *
     */
    public final void initFromPreferences() {
        String value = Main.pref.get(ProxyPreferencesPanel.PROXY_POLICY);
        if (value.isEmpty()) {
            proxyPolicy = ProxyPolicy.NO_PROXY;
        } else {
            proxyPolicy = ProxyPolicy.fromName(value);
            if (proxyPolicy == null) {
                Main.warn(tr("Unexpected value for preference ''{0}'' found. Got ''{1}''. Will use no proxy.",
                        ProxyPreferencesPanel.PROXY_POLICY, value));
                proxyPolicy = ProxyPolicy.NO_PROXY;
            }
        }
        String host = Main.pref.get(ProxyPreferencesPanel.PROXY_HTTP_HOST, null);
        int port = parseProxyPortValue(ProxyPreferencesPanel.PROXY_HTTP_PORT, Main.pref.get(ProxyPreferencesPanel.PROXY_HTTP_PORT, null));
        httpProxySocketAddress = null;
        if (proxyPolicy.equals(ProxyPolicy.USE_HTTP_PROXY)) {
            if (host != null && !host.trim().isEmpty() && port > 0) {
                httpProxySocketAddress = new InetSocketAddress(host, port);
            } else {
                Main.warn(tr("Unexpected parameters for HTTP proxy. Got host ''{0}'' and port ''{1}''.", host, port));
                Main.warn(tr("The proxy will not be used."));
            }
        }

        host = Main.pref.get(ProxyPreferencesPanel.PROXY_SOCKS_HOST, null);
        port = parseProxyPortValue(ProxyPreferencesPanel.PROXY_SOCKS_PORT, Main.pref.get(ProxyPreferencesPanel.PROXY_SOCKS_PORT, null));
        socksProxySocketAddress = null;
        if (proxyPolicy.equals(ProxyPolicy.USE_SOCKS_PROXY)) {
            if (host != null && !host.trim().isEmpty() && port > 0) {
                socksProxySocketAddress = new InetSocketAddress(host, port);
            } else {
                Main.warn(tr("Unexpected parameters for SOCKS proxy. Got host ''{0}'' and port ''{1}''.", host, port));
                Main.warn(tr("The proxy will not be used."));
            }
        }
        proxyExceptions = new HashSet<>(
            Main.pref.getCollection(ProxyPreferencesPanel.PROXY_EXCEPTIONS,
                    Arrays.asList("localhost", IPV4_LOOPBACK, IPV6_LOOPBACK))
        );
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Just log something. The network stack will also throw an exception which will be caught somewhere else
        Main.error(tr("Connection to proxy ''{0}'' for URI ''{1}'' failed. Exception was: {2}", sa.toString(), uri.toString(), ioe.toString()));
        // Remember errors to give a friendly user message asking to review proxy configuration
        errorResources.add(uri.toString());
        errorMessages.add(ioe.toString());
    }

    /**
     * Returns the set of current proxy resources that failed to be retrieved.
     * @return the set of current proxy resources that failed to be retrieved
     * @since 6523
     */
    public final Set<String> getErrorResources() {
        return new TreeSet<>(errorResources);
    }

    /**
     * Returns the set of current proxy error messages.
     * @return the set of current proxy error messages
     * @since 6523
     */
    public final Set<String> getErrorMessages() {
        return new TreeSet<>(errorMessages);
    }

    /**
     * Clear the sets of failed resources and error messages.
     * @since 6523
     */
    public final void clearErrors() {
        errorResources.clear();
        errorMessages.clear();
    }

    /**
     * Determines if proxy errors have occured.
     * @return {@code true} if errors have occured, {@code false} otherwise.
     * @since 6523
     */
    public final boolean hasErrors() {
        return !errorResources.isEmpty();
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (uri != null && proxyExceptions.contains(uri.getHost())) {
            return NO_PROXY_LIST;
        }
        switch(proxyPolicy) {
        case USE_SYSTEM_SETTINGS:
            if (!jvmWillUseSystemProxies) {
                Main.warn(tr("The JVM is not configured to lookup proxies from the system settings. "+
                        "The property ''java.net.useSystemProxies'' was missing at startup time.  Will not use a proxy."));
                return NO_PROXY_LIST;
            }
            // delegate to the former proxy selector
            return delegate.select(uri);
        case NO_PROXY:
            return NO_PROXY_LIST;
        case USE_HTTP_PROXY:
            if (httpProxySocketAddress == null)
                return NO_PROXY_LIST;
            return Collections.singletonList(new Proxy(Type.HTTP, httpProxySocketAddress));
        case USE_SOCKS_PROXY:
            if (socksProxySocketAddress == null)
                return NO_PROXY_LIST;
            return Collections.singletonList(new Proxy(Type.SOCKS, socksProxySocketAddress));
        }
        // should not happen
        return null;
    }
}
