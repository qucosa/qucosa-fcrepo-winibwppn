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

import javax.xml.bind.DatatypeConverter;

public class Credentials {

    final private String password;
    final private String username;

    private Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public int hashCode() {
        return (password + username).hashCode();
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Credentials
                && this.username.equals(((Credentials) that).username)
                && this.password.equals(((Credentials) that).password);
    }

    String getAsBase64Encoded() {
        return DatatypeConverter.printBase64Binary((String.format("%s:%s", username, password)).getBytes());
    }

    static Credentials fromColonSeparatedString(String creds) {
        String[] values = creds.split(":", 2);
        String username = values[0];
        String password = values[1];
        return new Credentials(username, password);
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }
}
