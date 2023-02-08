// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

/**
 * The OAuth versions ordered oldest to newest
 * @author Taylor Smock
 * @since 18650
 */
public enum OAuthVersion {
    /** <a href="https://oauth.net/core/1.0a/">OAuth 1.0a</a> */
    OAuth10a,
    /** <a href="https://datatracker.ietf.org/doc/html/rfc6749">OAuth 2.0</a> */
    OAuth20,
    /** <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-06">OAuth 2.1 (draft)</a> */
    OAuth21
}
