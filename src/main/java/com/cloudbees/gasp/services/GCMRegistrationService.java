/*
 * Copyright (c) 2013 Mark Prichard, CloudBees
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.gasp.services;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/gcm")
public class GCMRegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCMRegistrationService.class.getName());
    private static SNSMobile snsMobile = new SNSMobile();

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doRegister(@FormParam("regId") String regId) {

        try {
            CreatePlatformEndpointResult platformEndpointResult =
                    snsMobile.createPlatformEndpoint("Gasp GCM Platform Endpoint",
                                                     regId,
                                                     snsMobile.getGcmPlatformArn());

            GCMDataStore.registerArn(regId, platformEndpointResult.getEndpointArn());
            LOGGER.info("Registered: " + platformEndpointResult.getEndpointArn());

        } catch (AmazonServiceException ase) {
            LOGGER.debug("AmazonServiceException");
            LOGGER.debug("  Error Message:    " + ase.getMessage());
            LOGGER.debug("  HTTP Status Code: " + ase.getStatusCode());
            LOGGER.debug("  AWS Error Code:   " + ase.getErrorCode());
            LOGGER.debug("  Error Type:       " + ase.getErrorType());
            LOGGER.debug("  Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            LOGGER.debug("AmazonClientException");
            LOGGER.debug("  Error Message: " + ace.getMessage());
        }

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("unregister")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doUnregister(@FormParam("regId") String regId) {
        try {
            // Delete the SNS app endpoint
            snsMobile.deleteEndpointArn(GCMDataStore.getEndpointArn(regId));
            LOGGER.info("Deleted endpoint: " + GCMDataStore.getEndpointArn(regId));

            GCMDataStore.unregisterArn(regId);
            LOGGER.info("Unregistered device: " + regId);

        } catch (AmazonServiceException ase) {
            LOGGER.debug("AmazonServiceException");
            LOGGER.debug("  Error Message:    " + ase.getMessage());
            LOGGER.debug("  HTTP Status Code: " + ase.getStatusCode());
            LOGGER.debug("  AWS Error Code:   " + ase.getErrorCode());
            LOGGER.debug("  Error Type:       " + ase.getErrorType());
            LOGGER.debug("  Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            LOGGER.debug("AmazonClientException");
            LOGGER.debug("  Error Message: " + ace.getMessage());
        }

        return Response.status(Response.Status.OK).build();
    }
}
