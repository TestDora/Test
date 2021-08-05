/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.hooks.processor;

import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.SMSProviderIdParamName;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.hooks.domain.Hook;
import org.apache.fineract.infrastructure.hooks.domain.HookConfiguration;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.template.domain.Template;
import org.apache.fineract.template.domain.TemplateRepository;
import org.apache.fineract.template.service.TemplateMergeService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageGatewayHookProcessor implements HookProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageGatewayHookProcessor.class);

    private final ClientRepositoryWrapper clientRepository;
    private final TemplateRepository templateRepository;
    private final TemplateMergeService templateMergeService;

    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;

    @Autowired
    public MessageGatewayHookProcessor(ClientRepositoryWrapper clientRepository, TemplateRepository templateRepository,
            TemplateMergeService templateMergeService, SmsMessageRepository smsMessageRepository,
            SmsMessageScheduledJobService smsMessageScheduledJobService) {
        this.clientRepository = clientRepository;
        this.templateRepository = templateRepository;
        this.templateMergeService = templateMergeService;
        this.smsMessageRepository = smsMessageRepository;
        this.smsMessageScheduledJobService = smsMessageScheduledJobService;
    }

    @Override
    public void process(final Hook hook, @SuppressWarnings("unused") final AppUser appUser, final String payload, final String entityName,
            final String actionName, final String tenantIdentifier, final String authToken) throws IOException {

        final Set<HookConfiguration> config = hook.getHookConfig();

        Integer SMSProviderId = null;

        for (final HookConfiguration conf : config) {
            final String fieldName = conf.getFieldName();
            if (fieldName.equals(SMSProviderIdParamName)) {
                SMSProviderId = Integer.parseInt(conf.getFieldValue());
            }
        }

        String templateName = entityName + "_" + actionName;

        // 1 : find template via mapper using entity and action
        Template template;
        List<Template> templates = this.templateRepository.findByTemplateMapper("SMS_template_Key", templateName);
        if (templates.isEmpty()) {
            // load default template if set.
            template = hook.getUgdTemplate();
        } else {
            template = templates.get(0);
        }
        if (template == null) {
            LOG.error("Error : {} with name {}", "Template not found", templateName);
            throw new GeneralPlatformDomainRuleException("error.msg.templates.not.found", "Template not found", templateName);
        }

        // 2.1 : get customer details for basic template mapping
        // 2.2 : cook up scope map
        @SuppressWarnings("unchecked")
        final HashMap<String, Object> reqMap = new ObjectMapper().readValue(payload, HashMap.class);
        reqMap.put("BASE_URI", System.getProperty("baseUrl"));
        if (reqMap.get("clientId") != null) {
            final Long clientId = Long.valueOf(Integer.toString((int) reqMap.get("clientId")));
            Client client = clientRepository.findOneWithNotFoundDetection(clientId);
            if (client.mobileNo() != null && !client.mobileNo().isEmpty()) {
                reqMap.put("clientName", client.getDisplayName());

                // 3: compile template using Mustache
                this.templateMergeService.setAuthToken(authToken);
                String smsText = this.templateMergeService.compile(template, reqMap).replace("<p>", "").replace("</p>", "");
                // 4 : send message to the url

                SmsMessage smsMessage = SmsMessage.pendingSms(null, null, client, null, smsText, client.mobileNo(), null, false);
                this.smsMessageRepository.save(smsMessage);
                smsMessageScheduledJobService.sendTriggeredMessage(Collections.singleton(smsMessage), SMSProviderId);
            }
        }
    }

}
