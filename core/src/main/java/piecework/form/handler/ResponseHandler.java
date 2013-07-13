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
package piecework.form.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import piecework.Constants;
import piecework.common.ViewContext;
import piecework.engine.ProcessEngineRuntimeFacade;
import piecework.form.FormService;
import piecework.task.TaskCriteria;
import piecework.engine.exception.ProcessEngineException;
import piecework.exception.InternalServerError;
import piecework.exception.NotFoundError;
import piecework.exception.StatusCodeError;
import piecework.form.validation.FormValidation;
import piecework.model.*;
import piecework.model.Process;
import piecework.persistence.ProcessInstanceRepository;
import piecework.persistence.ProcessRepository;
import piecework.security.concrete.PassthroughSanitizer;
import piecework.ui.StreamingPageContent;
import piecework.persistence.ContentRepository;
import piecework.util.ConstraintUtil;
import piecework.util.FormDataUtil;
import piecework.util.ManyMap;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;


/**
 * @author James Renfro
 */
@Service
public class ResponseHandler {

    private static final Logger LOG = Logger.getLogger(ResponseHandler.class);

    @Autowired
    ContentRepository contentRepository;

    @Autowired
    Environment environment;

    @Autowired
    ProcessEngineRuntimeFacade facade;

    @Autowired
    FormService formService;

    @Autowired
    ProcessRepository processRepository;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    public Response handle(FormRequest formRequest, ViewContext viewContext) throws StatusCodeError {
        return handle(formRequest, viewContext, null);
    }

    public Response handle(FormRequest formRequest, ViewContext viewContext, FormValidation validation) throws StatusCodeError {

        Form form = buildResponseForm(formRequest, viewContext, validation);

        if (form != null && form.getScreen() != null) {
            String location = form.getScreen().getLocation();

            if (StringUtils.isNotEmpty(location)) {
                // If the location is not blank then retrieve from that location
                Content content;
                if (location.startsWith("classpath:")) {
                    ClassPathResource resource = new ClassPathResource(location.substring("classpath:".length()));
                    try {
                        content = new Content.Builder().inputStream(resource.getInputStream()).contentType("text/html").build();
                    } catch (IOException e) {
                        throw new InternalServerError();
                    }
                } else {
                    content = contentRepository.findByLocation(location);
                }
                return Response.ok(new StreamingPageContent(form, content), content.getContentType()).build();
            }
        }

        return Response.ok(form).build();
    }

