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
package piecework.process.concrete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import piecework.Constants;
import piecework.Sanitizer;
import piecework.authorization.AuthorizationRole;
import piecework.authorization.ResourceAuthority;
import piecework.common.view.SearchResults;
import piecework.common.view.ViewContext;
import piecework.exception.ForbiddenError;
import piecework.exception.GoneError;
import piecework.exception.NotFoundError;
import piecework.exception.StatusCodeError;
import piecework.form.FormPosition;
import piecework.form.model.Form;
import piecework.process.ProcessRepository;
import piecework.process.ProcessResource;
import piecework.process.exception.RecordNotFoundException;
import piecework.process.model.Interaction;
import piecework.process.model.Process;
import piecework.security.PassthroughSanitizer;

import com.google.common.collect.Sets;

/**
 * @author James Renfro
 */
@Service
public class ProcessResourceVersion1Impl implements ProcessResource {

	@Autowired
	ProcessRepository repository;
	
	@Autowired
	Sanitizer sanitizer;
	
	@Value("${base.application.uri}")
	String baseApplicationUri;
	
	@Value("${base.service.uri}")
	String baseServiceUri;
	
	@Override
	public Response create(Process process) throws StatusCodeError {
		Process.Builder builder = new Process.Builder(process, sanitizer);
		Process result = repository.save(builder.build());
		
		ResponseBuilder responseBuilder = Response.ok(new Process.Builder(result, new PassthroughSanitizer()).build(getViewContext()));
		return responseBuilder.build();
	}
	
	@Override
	public Response delete(String rawProcessDefinitionKey) throws StatusCodeError {
		// Sanitize all user input
		String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
		
		Process record = repository.findOne(processDefinitionKey);
		if (record == null)
			throw new NotFoundError(Constants.ExceptionCodes.process_does_not_exist, processDefinitionKey);
		
		Process.Builder builder = new Process.Builder(record, sanitizer);
		builder.delete();
		Process result = repository.save(builder.build());

		ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);		
		ViewContext context = getViewContext();
		String location = context != null ? context.getApplicationUri(result.getProcessDefinitionKey()) : null;
		if (location != null)
			responseBuilder.location(UriBuilder.fromPath(location).build());	
		return responseBuilder.build();
	}

	@Override
	public Response update(String rawProcessDefinitionKey, Process process) throws StatusCodeError {
		// Sanitize all user input
		String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
		String includedKey = sanitizer.sanitize(process.getProcessDefinitionKey());	
		
		// If the path param key is not the same as the one that's included in the process, then this put is a rename
		// of the key -- this means we delete the old one and create a new one, assuming that the new one doesn't conflict
		// with an existing key
		if (!processDefinitionKey.equals(includedKey)) {

			// Check for a process with the new key
			Process record = repository.findOne(includedKey);
				
			// This means that a process with that key already exists
			if (record != null)
				throw new ForbiddenError(Constants.ExceptionCodes.process_change_key_duplicate, processDefinitionKey, includedKey);
			
			record = repository.findOne(processDefinitionKey);
			if (record != null && !record.isDeleted()) {
				Process.Builder builder = new Process.Builder(record, sanitizer);
				builder.delete();
				repository.save(builder.build());
			}
		}
		
		Process.Builder builder = new Process.Builder(process, sanitizer);
		Process result = repository.save(builder.build());
		
		ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
		ViewContext context = getViewContext();
		String location = context != null ? context.getApplicationUri(result.getProcessDefinitionKey()) : null;
		if (location != null)
			responseBuilder.location(UriBuilder.fromPath(location).build());	
		
		return responseBuilder.build();
	}

	@Override
	public Response read(String rawProcessDefinitionKey) throws StatusCodeError {
		// Sanitize all user input
		String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
				
		Process result = repository.findOne(processDefinitionKey);
		
		if (result == null)
			throw new NotFoundError();
		if (result.isDeleted())
			throw new GoneError();
				
		ResponseBuilder responseBuilder = Response.ok(new Process.Builder(result, new PassthroughSanitizer()).build(getViewContext()));
		return responseBuilder.build();
	}
	
	@Override
	public SearchResults search(UriInfo uriInfo) throws StatusCodeError {	
		SearchResults.Builder resultsBuilder = new SearchResults.Builder();
		List<Process> processes = findProcesses(AuthorizationRole.OWNER, AuthorizationRole.CREATOR);
		for (Process process : processes) {
			resultsBuilder.item(new Process.Builder(process, sanitizer).build(getViewContext()));
		}
		
		return resultsBuilder.build();
	}

	@Override
	public ViewContext getViewContext() {
		return new ViewContext(baseApplicationUri, baseServiceUri, "v1", "process", "Process");
	}
	
	private List<piecework.process.model.Process> findProcesses(String ... allowedRoles) {
		SecurityContext context = SecurityContextHolder.getContext();
		Collection<? extends GrantedAuthority> authorities = context.getAuthentication().getAuthorities();
		
		Set<String> allowedRoleSet = allowedRoles != null && allowedRoles.length > 0 ? Sets.newHashSet(allowedRoles) : null;
		Set<String> allowedProcessDefinitionKeys = new HashSet<String>();
		if (authorities != null && !authorities.isEmpty()) {
			for (GrantedAuthority authority : authorities) {		
				if (authority instanceof ResourceAuthority) {
					ResourceAuthority resourceAuthority = ResourceAuthority.class.cast(authority);
					if (allowedRoleSet == null || allowedRoleSet.contains(resourceAuthority.getRole()))
						allowedProcessDefinitionKeys.addAll(resourceAuthority.getProcessDefinitionKeys());
				}
			}
		}
		
		List<piecework.process.model.Process> processes = new ArrayList<piecework.process.model.Process>();
		Iterator<Process> iterator = repository.findAll(allowedProcessDefinitionKeys).iterator();
		while (iterator.hasNext()) {
			Process record = iterator.next();
			if (!record.isDeleted())
				processes.add(record);
		}
		
		return processes;
	}
	
	private void addForm(FormPosition position, Form form) throws RecordNotFoundException {
		String processDefinitionKey = form.getProcessDefinitionKey();
		
		if (processDefinitionKey == null)
			throw new RecordNotFoundException(null);
		
		String taskDefinitionKey = form.getTaskDefinitionKey();
		Process record = repository.findOne(processDefinitionKey);
		
		if (record == null)
			throw new RecordNotFoundException(processDefinitionKey);
		
		String formId = form.getId();
		
		// Since the data is coming from storage, use the PassthroughSanitizer
		Process.Builder builder = new Process.Builder(record, new PassthroughSanitizer());
//		switch (position) {
//		case START_REQUEST:
//			builder.startRequestFormIdentifier(formId);
//			break;
//		case START_RESPONSE:
//			builder.startResponseFormIdentifier(formId);
//			break;
//		case TASK_REQUEST:
//			builder.taskRequestFormIdentifier(taskDefinitionKey, formId);
//			break;
//		case TASK_RESPONSE:
//			builder.taskResponseFormIdentifier(taskDefinitionKey, formId);
//			break;
//		}
		
		repository.save(builder.build());
	}
}