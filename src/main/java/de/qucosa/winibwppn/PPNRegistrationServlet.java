/*
 * Copyright 2016 Saxon State and University Library Dresden (SLUB)
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
import de.slub.urn.URNSyntaxException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static de.qucosa.winibwppn.PPNRegistrationAgent.PPNCardinality.MULTIPLE;
import static de.qucosa.winibwppn.PPNRegistrationAgent.PPNCardinality.SINGLE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

public class PPNRegistrationServlet extends HttpServlet {

    final private Logger log = LoggerFactory.getLogger(PPNRegistrationServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
        log.info("PPN registration startup finished");
    }

    @Override
    public void destroy() {
        super.destroy();
        log.info("PPN registration shutdown complete");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CloseableHttpClient closableHttpClient = null;
        try {
            URN urn = URN.fromString(request.getParameter("urn"));
            PPN ppn = PPN.fromString(request.getParameter("ppn"));

            FedoraConnectionParameters params = FedoraConnectionParameters.from(request, getServletContext());
            String hostUrl = params.getFedoraHostUrl();
            String username = params.getCredentials().getUsername();
            String password = params.getCredentials().getPassword();

            log.debug(String.format("Connecting to %s as %s", hostUrl, username));

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

            closableHttpClient = HttpClientBuilder.create()
                    .setConnectionManager(new PoolingHttpClientConnectionManager())
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();

            FedoraAPIAccess fedoraAPIAccess = new FedoraAPIAccess(hostUrl, closableHttpClient);

            log.debug(String.format("Registering PPN %s for object with PID %s", urn, ppn));

            PPNRegistrationAgent.PPNCardinality cardinality = SINGLE;
            String ppnCardinality = getServletContext().getInitParameter("ppn.cardinality");
            if (MULTIPLE.name().equalsIgnoreCase(ppnCardinality)) {
                log.debug("Allow multiple PPNs");
                cardinality = MULTIPLE;
            }

            PPNRegistrationAgent ppnRegistrationAgent = new PPNRegistrationAgent(fedoraAPIAccess);
            ppnRegistrationAgent.registerPPN(urn, ppn, cardinality);

            log.debug(String.format("PPN %s registered for object with PID %s", urn, ppn));
        } catch (URNSyntaxException e) {
            respond(response, SC_BAD_REQUEST, "URN Parameter fehlt oder ist inkorrekt");
        } catch (PPNSyntaxException e) {
            respond(response, SC_BAD_REQUEST, "PPN Parameter fehlt oder ist inkorrekt");
        } catch (CannotResolveIdentifier e) {
            respond(response, SC_NOT_FOUND, "Angegebene URN kann nicht gefunden werden");
        } catch (MissingRequiredParameter e) {
            respondAndLog(response, SC_NOT_ACCEPTABLE, "PPN Dienst ist nicht ausreichend konfiguriert", e);
        } catch (AuthenticationException e) {
            respondAndLog(response, SC_FORBIDDEN, "PPN Dienst ist nicht ausreichend autorisiert", e);
        } catch (RegistrationException e) {
            respondAndLog(response, SC_INTERNAL_SERVER_ERROR, "Fehler beim registrieren der PPN", e);
        } catch (MultipleIdentifiersNotAllowed e) {
            respond(response, SC_CONFLICT, "Kann nicht mehrere PPNs registrieren");
        } finally {
            if (closableHttpClient != null) {
                closableHttpClient.close();
            }
        }
    }

    private void respondAndLog(HttpServletResponse response, int statusCode, String message, Exception e) throws IOException {
        log.error(e.getMessage());
        respond(response, statusCode, message);
    }

    private void respond(HttpServletResponse servletResponse, int statusCode, String message) throws IOException {
        servletResponse.setStatus(statusCode);
        servletResponse.getWriter().print(message);
    }

}
