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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import piecework.Constants;
import piecework.common.RequestDetails;
import piecework.common.view.ViewContext;
import piecework.exception.BadRequestError;
import piecework.exception.ForbiddenError;
import piecework.exception.StatusCodeError;
import piecework.form.handler.RequestHandler;
import piecework.form.handler.ResponseHandler;
import piecework.form.validation.FormValidation;
import piecework.model.*;
import piecework.model.Process;
import piecework.process.ProcessInstancePayload;
import piecework.process.ProcessInstanceService;
import piecework.process.ProcessRepository;
import piecework.security.Sanitizer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * This "service" is really just to abstract logic that is shared between the two different form resources.
 *
 * @author James Renfro
 */
@Service
public class FormService {

    private static final Logger LOG = Logger.getLogger(FormService.class);

    @Autowired
    Environment environment;

    @Autowired
    ProcessInstanceService processInstanceService;

    @Autowired
    RequestHandler requestHandler;

    @Autowired
    ResponseHandler responseHandler;

    @Autowired
    Sanitizer sanitizer;


    public Response redirectToNewRequestResponse(HttpServletRequest request, ViewContext viewContext, Process process) throws StatusCodeError {
        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();
        FormRequest formRequest = requestHandler.create(requestDetails, process);

        return responseHandler.redirect(formRequest, viewContext);
    }

    public Response provideFormResponse(HttpServletRequest request, ViewContext viewContext, Process process, List<PathSegment> pathSegments) throws StatusCodeError {
        String requestId = null;

        if (pathSegments != null && !pathSegments.isEmpty()) {
            requestId = sanitizer.sanitize(pathSegments.iterator().next().getPath());
        }

        String certificateIssuerHeader = environment.getProperty("certificate.issuer.header");
        String certificateSubjectHeader = environment.getProperty("certificate.subject.header");
        RequestDetails requestDetails = new RequestDetails.Builder(request, certificateIssuerHeader, certificateSubjectHeader).build();

        FormRequest formRequest = requestHandler.handle(requestDetails, requestId);

        if (formRequest.getProcessDefinitionKey() == null || process.getProcessDefinitionKey() == null || !formRequest.getProcessDefinitionKey().equals(process.getProcessDefinitionKey()))
            throw new BadRequestError();

        return responseHandler.handle(formRequest, viewContext);
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

        ProcessInstancePayload payload = new ProcessInstancePayload().requestDetails(requestDetails).requestId(requestId).multipartBody(body);

        try {
            ProcessInstance stored = processInstanceService.submit(process, screen, payload);
            List<FormValue> formValues = stored != null ? stored.getFormData() : new ArrayList<FormValue>();
            FormRequest nextFormRequest = null;

            if (!formRequest.getSubmissionType().equals(Constants.SubmissionTypes.FINAL))
                nextFormRequest = requestHandler.create(requestDetails, process, stored.getProcessInstanceId(), null, formRequest);

            // FIXME: If the request handler doesn't have another request to process, then provide the generic thank you page back to the user
            if (nextFormRequest == null) {
                return Response.noContent().build();
            }

            return responseHandler.handle(nextFormRequest, viewContext);

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

        processInstanceService.validate(process, screen, payload);

        return Response.noContent().build();
    }


}
