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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FedoraConnectionParametersTest {

    private static final String FEDORA_HOST_URL = "http://local:8888/fedora";
    private static final String FEDORA_CONTENT_URL = "http://local:8888/content";
    private static final String FEDORA_CREDENTIALS = "fedoraUsr:fedoraPasswd";
    private ServletContext mockContext;
    private HttpServletRequest mockRequest;
    private Properties systemProperties;

    @Before
    public void backupSystemProperties() {
        systemProperties = System.getProperties();
    }

    @After
    public void restoreSystemProperties() {
        System.setProperties(systemProperties);
    }

    @Before
    public void setupMockServletContext() {
        mockContext = mock(ServletContext.class);
    }

    @Before
    public void setupMockRequest() {
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("pid")).thenReturn("test:1234");
    }

    @Test(expected = AuthenticationException.class)
    public void Creating_a_configuration_without_configured_credentials_throws_exception() throws Throwable {
        FedoraConnectionParameters.from(mockRequest, mockContext);
    }

    @Test(expected = AuthenticationException.class)
    public void Corrupted_base64_credentials_trigger_exception() throws Throwable {
        String invalidEncodedCredentials =
                "==0FFB8";
        when(mockRequest
                .getHeader("Authorization"))
                .thenReturn(invalidEncodedCredentials);

        FedoraConnectionParameters.from(mockRequest, mockContext);
    }

    @Test
    public void Basic_base64_credentials_can_be_given_via_authorization_request_header() throws Throwable {
        Credentials expected =
                Credentials.fromColonSeparatedString(FEDORA_CREDENTIALS);
        String encodedCredentials =
                "Basic " + expected.getAsBase64Encoded();
        when(mockRequest
                .getHeader("Authorization"))
                .thenReturn(encodedCredentials);

        FedoraConnectionParameters subject = FedoraConnectionParameters.from(mockRequest, mockContext);

        assertEquals(expected, subject.getCredentials());
    }

    @Test
    public void Fedora_content_URL_is_host_URL_if_not_explicitly_given() throws Throwable {
        System.setProperty(FedoraConnectionParameters.PARAM_FEDORA_HOST_URL, FEDORA_HOST_URL);
        when(mockContext
                .getInitParameter(FedoraConnectionParameters.PARAM_FEDORA_CREDENTIALS))
                .thenReturn(FEDORA_CREDENTIALS);

        FedoraConnectionParameters subject = FedoraConnectionParameters.from(mockRequest, mockContext);

        assertEquals(subject.getFedoraContentUrl(), subject.getFedoraHostUrl());
    }

    @Test
    public void Fedora_content_URL_can_be_explicitly_configured_via_context() throws Throwable {
        when(mockRequest.getRequestURL()).thenReturn(
                new StringBuffer(FEDORA_HOST_URL));
        when(mockContext
                .getInitParameter(FedoraConnectionParameters.PARAM_FEDORA_CREDENTIALS))
                .thenReturn(FEDORA_CREDENTIALS);
        when(mockContext
                .getInitParameter(FedoraConnectionParameters.PARAM_FEDORA_CONTENT_URL))
                .thenReturn(FEDORA_CONTENT_URL);

        FedoraConnectionParameters subject = FedoraConnectionParameters.from(mockRequest, mockContext);

        assertEquals(FEDORA_CONTENT_URL, subject.getFedoraContentUrl());
    }

    @Test
    public void Fedora_host_URL_gets_extracted_from_request() throws Throwable {
        when(mockRequest.getRequestURL()).thenReturn(
                new StringBuffer(FEDORA_HOST_URL));
        when(mockContext
                .getInitParameter(FedoraConnectionParameters.PARAM_FEDORA_CREDENTIALS))
                .thenReturn(FEDORA_CREDENTIALS);

        FedoraConnectionParameters subject = FedoraConnectionParameters.from(mockRequest, mockContext);

        assertEquals(FEDORA_HOST_URL, subject.getFedoraHostUrl());
    }

}
