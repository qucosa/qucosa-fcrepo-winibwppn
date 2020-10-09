/*
 * Copyright 2020 Saxon State and University Library Dresden (SLUB)
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
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.HasXPathMatcher;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

public class PPNRegistrationAgentTest {

    private FedoraAPIAccess mockRepository;
    private PPNRegistrationAgent ppnRegistrationAgent;

    @Before
    public void setup() {
        mockRepository = mock(FedoraAPIAccess.class);
        ppnRegistrationAgent = new PPNRegistrationAgent(mockRepository);
    }

    @Test
    public void Adds_PPN_Identifier_element() throws Exception {
        final String pid = "test:1234";
        final String dsid = "MODS";
        final URN urn = URN.fromString("urn:test:1234");
        final PPN ppn = PPN.fromString("123456789X");
        final String mods = "<mods xmlns=\"http://www.loc.gov/mods/v3\">" +
                "</mods>";
        final ArgumentCaptor<InputStream> capturedIS = ArgumentCaptor.forClass(InputStream.class);
        when(mockRepository.resolveIdentifier(urn)).thenReturn(pid);
        when(mockRepository.getDatastreamDissemination(pid, dsid)).thenReturn(mods);

        ppnRegistrationAgent.registerPPN(urn, ppn, PPNRegistrationAgent.PPNCardinality.SINGLE);
        verify(mockRepository).modifyDatastream(eq(pid), eq(dsid), capturedIS.capture());

        final Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("mods", "http://www.loc.gov/mods/v3");

        HasXPathMatcher xPathMatcher =
                hasXPath("//mods:identifier[@type='swb-ppn'][text()='123456789X']")
                .withNamespaceContext(prefix2Uri);
        assertThat(Input.fromStream(capturedIS.getValue()), xPathMatcher);
    }

}
