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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.mapping.Document;
import piecework.common.ViewContext;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author James Renfro
 */
@XmlRootElement(name = History.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = History.Constants.TYPE_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class History {

    @XmlElementWrapper(name="tasks")
    @XmlElementRef
    private final List<Task> tasks;

    @XmlElement
    private final User initiator;

    @XmlElement
    private final Date startTime;

    @XmlElement
    private final Date endTime;

    @XmlAttribute
    private final String link;

    @XmlAttribute
    private final String uri;

    private History() {
        this(new Builder(), new ViewContext());
    }

    private History(Builder builder, ViewContext context) {
        this.tasks = Collections.unmodifiableList(builder.tasks);
        this.initiator = builder.initiator;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.link = context != null ? context.getApplicationUri(builder.processDefinitionKey, builder.processInstanceId, Constants.ROOT_ELEMENT_NAME) : null;
        this.uri = context != null ? context.getServiceUri(builder.processDefinitionKey, builder.processInstanceId, Constants.ROOT_ELEMENT_NAME) : null;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public User getInitiator() {
        return initiator;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public final static class Builder {

        private String processDefinitionKey;
        private String processInstanceId;
        private List<Task> tasks;
        private User initiator;
        private Date startTime;
        private Date endTime;

        public Builder() {
            this.tasks = new ArrayList<Task>();
        }

        public History build() {
            return new History(this, null);
        }

        public History build(ViewContext viewContext) {
            return new History(this, viewContext);
        }

        public Builder processDefinitionKey(String processDefinitionKey) {
            this.processDefinitionKey = processDefinitionKey;
            return this;
        }

        public Builder processInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
            return this;
        }

        public Builder task(Task task) {
            if (this.tasks == null)
                this.tasks = new ArrayList<Task>();
            if (task != null)
                this.tasks.add(task);
            return this;
        }

        public Builder tasks(List<Task> tasks) {
            if (this.tasks == null)
                this.tasks = new ArrayList<Task>();
            if (tasks != null)
                this.tasks.addAll(tasks);
            return this;
        }

        public Builder initiator(User initiator) {
            this.initiator = initiator;
            return this;
        }

        public Builder startTime(Date startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Date endTime) {
            this.endTime = endTime;
            return this;
        }
    }


    public static class Constants {
        public static final String RESOURCE_LABEL = "History";
        public static final String ROOT_ELEMENT_NAME = "history";
        public static final String TYPE_NAME = "HistoryType";
    }
}
