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
package org.apache.fineract.infrastructure.reportmailingjob.service;

import java.util.Collection;

import org.apache.fineract.infrastructure.reportmailingjob.data.ReportMailingJobData;

public interface ReportMailingJobReadPlatformService {
    /** 
     * Retrieve all report mailing jobs that have the isDeleted property set to 0  
     **/
    Collection<ReportMailingJobData> retrieveAllReportMailingJobs();
    
    /** 
     * Retrieve a report mailing job that has the isDeleted property set to 0 
     **/
    ReportMailingJobData retrieveReportMailingJob(Long reportMailingJobId);
    
    /** 
     * Retrieve the report mailing job enumeration/dropdown options 
     **/
    ReportMailingJobData retrieveReportMailingJobEnumOptions();

    /** 
     * Retrieve all active report mailing jobs that have their isDeleted property set to 0 
     **/
    Collection<ReportMailingJobData> retrieveAllActiveReportMailingJobs();
}
