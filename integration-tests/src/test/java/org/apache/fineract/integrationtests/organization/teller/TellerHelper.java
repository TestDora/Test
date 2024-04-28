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

import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.integrationtests.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TellerHelper {

    private TellerHelper() {

    }

    private static final Logger LOG = LoggerFactory.getLogger(TellerHelper.class);

    private static final String CREATE_TELLER_URL = "/fineract-provider/api/v1/tellers";

    public static Integer createTeller(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        return (Integer) createTellerWithJson(requestSpec, responseSpec, createTellerAsJSON()).get("resourceId");
    }

    public static Map<String, Object> createTellerWithJson(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String json) {
        final String url = CREATE_TELLER_URL + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerPost(requestSpec, responseSpec, url, json, "");
    }

    public static String createTellerAsJSON() {

        final Map<String, Object> map = getMapWithStartDate();

        map.put("officeId", 1);
        map.put("name", Utils.uniqueRandomStringGenerator("Teller 1", 5));
        map.put("description", Utils.uniqueRandomStringGenerator("Teller For Testing", 4));
        map.put("status", 300);

        LOG.info("map :  {}", map);
        return new Gson().toJson(map);
    }

    public static Map<String, Object> getMapWithStartDate() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("startDate", "20 September 2011");

        return map;
    }

}
