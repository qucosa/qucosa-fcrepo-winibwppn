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
import org.w3c.dom.Document;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

class PPNRegistrationAgent {
    private final FedoraAPIAccess fedoraAPIAccess;

    PPNRegistrationAgent(FedoraAPIAccess fedoraAPIAccess) {
        this.fedoraAPIAccess = fedoraAPIAccess;
    }

    void registerPPN(URN urn, PPN ppn) throws RegistrationException, CannotResolveIdentifier {
        String pid = fedoraAPIAccess.resolveIdentifier(urn);

        try {
            String mods = fedoraAPIAccess.getDatastreamDissemination(pid, "MODS");
            String updatedMods = addPPN(mods, ppn);
            if (updatedMods != null) {
                System.out.println(updatedMods);
//                fedoraAPIAccess.modifyDatastream(pid, "MODS", new ByteArrayInputStream(updatedMods.getBytes()));
            }
        } catch (IOException e) {
            throw new RegistrationException(e);
        }

    }

    private String addPPN(String mods, PPN ppn) {
        // TODO Add PPN identifier to MODS if not already there

        return mods;
    }

}
