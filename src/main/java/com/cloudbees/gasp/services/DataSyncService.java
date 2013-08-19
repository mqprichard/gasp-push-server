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
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DataSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSyncService.class.getName());

    private static final Config config = new Config();

    private String getApnMessage(String msg) {
        Map<String, Object> appleMessageMap = new HashMap<String, Object>();
        Map<String, Object> appMessageMap = new HashMap<String, Object>();
        appMessageMap.put("alert", msg);
        appMessageMap.put("badge", 1);
        appMessageMap.put("sound", "default");
        appleMessageMap.put("aps", appMessageMap);
        return jsonify(appleMessageMap);
    }

    private String getGcmMessage(String msg) {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("message", msg);

        Map<String, Object> androidMessageMap = new HashMap<String, Object>();
        androidMessageMap.put("collapse_key", "Welcome");
        androidMessageMap.put("data", payload);
        androidMessageMap.put("delay_while_idle", true);
        androidMessageMap.put("time_to_live", 125);
        androidMessageMap.put("dry_run", false);
        return jsonify(androidMessageMap);
    }

    private static String jsonify(Object message) {
        try {
            return new ObjectMapper().writeValueAsString(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw (RuntimeException) e;
        }
    }


    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    public void reviewUpdateReceived(String jsonInput) {
        try {
            Review review = new Gson().fromJson(jsonInput, Review.class);
            LOGGER.info("Syncing Review Id: " + String.valueOf(review.getId()));

            //TODO: Retrieve all registered device tokens
            String apnEndpointArn = APNDataStore.getTokens().get(0);
            LOGGER.debug("APN endpoint ARN: " + apnEndpointArn);

            //TODO: Retrieve all registration IDs
            String gcmEndpointArn = GCMDataStore.getTokens().get(0);
            LOGGER.debug("GCM endpoint ARN: " + gcmEndpointArn);



            try {
                SNSMobile snsMobile = new SNSMobile();

                // Send message to APN endpoint
                snsMobile.pushNotification(SNSMobile.Platform.APNS_SANDBOX,
                                           apnEndpointArn,
                                           getApnMessage("Gasp! update: review " + review.getId()));

                // Send message to GCM endpoint
                snsMobile.pushNotification(SNSMobile.Platform.GCM,
                                           gcmEndpointArn,
                                           getGcmMessage("Gasp! update: review " + review.getId()));

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
