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
package piecework.resource;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import piecework.Resource;
import piecework.authorization.AuthorizationRole;
import piecework.model.SearchResults;
import piecework.exception.StatusCodeError;
import piecework.model.Screen;

/**
 * @author James Renfro
 */
@Path("screen/{processDefinitionKey}/{interactionId}")
public interface ScreenResource extends Resource {
	
	@POST
	@Path("")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.CREATOR})
	Response create(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("interactionId") String interactionId, Screen screen) throws StatusCodeError;
	
	@GET
	@Path("{interactionId}")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.CREATOR})
	Response read(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("interactionId") String interactionId, @PathParam("screenId") String screenId) throws StatusCodeError;
	
	@PUT
	@Path("{interactionId}")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.CREATOR})
	Response update(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("interactionId") String interactionId, @PathParam("screenId") String screenId, Screen screen) throws StatusCodeError;
	
	@DELETE
	@Path("{interactionId}")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.CREATOR})
	Response delete(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("interactionId") String interactionId, @PathParam("screenId") String screenId) throws StatusCodeError;
	
	@GET
	@Path("")
	@RolesAllowed({AuthorizationRole.OWNER, AuthorizationRole.CREATOR})
	SearchResults searchInteractions(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("interactionId") String interactionId, @Context UriInfo uriInfo) throws StatusCodeError;

}
