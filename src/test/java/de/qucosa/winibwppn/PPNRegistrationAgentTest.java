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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.HasXPathMatcher;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

public class PPNRegistrationAgentTest {

    public static final Map<String, String> PREFIX_2_URI = new HashMap<String, String>() {{
        put("mods", "http://www.loc.gov/mods/v3");
        put("xlink", "http://www.w3.org/1999/xlink");
    }};

    private FedoraAPIAccess mockRepository;
    private PPNRegistrationAgent ppnRegistrationAgent;

    @Before
    public void setup() {
        mockRepository = mock(FedoraAPIAccess.class);
        ppnRegistrationAgent = new PPNRegistrationAgent(mockRepository);
    }

    @Test
    public void Adds_PPN_identifier_element() throws Exception {
        final String pid = "test:1234";
        final String dsid = "MODS";
        final URN urn = URN.fromString("urn:test:1234");
        final PPN ppn = PPN.fromString("123456789X");
        final String mods = "<mods xmlns=\"http://www.loc.gov/mods/v3\"></mods>";
        final ArgumentCaptor<InputStream> capturedIS = ArgumentCaptor.forClass(InputStream.class);
        when(mockRepository.resolveIdentifier(urn)).thenReturn(pid);
        when(mockRepository.getDatastreamDissemination(pid, dsid)).thenReturn(mods);

        ppnRegistrationAgent.registerPPN(urn, ppn, PPNRegistrationAgent.PPNCardinality.SINGLE);
        verify(mockRepository).modifyDatastream(eq(pid), eq(dsid), capturedIS.capture());

        final Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("mods", "http://www.loc.gov/mods/v3");

        final HasXPathMatcher ppnIdentifierIsPresent =
                hasXPath("//mods:identifier[@type='swb-ppn'][text()='123456789X']")
                        .withNamespaceContext(prefix2Uri);
        assertThat(Input.fromStream(capturedIS.getValue()), ppnIdentifierIsPresent);
    }

    @Test
    // https://jira.slub-dresden.de/browse/CMR-1017
    public void AccessCondition_element_still_contains_xlink_href_after_supplementing_PPN_identifier_element() throws Exception {
        final String pid = "test:1234";
        final String dsid = "MODS";
        final URN urn = URN.fromString("urn:test:1234");
        final PPN ppn = PPN.fromString("123456789X");
        final String mods =
                "<mods xmlns=\"http://www.loc.gov/mods/v3\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                        "   <accessCondition type=\"use and reproduction\"" +
                        "                    xlink:href=\"https://creativecommons.org/licenses/by-nc-nd/4.0/\"/>" +
                        "</mods>";
        final ArgumentCaptor<InputStream> capturedIS = ArgumentCaptor.forClass(InputStream.class);
        when(mockRepository.resolveIdentifier(urn)).thenReturn(pid);
        when(mockRepository.getDatastreamDissemination(pid, dsid)).thenReturn(mods);

        ppnRegistrationAgent.registerPPN(urn, ppn, PPNRegistrationAgent.PPNCardinality.SINGLE);
        verify(mockRepository).modifyDatastream(eq(pid), eq(dsid), capturedIS.capture());

        final Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("mods", "http://www.loc.gov/mods/v3");
        prefix2Uri.put("xlink", "http://www.w3.org/1999/xlink");

        final HasXPathMatcher accessConditionIsPresent =
                hasXPath("//mods:accessCondition[@type='use and reproduction']" +
                        "[@xlink:href=\"https://creativecommons.org/licenses/by-nc-nd/4.0/\"]")
                        .withNamespaceContext(prefix2Uri);
        assertThat(Input.fromStream(capturedIS.getValue()), accessConditionIsPresent);
    }

}
