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
package piecework.engine;

import java.io.InputStream;

/**
 * @author James Renfro
 */
public class ProcessModelResource {

    private final String name;
    private final InputStream inputStream;

    private ProcessModelResource() {
        this(new Builder());
    }

    private ProcessModelResource(Builder builder) {
        this.name = builder.name;
        this.inputStream = builder.inputStream;
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public static final class Builder {
        private String name;
        private InputStream inputStream;

        public Builder() {

        }

        public ProcessModelResource build() {
            return new ProcessModelResource(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }
    }


}