    public Form buildResponseForm(FormRequest formRequest, ViewContext viewContext, FormValidation validation) throws StatusCodeError {
        int attachmentCount = 0;
        List<FormValue> formValues = findFormValues(formRequest, validation);
        if (formValues == null && StringUtils.isNotBlank(formRequest.getProcessInstanceId())) {
            ProcessInstance processInstance = processInstanceRepository.findOne(formRequest.getProcessInstanceId());
            formValues = processInstance.getFormData();
            attachmentCount = processInstance.getAttachments() != null ? processInstance.getAttachments().size() : 0;
        }
        Set<String> includedFieldNames = new HashSet<String>();

        Screen screen = formRequest.getScreen();
        Task task = null;

        if (StringUtils.isNotEmpty(formRequest.getTaskId())) {
            Process process = processRepository.findOne(formRequest.getProcessDefinitionKey());
            if (process == null)
                throw new NotFoundError(Constants.ExceptionCodes.process_does_not_exist);

            TaskCriteria criteria = new TaskCriteria.Builder()
                    .process(process)
                    .taskId(formRequest.getTaskId())
                    .build();

            try {
                task = facade.findTask(criteria);
                if (task == null)
                    throw new NotFoundError(Constants.ExceptionCodes.task_does_not_exist);
            } catch (ProcessEngineException e) {
                LOG.error("Process engine unable to find task ", e);
                throw new InternalServerError();
            }

            if (screen == null) {
                Interaction selectedInteraction = selectInteraction(process, task);

                if (selectedInteraction != null && !selectedInteraction.getScreens().isEmpty())
                    screen = selectedInteraction.getScreens().iterator().next();
            }
        }

        PassthroughSanitizer passthroughSanitizer = new PassthroughSanitizer();
        if (screen != null) {
            ManyMap<String, String> formValueMap = FormDataUtil.getFormValueMap(formValues);
            Screen.Builder screenBuilder = new Screen.Builder(screen, passthroughSanitizer, false);

            if (screen.getSections() != null) {
                Map<String, Field> fieldMap = new HashMap<String, Field>();
                for (Section section : screen.getSections()) {
                    if (section.getFields() == null)
                        continue;

                    for (Field field : section.getFields()) {
                        if (field.getName() == null)
                            continue;

                        fieldMap.put(field.getName(), field);
                        includedFieldNames.add(field.getName());
                    }
                }
                for (Section section : screen.getSections()) {
                    Section.Builder sectionBuilder = new Section.Builder(section, passthroughSanitizer, false);

                    for (Field field : section.getFields()) {
                        Field.Builder fieldBuilder = new Field.Builder(field, passthroughSanitizer);

                        List<Constraint> constraints = field.getConstraints();
                        if (constraints != null) {
                            if (!ConstraintUtil.checkAll(Constants.ConstraintTypes.IS_ONLY_VISIBLE_WHEN, fieldMap, formValueMap, constraints))
                                fieldBuilder.invisible();
                            if (ConstraintUtil.hasConstraint(Constants.ConstraintTypes.IS_STATE, constraints))
                                addStateOptions(fieldBuilder);
                            if (ConstraintUtil.hasConstraint(Constants.ConstraintTypes.IS_CONFIRMATION_NUMBER, constraints))
                                addConfirmationNumber(fieldBuilder, formRequest.getProcessInstanceId());
                        }

                        sectionBuilder.field(fieldBuilder.build());
                    }
                    screenBuilder.section(sectionBuilder.build());
                }
            }
            screen = screenBuilder.build();
        }

        List<FormValue> includedFormValues = new ArrayList<FormValue>();
        if (formValues != null) {
            for (FormValue formValue : formValues) {
                if (includedFieldNames.contains(formValue.getName()))
                    includedFormValues.add(new FormValue.Builder(formValue, passthroughSanitizer).build());
            }
        }

        return new Form.Builder()
                .formInstanceId(formRequest.getRequestId())
                .processDefinitionKey(formRequest.getProcessDefinitionKey())
                .submissionType(formRequest.getSubmissionType())
                .formValues(includedFormValues)
                .screen(screen)
                .task(task)
                .attachmentCount(attachmentCount)
                .build(viewContext);
    }

    public Response redirect(FormRequest formRequest, ViewContext viewContext) throws StatusCodeError {
        String hostUri = environment.getProperty("host.uri");
        return Response.status(Response.Status.SEE_OTHER).header(HttpHeaders.LOCATION, hostUri + formService.getFormViewContext().getApplicationUri(formRequest.getProcessDefinitionKey(), formRequest.getRequestId())).build();
    }

    private void addConfirmationNumber(Field.Builder fieldBuilder, String confirmationNumber) {
        String defaultValue = fieldBuilder.getDefaultValue();
        fieldBuilder.defaultValue(defaultValue.replaceAll("\\{ConfirmationNumber\\}", confirmationNumber));
    }

