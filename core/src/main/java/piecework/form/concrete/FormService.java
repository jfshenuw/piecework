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
package piecework.form.concrete;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import piecework.Constants;
import piecework.authorization.AuthorizationRole;
import piecework.common.RequestDetails;
import piecework.common.view.SearchResults;
import piecework.common.view.ViewContext;
import piecework.engine.ProcessEngineRuntimeFacade;
import piecework.exception.*;
import piecework.form.handler.AttachmentHandler;
import piecework.identity.InternalUserDetails;
import piecework.task.TaskCriteria;
import piecework.task.TaskResults;
import piecework.engine.exception.ProcessEngineException;
import piecework.form.handler.RequestHandler;
import piecework.form.handler.ResponseHandler;
import piecework.form.validation.FormValidation;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.ProcessInstancePayload;
import piecework.process.ProcessInstanceService;
import piecework.process.concrete.ResourceHelper;
import piecework.security.Sanitizer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * This "service" is really just to abstract logic that is shared between the two different form resources.
 *
 * @author James Renfro
 */
@Service
public class FormService {

    private static final Set<String> VALID_REQUEST_TYPES = Sets.newHashSet("submission", "task");
    private static final Logger LOG = Logger.getLogger(FormService.class);

    @Autowired
    Environment environment;

    @Autowired
    ProcessEngineRuntimeFacade facade;

    @Autowired
    ResourceHelper helper;

    @Autowired
    ProcessInstanceService processInstanceService;

    @Autowired
    AttachmentHandler attachmentHandler;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    ResponseHandler responseHandler;

    @Autowired
    Sanitizer sanitizer;

    public Response activate(HttpServletRequest request, ViewContext viewContext, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);

        String taskId = formRequest.getTaskId();

        if (StringUtils.isEmpty(taskId))
            throw new ForbiddenError(Constants.ExceptionCodes.task_id_required);

        InternalUserDetails user = helper.getAuthenticatedPrincipal();
        String participantId = user != null ? user.getInternalId() : null;

        try {
            Task task = facade.findTask(new TaskCriteria.Builder().process(process).taskId(taskId).participantId(participantId).build());

            ProcessInstance processInstance = processInstanceService.findOne(process, task.getProcessInstanceId());

            facade.activate(process, processInstance, "test");

        } catch (ProcessEngineException e) {
            LOG.error("Could not activate task", e);
        }

