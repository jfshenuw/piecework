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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import piecework.enumeration.StateChangeType;
import piecework.exception.StatusCodeError;
import piecework.model.*;
import piecework.model.Process;
import piecework.service.ProcessInstanceService;
import piecework.service.ProcessService;
import piecework.service.TaskService;
import piecework.task.TaskFactory;

/**
 * @author James Renfro
 */
@Service
public class EngineStateSynchronizer {

    private static final Logger LOG = Logger.getLogger(EngineStateSynchronizer.class);

    @Autowired
    Mediator mediator;

    @Autowired
    ProcessService processService;

    @Autowired
    ProcessInstanceService processInstanceService;

    @Autowired
    TaskService taskService;

    public void onProcessInstanceEvent(StateChangeType event, String processInstanceId) {
        ProcessInstance instance = null;
        switch (event) {
        case START_PROCESS:
            if (LOG.isDebugEnabled())
                LOG.debug("Process instance started " + processInstanceId);
            break;
        case COMPLETE_PROCESS:
            instance = processInstanceService.complete(processInstanceId);
            if (instance != null) {
                try {
                    Process process = processService.read(instance.getProcessDefinitionKey());
                    LOG.debug("Process instance completed " + processInstanceId);
                    mediator.notify(new StateChangeEvent.Builder(StateChangeType.COMPLETE_PROCESS).process(process).instance(instance).build());
                } catch (StatusCodeError e) {
                    LOG.error("Unable to find the process for this process instance -- complete process event will not be thrown" + processInstanceId);
                }
            } else {
                LOG.error("Unable to save final state of process instance with execution business key because the instance could not be found" + processInstanceId);
            }
        }
    }

    public void onTaskEvent(StateChangeType type, EngineTask delegateTask) {
        try {
            Process process = processService.read(delegateTask.getProcessDefinitionKey());
            if (process == null)
                return;

            ProcessInstance processInstance = processInstanceService.read(process, delegateTask.getProcessInstanceId(), true);
            if (processInstance == null)
                return;

            Task updated;

            switch(type) {
                case CREATE_TASK:
                    updated = TaskFactory.task(process, processInstance, delegateTask);
                    break;
                default:
                    Task task = taskService.read(processInstance, delegateTask.getTaskId());
                    updated = TaskFactory.task(task, delegateTask, type == StateChangeType.COMPLETE_TASK);
                    break;
            };

            if (updated != null) {
                if (taskService.update(processInstance.getProcessInstanceId(), updated)) {
                    LOG.debug("Stored task changes");
                    mediator.notify(new StateChangeEvent.Builder(type).process(process).instance(processInstance).task(updated).build());
                } else {
                    LOG.error("Failed to store task changes");
                }
            }
        } catch (StatusCodeError error) {
            LOG.error("Unable to save task state changes -- probably because the process or the instance could not be found", error);
        }
    }

}
