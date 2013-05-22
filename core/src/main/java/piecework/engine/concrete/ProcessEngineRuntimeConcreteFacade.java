/*
 * Copyright 2012 University of Washington
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
package piecework.engine.concrete;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import piecework.Registry;
import piecework.engine.*;
import piecework.model.*;
import piecework.model.Process;
import piecework.util.ManyMap;

/**
 * @author James Renfro
 */
@Service
public class ProcessEngineRuntimeConcreteFacade implements ProcessEngineRuntimeFacade {

	@Autowired
    Registry registry;

    @Override
    public String start(piecework.model.Process process, String alias, Map<String, ?> data) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, process.getEngine());
        return proxy.start(process, alias, data);
    }

    @Override
    public boolean cancel(Process process, String processInstanceId, String alias, String reason) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, process.getEngine());
        return proxy.cancel(process, processInstanceId, alias, reason);
    }

    @Override
    public ProcessExecution findExecution(ProcessExecutionCriteria criteria) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, criteria.getEngine());
        return proxy.findExecution(criteria);
    }

    @Override
    public List<ProcessExecution> findExecutions(ProcessExecutionCriteria criteria) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, criteria.getEngine());
        return proxy.findExecutions(criteria);
    }

    @Override
    public Task findTask(TaskCriteria criteria) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, criteria.getEngine());
        return proxy.findTask(criteria);
    }

    @Override
    public List<Task> findTasks(TaskCriteria criteria) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, criteria.getEngine());
        return proxy.findTasks(criteria);
    }

    @Override
    public void completeTask(Process process, String taskId) {
        ProcessEngineProxy proxy = registry.retrieve(ProcessEngineProxy.class, process.getEngine());
        proxy.completeTask(process, taskId);
    }

}
