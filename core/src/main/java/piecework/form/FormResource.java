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
package piecework.form;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import piecework.Resource;
import piecework.authorization.AuthorizationRole;
import piecework.exception.StatusCodeError;
import piecework.form.model.view.FormView;

/**
 * @author James Renfro
 */
@Path("secure/form")
@Produces("text/html")
public interface FormResource extends Resource {
	
	@GET
	@Path("{processDefinitionKey}")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.INITIATOR})
    Response read(@PathParam("processDefinitionKey") String processDefinitionKey, @Context HttpServletRequest request) throws StatusCodeError;
	
	@GET
	@Path("{processDefinitionKey}/current/{processInstanceId}")
	@RolesAllowed({AuthorizationRole.USER})
	FormView read(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("processInstanceId") String processInstanceId) throws StatusCodeError;

	@POST
	@Path("{processDefinitionKey}")
	@RolesAllowed({AuthorizationRole.INITIATOR})
	@Consumes("application/x-www-form-urlencoded")
	FormView submit(@PathParam("processDefinitionKey") String processDefinitionKey, MultivaluedMap<String, String> formData) throws StatusCodeError;
	
	@POST
	@Path("{processDefinitionKey}/{processBusinessKey}")
	@RolesAllowed({AuthorizationRole.INITIATOR})
	@Consumes("application/x-www-form-urlencoded")
	FormView submit(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("processBusinessKey") String processBusinessKey, MultivaluedMap<String, String> formData) throws StatusCodeError;

}
