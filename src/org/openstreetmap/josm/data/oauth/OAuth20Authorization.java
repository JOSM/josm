// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.openstreetmap.josm.io.remotecontrol.handler.AuthorizationHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Authorize the application
 * @since 18650
 */
public class OAuth20Authorization implements IOAuthAuthorization {
    /**
     * See <a href="https://www.rfc-editor.org/rfc/rfc7636">RFC7636</a>: PKCE
     * @param cryptographicallyRandomString A cryptographically secure string
     * @return The S256 bytes
     */
    private static String getPKCES256CodeChallenge(String cryptographicallyRandomString) {
        // S256: code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))
        try {
            byte[] encodedBytes = cryptographicallyRandomString.getBytes(StandardCharsets.US_ASCII);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new String(Base64.getUrlEncoder().encode(digest.digest(encodedBytes)), StandardCharsets.US_ASCII)
                    .replace("=", "").replace("+", "-").replace("/", "_");
        } catch (NoSuchAlgorithmException e) {
            throw new JosmRuntimeException(e);
        }
    }

    @Override
    public void authorize(IOAuthParameters parameters, Consumer<Optional<IOAuthToken>> consumer, Enum<?>... scopes) {
        final String state = UUID.randomUUID().toString();
        final String codeVerifier = UUID.randomUUID().toString(); // Cryptographically random string (ASCII)
        final String s256CodeChallenge = getPKCES256CodeChallenge(codeVerifier);

        // Enable authorization remote control
        new AuthorizationHandler().getPermissionPreference().put(true);
        String url = parameters.getAuthorizationUrl(state, scopes)
                + "&code_challenge_method=S256&code_challenge=" + s256CodeChallenge;
        AuthorizationHandler.addAuthorizationConsumer(state, new OAuth20AuthorizationHandler(state, codeVerifier, parameters, consumer));
        OpenBrowser.displayUrl(url);
    }

    private static class OAuth20AuthorizationHandler implements AuthorizationHandler.AuthorizationConsumer {

        private final String state;
        private final IOAuthParameters parameters;
        private final Consumer<Optional<IOAuthToken>> consumer;
        private final String codeVerifier;

        OAuth20AuthorizationHandler(String state, String codeVerifier, IOAuthParameters parameters, Consumer<Optional<IOAuthToken>> consumer) {
            this.state = state;
            this.parameters = parameters;
            this.consumer = consumer;
            this.codeVerifier = codeVerifier;
        }

        @Override
        public void validateRequest(String sender, String request, Map<String, String> args)
                throws RequestHandler.RequestHandlerBadRequestException {
            String argState = args.get("state");
            if (!Objects.equals(this.state, argState)) {
                throw new RequestHandler.RequestHandlerBadRequestException(
                        tr("Mismatched state: Expected {0} but got {1}", this.state, argState));
            }
        }

        @Override
        public AuthorizationHandler.ResponseRecord handleRequest(String sender, String request, Map<String, String> args)
                throws RequestHandler.RequestHandlerErrorException, RequestHandler.RequestHandlerBadRequestException {
            String code = args.get("code");
            try {
                HttpClient tradeCodeForToken = HttpClient.create(new URL(parameters.getAccessTokenUrl()), "POST");
                tradeCodeForToken.setRequestBody(("grant_type=authorization_code&client_id=" + parameters.getClientId()
                        + "&redirect_uri=" + parameters.getRedirectUri()
                        + "&code=" + code
                        + (this.codeVerifier != null ? "&code_verifier=" + this.codeVerifier : "")
                ).getBytes(StandardCharsets.UTF_8));
                tradeCodeForToken.setHeader("Content-Type", "application/x-www-form-urlencoded");
                try {
                    tradeCodeForToken.connect();
                    HttpClient.Response response = tradeCodeForToken.getResponse();
                    OAuth20Token oAuth20Token = new OAuth20Token(parameters, response.getContentReader());
                    consumer.accept(Optional.of(oAuth20Token));
                } catch (IOException | OAuth20Exception e) {
                    consumer.accept(Optional.empty());
                    throw new JosmRuntimeException(e);
                } finally {
                    tradeCodeForToken.disconnect();
                }
            } catch (MalformedURLException e) {
                throw new JosmRuntimeException(e);
            }
            return null;
        }
    }
}
