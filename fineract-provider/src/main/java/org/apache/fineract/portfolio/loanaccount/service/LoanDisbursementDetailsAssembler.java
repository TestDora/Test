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
package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;

@RequiredArgsConstructor
public class LoanDisbursementDetailsAssembler {

    private final FromJsonHelper fromApiJsonHelper;

    public List<LoanDisbursementDetails> fetchDisbursementData(final JsonObject command) {
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(command);
        final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(command);
        List<LoanDisbursementDetails> disbursementDatas = new ArrayList<>();
        if (command.has(LoanApiConstants.disbursementDataParameterName)) {
            final JsonArray disbursementDataArray = command.getAsJsonArray(LoanApiConstants.disbursementDataParameterName);
            if (disbursementDataArray != null && !disbursementDataArray.isEmpty()) {
                int i = 0;
                do {
                    final JsonObject jsonObject = disbursementDataArray.get(i).getAsJsonObject();
                    LocalDate expectedDisbursementDate = null;
                    LocalDate actualDisbursementDate = null;
                    BigDecimal principal = null;
                    BigDecimal netDisbursalAmount = null;

                    if (jsonObject.has(LoanApiConstants.expectedDisbursementDateParameterName)) {
                        expectedDisbursementDate = this.fromApiJsonHelper.extractLocalDateNamed(
                                LoanApiConstants.expectedDisbursementDateParameterName, jsonObject, dateFormat, locale);
                    }
                    if (jsonObject.has(LoanApiConstants.disbursementPrincipalParameterName)
                            && jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).isJsonPrimitive()
                            && StringUtils.isNotBlank(jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).getAsString())) {
                        principal = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementPrincipalParameterName).getAsBigDecimal();
                    }
                    if (jsonObject.has(LoanApiConstants.disbursementNetDisbursalAmountParameterName)
                            && jsonObject.get(LoanApiConstants.disbursementNetDisbursalAmountParameterName).isJsonPrimitive()
                            && StringUtils.isNotBlank(
                                    jsonObject.get(LoanApiConstants.disbursementNetDisbursalAmountParameterName).getAsString())) {
                        netDisbursalAmount = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementNetDisbursalAmountParameterName)
                                .getAsBigDecimal();
                    }
                    boolean isReversed = false;
                    if (jsonObject.has(LoanApiConstants.disbursementReversedParameterName)) {
                        isReversed = this.fromApiJsonHelper.extractBooleanNamed(LoanApiConstants.disbursementReversedParameterName,
                                jsonObject);
                    }
                    disbursementDatas.add(new LoanDisbursementDetails(expectedDisbursementDate, actualDisbursementDate, principal,
                            netDisbursalAmount, isReversed));
                    i++;
                } while (i < disbursementDataArray.size());
            }
        }
        return disbursementDatas;
    }
}
