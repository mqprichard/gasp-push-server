/*
 * Copyright 2012 Google Inc.
 * Copyright (c) 2013 Mark Prichard, CloudBees
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.gasp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is thread-safe but not persistent (it will lost the data when the
 * app is restarted) - it is meant just as an example.
 */
public final class GCMDataStore {

    private static final List<String> endpoints = new ArrayList<String>();
    private static final Map<String, String> tokenMap = new HashMap<String, String>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GCMDataStore.class.getName());

    private GCMDataStore() {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers a device.
     */
    public static void registerArn(String regId, String endpointArn) {
        LOGGER.debug("Registering Id: " + regId + " with endpoint Arn: " + endpointArn);

        synchronized (endpoints) {
            endpoints.add(endpointArn);
        }
        synchronized (tokenMap) {
            tokenMap.put(regId, endpointArn);
        }
    }

    /**
     * Unregisters a device.
     */
    public static void unregisterArn(String regId) {
        String endpointArn = tokenMap.get(regId);
        LOGGER.debug("Unregistering Id: " + regId + " with endpoint Arn: " + endpointArn);

        synchronized (endpoints) {
            endpoints.remove(endpointArn);
        }
        synchronized (tokenMap) {
            tokenMap.remove(regId);
        }
    }

    /**
     * Gets all registered devices.
     */
    public static List<String> getEndpoints() {
        synchronized (endpoints) {
            return new ArrayList<String>(endpoints);
        }
    }

    /**
     * Gets the endpoint Arn for a given device Id
     */
    public static String getEndpointArn(String regId) {
        return tokenMap.get(regId);
    }
}
