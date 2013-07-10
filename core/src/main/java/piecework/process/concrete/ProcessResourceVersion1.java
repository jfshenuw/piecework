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

import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import piecework.Constants;
import piecework.persistence.InteractionRepository;
import piecework.persistence.ScreenRepository;
import piecework.model.Interaction;
import piecework.model.Screen;
import piecework.security.Sanitizer;
import piecework.authorization.AuthorizationRole;
import piecework.model.SearchResults;
import piecework.common.ViewContext;
import piecework.exception.ForbiddenError;
import piecework.exception.GoneError;
import piecework.exception.NotFoundError;
import piecework.exception.StatusCodeError;
import piecework.model.Process;
import piecework.persistence.ProcessRepository;
import piecework.process.ProcessResource;
import piecework.security.concrete.PassthroughSanitizer;

/**
 * @author James Renfro
 */
@Service
public class ProcessResourceVersion1 implements ProcessResource {

	@Autowired
	ProcessRepository repository;

    @Autowired
    InteractionRepository interactionRepository;

    @Autowired
    ScreenRepository screenRepository;
	
	@Autowired
	ResourceHelper helper;
	
	@Autowired
	Sanitizer sanitizer;
	
	@Value("${base.application.uri}")
	String baseApplicationUri;
	
	@Value("${base.service.uri}")
	String baseServiceUri;
	
	@Override
	public Response create(Process rawProcess) throws StatusCodeError {
		Process.Builder builder = new Process.Builder(rawProcess, sanitizer);

        Process process = builder.build();

        PassthroughSanitizer passthroughSanitizer = new PassthroughSanitizer();
        builder = new Process.Builder(process, passthroughSanitizer);
        builder.interactions(null);

        if (process.getInteractions() != null && !process.getInteractions().isEmpty()) {
            for (Interaction interaction : process.getInteractions()) {
                Interaction.Builder interactionBuilder = new Interaction.Builder(interaction, passthroughSanitizer);
                interactionBuilder.screens(null);
                if (interaction.getScreens() != null && !interaction.getScreens().isEmpty()) {
                    for (Screen screen : interaction.getScreens()) {
                        Screen persistedScreen = screenRepository.save(screen);
                        interactionBuilder.screen(persistedScreen);
                    }
                }
                Interaction persistedInteraction = interactionRepository.save(interactionBuilder.build());
                builder.interaction(persistedInteraction);
            }
        }

		Process result = repository.save(builder.build());
		
		ResponseBuilder responseBuilder = Response.ok(new Process.Builder(result, new PassthroughSanitizer()).build(getViewContext()));
		return responseBuilder.build();
	}
	
	@Override
	public Response delete(String rawProcessDefinitionKey) throws StatusCodeError {
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
        PassthroughSanitizer passthroughSanitizer = new PassthroughSanitizer();

		// If the path param key is not the same as the one that's included in the process, then this put is a rename
		// of the key -- this means we delete the old one and create a new one, assuming that the new one doesn't conflict
		// with an existing key
		if (!processDefinitionKey.equals(includedKey)) {

			// Check for a process with the new key
			Process record = repository.findOne(includedKey);
				
			// This means that a process with that key already exists
			if (record != null && !record.isDeleted())
				throw new ForbiddenError(Constants.ExceptionCodes.process_change_key_duplicate, processDefinitionKey, includedKey);
			
			record = repository.findOne(processDefinitionKey);
			if (record != null) {
//				if (record.isEmpty()) {
					// Don't bother to keep old process definitions 
					Process.Builder builder = new Process.Builder(record, passthroughSanitizer);
					repository.delete(builder.build());
//				} else if (!record.isDeleted()) {
//					Process.Builder builder = new Process.Builder(record, passthroughSanitizer);
//					builder.delete();
//					processRepository.save(builder.build());
//				}
			}
		}

        Process.Builder builder = new Process.Builder(process, sanitizer);
        if (process.getInteractions() != null && !process.getInteractions().isEmpty()) {
            for (Interaction interaction : process.getInteractions()) {
                Interaction.Builder interactionBuilder = new Interaction.Builder(interaction, passthroughSanitizer);
                interactionBuilder.screens(null);
                if (interaction.getScreens() != null && !interaction.getScreens().isEmpty()) {
                    for (Screen screen : interaction.getScreens()) {
                        Screen persistedScreen = screenRepository.save(screen);
                        interactionBuilder.screen(persistedScreen);
                    }
                }
                Interaction persistedInteraction = interactionRepository.save(interactionBuilder.build());
                builder.interaction(persistedInteraction);
            }
        }

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
		SearchResults.Builder resultsBuilder = new SearchResults.Builder().resourceLabel("Processes").resourceName(Process.Constants.ROOT_ELEMENT_NAME);
		Set<Process> processes = helper.findProcesses(AuthorizationRole.OWNER, AuthorizationRole.CREATOR);
		int counter = 0;
        for (Process process : processes) {
			resultsBuilder.item(new Process.Builder(process, sanitizer).interactions(null).build(getViewContext()));
		    counter++;
        }

        int firstResult = counter > 0 ? 1 : 0;

        resultsBuilder.firstResult(0);
        resultsBuilder.maxResults(counter);
        resultsBuilder.firstResult(firstResult);
        resultsBuilder.total(Long.valueOf(counter));

		return resultsBuilder.build();
	}

	@Override
	public ViewContext getViewContext() {
		return new ViewContext(baseApplicationUri, baseServiceUri, getVersion(), "process", "Process");
	}

    @Override
    public String getVersion() {
        return "v1";
    }


}
