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

import de.slub.urn.URN;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

class FedoraAPIAccess {

    private static final String FEDORA_DATASTREAM_DISSEMINATION_URI_PATTERN =
            "%s/objects/%s/datastreams/%s/content";
    private static final String FEDORA_DATASTREAM_MODIFICATION_URI_PATTERN =
            "%s/objects/%s/datastreams/%s";
    private static final String FEDORA_PID_QUERY_URI_PATTERN =
            "%s/risearch?type=triples&lang=spo&format=N-Triples&distinct=on";
    private static final String END_OF_INPUT = "\\Z";

    private final String hostUrl;
    private final HttpClient httpClient;

    FedoraAPIAccess(String hostUrl, HttpClient httpClient) {
        this.hostUrl = hostUrl;
        this.httpClient = httpClient;
    }

    String resolveIdentifier(URN urn) throws CannotResolveIdentifier {
        HttpResponse response = null;

        List<NameValuePair> formEncodedQuery = new ArrayList<>();
        formEncodedQuery.add(new BasicNameValuePair("query", String.format("?s <dc:identifier> '%s'\n", urn)));

        try {
            HttpPost post = new HttpPost(String.format(FEDORA_PID_QUERY_URI_PATTERN, hostUrl));
            post.setEntity(new UrlEncodedFormEntity(formEncodedQuery));

            response = httpClient.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String subjectLiteral = new Scanner(response.getEntity().getContent())
                        .useDelimiter("\\s")
                        .next();
                return subjectLiteral.substring("<info:fedora/".length(), subjectLiteral.indexOf(">"));
            } else {
                throw new CannotResolveIdentifier(String.format(
                        "Identifier %s could not be resolved to object PID. Server responded: %s", urn, response.getStatusLine()));
            }
        } catch (IOException | NoSuchElementException e) {
            throw new CannotResolveIdentifier(String.format("Error resolving identifier %s", urn), e);
        } finally {
            consumeResponseEntity(response);
        }
    }

    String getDatastreamDissemination(String pid, String dsid) throws IOException {
        HttpResponse response = null;
        try {
            HttpGet get = new HttpGet(String.format(
                    FEDORA_DATASTREAM_DISSEMINATION_URI_PATTERN, hostUrl, pid, dsid));

            response = httpClient.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return new Scanner(response.getEntity().getContent())
                        .useDelimiter(END_OF_INPUT)
                        .next();
            } else {
                throw new IOException(String.format(
                        "Cannot load datastream %s of object %s. Server responded: %s", dsid, pid,
                        response.getStatusLine()));
            }
        } catch (NoSuchElementException e) {
            throw new IOException(String.format("Error parsing datastream %s of object %s.", dsid, pid));
        } finally {
            consumeResponseEntity(response);
        }

    }

    void modifyDatastream(String pid, String dsid, InputStream content) throws IOException {
        modifyDatastream(pid, dsid, null, content);
    }

    void modifyDatastream(String pid, String dsid, Boolean versionable, InputStream content) throws IOException {
        HttpResponse response = null;
        try {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(content);
            // Entity needs to be buffered because Fedora might reply in a way
            // forces resubmitting the entity
            BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entity);

            URIBuilder uriBuilder = new URIBuilder(
                    String.format(FEDORA_DATASTREAM_MODIFICATION_URI_PATTERN, hostUrl, pid, dsid));
            if (versionable != null) {
                uriBuilder.addParameter("versionable", String.valueOf(versionable));
            }
            URI uri = uriBuilder.build();

            HttpPut put = new HttpPut(uri);
            put.setEntity(bufferedHttpEntity);
            response = httpClient.execute(put);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(String.format(
                        "Cannot modify datastream %s of object %s. Server responded: %s", dsid, pid,
                        response.getStatusLine()));
            }
        } catch (URISyntaxException e) {
            throw new IOException("Cannot ", e);
        } finally {
            consumeResponseEntity(response);
        }
    }

    private void consumeResponseEntity(HttpResponse response) {
        try {
            if (response != null) {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException ignored) {
        }
    }
}
