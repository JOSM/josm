// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.openstreetmap.josm.tools.HttpClient;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.AbstractOAuthProvider;

/**
 * Adapters to make {@code oauth.signpost} work with {@link HttpClient}.
 */
public final class SignpostAdapters {

    private SignpostAdapters() {
        // Hide constructor for utility classes
    }

    /**
     * OAuth provider.
     */
    public static class OAuthProvider extends AbstractOAuthProvider {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new {@code OAuthProvider}.
         * @param requestTokenEndpointUrl request token endpoint URL
         * @param accessTokenEndpointUrl access token endpoint URL
         * @param authorizationWebsiteUrl authorization website URL
         */
        public OAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl, String authorizationWebsiteUrl) {
            super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
        }

        @Override
        protected HttpRequest createRequest(String endpointUrl) throws Exception {
            return new HttpRequest(HttpClient.create(new URL(endpointUrl)));
        }

        @Override
        protected HttpResponse sendRequest(oauth.signpost.http.HttpRequest request) throws Exception {
            return new HttpResponse(((HttpRequest) request).request.connect());
        }

        @Override
        protected void closeConnection(oauth.signpost.http.HttpRequest request, oauth.signpost.http.HttpResponse response) throws Exception {
            if (response != null) {
                ((HttpResponse) response).response.disconnect();
            } else if (request != null) {
                ((HttpRequest) request).request.disconnect();
            }
        }
    }

    /**
     * OAuth consumer.
     */
    public static class OAuthConsumer extends AbstractOAuthConsumer {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new {@code OAuthConsumer}.
         * @param consumerKey consumer key
         * @param consumerSecret consumer secret
         */
        public OAuthConsumer(String consumerKey, String consumerSecret) {
            super(consumerKey, consumerSecret);
        }

        @Override
        protected HttpRequest wrap(Object request) {
            return new HttpRequest((HttpClient) request);
        }
    }

    static final class HttpRequest implements oauth.signpost.http.HttpRequest {
        final HttpClient request;

        HttpRequest(HttpClient request) {
            this.request = request;
        }

        @Override
        public void setHeader(String name, String value) {
            request.setHeader(name, value);
        }

        @Override
        public String getMethod() {
            return request.getRequestMethod();
        }

        @Override
        public String getRequestUrl() {
            return request.getURL().toExternalForm();
        }

        @Override
        public String getContentType() {
            return request.getRequestHeader("Content-Type");
        }

        @Override
        public String getHeader(String name) {
            return request.getRequestHeader(name);
        }

        @Override
        public InputStream getMessagePayload() {
            return null;
        }

        @Override
        public void setRequestUrl(String url) {
            throw new IllegalStateException();
        }

        @Override
        public Map<String, String> getAllHeaders() {
            throw new IllegalStateException();
        }

        @Override
        public Object unwrap() {
            throw new IllegalStateException();
        }
    }

    static final class HttpResponse implements oauth.signpost.http.HttpResponse {
        final HttpClient.Response response;

        HttpResponse(HttpClient.Response response) {
            this.response = response;
        }

        @Override
        public int getStatusCode() {
            return response.getResponseCode();
        }

        @Override
        public String getReasonPhrase() {
            return response.getResponseMessage();
        }

        @Override
        public InputStream getContent() throws IOException {
            return response.getContent();
        }

        @Override
        public Object unwrap() {
            throw new IllegalStateException();
        }
    }
}
