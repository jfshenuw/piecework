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
package piecework.form.concrete;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import piecework.Constants;
import piecework.engine.TaskCriteria;
import piecework.engine.exception.ProcessEngineException;
import piecework.security.Sanitizer;
import piecework.common.view.ViewContext;
import piecework.engine.ProcessEngineRuntimeFacade;
import piecework.exception.*;
import piecework.form.*;
import piecework.form.validation.FormValidation;
import piecework.form.validation.ValidationService;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.*;
import piecework.security.concrete.PassthroughSanitizer;

/**
 * @author James Renfro
 */
@Service
public class FormResourceVersion1 implements FormResource {

    private static final Logger LOG = Logger.getLogger(FormResourceVersion1.class);

    @Autowired
    ProcessEngineRuntimeFacade facade;

    @Autowired
    ProcessRepository processRepository;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    ResponseHandler responseHandler;

    @Autowired
    SubmissionHandler submissionHandler;
	
	@Autowired
    Sanitizer sanitizer;

    @Autowired
    ValidationService validationService;

    @Value("${base.application.uri}")
    String baseApplicationUri;

    @Value("${base.service.uri}")
    String baseServiceUri;

	public Response read(final String rawProcessDefinitionKey, HttpServletRequest request) throws StatusCodeError {
		String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);

        Process process = processRepository.findOne(processDefinitionKey);

        if (process == null)
            throw new NotFoundError(Constants.ExceptionCodes.process_does_not_exist);

        List<Interaction> interactions = process.getInteractions();

        if (interactions == null || interactions.isEmpty())
            throw new InternalServerError();

        // Pick the first interaction and the first screen
        Interaction interaction = interactions.iterator().next();

        FormRequest formRequest = requestHandler.create(request, processDefinitionKey, null, interaction, null);

