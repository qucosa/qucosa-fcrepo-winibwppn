/*
 * Copyright 2017 Saxon State and University Library Dresden (SLUB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qucosa.winibwppn;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.net.URI;

class FedoraConnectionParameters {

    static final String PARAM_FEDORA_CONTENT_URL = "fedora.content.url";
    static final String PARAM_FEDORA_CREDENTIALS = "fedora.credentials";
    static final String PARAM_FEDORA_HOST_URL = "fedora.host.url";

    private final Credentials credentials;
    private final String fedoraContentUrl;
    private final String fedoraHostUrl;

    private FedoraConnectionParameters(String fedoraHostUrl, String fedoraContentUrl, Credentials credentials) {
        this.credentials = credentials;
        this.fedoraContentUrl = fedoraContentUrl;
        this.fedoraHostUrl = fedoraHostUrl;
    }

    static FedoraConnectionParameters from(HttpServletRequest request, ServletContext context)
            throws AuthenticationException, MissingRequiredParameter {

        final String fedoraHostUrl = firstOf(
                System.getProperty(PARAM_FEDORA_HOST_URL),
                context.getInitParameter(PARAM_FEDORA_HOST_URL),
                extractFromRequestUrl(request));

        final String fedoraContentUrl = firstOf(
                context.getInitParameter(PARAM_FEDORA_CONTENT_URL),
                fedoraHostUrl);

        final Credentials credentials = Credentials.fromColonSeparatedString(
                assertCredentials(firstOf(
                        extractBasicAuthCredentials(request),
                        context.getInitParameter(PARAM_FEDORA_CREDENTIALS))));

        return new FedoraConnectionParameters(fedoraHostUrl, fedoraContentUrl, credentials);
    }

    String getFedoraHostUrl() {
        return fedoraHostUrl;
    }

    String getFedoraContentUrl() {
        return fedoraContentUrl;
    }

    Credentials getCredentials() {
        return credentials;
    }

    private static boolean isNullOrEmpty(String s) {
        return (s == null) || s.isEmpty();
    }

    private static String firstOf(String... strings) {
        for (String s : strings) {
            if (!orEmptyString(s).isEmpty()) return s;
        }
        return null;
    }

    private static String extractFromRequestUrl(HttpServletRequest request) {
        final StringBuffer requestURL = request.getRequestURL();
        if (requestURL != null) {
            final URI requestUri = URI.create(requestURL.toString());
            return String.format("%s://%s:%s/fedora",
                    requestUri.getScheme(),
                    requestUri.getHost(),
                    requestUri.getPort());
        } else {
            return null;
        }
    }

    private static String assertCredentials(String s) throws AuthenticationException {
        if (isNullOrEmpty(s)) throw new AuthenticationException("No credentials provided");
        return s;
    }

    private static String orEmptyString(String s) {
        return (s != null) ? s : "";
    }

    private static String extractBasicAuthCredentials(HttpServletRequest request) throws AuthenticationException {
        String credentials = "";
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic")) {
            try {
                final String base64credentials = authHeader.substring("Basic".length()).trim();
                credentials = new String(DatatypeConverter.parseBase64Binary(base64credentials));
            } catch (IllegalArgumentException e) {
                throw new AuthenticationException("Provided credentials are invalid.");
            }
        }
        return credentials;
    }

}
