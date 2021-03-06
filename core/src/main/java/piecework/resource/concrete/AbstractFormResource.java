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

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import piecework.Constants;
import piecework.Versions;
import piecework.form.FormDisposition;
import piecework.model.RequestDetails;
import piecework.enumeration.ActionType;
import piecework.enumeration.DataInjectionStrategy;
import piecework.exception.*;
import piecework.form.FormFactory;
import piecework.handler.RequestHandler;
import piecework.identity.IdentityHelper;
import piecework.model.*;
import piecework.model.Process;
import piecework.persistence.ContentRepository;
import piecework.persistence.DeploymentRepository;
import piecework.security.Sanitizer;
import piecework.security.SecuritySettings;
import piecework.service.DeploymentService;
import piecework.service.FormService;
import piecework.service.UserInterfaceService;
import piecework.ui.streaming.StreamingPageContent;
import piecework.validation.FormValidation;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @author James Renfro
 */
public abstract class AbstractFormResource {

    private static final Logger LOG = Logger.getLogger(AbstractFormResource.class);

    @Autowired
    DeploymentService deploymentService;

    @Autowired
    FormFactory formFactory;

    @Autowired
    FormService formService;

    @Autowired
    IdentityHelper helper;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    protected Sanitizer sanitizer;

    @Autowired
    SecuritySettings securitySettings;

    @Autowired
    UserInterfaceService userInterfaceService;

    @Autowired
    Versions versions;

    protected abstract boolean isAnonymous();

    protected Response startForm(MessageContext context, Process process) throws StatusCodeError {
        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest request = requestHandler.create(requestDetails, process);

        return response(process, request);
    }

    protected Response requestForm(MessageContext context, Process process, String rawRequestId) throws StatusCodeError {
        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        String requestId = sanitizer.sanitize(rawRequestId);
        FormRequest request = requestHandler.handle(requestDetails, requestId);

        return response(process, request);
    }

    protected Response taskForm(MessageContext context, Process process, String rawTaskId) throws StatusCodeError {
        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        String taskId = sanitizer.sanitize(rawTaskId);
        FormRequest request = requestHandler.create(requestDetails, process, taskId, null);

        return response(process, request);
    }

    protected SearchResults search(MultivaluedMap<String, String> rawQueryParameters) throws StatusCodeError {
        Entity principal = helper.getPrincipal();
        return formService.search(rawQueryParameters, principal);
    }

    protected Response saveForm(MessageContext context, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        if (formRequest == null) {
            LOG.error("Forbidden: Attempting to save a form for a request id that doesn't exist");
            throw new ForbiddenError(Constants.ExceptionCodes.insufficient_permission);
        }

        return redirect(formService.saveForm(process, formRequest, body));
    }

    protected Response submitForm(MessageContext context, Process process, String rawRequestId, MultipartBody body) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        if (formRequest == null) {
            LOG.error("Forbidden: Attempting to submit a form for a request id that doesn't exist");
            throw new ForbiddenError(Constants.ExceptionCodes.insufficient_permission);
        }

        try {
            return redirect(formService.submitForm(process, formRequest, requestDetails, body));
        } catch (Exception e) {
            FormValidation validation = null;
            Explanation explanation = null;

            if (e instanceof BadRequestError)
                validation = ((BadRequestError)e).getValidation();
            else {
                String detail = e.getMessage() != null ? e.getMessage() : "";
                explanation = new Explanation();
                explanation.setMessage("Unable to complete task");
                explanation.setMessageDetail("Caught an unexpected exception trying to process form submission " + detail);
            }

            Map<String, List<Message>> results = validation != null ? validation.getResults() : null;

            if (results != null && !results.isEmpty()) {
                for (Map.Entry<String, List<Message>> result : results.entrySet()) {
                    LOG.warn("Validation error " + result.getKey() + " : " + result.getValue().iterator().next().getText());
                }
            }

            List<MediaType> acceptableMediaTypes = requestDetails.getAcceptableMediaTypes();
            boolean isJSON = acceptableMediaTypes.size() == 1 && acceptableMediaTypes.contains(MediaType.APPLICATION_JSON_TYPE);

            if (isJSON && e instanceof BadRequestError)
                throw (BadRequestError)e;

            FormRequest invalidRequest = requestHandler.create(requestDetails, process, formRequest.getInstance(), formRequest.getTask(), ActionType.CREATE, validation);
            return response(process, invalidRequest, ActionType.CREATE, MediaType.TEXT_HTML_TYPE, validation, explanation);
        }
    }

    protected Response validateForm(MessageContext context, Process process, MultipartBody body, String rawRequestId, String rawValidationId) throws StatusCodeError {
        String requestId = sanitizer.sanitize(rawRequestId);
        String validationId = sanitizer.sanitize(rawValidationId);

        if (StringUtils.isEmpty(requestId))
            throw new ForbiddenError(Constants.ExceptionCodes.request_id_required);

        RequestDetails requestDetails = new RequestDetails.Builder(context, securitySettings).build();
        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);
        if (formRequest == null) {
            LOG.error("Forbidden: Attempting to validate a form for a request id that doesn't exist");
            throw new ForbiddenError(Constants.ExceptionCodes.insufficient_permission);
        }
        formService.validateForm(process, formRequest, body, validationId);

        return Response.noContent().build();
    }

    private Response redirect(FormRequest formRequest) throws StatusCodeError {
        if (formRequest == null)
            throw new InternalServerError(Constants.ExceptionCodes.process_is_misconfigured);

        return Response.status(Response.Status.SEE_OTHER).header(HttpHeaders.LOCATION, versions.getVersion1().getApplicationUri(Form.Constants.ROOT_ELEMENT_NAME, formRequest.getProcessDefinitionKey(), "page", formRequest.getRequestId())).build();
    }

    private Response response(Process process, FormRequest request) throws StatusCodeError {
        return response(process, request, request.getAction(), MediaType.TEXT_HTML_TYPE, null, null);
    }

    private Response response(Process process, FormRequest request, ActionType actionType, MediaType mediaType, FormValidation validation, Explanation explanation) throws StatusCodeError {
        if (!request.validate(process))
            throw new BadRequestError();

        Entity principal = helper.getPrincipal();
        try {
            ProcessDeployment deployment = deploymentService.read(process, request.getInstance());
            Form form = formFactory.form(process, deployment, request, actionType, principal, mediaType, validation, explanation, isAnonymous());
            FormDisposition formDisposition = form.getDisposition();

            switch (formDisposition.getType()) {
                case REMOTE:
                    return Response.seeOther(formDisposition.getUri()).build();
                case CUSTOM:
                    return Response.ok(userInterfaceService.getCustomPageAsStreaming(form), MediaType.TEXT_HTML_TYPE).build();
            }

            return Response.ok(form).build();
        } catch (IOException ioe) {
            LOG.error("IOException serving page", ioe);
            throw new InternalServerError(Constants.ExceptionCodes.process_is_misconfigured);
        } catch (MisconfiguredProcessException mpe) {
            LOG.error("Process is misconfigured", mpe);
            throw new InternalServerError(Constants.ExceptionCodes.process_is_misconfigured);
        } catch (FormBuildingException fbe) {
            LOG.error("Unable to build form", fbe);
            throw new InternalServerError(Constants.ExceptionCodes.process_is_misconfigured);
        }
    }


}
