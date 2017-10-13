// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.tools.Logging;

/**
 * Partial implementation of the {@link CredentialsAgent} interface.
 * <p>
 * Provides a memory cache for the credentials and means to query the information from the user.
 * @since 4246
 */
public abstract class AbstractCredentialsAgent implements CredentialsAgent {

    /**
     * Synchronous credentials provider. Called if no credentials are cached. Can be used for user login prompt.
     * @since 12821
     */
    @FunctionalInterface
    public interface CredentialsProvider {
        /**
         * Fills the given response with appropriate user credentials.
         * @param requestorType type of the entity requesting authentication
         * @param agent the credentials agent requesting credentials
         * @param response authentication response to fill
         * @param username the known username, if any. Likely to be empty
         * @param password the known password, if any. Likely to be empty
         * @param host the host against authentication will be performed
         */
        void provideCredentials(RequestorType requestorType, AbstractCredentialsAgent agent, CredentialsAgentResponse response,
                String username, String password, String host);
    }

    private static volatile CredentialsProvider credentialsProvider =
            (a, b, c, d, e, f) -> Logging.error("Credentials provider has not been set");

    /**
     * Sets the global credentials provider.
     * @param provider credentials provider. Called if no credentials are cached. Can be used for user login prompt
     */
    public static void setCredentialsProvider(CredentialsProvider provider) {
        credentialsProvider = Objects.requireNonNull(provider, "provider");
    }

    protected Map<RequestorType, PasswordAuthentication> memoryCredentialsCache = new EnumMap<>(RequestorType.class);

    @Override
    public CredentialsAgentResponse getCredentials(final RequestorType requestorType, final String host, boolean noSuccessWithLastResponse)
            throws CredentialsAgentException {
        if (requestorType == null)
            return null;
        PasswordAuthentication credentials = lookup(requestorType, host);
        final String username = (credentials == null || credentials.getUserName() == null) ? "" : credentials.getUserName();
        final String password = (credentials == null || credentials.getPassword() == null) ? "" : String.valueOf(credentials.getPassword());

        final CredentialsAgentResponse response = new CredentialsAgentResponse();

        /*
         * Last request was successful and there was no credentials stored in file (or only the username is stored).
         * -> Try to recall credentials that have been entered manually in this session.
         */
        if (!noSuccessWithLastResponse && memoryCredentialsCache.containsKey(requestorType) &&
                (credentials == null || credentials.getPassword() == null || credentials.getPassword().length == 0)) {
            PasswordAuthentication pa = memoryCredentialsCache.get(requestorType);
            response.setUsername(pa.getUserName());
            response.setPassword(pa.getPassword());
            response.setCanceled(false);
        /*
         * Prompt the user for credentials. This happens the first time each
         * josm start if the user does not save the credentials to preference
         * file (username=="") and each time after authentication failed
         * (noSuccessWithLastResponse == true).
         */
        } else if (noSuccessWithLastResponse || username.isEmpty() || password.isEmpty()) {
            credentialsProvider.provideCredentials(requestorType, this, response, username, password, host);
            if (response.isCanceled() || response.getUsername() == null || response.getPassword() == null) {
                return response;
            }
            if (response.isSaveCredentials()) {
                store(requestorType, host, new PasswordAuthentication(
                        response.getUsername(),
                        response.getPassword()
                ));
            } else {
                // User decides not to save credentials to file. Keep it in memory so we don't have to ask over and over again.
                memoryCredentialsCache.put(requestorType, new PasswordAuthentication(response.getUsername(), response.getPassword()));
            }
        } else {
            // We got it from file.
            response.setUsername(username);
            response.setPassword(password.toCharArray());
            response.setCanceled(false);
        }
        return response;
    }

    @Override
    public final void purgeCredentialsCache(RequestorType requestorType) {
        memoryCredentialsCache.remove(requestorType);
    }

    /**
     * Provide the text for a checkbox that offers to save the
     * username and password that has been entered by the user.
     * @return checkbox text
     */
    public abstract String getSaveUsernameAndPasswordCheckboxText();
}
