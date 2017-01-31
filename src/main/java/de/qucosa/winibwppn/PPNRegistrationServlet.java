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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PPNRegistrationServlet extends HttpServlet {

    final private Logger log = LoggerFactory.getLogger(PPNRegistrationServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            URN urn = URN.fromString(req.getParameter("urn"));
            PPN ppn = PPN.fromString(req.getParameter("ppn"));
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            resp.getWriter().print(String.format("Ok, fine. I'll register PPN `%s` for object `%s`.", ppn, urn));
        } catch (URNSyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("URN Parameter fehlt oder ist inkorrekt");
        } catch (PPNSyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("PPN Parameter fehlt oder ist inkorrekt");
        }
    }

}
