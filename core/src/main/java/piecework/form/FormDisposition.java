/*
 * Copyright 2013 University of Washington
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piecework.form;

import java.net.URI;

/**
 * @author James Renfro
 */
public class FormDisposition {

    public enum FormDispositionType { DEFAULT, CUSTOM, REMOTE };

    private final FormDispositionType type;
    private final URI uri;
    private final String path;

    public FormDisposition(FormDispositionType type, URI uri, String path) {
        this.type = type;
        this.uri = uri;
        this.path = path;
    }

    public FormDisposition() {
        this(FormDispositionType.DEFAULT, null, null);
    }

    public FormDisposition(URI uri) {
        this(FormDispositionType.REMOTE, uri, null);
    }

    public FormDisposition(String path) {
        this(FormDispositionType.CUSTOM, null, path);
    }

    public FormDispositionType getType() {
        return type;
    }

    public URI getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }
}
