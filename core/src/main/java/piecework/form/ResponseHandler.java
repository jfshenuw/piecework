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
package piecework.form;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import piecework.exception.StatusCodeError;
import piecework.model.Form;
import piecework.model.FormRequest;

import javax.ws.rs.core.Response;


/**
 * @author James Renfro
 */
public class ResponseHandler {

    @Autowired
    ContentRepository contentRepository;

    @Autowired
    GridFsTemplate gridFsTemplate;

    public Response handle(FormRequest formRequest) throws StatusCodeError {

        Form form = new Form.Builder()
                .formInstanceId(formRequest.getRequestId())
                .processDefinitionKey(formRequest.getProcessDefinitionKey())
                .submissionType(formRequest.getSubmissionType())
                .screen(formRequest.getScreen())
                .build();

        String location = formRequest.getScreen().getLocation();

        if (StringUtils.isNotEmpty(location)) {
            // If the location is not blank then delegate to the
            GridFsResource resource = gridFsTemplate.getResource(location);
            String contentType = resource.getContentType();
            return Response.ok(new StreamingPageContent(form, resource), contentType).build();
        }

        return Response.ok(form).build();
    }



}
