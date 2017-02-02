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
import loc.gov.mods.IdentifierDefinition;
import loc.gov.mods.ModsDefinition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

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
                fedoraAPIAccess.modifyDatastream(pid, "MODS", new ByteArrayInputStream(updatedMods.getBytes()));
            }
        } catch (IOException | JAXBException e) {
            throw new RegistrationException(e);
        }

    }

    private String addPPN(String mods, PPN ppn) throws IOException, JAXBException {
        // FIXME Working with XJC and MODS schema is an impossible task.
        // I've no idea how any of this works. It should, in principle, parse the MODS
        // XML, look for identifier elements and add an identifier element if needed.
        // No sane person must forced to understand this.

        JAXBContext jaxbContext = JAXBContext.newInstance("loc.gov.mods");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        @SuppressWarnings("unchecked") JAXBElement<ModsDefinition> modsDefinitionJAXBElement =
                (JAXBElement<ModsDefinition>) unmarshaller.unmarshal(new ByteArrayInputStream(mods.getBytes()));
        ModsDefinition modsDefinition = modsDefinitionJAXBElement.getValue();

        String ppnString = ppn.toString();

        List<Object> serializables = modsDefinition.getModsGroup();
        for (Object o : serializables) {
            if (o instanceof IdentifierDefinition) {
                IdentifierDefinition identifierDefinition = (IdentifierDefinition) o;
                if ("swb-ppn".equals(identifierDefinition.getType()) &&
                        ppnString.equals(identifierDefinition.getValue())) {
                    return null;
                }
            }
        }

        IdentifierDefinition ppnIdentifier = new IdentifierDefinition();
        ppnIdentifier.setType("swb-ppn");
        ppnIdentifier.setValue(ppn.toString());
        serializables.add(ppnIdentifier);

        QName qName = new QName("http://www.loc.gov/mods/v3", "mods");
        JAXBElement<ModsDefinition> root = new JAXBElement<>(qName, ModsDefinition.class, modsDefinition);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter sw = new StringWriter();
        marshaller.marshal(root, sw);
        return sw.toString();
    }

}
