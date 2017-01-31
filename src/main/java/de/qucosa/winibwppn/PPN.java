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

public class PPN {

    private final String literal;

    private PPN(String literal) {
        this.literal = literal;
    }

    @Override
    public String toString() {
        return literal;
    }

    static PPN fromString(String s) throws PPNSyntaxException {
        if (s == null || s.isEmpty() || !isValidPPN(s)) throw new PPNSyntaxException();
        return new PPN(s);
    }

    private static boolean isValidPPN(String s) {
        // FIXME Find out how to really validate PPNs
        return (s.length() == 8) || (s.length() == 9);
    }

}
