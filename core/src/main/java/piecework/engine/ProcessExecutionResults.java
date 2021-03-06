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

import piecework.model.ProcessExecution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James Renfro
 */
public class ProcessExecutionResults {

    private final List<ProcessExecution> executions;
    private final int firstResult;
    private final int maxResults;
    private final long total;

    private ProcessExecutionResults() {
        this(new Builder());
    }

    private ProcessExecutionResults(Builder builder) {
        this.executions = builder.executions;
        this.firstResult = builder.firstResult;
        this.maxResults = builder.maxResults;
        this.total = builder.total;
    }

    public List<ProcessExecution> getExecutions() {
        return executions;
    }

    public int getFirstResult() {
        return firstResult;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public long getTotal() {
        return total;
    }

    public final static class Builder {
        private List<ProcessExecution> executions;
        private int firstResult;
        private int maxResults;
        private long total;

        public Builder() {

        }

        public Builder(ProcessExecutionResults results) {
            if (results.executions != null) {
                this.executions = new ArrayList<ProcessExecution>();
                this.executions.addAll(results.executions);
            }
            this.total = results.total;
            this.firstResult = results.firstResult;
            this.maxResults = results.maxResults;
        }


        public ProcessExecutionResults build() {
            return new ProcessExecutionResults(this);
        }

        public Builder execution(ProcessExecution execution) {
            if (this.executions == null)
                this.executions = new ArrayList<ProcessExecution>();
            this.executions.add(execution);
            return this;
        }

        public Builder executions(List<ProcessExecution> executions) {
            if (this.executions == null)
                this.executions = new ArrayList<ProcessExecution>();
            this.executions.addAll(executions);
            return this;
        }

        public Builder firstResult(int firstResult) {
            this.firstResult = firstResult;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public Builder addToTotal(long total) {
            this.total += total;
            return this;
        }

    }
}
