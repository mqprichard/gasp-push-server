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
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DataSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSyncService.class.getName());

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

    private void sendPushNotifications(String messageText) {
        try {
            SNSMobile snsMobile = new SNSMobile();

            // Send update to all registered APN endpoints
            for (String endpointArn: APNDataStore.getEndpoints() ) {
                LOGGER.info("Sending update to APN endpoint ARN: " + endpointArn);
                snsMobile.pushNotification(SNSMobile.Platform.APNS_SANDBOX,
                                           endpointArn,
                                           getApnMessage(messageText));
            }

            // Send update to all registered GCM endpoints
            for (String endpointArn: GCMDataStore.getEndpoints()) {
                LOGGER.info("Sending update to GCM endpoint ARN: " + endpointArn);
                snsMobile.pushNotification(SNSMobile.Platform.GCM,
                                           endpointArn,
                                           getGcmMessage(messageText));
            }
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
        } catch (Exception e) {
            return;
        }
    }

    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reviewUpdateReceived(String jsonInput) {
        try {
            Review review = new Gson().fromJson(jsonInput, Review.class);
            LOGGER.info("Syncing Review Id: " + String.valueOf(review.getId()));

            // Send push notifications to all registered devices
            sendPushNotifications("Gasp! update: reviews/" + review.getId());
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/restaurants")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restaurantUpdateReceived(String jsonInput) {
        try {
            Restaurant restaurant = new Gson().fromJson(jsonInput, Restaurant.class);
            LOGGER.info("Syncing Restaurant Id: " + String.valueOf(restaurant.getId()));

            // Send push notifications to all registered devices
            sendPushNotifications("Gasp! update: restaurants/" + restaurant.getId());
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userUpdateReceived(String jsonInput) {
        try {
            User user = new Gson().fromJson(jsonInput, User.class);
            LOGGER.info("Syncing User Id: " + String.valueOf(user.getId()));

            // Send push notifications to all registered devices
            sendPushNotifications("Gasp! update: users/" + user.getId());
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();
    }
}
