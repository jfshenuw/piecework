package piecework.engine;

import piecework.engine.exception.ProcessEngineException;
import piecework.enumeration.ActionType;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.ProcessInstanceSearchCriteria;
import piecework.task.TaskCriteria;
import piecework.task.TaskResults;
import piecework.validation.FormValidation;

import java.util.Map;

/**
 * @author James Renfro
 */
public interface ProcessEngineCapabilities {

    String start(Process process, ProcessInstance instance) throws ProcessEngineException;

    boolean activate(Process process, ProcessInstance instance) throws ProcessEngineException;

    boolean assign(Process process, String taskId, User user) throws ProcessEngineException;

    boolean cancel(Process process, ProcessInstance instance) throws ProcessEngineException;

    boolean suspend(Process process, ProcessInstance instance) throws ProcessEngineException;

    ProcessExecution findExecution(ProcessInstanceSearchCriteria criteria) throws ProcessEngineException;

    ProcessExecutionResults findExecutions(ProcessInstanceSearchCriteria criteria) throws ProcessEngineException;

    Task findTask(TaskCriteria ... criterias) throws ProcessEngineException;

    TaskResults findTasks(TaskCriteria ... criterias) throws ProcessEngineException;

    boolean completeTask(Process process, String taskId, ActionType action, FormValidation validation) throws ProcessEngineException;

    void deploy(Process process, ProcessDeployment deployment) throws ProcessEngineException;

}
