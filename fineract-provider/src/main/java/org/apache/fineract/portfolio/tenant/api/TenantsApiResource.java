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
package org.apache.fineract.portfolio.tenant.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Arrays;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Path("/tenants")
@Component
@Scope("singleton")
public class TenantsApiResource {

    private static final Logger LOG = LoggerFactory.getLogger(TenantsApiResource.class);

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer toApiJsonSerializer;

    @Autowired
    public TenantsApiResource(final PlatformSecurityContext context,
            final DefaultToApiJsonSerializer<TenantsApiResource> toApiJsonSerializer) {
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @POST
    @Path("create")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public void createDatabase(@RequestBody Map<String, String> params) {

        if (isIncomplete(Arrays.asList("schema_name"))) {
            // TODO
            LOG.error("TenantsApiResource:createDatabase - Invalid schema_name");
        } else {
            try {
                // TODO: create tenant database

                String dbUrl = getEnvVar("fineract_tenants_url", "jdbc:mysql:thin://localhost:3306/fineract_tenants");
                String dbUid = getEnvVar("FINERACT_DEFAULT_TENANTDB_UID", "root");
                String dbPwd = getEnvVar("FINERACT_DEFAULT_TENANTDB_PWD", "mysql");

                // SQL command to create a database in MySQL.
                String sql = "CREATE DATABASE IF NOT EXISTS " + params.get("schema_name");

                Connection conn = DriverManager.getConnection(dbUrl, dbUid, dbPwd);
                PreparedStatement stmt = conn.prepareStatement(sql);

                stmt.execute();

                return new ResponseEntity<Object>(renderJsonResponse("200", "Tenant database successfully created"),
                        HttpStatus.INTERNAL_SERVER_ERROR);

            } catch (Exception e) {
                LOG.error("TenantsApiResource:createDatabase - Exception: {}", e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<Object>(renderJsonResponse("500", "Error in logging in."), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private boolean isIncomplete(List<String> params) {
        for (String param : params) {
            if (param.isEmpty() || param == null) {
                return true;
            }
        }
        return false;
    }

    public String renderJsonResponse(String statusCode, String message) {
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("message", message);
        return response.toString();
    }

    private String getEnvVar(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

}
