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
package org.apache.fineract.integrationtests.organization.teller;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.fineract.client.models.GetTellersTellerIdCashiersCashiersIdSummaryAndTransactionsResponse;
import org.apache.fineract.client.models.GetTellersTellerIdCashiersCashiersIdTransactionsResponse;
import org.apache.fineract.integrationtests.client.IntegrationTest;

public class CashierTransactionsHelper extends IntegrationTest {

    private final ResponseSpecification responseSpecification;
    private final RequestSpecification requestSpecification;

    public CashierTransactionsHelper(final RequestSpecification requestSpecification, final ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public GetTellersTellerIdCashiersCashiersIdTransactionsResponse getTellersTellerIdCashiersCashiersIdTransactionsResponse(Long tellerId,
            Long cashierId, String currencyCode, int offset, int limit, String orderBy, String sortOrder) {
        return ok(fineract().tellers.getTransactionsForCashier(tellerId, cashierId, currencyCode, offset, limit, orderBy, sortOrder));
    }

    public GetTellersTellerIdCashiersCashiersIdSummaryAndTransactionsResponse getTellersTellerIdCashiersCashiersIdSummaryAndTransactionsResponse(
            Long tellerId, Long cashierId, String currencyCode, int offset, int limit, String orderBy, String sortOrder) {
        return ok(fineract().tellers.getTransactionsWithSummaryForCashier(tellerId, cashierId, currencyCode, offset, limit, orderBy,
                sortOrder));
    }

}
