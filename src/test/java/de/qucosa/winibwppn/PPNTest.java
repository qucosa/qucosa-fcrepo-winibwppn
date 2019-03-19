/*
 * Copyright 2019 Saxon State and University Library Dresden (SLUB)
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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PPNTest {

    @Test
    public void Test_valid_PPNs() throws PPNSyntaxException {
        List<String> valid = Arrays.asList("12345678", "123456789", "123456789X");
        for (String s : valid) {
            PPN ppn = PPN.fromString(s);
            assertEquals(s, ppn.toString());
        }
    }

    @Test
    public void Test_invalid_PPNs() {
        List<String> valid = Arrays.asList("1234567", "123456789XX", "");
        for (String s : valid) {
            try {
                PPN.fromString(s);
                fail();
            } catch (PPNSyntaxException ignored) {
            }
        }
    }

}
