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
package piecework.engine.activiti;

import java.util.*;

import com.google.common.collect.Sets;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.query.Query;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import piecework.Constants;
import piecework.identity.InternalUserDetailsService;
import piecework.model.User;
import piecework.engine.*;
import piecework.engine.exception.ProcessEngineException;
import piecework.identity.InternalUserDetails;
import piecework.model.Process;
import piecework.model.ProcessExecution;
import piecework.model.ProcessInstance;
import piecework.model.Task;
import piecework.persistence.ProcessInstanceRepository;
import piecework.process.ProcessInstanceSearchCriteria;
import piecework.process.concrete.ResourceHelper;
import piecework.task.TaskCriteria;
import piecework.task.TaskResults;
import piecework.util.ManyMap;

/**
 * @author James Renfro
 */
@Service
public class ActivitiEngineProxy implements ProcessEngineProxy {

    @Autowired
    ResourceHelper helper;

    @Autowired
    IdentityService identityService;

    @Autowired
    HistoryService historyService;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    @Autowired
    InternalUserDetailsService userDetailsService;

	@Autowired
	RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    TaskService taskService;

	@Override
	public String start(Process process, String processBusinessKey, Map<String, ?> data) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

		String engineProcessDefinitionKey = process.getEngineProcessDefinitionKey();
		Map<String, Object> engineData = data != null ? new HashMap<String, Object>(data) : null;
		org.activiti.engine.runtime.ProcessInstance activitiInstance = runtimeService.startProcessInstanceByKey(engineProcessDefinitionKey, processBusinessKey, engineData);
		return activitiInstance.getId();
	}

    @Override
    public boolean activate(Process process, ProcessInstance instance) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

        String engineProcessDefinitionKey = process.getEngineProcessDefinitionKey();
        org.activiti.engine.runtime.ProcessInstance activitiInstance = findActivitiInstance(engineProcessDefinitionKey, instance.getEngineProcessInstanceId(), null);

        if (activitiInstance != null) {
            runtimeService.activateProcessInstanceById(activitiInstance.getProcessInstanceId());
            return true;
        }

        return false;
    }

	@Override
	public boolean cancel(Process process, ProcessInstance instance) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

        String engineProcessDefinitionKey = process.getEngineProcessDefinitionKey();
        org.activiti.engine.runtime.ProcessInstance activitiInstance = findActivitiInstance(engineProcessDefinitionKey, instance.getEngineProcessInstanceId(), null);
		
		if (activitiInstance != null) {
			runtimeService.deleteProcessInstance(activitiInstance.getProcessInstanceId(), Constants.DeleteReasons.CANCELLED);
			return true;
		}
		
		return false;
	}

    @Override
    public boolean suspend(Process process, ProcessInstance instance) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

        String engineProcessDefinitionKey = process.getEngineProcessDefinitionKey();
        org.activiti.engine.runtime.ProcessInstance activitiInstance = findActivitiInstance(engineProcessDefinitionKey, instance.getEngineProcessInstanceId(), null);

        if (activitiInstance != null) {
            runtimeService.suspendProcessInstanceById(activitiInstance.getProcessInstanceId());
            return true;
        }

        return false;
    }

    @Override
    public boolean completeTask(Process process, String taskId, String action) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

        String engineProcessDefinitionKey = process.getEngineProcessDefinitionKey();

        try {
            org.activiti.engine.task.Task activitiTask = taskService.createTaskQuery().processDefinitionKey(engineProcessDefinitionKey).taskId(taskId).singleResult();

            if (activitiTask != null)  {
                if (action != null) {
                    String variableName = activitiTask.getTaskDefinitionKey() + "_action";
                    taskService.setVariable(taskId, variableName, action);
                }
                // Always assign the task to the user before completing it
                if (StringUtils.isNotEmpty(userId))
                    taskService.setAssignee(taskId, userId);
                taskService.complete(taskId);
                return true;
            }

        } catch (ActivitiException exception) {
            throw new ProcessEngineException("Activiti unable to complete task ", exception);
        }

        return false;
    }

    @Override
    public void deploy(Process process, String name, ProcessModelResource... resources) throws ProcessEngineException {
        InternalUserDetails principal = helper.getAuthenticatedPrincipal();
        String userId = principal != null ? principal.getInternalId() : null;
        identityService.setAuthenticatedUserId(userId);

        if (! process.getEngine().equals(getKey()))
            return;

        DeploymentBuilder builder = repositoryService.createDeployment();

        if (resources != null) {
            for (ProcessModelResource resource : resources) {
                builder.addInputStream(resource.getName(), resource.getInputStream());
            }

            builder.deploy();
        }
    }

    @Override
	public ProcessExecution findExecution(ProcessInstanceSearchCriteria criteria) throws ProcessEngineException {

        if (! criteria.getEngines().contains(getKey()))
            return null;

        boolean includeVariables = false;

        HistoricProcessInstance instance = instanceQuery(criteria).singleResult();

        if (instance != null) {
            ProcessExecution.Builder executionBuilder = new ProcessExecution.Builder()
                .executionId(instance.getId())
                .businessKey(instance.getBusinessKey())
                .initiatorId(instance.getStartUserId())
                .deleteReason(instance.getDeleteReason());

            if (criteria.isIncludeVariables()) {
                Map<String, Object> variables = runtimeService.getVariables(instance.getId());
                executionBuilder.data(variables);
            }

            return executionBuilder.build();
        }

        return null;
	}

	@Override
	public ProcessExecutionResults findExecutions(ProcessInstanceSearchCriteria criteria) throws ProcessEngineException {
        if (! criteria.getEngines().contains(getKey()))
            return null;

        HistoricProcessInstanceQuery query = instanceQuery(criteria);

        ProcessExecutionResults.Builder resultsBuilder = new ProcessExecutionResults.Builder();

        List<HistoricProcessInstance> instances;

        // Can't use paging since we're going to filter after the fact
        instances = query.list();
        int size = instances.size();

        resultsBuilder.firstResult(0);
        resultsBuilder.maxResults(size);
        resultsBuilder.total(size);

        // Only need to worry about filtering if there are more than 1 key, since with 1 key
        // it's part of the search that Activiti performs --- see the instanceQuery() method
        SortedSet<String> engineProcessDefinitionKeys = new TreeSet<String>(criteria.getEngineProcessDefinitionKeys());
        Set<String> processDefinitionIds = getProcessDefinitionIds(engineProcessDefinitionKeys.toArray(new String[engineProcessDefinitionKeys.size()]));

        List<ProcessExecution> executions;
        if (instances != null && !instances.isEmpty()) {
            executions = new ArrayList<ProcessExecution>(instances.size());

            for (HistoricProcessInstance instance : instances) {
                if (!processDefinitionIds.contains(instance.getProcessDefinitionId()))
                    continue;

                ProcessExecution.Builder executionBuilder = new ProcessExecution.Builder()
                        .executionId(instance.getId())
                        .businessKey(instance.getBusinessKey())
                        .initiatorId(instance.getStartUserId())
                        .deleteReason(instance.getDeleteReason());

                if (criteria.isIncludeVariables()) {
                    Map<String, Object> variables = runtimeService.getVariables(instance.getId());
                    executionBuilder.data(variables);
                }

                executions.add(executionBuilder.build());
            }
        } else {
            executions = Collections.emptyList();
        }

        resultsBuilder.executions(executions);

        return resultsBuilder.build();
	}

    @Override
    public Task findTask(TaskCriteria criteria) throws ProcessEngineException {
        if (criteria.getProcesses() == null || criteria.getProcesses().size() != 1)
            throw new ProcessEngineException("Must provide the process for a task that is being looked up");

        Process process = criteria.getProcesses().iterator().next();

        org.activiti.engine.task.Task activitiTask = taskQuery(criteria).singleResult();
        HistoricTaskInstance historicTask = null;

        String engineProcessInstanceId;
        if (activitiTask != null) {
            engineProcessInstanceId = activitiTask.getProcessInstanceId();
        } else {
            historicTask = historicTaskQuery(criteria).singleResult();
            if (historicTask == null)
                return null;

            engineProcessInstanceId = historicTask.getProcessInstanceId();
        }

        ProcessInstance processInstance = processInstanceRepository.findByProcessDefinitionKeyAndEngineProcessInstanceId(process.getProcessDefinitionKey(), engineProcessInstanceId);

//        ManyMap<String, Process> processDefinitionIdMap = getProcessDefinitionIdMap(criteria.getProcesses());
//        List<Process> processes = processDefinitionIdMap.get(processDefinitionId);
//        if (processes == null)
//            throw new ProcessEngineException("Not authorized to see tasks of this process");
//
//        List<ProcessInstance> processInstances = processInstanceRepository.findByProcessDefinitionKeyInAndEngineProcessInstanceIdIn(Sets.newHashSet(process.getProcessDefinitionKey()), Sets.newHashSet(processInstanceId));
//        if (processInstances.isEmpty() || processInstances.size() > 1)
//            throw new ProcessEngineException("Unable to find a single process instance for this task");

        if (historicTask != null)
            return convert(historicTask, process, processInstance, true);

        return convert(activitiTask, process, processInstance, true);
    }

    public TaskResults findTasks(TaskCriteria criteria) throws ProcessEngineException {
        TaskResults.Builder resultsBuilder = new TaskResults.Builder();

        if (criteria.getProcesses() == null || criteria.getProcesses().isEmpty())
            return resultsBuilder.build();

        Query query;

        if (StringUtils.isEmpty(criteria.getProcessStatus()) || criteria.getProcessStatus().equals(Constants.ProcessStatuses.OPEN) ||
                criteria.getProcessStatus().equals(Constants.ProcessStatuses.SUSPENDED)) {
            query = taskQuery(criteria);
        } else {
            query = historicTaskQuery(criteria);
        }

        List<?> instances = query.list();
        int size = instances.size();

        resultsBuilder.firstResult(0);
        resultsBuilder.maxResults(size);
        resultsBuilder.total(size);

        // Only need to worry about filtering if there are more than 1 key, since with 1 key
        // it's part of the search that Activiti performs --- see the instanceQuery() method
        ManyMap<String, Process> processDefinitionIdMap = getProcessDefinitionIdMap(criteria.getProcesses());
        Set<String> allowedProcessDefinitionKeys = getProcessDefinitionKeys(criteria.getProcesses());

        List<Task> tasks;
        if (instances != null && !instances.isEmpty()) {
            tasks = new ArrayList<Task>(instances.size());

            Set<String> engineProcessInstanceIds = Sets.newHashSet();
            for (Object instance : instances) {
                if (instance instanceof org.activiti.engine.task.Task) {
                    engineProcessInstanceIds.add(((org.activiti.engine.task.Task)instance).getProcessInstanceId());
                } else if (instance instanceof HistoricTaskInstance) {
                    engineProcessInstanceIds.add(((HistoricTaskInstance)instance).getProcessInstanceId());
                }
            }

            List<ProcessInstance> processInstances = processInstanceRepository.findByProcessDefinitionKeyInAndEngineProcessInstanceIdIn(allowedProcessDefinitionKeys, engineProcessInstanceIds);
            if (processInstances != null && !processInstances.isEmpty()) {
                MultiKeyMap processInstanceMap = new MultiKeyMap();
                for (ProcessInstance processInstance : processInstances) {
                    if (processInstance == null)
                        continue;
                    if (org.apache.cxf.common.util.StringUtils.isEmpty(processInstance.getProcessDefinitionKey()))
                        continue;
                    if (org.apache.cxf.common.util.StringUtils.isEmpty(processInstance.getEngineProcessInstanceId()))
                        continue;

                    processInstanceMap.put(processInstance.getProcessDefinitionKey(), processInstance.getEngineProcessInstanceId(), processInstance);
                }
                for (Object instance : instances) {
                    String processDefinitionId = null;
                    String processInstanceId = null;

                    if (instance instanceof org.activiti.engine.task.Task) {
                        processDefinitionId = ((org.activiti.engine.task.Task)instance).getProcessDefinitionId();
                        processInstanceId = ((org.activiti.engine.task.Task)instance).getProcessInstanceId();
                    } else if (instance instanceof HistoricTaskInstance) {
                        processDefinitionId = ((HistoricTaskInstance)instance).getProcessDefinitionId();
                        processInstanceId = ((HistoricTaskInstance)instance).getProcessInstanceId();
                    }

                    List<Process> processes = processDefinitionIdMap.get(processDefinitionId);
                    if (processes == null)
                        continue;
                    if (instance == null)
                        continue;

                    if (org.apache.cxf.common.util.StringUtils.isEmpty(processInstanceId))
                        continue;

                    for (Process process : processes) {
                        if (org.apache.cxf.common.util.StringUtils.isEmpty(process.getProcessDefinitionKey()))
                            continue;

                        ProcessInstance processInstance = ProcessInstance.class.cast(processInstanceMap.get(process.getProcessDefinitionKey(), processInstanceId));

                        if (processInstance != null) {
                            if (instance instanceof org.activiti.engine.task.Task)
                                tasks.add(convert(((org.activiti.engine.task.Task)instance), process, processInstance, false));
                            else if (instance instanceof HistoricTaskInstance)
                                tasks.add(convert(((HistoricTaskInstance)instance), process, processInstance, false));
                        }
                    }
                }
            }
        } else {
            tasks = Collections.emptyList();
        }

        resultsBuilder.tasks(tasks);

        return resultsBuilder.build();
    }

    private Task convert(org.activiti.engine.task.Task instance, Process process, ProcessInstance processInstance, boolean includeDetails) {
        Task.Builder taskBuilder = new Task.Builder()
                .taskInstanceId(instance.getId())
                .taskDefinitionKey(instance.getTaskDefinitionKey())
                .taskLabel(instance.getName())
                .taskDescription(instance.getDescription())
                .taskStatus(instance.isSuspended() ? Constants.TaskStatuses.SUSPENDED : Constants.TaskStatuses.OPEN)
                .processInstanceId(processInstance.getProcessInstanceId())
                .processInstanceAlias(processInstance.getAlias())
                .processInstanceLabel(processInstance.getProcessInstanceLabel())
                .processDefinitionKey(process.getProcessDefinitionKey())
                .processDefinitionLabel(process.getProcessDefinitionLabel())
                .priority(instance.getPriority())
                .startTime(instance.getCreateTime())
                .dueDate(instance.getDueDate());

        if (!instance.isSuspended())
            taskBuilder.active();

        if (includeDetails) {
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(instance.getId());

            if (identityLinks != null && !identityLinks.isEmpty()) {
                for (IdentityLink identityLink : identityLinks) {
                    String type = identityLink.getType();

                    if (type == null)
                        continue;

                    if (type.equals(IdentityLinkType.ASSIGNEE) && identityLink.getUserId() != null)
                        taskBuilder.assignee(userDetailsService.getUser(identityLink.getUserId()));
                    else if (type.equals(IdentityLinkType.CANDIDATE) && identityLink.getUserId() != null)
                        taskBuilder.candidateAssignee(userDetailsService.getUser(identityLink.getUserId()));
                }
            }
        } else if (StringUtils.isNotEmpty(instance.getAssignee())) {
            taskBuilder.assignee(userDetailsService.getUser(instance.getAssignee()));
        }

        return taskBuilder.build();
    }

    private Task convert(HistoricTaskInstance instance, Process process, ProcessInstance processInstance, boolean includeDetails) {
        Task.Builder taskBuilder = new Task.Builder()
                .taskInstanceId(instance.getId())
                .taskDefinitionKey(instance.getTaskDefinitionKey())
                .taskLabel(instance.getName())
                .taskDescription(instance.getDescription())
                .processInstanceId(processInstance.getProcessInstanceId())
                .processInstanceAlias(processInstance.getAlias())
                .processInstanceLabel(processInstance.getProcessInstanceLabel())
                .processDefinitionKey(process.getProcessDefinitionKey())
                .processDefinitionLabel(process.getProcessDefinitionLabel())
                .priority(instance.getPriority())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .claimTime(instance.getClaimTime())
                .dueDate(instance.getDueDate())
                .finished();

        if (instance.getDeleteReason() != null && instance.getDeleteReason().equals(Constants.DeleteReasons.CANCELLED))
            taskBuilder.taskStatus(Constants.TaskStatuses.CANCELLED);
        else if (instance.getEndTime() != null)
            taskBuilder.taskStatus(Constants.TaskStatuses.COMPLETE);
        else
            taskBuilder.taskStatus(Constants.TaskStatuses.OPEN);

        if (includeDetails) {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(instance.getId());

            if (identityLinks != null && !identityLinks.isEmpty()) {
                for (HistoricIdentityLink identityLink : identityLinks) {
                    String type = identityLink.getType();

                    if (type == null)
                        continue;

                    if (type.equals(IdentityLinkType.ASSIGNEE) && identityLink.getUserId() != null)
                        taskBuilder.assignee(userDetailsService.getUser(identityLink.getUserId()));
                    else if (type.equals(IdentityLinkType.CANDIDATE) && identityLink.getUserId() != null)
                        taskBuilder.candidateAssignee(userDetailsService.getUser(identityLink.getUserId()));
                }
            }
        } else if (StringUtils.isNotEmpty(instance.getAssignee())) {
            taskBuilder.assignee(userDetailsService.getUser(instance.getAssignee()));
        }

        return taskBuilder.build();
    }


    private org.activiti.engine.runtime.ProcessInstance findActivitiInstance(String engineProcessDefinitionKey, String engineProcessInstanceId, String processBusinessKey) {
		org.activiti.engine.runtime.ProcessInstance activitiInstance = null;
		if (engineProcessInstanceId == null)
			activitiInstance = runtimeService
				.createProcessInstanceQuery()
				.processDefinitionKey(engineProcessDefinitionKey)
				.processInstanceBusinessKey(processBusinessKey)
				.singleResult();
		else
			activitiInstance = runtimeService
				.createProcessInstanceQuery()
				.processDefinitionKey(engineProcessDefinitionKey)
				.processInstanceId(engineProcessInstanceId)
				.singleResult();

		return activitiInstance;
	}

    private org.activiti.engine.history.HistoricProcessInstance findActivitiHistoricInstance(String engineProcessDefinitionKey, String engineProcessInstanceId, String processBusinessKey) {
        org.activiti.engine.history.HistoricProcessInstance activitiInstance = null;
        if (engineProcessInstanceId == null)
            activitiInstance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processDefinitionKey(engineProcessDefinitionKey)
                    .processInstanceBusinessKey(processBusinessKey)
                    .singleResult();
        else
            activitiInstance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processDefinitionKey(engineProcessDefinitionKey)
                    .processInstanceId(engineProcessInstanceId)
                    .singleResult();

        return activitiInstance;
    }

    private HistoricProcessInstanceQuery instanceQuery(ProcessInstanceSearchCriteria criteria) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

        // Activiti only allows us to filter by a single process definition key at a time -- so if there are more than 1
        // we have to filter the result set
        if (criteria.getEngineProcessDefinitionKeys() != null && criteria.getEngineProcessDefinitionKeys().size() == 1)
            query.processDefinitionKey(criteria.getEngineProcessDefinitionKeys().iterator().next());

        List<String> executionIds = criteria.getExecutionIds();
        if (executionIds != null && !executionIds.isEmpty())
            query.processInstanceIds(new HashSet<String>(criteria.getExecutionIds()));

        if (criteria.getBusinessKey() != null)
            query.processInstanceBusinessKey(criteria.getBusinessKey());

        if (criteria.getStartedAfter() != null)
            query.startedAfter(criteria.getStartedAfter());

        if (criteria.getStartedBefore() != null)
            query.startedBefore(criteria.getStartedBefore());

        if (criteria.getCompletedBefore() != null)
            query.finishedBefore(criteria.getCompletedBefore());

        if (criteria.getCompletedAfter() != null)
            query.finishedAfter(criteria.getCompletedAfter());

        if (criteria.getInitiatedBy() != null)
            query.startedBy(criteria.getInitiatedBy());

        ProcessInstanceSearchCriteria.OrderBy orderBy = criteria.getOrderBy();
        if (orderBy != null) {
            switch (orderBy) {
                case START_TIME_ASC:
                    query.orderByProcessInstanceStartTime().asc();
                    break;
                case START_TIME_DESC:
                    query.orderByProcessInstanceStartTime().desc();
                    break;
                case END_TIME_ASC:
                    query.orderByProcessInstanceEndTime().asc();
                    break;
                case END_TIME_DESC:
                    query.orderByProcessInstanceEndTime().desc();
                    break;
            }
        } else {
            query.orderByProcessInstanceEndTime().desc();
        }

        if (criteria.getComplete() != null) {
            if (criteria.getComplete().booleanValue())
                query.finished();
            else
                query.unfinished();
        }

        return query;
    }

    private TaskQuery taskQuery(TaskCriteria criteria) {
        TaskQuery query = taskService.createTaskQuery();

        if (criteria.getProcesses() != null && criteria.getProcesses().size() == 1)
            query.processDefinitionKey(criteria.getProcesses().iterator().next().getEngineProcessDefinitionKey());

        List<String> taskIds = criteria.getTaskIds();
        if (taskIds != null && taskIds.size() == 1)
            query.taskId(taskIds.iterator().next());

        if (StringUtils.isNotEmpty(criteria.getProcessStatus()) && !criteria.getProcessStatus().equals(Constants.ProcessStatuses.OPEN)) {
            if (criteria.getProcessStatus().equals(Constants.ProcessStatuses.SUSPENDED))
                query.suspended();
        } else {
            query.active();
        }

        if (StringUtils.isNotEmpty(criteria.getExecutionId()))
            query.processInstanceId(criteria.getExecutionId());

        if (StringUtils.isNotEmpty(criteria.getBusinessKey()))
            query.processInstanceBusinessKey(criteria.getBusinessKey());

        if (StringUtils.isNotEmpty(criteria.getAssigneeId()))
            query.taskAssignee(criteria.getAssigneeId());

        if (StringUtils.isNotEmpty(criteria.getCandidateAssigneeId()))
            query.taskCandidateUser(criteria.getCandidateAssigneeId());

        if (StringUtils.isNotEmpty(criteria.getParticipantId()))
            query.taskInvolvedUser(criteria.getParticipantId());

        if (criteria.getCreatedAfter() != null)
            query.taskCreatedAfter(criteria.getCreatedAfter());

        if (criteria.getCreatedBefore() != null)
            query.taskCreatedBefore(criteria.getCreatedBefore());

        if (criteria.getDueBefore() != null)
            query.dueBefore(criteria.getDueBefore());

        if (criteria.getDueAfter() != null)
            query.dueAfter(criteria.getDueAfter());

        if (criteria.getMaxPriority() != null)
            query.taskMaxPriority(criteria.getMaxPriority());

        if (criteria.getMinPriority() != null)
            query.taskMinPriority(criteria.getMinPriority());

        TaskCriteria.OrderBy orderBy = criteria.getOrderBy();
        if (orderBy != null) {
           switch (orderBy) {
               case CREATED_TIME_ASC:
                   query.orderByTaskCreateTime().asc();
                   break;
               case CREATED_TIME_DESC:
                   query.orderByTaskCreateTime().desc();
                   break;
               case DUE_TIME_ASC:
                   query.orderByDueDate().asc();
                   break;
               case DUE_TIME_DESC:
                   query.orderByDueDate().desc();
                   break;
               case PRIORITY_ASC:
                   query.orderByTaskPriority().asc();
                   break;
               case PRIORITY_DESC:
                   query.orderByTaskPriority().desc();
                   break;
           }
        } else {
            query.orderByDueDate().orderByTaskCreateTime().desc();
        }

        if (criteria.getActive() != null) {
            if (criteria.getActive().booleanValue())
                query.active();
            else
                query.suspended();
        }

        return query;
    }

    private HistoricTaskInstanceQuery historicTaskQuery(TaskCriteria criteria) {

        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

        if (criteria.getProcesses() != null && criteria.getProcesses().size() == 1)
            query.processDefinitionKey(criteria.getProcesses().iterator().next().getEngineProcessDefinitionKey());

        if (StringUtils.isNotEmpty(criteria.getProcessStatus()) && !criteria.getProcessStatus().equals(Constants.ProcessStatuses.OPEN)) {
            if (criteria.getProcessStatus().equals(Constants.ProcessStatuses.COMPLETE))
                query.finished();
            if (criteria.getProcessStatus().equals(Constants.ProcessStatuses.CANCELLED))
                query.taskDeleteReason(Constants.DeleteReasons.CANCELLED);
        } else {
            query.unfinished();
        }

        List<String> taskIds = criteria.getTaskIds();
        if (taskIds != null && taskIds.size() == 1)
            query.taskId(taskIds.iterator().next());

        if (criteria.getExecutionId() != null)
            query.processInstanceId(criteria.getExecutionId());

        if (criteria.getAssigneeId() != null)
            query.taskAssignee(criteria.getAssigneeId());

        if (criteria.getDueBefore() != null)
            query.taskDueBefore(criteria.getDueBefore());

        if (criteria.getDueAfter() != null)
            query.taskDueAfter(criteria.getDueAfter());

        TaskCriteria.OrderBy orderBy = criteria.getOrderBy();
        if (orderBy != null) {
            switch (orderBy) {
                case CREATED_TIME_ASC:
                    query.orderByHistoricTaskInstanceStartTime().asc();
                    break;
                case CREATED_TIME_DESC:
                    query.orderByHistoricTaskInstanceStartTime().desc();
                    break;
                case DUE_TIME_ASC:
                    query.orderByTaskDueDate().asc();
                    break;
                case DUE_TIME_DESC:
                    query.orderByTaskDueDate().desc();
                    break;
                case PRIORITY_ASC:
                    query.orderByTaskPriority().asc();
                    break;
                case PRIORITY_DESC:
                    query.orderByTaskPriority().desc();
                    break;
            }
        }

        if (criteria.getComplete() != null) {
            if (criteria.getComplete().booleanValue())
                query.finished();
            else
                query.unfinished();
        }

        return query;
    }

    @Cacheable("processDefinitionIds")
    private Set<String> getProcessDefinitionIds(String ... keys) {
        Set<String> keySet = Sets.newHashSet(keys);
        Set<String> set = new HashSet<String>();
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().list()) {
            if (keySet.contains(processDefinition.getKey()))
                set.add(processDefinition.getId());
        }

        return Collections.unmodifiableSet(set);
    }

    @Cacheable("processDefinitionKeys")
    private Set<String> getProcessDefinitionKeys(Set<Process> processes) {
        Set<String> set = Sets.newHashSet();
        for (Process process : processes) {
            if (process.getEngine() != null && getKey() != null && process.getEngine().equals(getKey()) && StringUtils.isNotEmpty(process.getProcessDefinitionKey()))
                set.add(process.getProcessDefinitionKey());
        }
        return set;
    }

    @Cacheable("processDefinitionIdMap")
    private ManyMap<String, Process> getProcessDefinitionIdMap(Set<Process> processes) {
        ManyMap<String, Process> processDefinitionKeyMap = new ManyMap<String, Process>();
        for (Process process : processes) {
            if (process.getEngine() != null && getKey() != null && process.getEngine().equals(getKey()))
                processDefinitionKeyMap.putOne(process.getEngineProcessDefinitionKey(), process);
        }

        ManyMap<String, Process> map = new ManyMap<String, Process>();
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().list()) {
            List<Process> matchingProcesses = processDefinitionKeyMap.get(processDefinition.getKey());
            if (matchingProcesses != null && !matchingProcesses.isEmpty())
                map.put(processDefinition.getId(), matchingProcesses);
        }

        return map;
    }

    @Override
    public Class<ProcessEngineProxy> getType() {
        return ProcessEngineProxy.class;
    }

    @Override
    public String getKey() {
        return "activiti";
    }
}