    private void addStateOptions(Field.Builder fieldBuilder) {
        fieldBuilder.option(new Option.Builder().value("").name("").build())
            .option(new Option.Builder().value("AL").name("Alabama").build())
            .option(new Option.Builder().value("AK").name("Alaska").build())
            .option(new Option.Builder().value("AZ").name("Arizona").build())
            .option(new Option.Builder().value("AR").name("Arkansas").build())
            .option(new Option.Builder().value("CA").name("California").build())
            .option(new Option.Builder().value("CO").name("Colorado").build())
            .option(new Option.Builder().value("CT").name("Connecticut").build())
            .option(new Option.Builder().value("DE").name("Delaware").build())
            .option(new Option.Builder().value("DC").name("District Of Columbia").build())
            .option(new Option.Builder().value("FL").name("Florida").build())
            .option(new Option.Builder().value("GA").name("Georgia").build())
            .option(new Option.Builder().value("HI").name("Hawaii").build())
            .option(new Option.Builder().value("ID").name("Idaho").build())
            .option(new Option.Builder().value("IL").name("Illinois").build())
            .option(new Option.Builder().value("IN").name("Indiana").build())
            .option(new Option.Builder().value("IA").name("Iowa").build())
            .option(new Option.Builder().value("KS").name("Kansas").build())
            .option(new Option.Builder().value("KY").name("Kentucky").build())
            .option(new Option.Builder().value("LA").name("Louisiana").build())
            .option(new Option.Builder().value("ME").name("Maine").build())
            .option(new Option.Builder().value("MD").name("Maryland").build())
            .option(new Option.Builder().value("MA").name("Massachusetts").build())
            .option(new Option.Builder().value("MI").name("Michigan").build())
            .option(new Option.Builder().value("MN").name("Minnesota").build())
            .option(new Option.Builder().value("MS").name("Mississippi").build())
            .option(new Option.Builder().value("MO").name("Missouri").build())
            .option(new Option.Builder().value("MT").name("Montana").build())
            .option(new Option.Builder().value("NE").name("Nebraska").build())
            .option(new Option.Builder().value("NV").name("Nevada").build())
            .option(new Option.Builder().value("NH").name("New Hampshire").build())
            .option(new Option.Builder().value("NJ").name("New Jersey").build())
            .option(new Option.Builder().value("NM").name("New Mexico").build())
            .option(new Option.Builder().value("NY").name("New York").build())
            .option(new Option.Builder().value("NC").name("North Carolina").build())
            .option(new Option.Builder().value("ND").name("North Dakota").build())
            .option(new Option.Builder().value("OH").name("Ohio").build())
            .option(new Option.Builder().value("OK").name("Oklahoma").build())
            .option(new Option.Builder().value("OR").name("Oregon").build())
            .option(new Option.Builder().value("PA").name("Pennsylvania").build())
            .option(new Option.Builder().value("RI").name("Rhode Island").build())
            .option(new Option.Builder().value("SC").name("South Carolina").build())
            .option(new Option.Builder().value("SD").name("South Dakota").build())
            .option(new Option.Builder().value("TN").name("Tennessee").build())
            .option(new Option.Builder().value("TX").name("Texas").build())
            .option(new Option.Builder().value("UT").name("Utah").build())
            .option(new Option.Builder().value("VT").name("Vermont").build())
            .option(new Option.Builder().value("VA").name("Virginia").build())
            .option(new Option.Builder().value("WA").name("Washington").build())
            .option(new Option.Builder().value("WV").name("West Virginia").build())
            .option(new Option.Builder().value("WI").name("Wisconsin").build())
            .option(new Option.Builder().value("WY").name("Wyoming").build());
    }

    private List<FormValue> findFormValues(FormRequest formRequest, FormValidation validation) {
        List<FormValue> formValues = null;
        if (validation != null && validation.getResults() != null) {
            Map<String, List<String>> validationFormValueMap = validation.getFormValueMap();
            formValues = new ArrayList<FormValue>();
            for (ValidationResult result : validation.getResults()) {
                List<String> values = validationFormValueMap != null ? validationFormValueMap.get(result.getPropertyName()) : null;
                formValues.add(new FormValue.Builder()
                        .name(result.getPropertyName())
                        .values(values)
                        .message(new Message.Builder()
                                .text(result.getMessage())
                                .type(result.getType())
                                .build())
                        .build());
            }
        }

        return formValues;
    }

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

}
