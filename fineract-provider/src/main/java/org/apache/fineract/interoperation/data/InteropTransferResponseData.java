/*
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
package org.apache.fineract.interoperation.data;

import org.apache.fineract.interoperation.domain.InteropActionState;
import org.joda.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import java.beans.Transient;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class InteropTransferResponseData extends InteropResponseData {

    @NotNull
    private final String transferCode;

    private String completedTimestamp;

    private InteropTransferResponseData(Long resourceId, Long officeId, Long commandId, Map<String, Object> changesOnly,
                                        @NotNull String transactionCode, @NotNull InteropActionState state, LocalDateTime expiration,
                                        List<ExtensionData> extensionList, @NotNull String transferCode, LocalDateTime completedTimestamp) {
        super(resourceId, officeId, commandId, changesOnly, transactionCode, state, expiration, extensionList);
        this.transferCode = transferCode;
        this.completedTimestamp = format(completedTimestamp);
    }

    public static InteropTransferResponseData build(Long commandId, @NotNull String transactionCode, @NotNull InteropActionState state,
                                                    LocalDateTime expiration, List<ExtensionData> extensionList, @NotNull String transferCode,
                                                    LocalDateTime completedTimestamp) {
        return new InteropTransferResponseData(null, null, commandId, null, transactionCode, state, expiration, extensionList,
                transferCode, completedTimestamp);
    }

    public static InteropTransferResponseData build(@NotNull String transactionCode, @NotNull InteropActionState state,
                                                    List<ExtensionData> extensionList, @NotNull String transferCode,
                                                    LocalDateTime completedTimestamp) {
        return build(null, transactionCode, state, null, extensionList, transferCode, completedTimestamp);
    }

    public static InteropTransferResponseData build(Long commandId, @NotNull String transactionCode, @NotNull InteropActionState state,
                                                    @NotNull String transferCode) {
        return build(commandId, transactionCode, state, null, null, transferCode, null);
    }

    public static InteropTransferResponseData build(@NotNull String transactionCode, @NotNull InteropActionState state, @NotNull String transferCode) {
        return build(null, transactionCode, state, transferCode);
    }

    public String getTransferCode() {
        return transferCode;
    }

    public String getCompletedTimestamp() {
        return completedTimestamp;
    }

    @Transient
    public LocalDateTime getCompletedTimestampDate() throws ParseException {
        return parse(completedTimestamp);
    }
}
