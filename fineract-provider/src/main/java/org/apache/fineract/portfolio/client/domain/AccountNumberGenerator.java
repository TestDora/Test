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
package org.apache.fineract.portfolio.client.domain;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatEnumerations.AccountNumberPrefixType;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepository;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Example {@link AccountNumberGenerator} for clients that takes an entities auto generated database id and zero fills
 * it ensuring the identifier is always of a given <code>maxLength</code>.
 */
@Component
public class AccountNumberGenerator {

    private static final int maxLength = 9;

    private static final String ID = "id";
    private static final String CLIENT_TYPE = "clientType";
    private static final String OFFICE_NAME = "officeName";
    private static final String LOAN_PRODUCT_SHORT_NAME = "loanProductShortName";
    private static final String SAVINGS_PRODUCT_SHORT_NAME = "savingsProductShortName";
    private static final String SHARE_PRODUCT_SHORT_NAME = "sharesProductShortName";
    private static final String PREFIX_SHORT_NAME = "prefixShortName";
    private final AccountNumberFormatRepository accountNumberFormatRepository;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    @Autowired
    public AccountNumberGenerator(final ConfigurationReadPlatformService configurationReadPlatformService,
            final AccountNumberFormatRepository accountNumberFormatRepository) {
        this.configurationReadPlatformService = configurationReadPlatformService;
        this.accountNumberFormatRepository = accountNumberFormatRepository;
    }

    public String generate(Client client, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, client.getId().toString());
        propertyMap.put(OFFICE_NAME, client.getOffice().getName());
        CodeValue clientType = client.clientType();
        if (clientType != null) {
            propertyMap.put(CLIENT_TYPE, clientType.label());
        }
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generate(Loan loan, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, loan.getId().toString());
        propertyMap.put(OFFICE_NAME, loan.getOffice().getName());
        propertyMap.put(LOAN_PRODUCT_SHORT_NAME, loan.loanProduct().getShortName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generate(SavingsAccount savingsAccount, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, savingsAccount.getId().toString());
        propertyMap.put(OFFICE_NAME, savingsAccount.office().getName());
        propertyMap.put(SAVINGS_PRODUCT_SHORT_NAME, savingsAccount.savingsProduct().getShortName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generate(ShareAccount shareaccount, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, shareaccount.getId().toString());
        propertyMap.put(SHARE_PRODUCT_SHORT_NAME, shareaccount.getShareProduct().getShortName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    private String generateAccountNumber(Map<String, String> propertyMap, AccountNumberFormat accountNumberFormat) {
        int accountMaxLength = AccountNumberGenerator.maxLength;

        // find if the custom length is defined
        final GlobalConfigurationPropertyData customLength = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("custom-account-number-length");

        if (customLength.isEnabled()) {
            // if it is enabled, and has the value, get it from the repository.
            if (customLength.getValue() != null) {
                accountMaxLength = customLength.getValue().intValue();
            }
        }

        String accountNumber = StringUtils.leftPad(propertyMap.get(ID), accountMaxLength, '0');
        if (accountNumberFormat != null && accountNumberFormat.getPrefixEnum() != null) {
            AccountNumberPrefixType accountNumberPrefixType = AccountNumberPrefixType.fromInt(accountNumberFormat.getPrefixEnum());
            String prefix = null;
            switch (accountNumberPrefixType) {
                case CLIENT_TYPE:
                    prefix = propertyMap.get(CLIENT_TYPE);
                break;

                case OFFICE_NAME:
                    prefix = propertyMap.get(OFFICE_NAME);
                break;

                case LOAN_PRODUCT_SHORT_NAME:
                    prefix = propertyMap.get(LOAN_PRODUCT_SHORT_NAME);
                break;

                case SAVINGS_PRODUCT_SHORT_NAME:
                    prefix = propertyMap.get(SAVINGS_PRODUCT_SHORT_NAME);
                break;

                case PREFIX_SHORT_NAME:
                    generatePrefix(propertyMap, propertyMap.get(ID), accountMaxLength, accountNumberFormat);
                    prefix = propertyMap.get(PREFIX_SHORT_NAME);
                break;
            }

            // FINERACT-590
            // Because account_no is limited to 20 chars, we can only use the
            // first 10 chars of prefix - trim if necessary
            if (prefix != null) {
                prefix = prefix.substring(0, Math.min(prefix.length(), 10));
            }
            if (accountNumberPrefixType.getValue().equals(AccountNumberPrefixType.PREFIX_SHORT_NAME.getValue())) {
                Integer prefixLength = prefix.length();
                Integer numberLength = accountMaxLength - prefixLength;
                accountNumber = StringUtils.leftPad(propertyMap.get(ID), numberLength, '0');
            } else {
                accountNumber = StringUtils.leftPad(accountNumber, Integer.valueOf(propertyMap.get(ID).length()), '0');
            }

            accountNumber = StringUtils.overlay(accountNumber, prefix, 0, 0);
        }
        return accountNumber;
    }

    private Map<String, String> generatePrefix(Map<String, String> propertyMap, String accountNumber, Integer accountMaxLength,
            AccountNumberFormat accountNumberFormat) {

        String prefix = accountNumberFormat.getPrefixCharacter();
        Integer prefixLength = prefix.length();

        Integer totalLength = prefixLength + Integer.valueOf(propertyMap.get(ID).length());

        prefixLength = totalLength - accountMaxLength;

        if (prefixLength > 0) {
            prefix = prefix.substring(0, prefix.length() - prefixLength);
        }

        propertyMap.put(PREFIX_SHORT_NAME, prefix);

        return propertyMap;
    }

    public String generateGroupAccountNumber(Group group, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, group.getId().toString());
        propertyMap.put(OFFICE_NAME, group.getOffice().getName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generateCenterAccountNumber(Group group, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, group.getId().toString());
        propertyMap.put(OFFICE_NAME, group.getOffice().getName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

}
