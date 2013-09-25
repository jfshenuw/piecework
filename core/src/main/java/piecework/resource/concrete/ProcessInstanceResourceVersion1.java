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
package piecework.resource.concrete;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import piecework.*;
import piecework.authorization.AuthorizationRole;
import piecework.common.RequestDetails;
import piecework.enumeration.ActionType;
import piecework.handler.SubmissionHandler;
import piecework.service.ProcessHistoryService;
import piecework.service.ProcessInstanceService;
import piecework.service.ProcessService;
import piecework.service.ValuesService;
import piecework.validation.SubmissionTemplate;
import piecework.validation.SubmissionTemplateFactory;
import piecework.exception.*;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.*;
import piecework.identity.IdentityHelper;
import piecework.resource.ProcessInstanceResource;
import piecework.security.Sanitizer;
import piecework.model.SearchResults;
import piecework.common.ViewContext;
import piecework.handler.RequestHandler;
import piecework.security.SecuritySettings;
import piecework.security.concrete.PassthroughSanitizer;
import piecework.service.TaskService;
import piecework.ui.StreamingAttachmentContent;
import piecework.form.FormFactory;
import piecework.util.ProcessInstanceUtility;

import java.util.List;
import java.util.Map;

/**
 * @author James Renfro
 */
@Service
public class ProcessInstanceResourceVersion1 implements ProcessInstanceResource {

    private static final Logger LOG = Logger.getLogger(ProcessInstanceResourceVersion1.class);

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    Environment environment;

    @Autowired
    IdentityHelper helper;

    @Autowired
    ProcessHistoryService historyService;

    @Autowired
    ProcessService processService;

    @Autowired
    ProcessInstanceService processInstanceService;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    SubmissionHandler submissionHandler;

    @Autowired
    SubmissionTemplateFactory submissionTemplateFactory;

	@Autowired
	Sanitizer sanitizer;

    @Autowired
    SecuritySettings securitySettings;

    @Autowired
    TaskService taskService;

    @Autowired
    ValuesService valuesService;

    @Autowired
    Versions versions;

    @Override
    public Response activate(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawReason) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String reason = sanitizer.sanitize(rawReason);

        if (!helper.hasRole(process, AuthorizationRole.OVERSEER) && !taskService.hasAllowedTask(process, instance, false))
            throw new ForbiddenError(Constants.ExceptionCodes.task_required);

