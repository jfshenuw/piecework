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
package piecework.engine.activiti;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import piecework.Constants;
import piecework.identity.InternalUserDetails;
import piecework.model.*;
import piecework.model.Process;
import piecework.persistence.ProcessInstanceRepository;
import piecework.process.ProcessRepository;
import piecework.util.ManyMap;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * @author James Renfro
 */
@Service("generalUserTaskListener")
public class GeneralUserTaskListener implements TaskListener {

    private static final Logger LOG = Logger.getLogger(GeneralUserTaskListener.class);

    @Autowired
    Environment environment;

    @Autowired
    ProcessRepository processRepository;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    @Autowired
    MongoOperations mongoOperations;

    @Autowired
    UserDetailsService userDetailsService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String engineProcessInstanceId = delegateTask.getProcessInstanceId();
        if (StringUtils.isEmpty(engineProcessInstanceId))
            return;

        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        if (StringUtils.isEmpty(taskDefinitionKey))
            return;

        Map<String, Object> variables = delegateTask.getVariables();
        String processDefinitionKey = String.class.cast(variables.get("PIECEWORK_PROCESS_DEFINITION_KEY"));
        String processInstanceId = String.class.cast(variables.get("PIECEWORK_PROCESS_INSTANCE_ID"));

        ProcessInstance processInstance = processInstanceRepository.findOne(processInstanceId);
        if (processInstance == null)
            return;

        Process process = processRepository.findOne(processDefinitionKey);
        if (process == null)
            return;

        Set<Candidate> approvers = new HashSet<Candidate>();
        Set<Candidate> watchers = new HashSet<Candidate>();

        List<Interaction> interactions = process.getInteractions();
        if (interactions != null) {
            Interaction selectedInteraction = null;
            for (Interaction interaction : interactions) {
                if (interaction.getTaskDefinitionKeys() != null && interaction.getTaskDefinitionKeys().contains(taskDefinitionKey)) {
                    selectedInteraction = interaction;
                    break;
                }
            }

            if (selectedInteraction != null) {
                List<Candidate> candidates = selectedInteraction.getCandidates();
                if (candidates == null)
                    return;

                for (Candidate candidate : candidates) {
                    if (candidate.getCandidateId() == null)
                        continue;
                    if (candidate.getType() == null)
                        continue;

                    if (candidate.getType().equals(Constants.CandidateTypes.PERSON))
                        delegateTask.addCandidateUser(candidate.getCandidateId());
                    else if (candidate.getType().equals(Constants.CandidateTypes.GROUP))
                        delegateTask.addCandidateGroup(candidate.getCandidateId());

                    if (candidate.getRole() != null && candidate.getRole().equals(Constants.CandidateRoles.APPROVER))
                        approvers.add(candidate);
                    else if (candidate.getRole() != null && candidate.getRole().equals(Constants.CandidateRoles.WATCHER))
                        watchers.add(candidate);
                }
            }
        }

        String taskEventType = null;

        if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_CREATE))
            taskEventType = Constants.TaskEventTypes.CREATE;
        if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_COMPLETE))
            taskEventType = Constants.TaskEventTypes.COMPLETE;

        List<Notification> notifications = process.getNotifications();
        if (notifications != null && !notifications.isEmpty()) {
            Map<String, String> context = new HashMap<String, String>();
            ManyMap<String, String> formValueMap = processInstance.getFormValueMap();
            if (formValueMap != null && !formValueMap.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : formValueMap.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();

                    if (StringUtils.isNotEmpty(key) && !values.isEmpty()) {
                        context.put(key, values.iterator().next());
                    }
                }
            }

            for (Notification notification : notifications) {
                if (notification == null)
                    continue;

                if (taskEventType != null && notification.getTaskEvents() != null && notification.getTaskEvents().contains(taskEventType)) {
                    if (taskDefinitionKey != null && notification.getTaskDefinitionKeys() != null && notification.getTaskDefinitionKeys().contains(taskDefinitionKey)) {
                        Set<Candidate> candidates = new HashSet<Candidate>();
                        if (notification.getCandidateRoles().isEmpty()) {
                            candidates.addAll(approvers);
                            candidates.addAll(watchers);
                        } else if (notification.getCandidateRoles().contains(Constants.CandidateRoles.APPROVER)) {
                            candidates.addAll(approvers);
                        } else if (notification.getCandidateRoles().contains(Constants.CandidateRoles.APPROVER)) {
                            candidates.addAll(watchers);
                        }

                        sendNotification(candidates, notification, context);
                    }
                }
            }
        }
    }

    private void sendNotification(Set<Candidate> candidates, Notification notification, Map<String, String> context) {
        if (candidates.isEmpty())
            return;

        String mailServerHost = environment.getProperty("mail.server.host");
        int mailServerPort = environment.getProperty("mail.server.port", Integer.class, 25);
        String mailFromAddress = environment.getProperty("mail.from.address");
        String mailFromLabel = environment.getProperty("mail.from.label");

        MustacheFactory mf = new DefaultMustacheFactory();

        StringWriter writer = new StringWriter();
        Mustache mustache = mf.compile(new StringReader(notification.getSubject()), "subject");
        mustache.execute(writer, context);

        String subject = writer.toString();

        writer = new StringWriter();
        mustache = mf.compile(new StringReader(notification.getText()), "text");
        mustache.execute(writer, context);

        String body = writer.toString();

        try {
            SimpleEmail email = new SimpleEmail();
            email.setHostName(mailServerHost);
            email.setSmtpPort(mailServerPort);

            for (Candidate candidate : candidates) {
                if (StringUtils.isEmpty(candidate.getType()) || StringUtils.isEmpty(candidate.getCandidateId()))
                    continue;

                if (candidate.getType().equals(Constants.CandidateTypes.PERSON)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(candidate.getCandidateId());

                    if (userDetails instanceof InternalUserDetails) {
                        InternalUserDetails internalUserDetails = InternalUserDetails.class.cast(userDetails);
                        if (StringUtils.isNotEmpty(internalUserDetails.getEmailAddress()))
                            email.addTo(internalUserDetails.getEmailAddress(), internalUserDetails.getDisplayName());
                    }
                }
            }
            email.setFrom(mailFromAddress, mailFromLabel);
            email.setSubject(subject);
            email.setMsg(body);

            LOG.debug("Subject: " + email.getSubject());
            LOG.debug(email.getMimeMessage());
            email.send();
        } catch (EmailException e) {
            LOG.error("Unable to send email with subject " + subject);
        }
    }

}
