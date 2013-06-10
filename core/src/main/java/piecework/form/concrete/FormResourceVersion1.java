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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import piecework.Constants;
import piecework.common.RequestDetails;
import piecework.engine.TaskCriteria;
import piecework.engine.exception.ProcessEngineException;
import piecework.form.handler.RequestHandler;
import piecework.form.handler.ResponseHandler;
import piecework.security.Sanitizer;
import piecework.common.view.ViewContext;
import piecework.engine.ProcessEngineRuntimeFacade;
import piecework.exception.*;
import piecework.form.*;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.*;

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
    ProcessInstanceService processInstanceService;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    ResponseHandler responseHandler;
	
	@Autowired
    Sanitizer sanitizer;


    @Value("${base.application.uri}")
    String baseApplicationUri;

    @Value("${base.service.uri}")
    String baseServiceUri;

    @Value("${certificate.issuer.header}")
    String certificateIssuerHeader;

    @Value("${certificate.subject.header}")
    String certificateSubjectHeader;


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

        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.create(requestDetails, processDefinitionKey, interaction, null, null);

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
            Interaction selectedInteraction = selectInteraction(process, task);

            RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
            FormRequest formRequest = requestHandler.create(requestDetails, processDefinitionKey, selectedInteraction, null, null);

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

        // Make sure that we're dealing with an existing process and that the request identifier is not empty
        piecework.model.Process process = verifyInputs(processDefinitionKey, requestId);

        // This will guarantee that the request is valid
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        Screen screen = formRequest.getScreen();

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).multipartBody(body);
        ProcessInstance stored = processInstanceService.submit(process, screen, payload);

        Interaction interaction = formRequest.getInteraction();

        if (interaction == null)
            throw new InternalServerError();

        List<Screen> screens = interaction.getScreens();

        if (screens == null || screens.isEmpty())
            throw new InternalServerError();

        FormRequest nextFormRequest = null;

        if (!formRequest.getSubmissionType().equals(Constants.SubmissionTypes.FINAL))
            nextFormRequest = requestHandler.create(requestDetails, processDefinitionKey, interaction, formRequest.getScreen(), stored.getProcessInstanceId());

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

        // Make sure that we're dealing with an existing process and that the request identifier is not empty
        piecework.model.Process process = verifyInputs(processDefinitionKey, requestId);

        // This will guarantee that the request is valid
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        Screen screen = formRequest.getScreen();

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).validationId(validationId).multipartBody(body);

        processInstanceService.validate(process, screen, payload);

        return Response.noContent().build();
    }
	
	@Override
	public ViewContext getViewContext() {
		return new ViewContext(baseApplicationUri, baseServiceUri, null, "form", "Form");
	}
	
//	private void storeAttachments(String processDefinitionKey, String processInstanceId, String submissionId) throws StatusCodeError {
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
//	}
	
//	private String getSubmissionId(Map<String, List<String>> parameters) {
//		List<String> submissionIds = parameters.get("_submissionId");
//		if (submissionIds != null && !submissionIds.isEmpty()) {
//			String submissionId = submissionIds.get(0);
//			return submissionId;
//		}
//		return null;
//	}

    private Interaction selectInteraction(Process process, Task task) {
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
        return selectedInteraction;
    }

    private Process verifyInputs(String processDefinitionKey, String requestId) throws StatusCodeError {
        piecework.model.Process process = processRepository.findOne(processDefinitionKey);

        if (process == null)
            throw new ForbiddenError(Constants.ExceptionCodes.process_does_not_exist);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        return process;
    }
}
