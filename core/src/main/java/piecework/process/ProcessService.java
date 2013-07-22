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
package piecework.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import piecework.Constants;
import piecework.authorization.AuthorizationRole;
import piecework.common.ViewContext;
import piecework.exception.ForbiddenError;
import piecework.exception.GoneError;
import piecework.exception.NotFoundError;
import piecework.exception.StatusCodeError;
import piecework.model.*;
import piecework.model.Process;
import piecework.persistence.InteractionRepository;
import piecework.persistence.ProcessRepository;
import piecework.persistence.ScreenRepository;
import piecework.process.concrete.ResourceHelper;
import piecework.security.Sanitizer;
import piecework.security.concrete.PassthroughSanitizer;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author James Renfro
 */
@Service
public class ProcessService {

    @Autowired
    Environment environment;

    @Autowired
    ResourceHelper helper;

    @Autowired
    ProcessRepository processRepository;

    @Autowired
    InteractionRepository interactionRepository;

    @Autowired
    ScreenRepository screenRepository;

    @Autowired
    Sanitizer sanitizer;

    public Process create(Process rawProcess) {
        piecework.model.Process.Builder builder = new Process.Builder(rawProcess, sanitizer);

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

        return processRepository.save(builder.build());
    }

    public Process delete(String rawProcessDefinitionKey) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);

        Process record = processRepository.findOne(processDefinitionKey);
        if (record == null)
            throw new NotFoundError(Constants.ExceptionCodes.process_does_not_exist, processDefinitionKey);

        Process.Builder builder = new Process.Builder(record, sanitizer);
        builder.delete();
        return processRepository.save(builder.build());
    }

    public Process read(String rawProcessDefinitionKey) throws StatusCodeError {
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);

        Process result = processRepository.findOne(processDefinitionKey);

        if (result == null)
            throw new NotFoundError();
        if (result.isDeleted())
            throw new GoneError();

        return result;
    }

    public SearchResults search(MultivaluedMap<String, String> queryParameters) {
        List<Process> results;
        Set<Process> processes = helper.findProcesses(AuthorizationRole.OWNER, AuthorizationRole.CREATOR);

        if (processes != null && !processes.isEmpty()) {
            results = new ArrayList<Process>(processes.size());
            for (Process process : processes) {
                results.add(new Process.Builder(process, sanitizer).interactions(null).build(getProcessViewContext()));
            }
        } else {
            results = Collections.emptyList();
        }

        SearchResults.Builder resultsBuilder = new SearchResults.Builder().resourceLabel("Processes").resourceName(Process.Constants.ROOT_ELEMENT_NAME);

        int firstResult = processes.size() > 0 ? 1 : 0;
        resultsBuilder.items(results);
        resultsBuilder.firstResult(0);
        resultsBuilder.maxResults(processes.size());
        resultsBuilder.firstResult(firstResult);
        resultsBuilder.total(Long.valueOf(processes.size()));

        return resultsBuilder.build();
    }


    public Process update(String rawProcessDefinitionKey, Process rawProcess) throws StatusCodeError {
        // Sanitize all user input
        String processDefinitionKey = sanitizer.sanitize(rawProcessDefinitionKey);
        String includedKey = sanitizer.sanitize(rawProcess.getProcessDefinitionKey());
        PassthroughSanitizer passthroughSanitizer = new PassthroughSanitizer();

        // If the path param key is not the same as the one that's included in the process, then this put is a rename
        // of the key -- this means we delete the old one and create a new one, assuming that the new one doesn't conflict
        // with an existing key
        if (!processDefinitionKey.equals(includedKey)) {

            // Check for a process with the new key
            Process record = processRepository.findOne(includedKey);

            // This means that a process with that key already exists
            if (record != null && !record.isDeleted())
                throw new ForbiddenError(Constants.ExceptionCodes.process_change_key_duplicate, processDefinitionKey, includedKey);

            record = processRepository.findOne(processDefinitionKey);
            if (record != null) {
                Process.Builder builder = new Process.Builder(record, passthroughSanitizer);
                processRepository.delete(builder.build());
            }
        }

        Process.Builder builder = new Process.Builder(rawProcess, sanitizer);
        builder.clearInteractions();
        if (rawProcess.getInteractions() != null && !rawProcess.getInteractions().isEmpty()) {
            for (Interaction interaction : rawProcess.getInteractions()) {
                Interaction.Builder interactionBuilder = new Interaction.Builder(interaction, sanitizer);
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

        return processRepository.save(builder.build());
    }

    public ViewContext getProcessViewContext() {
        String baseApplicationUri = environment.getProperty(Constants.Settings.BASE_APPLICATION_URI);
        String baseServiceUri = environment.getProperty(Constants.Settings.BASE_SERVICE_URI);
        return new ViewContext(baseApplicationUri, baseServiceUri, getVersion(), "process", "Process");
    }

    public String getVersion() {
        return "v1";
    }

}