        processInstanceService.activate(process, instance, reason);
        return Response.noContent().build();
    }

    @Override
    public Response activate(String rawProcessDefinitionKey, String rawProcessInstanceId, OperationDetails details) throws StatusCodeError {
        return activate(rawProcessDefinitionKey, rawProcessInstanceId, details.getReason());
    }

    @Override
    public Response attach(String rawProcessDefinitionKey, String rawProcessInstanceId, MultivaluedMap<String, String> formData) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);

        Task task = taskService.allowedTask(process, instance, true);
        if (task == null)
            throw new ForbiddenError();

        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(process);
        Submission submission = submissionHandler.handle(process, template, formData);

        attachmentService.attach(process, instance, task, template, submission);
        return Response.noContent().build();
    }

    @Override
    public Response attach(String rawProcessDefinitionKey, String rawProcessInstanceId, MultipartBody body) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);

        Task task = taskService.allowedTask(process, instance, true);
        if (task == null)
            throw new ForbiddenError();

        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(process);
        Submission submission = submissionHandler.handle(process, template, body);

        attachmentService.attach(process, instance, task, template, submission);
        return Response.noContent().build();
    }

    @Override
    public Response attachments(String rawProcessDefinitionKey, String rawProcessInstanceId, AttachmentQueryParameters queryParameters) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, true);

        if (!taskService.hasAllowedTask(process, instance, false))
            throw new ForbiddenError();

        SearchResults searchResults = attachmentService.search(process, instance, queryParameters);
        return Response.ok(searchResults).build();
    }

    @Override
    public Response attachment(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawAttachmentId) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, true);
        String attachmentId = sanitizer.sanitize(rawAttachmentId);

        if (!taskService.hasAllowedTask(process, instance, true))
            throw new ForbiddenError();

        StreamingAttachmentContent content = attachmentService.content(process, instance, attachmentId);

        if (content == null)
            throw new NotFoundError(Constants.ExceptionCodes.attachment_does_not_exist, attachmentId);

        String contentDisposition = new StringBuilder("attachment; filename=").append(content.getAttachment().getDescription()).toString();
        return Response.ok(content, content.getAttachment().getContentType()).header("Content-Disposition", contentDisposition).build();
    }

    @Override
    public Response cancel(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawReason) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String reason = sanitizer.sanitize(rawReason);

        String currentUserId = helper.getAuthenticatedSystemOrUserId();
        boolean isInitiator = instance.getInitiatorId() != null && currentUserId != null && instance.getInitiatorId().equals(currentUserId);

        if (!isInitiator && !helper.hasRole(process, AuthorizationRole.OVERSEER))
            throw new ForbiddenError(Constants.ExceptionCodes.insufficient_permission);

        processInstanceService.cancel(process, instance, reason);
        return Response.noContent().build();
    }

    @Override
    public Response cancel(String rawProcessDefinitionKey, String rawProcessInstanceId, OperationDetails details) throws StatusCodeError {
        return cancel(rawProcessDefinitionKey, rawProcessInstanceId, details.getReason());
    }

    @Override
	public Response create(MessageContext context, String rawProcessDefinitionKey, Submission rawSubmission) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.create(requestDetails, process);
        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(process, formRequest.getScreen());
        Submission submission = submissionHandler.handle(process, template, rawSubmission, formRequest, ActionType.CREATE);
        ProcessInstance instance = processInstanceService.submit(process, null, null, template, submission);

        return Response.ok(new ProcessInstance.Builder(instance).build(versions.getVersion1())).build();
	}
	
	@Override
	public Response create(MessageContext context, String rawProcessDefinitionKey, MultivaluedMap<String, String> formData) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.create(requestDetails, process);
        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(process, formRequest.getScreen());
        Submission submission = submissionHandler.handle(process, template, formData);
        ProcessInstance instance = processInstanceService.submit(process, null, null, template, submission);

        return Response.ok(new ProcessInstance.Builder(instance).build(versions.getVersion1())).build();
	}

	@Override
	public Response createMultipart(MessageContext context, String rawProcessDefinitionKey, MultipartBody body) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.create(requestDetails, process);
        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(process, formRequest.getScreen());
        Submission submission = submissionHandler.handle(process, template, body, formRequest);
        ProcessInstance instance = processInstanceService.submit(process, null, null, template, submission);

        return Response.ok(new ProcessInstance.Builder(instance).build(versions.getVersion1())).build();
	}

    @Override
    public Response detach(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawAttachmentId) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String attachmentId = sanitizer.sanitize(rawAttachmentId);

        Task task = taskService.allowedTask(process, instance, true);
        if (!helper.isAuthenticatedSystem() && task == null)
            throw new ForbiddenError();

        attachmentService.delete(process, instance, attachmentId);
        return Response.noContent().build();
    }

    @Override
    public Response history(String rawProcessDefinitionKey, String rawProcessInstanceId) throws StatusCodeError {
        History history = historyService.read(rawProcessDefinitionKey, rawProcessInstanceId);
        return Response.ok(history).build();
    }

    @Override
	public Response read(String rawProcessDefinitionKey, String rawProcessInstanceId) throws StatusCodeError {
		String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
		String processInstanceId = sanitizer.sanitize(rawProcessInstanceId);
		
		Process process = processService.read(processDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, processInstanceId, false);

        ProcessInstance.Builder builder = new ProcessInstance.Builder(instance)
                .processDefinitionKey(processDefinitionKey)
                .processDefinitionLabel(process.getProcessDefinitionLabel());

        return Response.ok(builder.build(versions.getVersion1())).build();
	}

    @Override
    public Response suspend(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawReason) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String reason = sanitizer.sanitize(rawReason);

        if (!helper.hasRole(process, AuthorizationRole.OVERSEER) && !taskService.hasAllowedTask(process, instance, true))
            throw new ForbiddenError(Constants.ExceptionCodes.active_task_required);

        processInstanceService.suspend(process, instance, reason);
        return Response.noContent().build();
    }

    @Override
    public Response suspend(String rawProcessDefinitionKey, String rawProcessInstanceId, OperationDetails details) throws StatusCodeError {
        return suspend(rawProcessDefinitionKey, rawProcessInstanceId, details.getReason());
    }

    @Override
    public Response update(String rawProcessDefinitionKey, String rawProcessInstanceId, ProcessInstance instance) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
        String processInstanceId = sanitizer.sanitize(rawProcessInstanceId);

        processInstanceService.update(processDefinitionKey, processInstanceId, instance);

        ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
        ViewContext context = versions.getVersion1();
        String location = context != null ? context.getApplicationUri(instance.getProcessDefinitionKey(), instance.getProcessInstanceId()) : null;
        if (location != null)
            responseBuilder.location(UriBuilder.fromPath(location).build());
        return responseBuilder.build();
    }

    @Override
	public Response delete(String rawProcessDefinitionKey, String rawProcessInstanceId) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String reason = null;

        processInstanceService.cancel(process, instance, reason);

        ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
        ViewContext context = versions.getVersion1();
        String location = context != null ? context.getApplicationUri(instance.getProcessDefinitionKey(), instance.getProcessInstanceId()) : null;
        if (location != null)
            responseBuilder.location(UriBuilder.fromPath(location).build());
        return responseBuilder.build();
	}

	@Override
	public SearchResults search(UriInfo uriInfo) throws StatusCodeError {
		MultivaluedMap<String, String> rawQueryParameters = uriInfo != null ? uriInfo.getQueryParameters() : null;
		return processInstanceService.search(rawQueryParameters);
	}

    @Override
    public Response remove(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawFieldName, String rawValueId) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String fieldName = sanitizer.sanitize(rawFieldName);
        String valueId = sanitizer.sanitize(rawValueId);

        if (!helper.hasRole(process, AuthorizationRole.OVERSEER) && !taskService.hasAllowedTask(process, instance, true))
            throw new ForbiddenError(Constants.ExceptionCodes.active_task_required);

        valuesService.delete(process, instance, fieldName, valueId);
        return Response.noContent().build();
    }

    @Override
    public Response value(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawFieldName, String rawValueId) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String fieldName = sanitizer.sanitize(rawFieldName);
        String valueId = sanitizer.sanitize(rawValueId);

        if (!helper.hasRole(process, AuthorizationRole.OVERSEER) && !taskService.hasAllowedTask(process, instance, true))
            throw new ForbiddenError(Constants.ExceptionCodes.active_task_required);

        return valuesService.read(process, instance, fieldName, valueId);
    }

    @Override
    public Response value(MessageContext context, String rawProcessDefinitionKey, String rawProcessInstanceId, String rawFieldName, MultipartBody body) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String fieldName = sanitizer.sanitize(rawFieldName);

        Task task = taskService.allowedTask(process, instance, true);
        if (task == null && !helper.isAuthenticatedSystem())
            throw new ForbiddenError(Constants.ExceptionCodes.active_task_required);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.create(requestDetails, process, instance, task);

        Screen screen = formRequest.getScreen();

        if (screen == null)
            throw new ConflictError();

        Field field = FormFactory.getField(process, screen, fieldName);
        if (field == null)
            throw new NotFoundError();

        SubmissionTemplate template = submissionTemplateFactory.submissionTemplate(field);
        Submission submission = submissionHandler.handle(process, template, body);
        ProcessInstance stored = processInstanceService.save(process, instance, task, template, submission);

        Map<String, List<Value>> data = stored.getData();

        ViewContext version1 = versions.getVersion1();
        String location = null;
        if (data != null) {
            File file = ProcessInstanceUtility.firstFile(fieldName, data);

            if (file != null) {
                location =
                    new File.Builder(file, new PassthroughSanitizer())
                        .processDefinitionKey(process.getProcessDefinitionKey())
                        .processInstanceId(stored.getProcessInstanceId())
                        .fieldName(fieldName)
                        .build(version1)
                        .getLink();
            }
        }

        ResponseBuilder builder = Response.noContent();

        if (location != null)
            builder.header(HttpHeaders.LOCATION, location);

        return builder.build();
    }

    @Override
    public Response values(String rawProcessDefinitionKey, String rawProcessInstanceId, String rawFieldName) throws StatusCodeError {
        Process process = processService.read(rawProcessDefinitionKey);
        ProcessInstance instance = processInstanceService.read(process, rawProcessInstanceId, false);
        String fieldName = sanitizer.sanitize(rawFieldName);

        if (!helper.hasRole(process, AuthorizationRole.OVERSEER) && !taskService.hasAllowedTask(process, instance, true))
            throw new ForbiddenError(Constants.ExceptionCodes.active_task_required);

        List<Value> files = valuesService.searchValues(process, instance, fieldName);
        SearchResults searchResults = new SearchResults.Builder()
                .items(files)
                .build();

        return Response.ok(searchResults).build();
    }

    @Override
    public String getVersion() {
        return versions.getVersion1().getVersion();
    }

}
