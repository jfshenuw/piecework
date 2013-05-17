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
package piecework.process;

import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Service;

import piecework.process.model.ProcessInstance;
import piecework.process.model.Task;

/**
 * @author James Renfro
 */
public interface ProcessEngineRuntimeFacade {

	ProcessInstance start(String processDefinitionKey, String processBusinessKey, Map<String, ?> data);

    ProcessInstance findInstance(String processDefinitionKey, String processInstanceId, String processInstanceAlias);

    List<ProcessInstance> findInstances(String processDefinitionKey);

    Task findTask(String processDefinitionKey, String taskId);

    List<Task> findTasks(String processDefinitionKey, String userId);

    void completeTask(String processDefinitionKey, String taskId);



}
