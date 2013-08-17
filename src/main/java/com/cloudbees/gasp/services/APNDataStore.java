/*
 * Copyright 2012 Google Inc.
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
import java.util.List;


/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is thread-safe but not persistent (it will lost the data when the
 * app is restarted) - it is meant just as an example.
 */
public final class APNDataStore {

  private static final List<String> tokens = new ArrayList<String>();
  private static final Logger LOGGER = LoggerFactory.getLogger(APNDataStore.class.getName());

  private APNDataStore() {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a device.
   */
  public static void register(String token) {
    LOGGER.debug("Registering " + token);
    synchronized (tokens) {
      tokens.add(token);
    }
  }

  /**
   * Unregisters a device.
   */
  public static void unregister(String token) {
    LOGGER.debug("Unregistering " + token);
    synchronized (tokens) {
      tokens.remove(token);
    }
  }

  /**
   * Gets all registered devices.
   */
  public static List<String> getTokens() {
    synchronized (tokens) {
      return new ArrayList<String>(tokens);
    }
  }
}
