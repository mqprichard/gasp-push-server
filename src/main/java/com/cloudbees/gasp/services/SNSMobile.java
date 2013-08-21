/*
 * Copyright (c) 2013 Mark Prichard, CloudBees
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class SNSMobile {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNSMobile.class.getName());
    private static final String defaultMessage = "";

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static enum Platform {
        APNS, APNS_SANDBOX, ADM, GCM
    }

    private static AmazonSNS snsClient = null;
    private static String gcmPlatformArn;
    private static String apnPlatformArn;

    public AmazonSNS getSnsClient() {
        return snsClient;
    }

    public void setSnsClient(AmazonSNS snsClient) {
        SNSMobile.snsClient = snsClient;
    }

    public String getGcmPlatformArn() {
        return gcmPlatformArn;
    }

    public void setGcmPlatformArn(String gcmPlatformArn) {
        this.gcmPlatformArn = gcmPlatformArn;
    }

    public String getApnPlatformArn() {
        return apnPlatformArn;
    }

    public void setApnPlatformArn(String apnPlatformArn) {
        this.apnPlatformArn = apnPlatformArn;
    }

    public String getPlatformArn(Platform platform,
                                 String principal,
                                 String credential,
                                 String applicationName) {
        switch(platform) {
            case APNS_SANDBOX:
            case GCM:
                // Create Platform Application. This corresponds to an app on a platform.
                CreatePlatformApplicationResult platformApplicationResult = createPlatformApplication(
                        applicationName, platform, principal, credential);

                // The Platform Application Arn can be used to uniquely identify the Platform Application.
                String platformApplicationArn = platformApplicationResult.getPlatformApplicationArn();

                LOGGER.debug("Created Platform Application Arn: " + platformApplicationArn);
                return platformApplicationArn;

            default:
                throw new IllegalArgumentException("Platform Not supported : " + platform.name());
        }
    }

    public void pushNotification(Platform platform,
                                 String platformEndpointArn,
                                 String theMessage) {
        // Publish a push notification to an Endpoint.
        PublishResult publishResult = apnPublish(platformEndpointArn, platform, theMessage);
    }

    private PublishResult apnPublish(String endpointArn, Platform platform, String theMessage) {
        PublishRequest publishRequest = new PublishRequest();
        Map<String, String> messageMap = new HashMap<String, String>();
        String message;
        messageMap.put("default", defaultMessage);
        messageMap.put(platform.name(), theMessage);
        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);
        publishRequest.setMessageStructure("json");
        message = jsonify(messageMap);

        // Display the message that will be sent to the endpoint/
        LOGGER.debug(message);

        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }

    private CreatePlatformApplicationResult createPlatformApplication(
            String applicationName, Platform platform, String principal, String credential) {
        CreatePlatformApplicationRequest platformApplicationRequest = new CreatePlatformApplicationRequest();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("PlatformPrincipal", principal);
        attributes.put("PlatformCredential", credential);
        platformApplicationRequest.setAttributes(attributes);
        platformApplicationRequest.setName(applicationName);
        platformApplicationRequest.setPlatform(platform.name());
        return snsClient.createPlatformApplication(platformApplicationRequest);
    }

    public CreatePlatformEndpointResult createPlatformEndpoint(
            String customData, String platformToken, String applicationArn) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setCustomUserData(customData);
        platformEndpointRequest.setToken(platformToken);
        platformEndpointRequest.setPlatformApplicationArn(applicationArn);
        return snsClient.createPlatformEndpoint(platformEndpointRequest);
    }

    public void deletePlatformApplication(String applicationArn) {
        DeletePlatformApplicationRequest request = new DeletePlatformApplicationRequest();
        request.setPlatformApplicationArn(applicationArn);
        snsClient.deletePlatformApplication(request);
    }

    public void deleteEndpointArn (String endpointArn){
        DeleteEndpointRequest request = new DeleteEndpointRequest();
        request.setEndpointArn(endpointArn);
        snsClient.deleteEndpoint(request);
    }

    private static String jsonify(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw (RuntimeException) e;
        }
    }
}
