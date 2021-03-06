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
package piecework.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import piecework.enumeration.DataInjectionStrategy;

import java.io.Serializable;
import java.net.URI;

/**
 * @author James Renfro
 */
public class Action implements Serializable {

    private final Container container;
    private final String location;
    private final DataInjectionStrategy strategy;

    public Action() {
        this(null, null, null);
    }

    public Action(Container container, String location, DataInjectionStrategy strategy) {
        this.container = container;
        this.location = location;
        this.strategy = strategy;
    }

    @JsonIgnore
    public URI getUri(Task task) throws IllegalArgumentException {
        if (StringUtils.isNotEmpty(location)) {
            String remoteLocation = location;
            if (remoteLocation.contains("{formRequestId}") && task != null)
                remoteLocation = remoteLocation.replace("{formRequestId}", task.getTaskInstanceId());
            URI uri = URI.create(remoteLocation);
            return uri;
        }
        return null;
    }

    public Container getContainer() {
        return container;
    }

    public String getLocation() {
        return location;
    }

    public DataInjectionStrategy getStrategy() {
        return strategy;
    }

}
