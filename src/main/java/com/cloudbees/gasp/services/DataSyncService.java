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
import com.cloudbees.gasp.model.Restaurant;
import com.cloudbees.gasp.model.Review;
import com.cloudbees.gasp.model.User;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/")
public class DataSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSyncService.class.getName());

    private static final Config config = new Config();

    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    public void reviewUpdateReceived(String jsonInput) {
        try {
            Review review = new Gson().fromJson(jsonInput, Review.class);
            LOGGER.info("Syncing Review Id: " + String.valueOf(review.getId()));

            //TODO: Retrieve all registered device tokens
            String deviceToken = APNDataStore.getTokens().get(0);
            LOGGER.debug("APNS device token: " + deviceToken);

            //TODO: Retrieve all registration IDs
            String registrationId = GCMDataStore.getTokens().get(0);
            LOGGER.debug("GCM Registration ID: " + registrationId);

            String applicationName = "gasp-snsmobile-service";
            LOGGER.debug("Application name: " + applicationName);

            try {
                SNSMobile snsMobile = new SNSMobile();
                snsMobile.setSnsClient(Config.getAmazonSNS());

                //snsMobile.pushNotification(SNSMobile.Platform.GCM,
                //                           "",
                //                           Config.getGcmApiKey(),
                //                           registrationId,
                //                           applicationName);

                snsMobile.pushNotification(SNSMobile.Platform.APNS_SANDBOX,
                                           Config.getApnsCertificate(),
                                           Config.getApnsKey(),
                                           deviceToken,
                                           applicationName);

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
        } catch (Exception e) {
            return;
        }
    }

    @POST
    @Path("/restaurants")
    @Consumes(MediaType.APPLICATION_JSON)
    public void restaurantUpdateReceived(String jsonInput) {
        try {
            Restaurant restaurant = new Gson().fromJson(jsonInput, Restaurant.class);
            LOGGER.info("Syncing Restaurant Id: " + String.valueOf(restaurant.getId()));

            //TODO: Retrieve all registered device tokens
            String token = APNDataStore.getTokens().get(0);
            LOGGER.debug("APNS Device Token: " + token);

        } catch (Exception e) {
            return;
        }
    }

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void userUpdateReceived(String jsonInput) {
        try {
            User user = new Gson().fromJson(jsonInput, User.class);
            LOGGER.info("Syncing User Id: " + String.valueOf(user.getId()));

            //TODO: Retrieve all registered device tokens
            String token = APNDataStore.getTokens().get(0);
            LOGGER.debug("APNS Device Token: " + token);

        } catch (Exception e) {
            return;
        }
    }
}
