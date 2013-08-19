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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/apn")
public class APNRegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(APNRegistrationService.class.getName());

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doRegister(@FormParam("token") String regId) {

        APNDataStore.register(regId);
        LOGGER.info("Registered: " + regId);

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("unregister")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doUnregister(@FormParam("token") String regId) {

        APNDataStore.unregister(regId);
        LOGGER.info("Unregistered: " + regId);

        return Response.status(Response.Status.OK).build();
    }
}
