package piecework.process;

import piecework.Resource;
import piecework.authorization.AuthorizationRole;
import piecework.common.view.SearchResults;
import piecework.exception.StatusCodeError;
import piecework.model.ProcessInstance;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

/**
 * @author James Renfro
 */
@Path("secure/v1/instance")
public interface ProcessInstanceResource extends Resource {

    @POST
    @Path("{processDefinitionKey}")
    @RolesAllowed({AuthorizationRole.INITIATOR})
    @Consumes({"application/xml","application/json"})
    Response create(@PathParam("processDefinitionKey") String processDefinitionKey, ProcessInstance instance) throws StatusCodeError;

    @POST
	@Path("{processDefinitionKey}")
	@RolesAllowed({AuthorizationRole.INITIATOR})
	@Consumes("application/x-www-form-urlencoded")
    Response create(@PathParam("processDefinitionKey") String processDefinitionKey, MultivaluedMap<String, String> formData) throws StatusCodeError;
	
	@POST
	@Path("{processDefinitionKey}")
	@RolesAllowed({AuthorizationRole.INITIATOR})
	@Consumes("multipart/form-data")
	Response createMultipart(@PathParam("processDefinitionKey") String processDefinitionKey, MultipartBody body) throws StatusCodeError;
	
    @GET
    @Path("{processDefinitionKey}/{processInstanceId}")
    @RolesAllowed({AuthorizationRole.OVERSEER})
    Response read(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("processInstanceId") String processInstanceId) throws StatusCodeError;

    @DELETE
    @Path("{processDefinitionKey}/{processInstanceId}")
    @RolesAllowed({AuthorizationRole.OWNER})
    Response delete(@PathParam("processDefinitionKey") String processDefinitionKey, @PathParam("processInstanceId") String processInstanceId) throws StatusCodeError;

    @GET
    @Path("")
    @RolesAllowed({AuthorizationRole.OVERSEER})
    SearchResults search(@Context UriInfo uriInfo) throws StatusCodeError;

}
