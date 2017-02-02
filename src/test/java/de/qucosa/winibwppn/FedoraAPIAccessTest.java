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

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FedoraAPIAccessTest {

    private static final String THIS_VALUE_DOESNT_MATTER = "this-value-doesnt-matter";
    private static final String NO_CONTENT = null;
    private CloseableHttpClient mockHttpClient;
    private FedoraAPIAccess subject;

    @Before
    public void setupMockHttpClient() throws IOException {
        mockHttpClient = mock(CloseableHttpClient.class);
        subject = new FedoraAPIAccess("http://test.fedora:8080", mockHttpClient);
    }

    @Test(expected = IOException.class)
    public void Calling_getDatastreamDissemination_throws_exception_if_response_is_not_200OK() throws IOException {
        CloseableHttpResponse mockReponse = httpResponse(NO_CONTENT, statusLine(SC_NOT_FOUND, "Not found"));
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockReponse);
        subject.getDatastreamDissemination(THIS_VALUE_DOESNT_MATTER, THIS_VALUE_DOESNT_MATTER);
    }

    @Test
    public void Calling_getDatastreamDissemination_issues_GET_request() throws IOException {
        CloseableHttpResponse mockResponse = httpResponse("some-arbitrary-response-value", statusLine(SC_OK, "All is well"));
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        subject.getDatastreamDissemination(THIS_VALUE_DOESNT_MATTER, THIS_VALUE_DOESNT_MATTER);
        verify(mockHttpClient, atLeastOnce()).execute(any(HttpGet.class));
    }

    @Test
    public void Calling_modifyDatastream_issues_PUT_request_containing_new_datastream_content() throws IOException {
        String pid = "test:1234";
        String dsid = "DATA";
        InputStream content = new ByteArrayInputStream("content".getBytes());

        CloseableHttpResponse mockResponse = httpResponse(null, statusLine(SC_OK, "All is well"));
        when(mockHttpClient.execute(any(HttpPut.class))).thenReturn(mockResponse);
        subject.modifyDatastream(pid, dsid, null, content);

        verify(mockHttpClient, atLeastOnce()).execute(any(HttpPut.class));
    }

    @Test
    public void Calling_modifyDatastream_with_versionable_true_adds_versionable_parameter_to_request_URL() throws IOException {
        String pid = "test:1234";
        String dsid = "DATA";
        InputStream content = new ByteArrayInputStream("content".getBytes());

        Boolean versionable = Boolean.TRUE;
        CloseableHttpResponse mockResponse = httpResponse(null, statusLine(SC_OK, "All is well"));
        when(mockHttpClient.execute(any(HttpPut.class))).thenReturn(mockResponse);
        subject.modifyDatastream(pid, dsid, versionable, content);

        ArgumentCaptor<HttpPut> argument = ArgumentCaptor.forClass(HttpPut.class);
        verify(mockHttpClient).execute(argument.capture());

        HttpPut httpPut = argument.getValue();
        assertTrue(httpPut.getURI().getQuery().contains("versionable=" + versionable));
    }

    private CloseableHttpResponse httpResponse(String content, StatusLine statusLine) throws IOException {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        if (statusLine != null) when(mockResponse.getStatusLine()).thenReturn(statusLine);
        if (content != null) {
            HttpEntity mockEntity = mock(HttpEntity.class);
            when(mockEntity.getContent()).thenReturn(stringToStream(content));
            when(mockResponse.getEntity()).thenReturn(mockEntity);
        }
        return mockResponse;
    }

    private InputStream stringToStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private String findObjectsResponseXml(String sessionToken, String[] pids) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<result xmlns=\"http://www.fedora.info/definitions/1/0/types/\" ")
                .append("xmlns:types=\"http://www.fedora.info/definitions/1/0/types/\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/types/ http://localhost:8080/fedora/schema/findObjects.xsd\">");

        if (sessionToken != null) {
            stringBuilder.append("<listSession>");
            stringBuilder.append("<token>")
                    .append(sessionToken)
                    .append("</token>");
            stringBuilder.append("</listSession>");
        }

        if (pids.length > 0) {
            stringBuilder.append("<resultList>");
            for (String pid : pids) {
                stringBuilder
                        .append("<objectFields>")
                        .append("<pid>")
                        .append(pid)
                        .append("</pid>")
                        .append("</objectFields>");
            }
            stringBuilder.append("</resultList>");
        } else {
            stringBuilder.append("  <resultList/>\n");

        }

        stringBuilder.append("</result>");
        return stringBuilder.toString();
    }

    private static StatusLine statusLine(final int status, final String reasonPhrase) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("HTTP", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return status;
            }

            @Override
            public String getReasonPhrase() {
                return reasonPhrase;
            }
        };
    }

}