        return Response.noContent().build();
    }

    public Response attach(HttpServletRequest request, ViewContext viewContext, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        // This will guarantee that the request is valid
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        Screen screen = formRequest.getScreen();

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).processInstanceId(formRequest.getProcessInstanceId()).multipartBody(body);

        try {
            String taskId = formRequest.getTaskId();
            Task task = null;
            if (StringUtils.isNotEmpty(taskId)) {
                try {
                    InternalUserDetails user = helper.getAuthenticatedPrincipal();
                    task = facade.findTask(new TaskCriteria.Builder().process(process).participantId(user.getInternalId()).taskId(taskId).build());
                    if (task == null || !task.isActive())
                        throw new ForbiddenError();

                } catch (ProcessEngineException e) {
                    throw new InternalServerError();
                }
            }

            ProcessInstance stored = processInstanceService.attach(process, screen, payload);

            return attachmentHandler.handle(formRequest, viewContext, null);

        } catch (BadRequestError e) {
            FormValidation validation = e.getValidation();
            return responseHandler.handle(formRequest, viewContext, validation);
        }
    }

    public Response delete(HttpServletRequest request, ViewContext viewContext, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);

        String taskId = formRequest.getTaskId();

        if (StringUtils.isEmpty(taskId))
            throw new ForbiddenError(Constants.ExceptionCodes.task_id_required);

        InternalUserDetails user = helper.getAuthenticatedPrincipal();
        String participantId = user != null ? user.getInternalId() : null;

        try {
            Task task = facade.findTask(new TaskCriteria.Builder().process(process).taskId(taskId).participantId(participantId).build());

            ProcessInstance processInstance = processInstanceService.findOne(process, task.getProcessInstanceId());

            facade.cancel(process, processInstance, "Cancelled");

        } catch (ProcessEngineException e) {
            LOG.error("Could not delete task", e);
        }

        return Response.noContent().build();
    }

    public Response suspend(HttpServletRequest request, ViewContext viewContext, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);

        String taskId = formRequest.getTaskId();

        if (StringUtils.isEmpty(taskId))
            throw new ForbiddenError(Constants.ExceptionCodes.task_id_required);

        InternalUserDetails user = helper.getAuthenticatedPrincipal();
        String participantId = user != null ? user.getInternalId() : null;

        try {
            Task task = facade.findTask(new TaskCriteria.Builder().process(process).taskId(taskId).participantId(participantId).build());

            ProcessInstance processInstance = processInstanceService.findOne(process, task.getProcessInstanceId());

            facade.suspend(process, processInstance, "test");

        } catch (ProcessEngineException e) {
            LOG.error("Could not suspend task", e);
        }

        return Response.noContent().build();
    }


    public Response provideFormResponse(HttpServletRequest request, ViewContext viewContext, Process process, List<PathSegment> pathSegments) throws StatusCodeError {
        String requestId = null;
        String attachmentId = null;

        boolean isAttachmentResource = false;
        boolean isSubmissionResource;

        if (pathSegments != null && !pathSegments.isEmpty()) {
            Iterator<PathSegment> pathSegmentIterator = pathSegments.iterator();
            requestId = sanitizer.sanitize(pathSegmentIterator.next().getPath());

            isAttachmentResource = StringUtils.isNotEmpty(requestId) && requestId.equals("attachment");
            isSubmissionResource = StringUtils.isNotEmpty(requestId) && requestId.equals("submission");

            if (isAttachmentResource && pathSegmentIterator.hasNext()) {
                requestId = sanitizer.sanitize(pathSegmentIterator.next().getPath());

                if (pathSegmentIterator.hasNext())
                    attachmentId = sanitizer.sanitize(pathSegmentIterator.next().getPath());
            } else if (isSubmissionResource && pathSegmentIterator.hasNext()) {
                requestId = sanitizer.sanitize(pathSegmentIterator.next().getPath());
            }
        }

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();

        FormRequest formRequest = null;

        if (StringUtils.isNotEmpty(requestId)) {
            try {
                formRequest = requestHandler.create(requestDetails, process, null, requestId, null);
            } catch (NotFoundError e) {
                formRequest = requestHandler.handle(requestDetails, requestId);
            }
        } else
            formRequest = requestHandler.create(requestDetails, process);

        if (formRequest.getProcessDefinitionKey() == null || process.getProcessDefinitionKey() == null || !formRequest.getProcessDefinitionKey().equals(process.getProcessDefinitionKey()))
            throw new BadRequestError();

        if (isAttachmentResource)
            return attachmentHandler.handle(formRequest, viewContext, attachmentId);

        return responseHandler.handle(formRequest, viewContext);
    }

    public SearchResults search(MultivaluedMap<String, String> rawQueryParameters, ViewContext viewContext) throws StatusCodeError {

        Set<Process> allowedProcesses = helper.findProcesses(AuthorizationRole.USER);

        TaskCriteria.Builder executionCriteriaBuilder = new TaskCriteria.Builder(allowedProcesses, rawQueryParameters, sanitizer);

        InternalUserDetails user = helper.getAuthenticatedPrincipal();
        executionCriteriaBuilder.participantId(user.getInternalId());

        SearchResults.Builder resultsBuilder = new SearchResults.Builder()
                .resourceLabel("Tasks")
                .resourceName(Form.Constants.ROOT_ELEMENT_NAME)
                .link(viewContext.getApplicationUri());

        Set<String> allowedProcessDefinitionKeys = Sets.newHashSet();
        if (!allowedProcesses.isEmpty()) {
            for (Process allowedProcess : allowedProcesses) {
                if (StringUtils.isEmpty(allowedProcess.getProcessDefinitionKey()))
                    continue;

                allowedProcessDefinitionKeys.add(allowedProcess.getProcessDefinitionKey());
                resultsBuilder.definition(new Form.Builder().processDefinitionKey(allowedProcess.getProcessDefinitionKey()).task(new Task.Builder().processDefinitionKey(allowedProcess.getProcessDefinitionKey()).processDefinitionLabel(allowedProcess.getProcessDefinitionLabel()).build(viewContext)).build(viewContext));
            }
        }
        TaskCriteria executionCriteria = executionCriteriaBuilder.build();

        try {
            TaskResults results = facade.findTasks(executionCriteria);

            List<Task> tasks = results.getTasks();
            if (tasks != null && !tasks.isEmpty()) {

                for (Task task : tasks) {
                    resultsBuilder.item(new Form.Builder()
                            .formInstanceId(task.getTaskInstanceId())
                            .task(task)
                            .processDefinitionKey(task.getProcessDefinitionKey())
                            .build(viewContext));
                }
            }

            resultsBuilder.firstResult(results.getFirstResult());
            resultsBuilder.maxResults(results.getMaxResults());
            resultsBuilder.total(Long.valueOf(results.getTotal()));

        } catch (ProcessEngineException e) {
            LOG.error("Could not find tasks", e);
        }

        return resultsBuilder.build(viewContext);
    }

    public Response submitForm(HttpServletRequest request, ViewContext viewContext, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        // This will guarantee that the request is valid
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        Screen screen = formRequest.getScreen();

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).processInstanceId(formRequest.getProcessInstanceId()).multipartBody(body);

        try {
            String taskId = formRequest.getTaskId();
            Task task = null;
            if (StringUtils.isNotEmpty(taskId)) {
                try {
                    InternalUserDetails user = helper.getAuthenticatedPrincipal();
                    task = facade.findTask(new TaskCriteria.Builder().process(process).participantId(user.getInternalId()).taskId(taskId).build());
                    if (task == null || !task.isActive())
                        throw new ForbiddenError();

                } catch (ProcessEngineException e) {
                    throw new InternalServerError();
                }
            }

            ProcessInstance stored = processInstanceService.submit(process, screen, payload);

            if (task != null) {
                try {
                    String actionValue = null;
                    if (payload.getFormData() != null) {
                        List<String> actionValues = payload.getFormData().get("actionButton");
                        actionValue = actionValues != null && !actionValues.isEmpty() ? actionValues.get(0) : null;
                    }

                    facade.completeTask(process, task.getTaskInstanceId(), actionValue);
                } catch (ProcessEngineException e) {
                    throw new InternalServerError();
                }
            }

            List<FormValue> formValues = stored != null ? stored.getFormData() : new ArrayList<FormValue>();
            FormRequest nextFormRequest = null;

            if (!formRequest.getSubmissionType().equals(Constants.SubmissionTypes.FINAL))
                nextFormRequest = requestHandler.create(requestDetails, process, stored, Task.class.cast(null), formRequest);

            // FIXME: If the request handler doesn't have another request to process, then provide the generic thank you page back to the user
            if (nextFormRequest == null) {
                return Response.noContent().build();
            }

            return responseHandler.redirect(nextFormRequest, viewContext);

        } catch (BadRequestError e) {
            FormValidation validation = e.getValidation();
            return responseHandler.handle(formRequest, viewContext, validation);
        }
    }

    public Response validateForm(HttpServletRequest request, ViewContext viewContext, Process process, MultipartBody body, String rawRequestId, String rawValidationId) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);
        String validationId = sanitizer.sanitize(rawValidationId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        // This will guarantee that the request is valid
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        Screen screen = formRequest.getScreen();

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).validationId(validationId).multipartBody(body);

        processInstanceService.validate(process, screen, payload, true);

        return Response.noContent().build();
    }


}