        return responseHandler.handle(formRequest);
	}

    @Override
    public Response read(String rawProcessDefinitionKey, String rawTaskId, HttpServletRequest request) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
        String taskId = sanitizer.sanitize(rawTaskId);

        Process process = processRepository.findOne(processDefinitionKey);

        if (process == null)
            throw new NotFoundError(Constants.ExceptionCodes.process_does_not_exist);

        TaskCriteria criteria = new TaskCriteria.Builder()
                .engine(process.getEngine())
                .engineProcessDefinitionKey(process.getEngineProcessDefinitionKey())
                .taskId(taskId)
                .build();

        try {
            Task task = facade.findTask(criteria);

            Interaction selectedInteraction = null;
            List<Interaction> interactions = process.getInteractions();
            if (interactions != null && !interactions.isEmpty()) {
                for (Interaction interaction : interactions) {
                     if (interaction.getTaskDefinitionKeys().contains(task.getTaskDefinitionKey())) {
                         selectedInteraction = interaction;
                         break;
                     }
                }
            }

            FormRequest formRequest = requestHandler.create(request, processDefinitionKey, null, selectedInteraction, null);

            return responseHandler.handle(formRequest);

        } catch (ProcessEngineException e) {
            LOG.error("Process engine unable to find task ", e);
            throw new InternalServerError();
        }
    }

    @Override
    public Response submit(@PathParam("processDefinitionKey") String rawProcessDefinitionKey, String rawRequestId, HttpServletRequest request, MultipartBody body) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
        String requestId = sanitizer.sanitize(rawRequestId);

        Process process = processRepository.findOne(processDefinitionKey);

        if (process == null)
            throw new ForbiddenError(Constants.ExceptionCodes.process_does_not_exist);

        FormRequest formRequest = requestHandler.handle(request, requestId);

        ProcessInstancePayload payload = new ProcessInstancePayload().multipartBody(body);
        FormSubmission submission = submissionHandler.handle(formRequest, payload);

        ProcessInstance instance = null;
        if (formRequest.getProcessInstanceId() != null)
            instance = processInstanceRepository.findOne(formRequest.getProcessInstanceId());

        FormValidation validation = validationService.validate(submission, instance, formRequest.getScreen(), null);

        List<ValidationResult> results = validation.getResults();
        if (results != null && !results.isEmpty()) {
            throw new BadRequestError(new ValidationResultList(results));
        }

        ProcessInstance previous = formRequest.getProcessInstanceId() != null ? processInstanceRepository.findOne(formRequest.getProcessInstanceId()) : null;

        ProcessInstance.Builder instanceBuilder;

        if (previous != null) {
            instanceBuilder = new ProcessInstance.Builder(previous, new PassthroughSanitizer());

        } else {
            try {
                String engineInstanceId = facade.start(process, null, validation.getFormValueMap());

                instanceBuilder = new ProcessInstance.Builder()
                        .processDefinitionKey(process.getProcessDefinitionKey())
                        .processDefinitionLabel(process.getProcessDefinitionLabel())
                        .processInstanceLabel(validation.getTitle())
                        .engineProcessInstanceId(engineInstanceId);

            } catch (ProcessEngineException e) {
                LOG.error("Process engine unable to start instance ", e);
                throw new InternalServerError();
            }
        }

        instanceBuilder.formValueMap(validation.getFormValueMap())
                .restrictedValueMap(validation.getRestrictedValueMap())
                .submission(submission);

        ProcessInstance stored = processInstanceRepository.save(instanceBuilder.build());

        List<Interaction> interactions = process.getInteractions();

        Interaction interaction = formRequest.getInteraction();

        if (interaction == null)
            throw new InternalServerError();

        List<Screen> screens = interaction.getScreens();

        if (screens == null || screens.isEmpty())
            throw new InternalServerError();

        FormRequest nextFormRequest = null;

        if (!formRequest.getSubmissionType().equals(Constants.SubmissionTypes.FINAL))
            nextFormRequest = requestHandler.create(request, processDefinitionKey, stored.getProcessInstanceId(), interaction, formRequest.getScreen());

        // FIXME: If the request handler doesn't have another request to process, then provide the generic thank you page back to the user
        if (nextFormRequest == null) {

            return Response.noContent().build();
        }

        return responseHandler.handle(nextFormRequest);
    }

    @Override
    public Response validate(@PathParam("processDefinitionKey") String rawProcessDefinitionKey, @PathParam("requestId") String rawRequestId, @PathParam("validationId") String validationId, @Context HttpServletRequest request, MultipartBody body) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
        String requestId = sanitizer.sanitize(rawRequestId);

        Process process = processRepository.findOne(processDefinitionKey);

        if (process == null)
            throw new ForbiddenError(Constants.ExceptionCodes.process_does_not_exist);

        FormRequest formRequest = requestHandler.handle(request, requestId);

        ProcessInstance instance = null;
        if (formRequest.getProcessInstanceId() != null)
            instance = processInstanceRepository.findOne(formRequest.getProcessInstanceId());

        ProcessInstancePayload payload = new ProcessInstancePayload().multipartBody(body);
        FormSubmission submission = submissionHandler.handle(formRequest, payload);

        FormValidation validation = validationService.validate(submission, instance, formRequest.getScreen(), validationId);

        List<ValidationResult> results = validation.getResults();
        if (results != null && !results.isEmpty()) {
            throw new BadRequestError(new ValidationResultList(results));
        }

        return Response.noContent().build();
    }
	
	@Override
	public ViewContext getViewContext() {
		return new ViewContext(baseApplicationUri, baseServiceUri, null, "form", "Form");
	}
	
	private void storeAttachments(String processDefinitionKey, String processInstanceId, String submissionId) throws StatusCodeError {
//		List<AttachmentReference> attachments = listAttachments(processDefinitionKey, submissionId);
//		if (attachments != null && !attachments.isEmpty()) {
//			for (AttachmentReference attachment : attachments) {
//				String label = attachment.getLabel();
//				String description = attachment.getDescription();
//				String contentType = attachment.getContentType();				
//				String taskId = null;
//				String url = attachment.getLocation();
//				
//				capability.attach().storeAttachment(namespace, processDefinitionKey, processInstanceId, taskId, label, description, contentType, userId, url);
//			}
//		}
	}
	
	private String getSubmissionId(Map<String, List<String>> parameters) {
		List<String> submissionIds = parameters.get("_submissionId");
		if (submissionIds != null && !submissionIds.isEmpty()) {
			String submissionId = submissionIds.get(0);
			return submissionId;
		}
		return null;
	}
}
