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
package org.apache.fineract.portfolio.delinquency.validator;

import static org.apache.fineract.portfolio.delinquency.domain.DelinquencyAction.RESUME;

import com.google.gson.JsonElement;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.validator.ParseAndValidator;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyAction;
import org.apache.fineract.portfolio.delinquency.domain.LoanDelinquencyAction;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DelinquencyActionParseAndValidator extends ParseAndValidator {

    private final FromJsonHelper jsonHelper;

    public LoanDelinquencyAction validateAndParseUpdate(@NotNull final JsonCommand command, Loan loan,
            List<LoanDelinquencyAction> savedDelinquencyActions, LocalDate businessDate) {
        List<LoanDelinquencyActionData> effectiveDelinquencyList = calculateEffectiveDelinquencyList(savedDelinquencyActions);
        LoanDelinquencyAction parsedDelinquencyAction = parseCommand(command);
        validateLoanIsActive(loan);
        if (DelinquencyAction.PAUSE.equals(parsedDelinquencyAction.getAction())) {
            validatePauseStartAndEndDate(parsedDelinquencyAction, businessDate);
            validatePauseShallNotOverlap(parsedDelinquencyAction, effectiveDelinquencyList);
        } else if (DelinquencyAction.RESUME.equals(parsedDelinquencyAction.getAction())) {
            validateResumeStartDate(parsedDelinquencyAction, businessDate);
            validateResumeNoEndDate(parsedDelinquencyAction);
            validateResumeShouldBeOnActivePause(parsedDelinquencyAction, effectiveDelinquencyList);
        }
        return parsedDelinquencyAction;
    }

    private List<LoanDelinquencyActionData> calculateEffectiveDelinquencyList(List<LoanDelinquencyAction> savedDelinquencyActions) {
        // partition them based on type
        Map<DelinquencyAction, List<LoanDelinquencyAction>> partitioned = savedDelinquencyActions.stream()
                .collect(Collectors.groupingBy(LoanDelinquencyAction::getAction));
        List<LoanDelinquencyActionData> effective = new ArrayList<>();
        List<LoanDelinquencyAction> pauses = partitioned.get(DelinquencyAction.PAUSE);
        if (pauses != null && pauses.size() > 0) {
            for (LoanDelinquencyAction loanDelinquencyAction : pauses) {
                Optional<LoanDelinquencyAction> resume = findMatchingResume(loanDelinquencyAction, partitioned.get(RESUME));
                LoanDelinquencyActionData loanDelinquencyActionData = new LoanDelinquencyActionData(loanDelinquencyAction);
                resume.ifPresent(r -> loanDelinquencyActionData.setEndDate(r.getStartDate()));
                effective.add(loanDelinquencyActionData);
            }
        }
        return effective;
    }

    private Optional<LoanDelinquencyAction> findMatchingResume(LoanDelinquencyAction pause, List<LoanDelinquencyAction> resumes) {
        if (resumes != null && resumes.size() > 0) {
            for (LoanDelinquencyAction resume : resumes) {
                if (!pause.getStartDate().isAfter(resume.getStartDate()) && !resume.getStartDate().isAfter(pause.getEndDate())) {
                    return Optional.of(resume);
                }
            }
        }
        return Optional.empty();
    }

    private void validateResumeShouldBeOnActivePause(LoanDelinquencyAction parsedDelinquencyAction,
            List<LoanDelinquencyActionData> savedDelinquencyActions) {
        boolean match = savedDelinquencyActions.stream()
                .anyMatch(lda -> !DateUtils.isBefore(parsedDelinquencyAction.getStartDate(), lda.getStartDate())
                        && !DateUtils.isAfter(parsedDelinquencyAction.getStartDate(), lda.getEndDate()));
        if (!match) {
            raiseValidationError("loan-delinquency-action-resume-should-be-on-pause",
                    "Resume Delinquency Action can only be created during an active pause");
        }
    }

    private void validateResumeNoEndDate(LoanDelinquencyAction parsedDelinquencyAction) {
        if (parsedDelinquencyAction.getEndDate() != null) {
            raiseValidationError("loan-delinquency-action-resume-should-have-no-end-date",
                    "Resume Delinquency action can not have end date");
        }
    }

    private void validateResumeStartDate(LoanDelinquencyAction parsedDelinquencyAction, LocalDate businessDate) {
        if (!parsedDelinquencyAction.getStartDate().equals(businessDate)) {
            raiseValidationError("loan-delinquency-action-invalid-start-date",
                    "Start date of the Resume Delinquency action must be the current business date");
        }
    }

    private void validatePauseStartAndEndDate(LoanDelinquencyAction parsedDelinquencyAction, LocalDate businessDate) {
        if (parsedDelinquencyAction.getStartDate().equals(parsedDelinquencyAction.getEndDate())) {
            raiseValidationError("loan-delinquency-action-invalid-start-date-and-end-date",
                    "Delinquency pause period must be at least one day");
        }

        if (businessDate.isAfter(parsedDelinquencyAction.getStartDate())) {
            raiseValidationError("loan-delinquency-action-invalid-start-date", "Start date of pause period must be in the future");
        }
    }

    private void validateLoanIsActive(Loan loan) {
        if (!loan.getStatus().isActive()) {
            raiseValidationError("loan-delinquency-action-invalid-loan-state", "Delinquency actions can be created only for active loans.");
        }
    }

    private void validatePauseShallNotOverlap(LoanDelinquencyAction parsedDelinquencyAction,
            List<LoanDelinquencyActionData> delinquencyActions) {
        if (delinquencyActions.stream().filter(lda -> lda.getAction().equals(DelinquencyAction.PAUSE))
                .anyMatch(lda -> isOverlapping(parsedDelinquencyAction, lda))) {
            raiseValidationError("loan-delinquency-action-overlapping",
                    "Delinquency pause period cannot overlap with another pause period");
        }
    }

    private boolean isOverlapping(LoanDelinquencyAction parsedDelinquencyAction, LoanDelinquencyActionData ldad) {
        return ((!parsedDelinquencyAction.getStartDate().isAfter(ldad.getStartDate())
                && !ldad.getStartDate().isAfter(parsedDelinquencyAction.getEndDate()))
                || (!parsedDelinquencyAction.getStartDate().isAfter(ldad.getEndDate())
                        && !ldad.getEndDate().isAfter(parsedDelinquencyAction.getEndDate())));
    }

    @org.jetbrains.annotations.NotNull
    private LoanDelinquencyAction parseCommand(@org.jetbrains.annotations.NotNull JsonCommand command) {
        LoanDelinquencyAction parsedDelinquencyAction = new LoanDelinquencyAction();
        parsedDelinquencyAction.setAction(extractAction(command.parsedJson()));
        parsedDelinquencyAction.setStartDate(extractStartDate(command.parsedJson()));
        parsedDelinquencyAction.setEndDate(extractEndDate(command.parsedJson()));
        return parsedDelinquencyAction;
    }

    private DelinquencyAction extractAction(JsonElement json) {
        String actionString = jsonHelper.extractStringNamed(DelinquencyActionParameters.ACTION, json);
        validateActionString(actionString);
        if ("pause".equalsIgnoreCase(actionString)) {
            return DelinquencyAction.PAUSE;
        } else if ("resume".equalsIgnoreCase(actionString)) {
            return DelinquencyAction.RESUME;
        } else {
            throw new PlatformApiDataValidationException(List.of(ApiParameterError.generalError("loan-delinquency-action-invalid-action",
                    "Invalid Delinquency Action: " + actionString)));
        }
    }

    private void validateActionString(String actionString) {
        if (StringUtils.isEmpty(actionString)) {
            raiseValidationError("loan-delinquency-action-missing-action", "Delinquency Action must not be null or empty");
        }
    }

    private LocalDate extractStartDate(JsonElement json) {
        String dateFormat = jsonHelper.extractStringNamed(DelinquencyActionParameters.DATE_FORMAT, json);
        String locale = jsonHelper.extractStringNamed(DelinquencyActionParameters.LOCALE, json);
        return jsonHelper.extractLocalDateNamed(DelinquencyActionParameters.START_DATE, json, dateFormat,
                JsonParserHelper.localeFromString(locale));
    }

    private LocalDate extractEndDate(JsonElement json) {
        String dateFormat = jsonHelper.extractStringNamed(DelinquencyActionParameters.DATE_FORMAT, json);
        String locale = jsonHelper.extractStringNamed(DelinquencyActionParameters.LOCALE, json);
        return jsonHelper.extractLocalDateNamed(DelinquencyActionParameters.END_DATE, json, dateFormat,
                JsonParserHelper.localeFromString(locale));
    }

    private void raiseValidationError(String globalisationMessageCode, String msg) throws PlatformApiDataValidationException {
        throw new PlatformApiDataValidationException(List.of(ApiParameterError.generalError(globalisationMessageCode, msg)));
    }

}
