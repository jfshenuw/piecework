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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author James Renfro
 */
public class ProcessExecutionCriteria {

    public enum OrderBy { START_TIME_ASC, START_TIME_DESC, END_TIME_ASC, END_TIME_DESC };

    private final String engine;
    private final List<String> engineProcessDefinitionKeys;
    private final String businessKey;
    private final List<String> executionIds;
    private final Boolean complete;
    private final Date startedBefore;
    private final Date startedAfter;
    private final Date completedBefore;
    private final Date completedAfter;
    private final String initiatedBy;
    private final Integer firstResult;
    private final Integer maxResults;
    private final boolean includeVariables;
    private final OrderBy orderBy;

    private ProcessExecutionCriteria() {
        this(new Builder());
    }

    private ProcessExecutionCriteria(Builder builder) {
        this.engine = builder.engine;
        this.engineProcessDefinitionKeys = builder.engineProcessDefinitionKeys != null ? Collections.unmodifiableList(builder.engineProcessDefinitionKeys) : null;
        this.businessKey = builder.businessKey;
        this.executionIds = builder.executionIds;
        this.complete = builder.complete;
        this.startedBefore = builder.startedBefore;
        this.startedAfter = builder.startedAfter;
        this.completedBefore = builder.completedBefore;
        this.completedAfter = builder.completedAfter;
        this.initiatedBy = builder.initiatedBy;
        this.orderBy = builder.orderBy;
        this.firstResult = builder.firstResult;
        this.maxResults = builder.maxResults;
        this.includeVariables = builder.includeVariables;
    }

    public String getEngine() {
        return engine;
    }

    public List<String> getEngineProcessDefinitionKeys() {
        return engineProcessDefinitionKeys;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public List<String> getExecutionIds() {
        return executionIds;
    }

    public Boolean getComplete() {
        return complete;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public Date getCompletedBefore() {
        return completedBefore;
    }

    public Date getCompletedAfter() {
        return completedAfter;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public Integer getFirstResult() {
        return firstResult;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public boolean isIncludeVariables() {
        return includeVariables;
    }

    public final static class Builder {

        private String engine;
        private List<String> engineProcessDefinitionKeys;
        private String businessKey;
        private List<String> executionIds;
        private Boolean complete;
        private Date startedBefore;
        private Date startedAfter;
        private Date completedBefore;
        private Date completedAfter;
        private String initiatedBy;
        private OrderBy orderBy;
        private Integer firstResult;
        private Integer maxResults;
        private boolean includeVariables;

        public Builder() {
            super();
        }

        public ProcessExecutionCriteria build() {
            return new ProcessExecutionCriteria(this);
        }

        public Builder engine(String engine) {
            this.engine = engine;
            return this;
        }

        public Builder engineProcessDefinitionKey(String engineProcessDefinitionKey) {
            if (this.engineProcessDefinitionKeys == null)
                this.engineProcessDefinitionKeys = new ArrayList<String>();
            this.engineProcessDefinitionKeys.add(engineProcessDefinitionKey);
            return this;
        }

        public Builder businessKey(String businessKey) {
            this.businessKey = businessKey;
            return this;
        }

        public Builder executionId(String executionId) {
            if (this.executionIds == null)
                this.executionIds = new ArrayList<String>();
            this.executionIds.add(executionId);
            return this;
        }

        public Builder executionIds(List<String> executionIds) {
            if (this.executionIds == null)
                this.executionIds = new ArrayList<String>();
            this.executionIds.addAll(executionIds);
            return this;
        }

        public Builder complete(Boolean complete) {
            this.complete = complete;
            return this;
        }

        public Builder startedBefore(Date startedBefore) {
            this.startedBefore = startedBefore;
            return this;
        }

        public Builder startedAfter(Date startedAfter) {
            this.startedAfter = startedAfter;
            return this;
        }

        public Builder completedBefore(Date completedBefore) {
            this.completedBefore = completedBefore;
            return this;
        }

        public Builder completedAfter(Date completedAfter) {
            this.completedAfter = completedAfter;
            return this;
        }

        public Builder initiatedBy(String initiatedBy) {
            this.initiatedBy = initiatedBy;
            return this;
        }

        public Builder orderBy(OrderBy orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder firstResult(Integer firstResult) {
            this.firstResult = firstResult;
            return this;
        }

        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder includeVariables() {
            this.includeVariables = true;
            return this;
        }

    }

}
