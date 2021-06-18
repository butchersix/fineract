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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Parameter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.TenantDatabaseUpgradeService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Path("/tenants")
@Component
@Scope("singleton")
public class TenantsApiResource {

    private static final Logger LOG = LoggerFactory.getLogger(TenantsApiResource.class);

    private final PlatformSecurityContext platformSecurityContext;
    private final DefaultToApiJsonSerializer toApiJsonSerializer;

    private final TenantDetailsService tenantDetailsService;

    @Autowired
    private TenantDatabaseUpgradeService tenantDatabaseUpgradeService;

    @Autowired
    ApplicationContext context;

    @Autowired
    public TenantsApiResource(final PlatformSecurityContext platformSecurityContext,
            final DefaultToApiJsonSerializer<TenantsApiResource> toApiJsonSerializer, final TenantDetailsService detailsService) {
        this.platformSecurityContext = platformSecurityContext;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.tenantDetailsService = detailsService;
    }

    @POST
    @Path("create")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @SuppressWarnings("try")
    public ResponseEntity createDatabase(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        Map<String, Object> params = new Gson().fromJson(apiRequestBodyAsJson, new TypeToken<HashMap<String, Object>>() {}.getType());

        if (isIncomplete(Arrays.asList("identifier", "username", "password", "params"))) {
            // TODO
            LOG.error("TenantsApiResource:createDatabase - Invalid schema_name");
            return new ResponseEntity<Object>("Invalid schema_name", HttpStatus.NOT_FOUND);
        } else {
            try {
                // TODO: create tenant database
                String identifier = params.get("identifier").toString();
                String defaultParams = params.get("params").toString();
                String username = params.get("username").toString();
                String password = params.get("password").toString();
                String dbUrl = getEnvVar("PROTOCOL", "jdbc") + ":mysql://localhost:3306/fineract_" + identifier + defaultParams;
                String dbUid = username;
                String dbPwd = password;

                // try {
                // FineractPlatformTenant tenant = tenantDetailsService.loadTenantById(identifier);
                // return new ResponseEntity<Object>("Tenant already exists.", HttpStatus.CONFLICT);
                // } catch (InvalidTenantIdentiferException e) {
                // }

                LOG.info("url: {}, username: {}, password: {}", dbUrl, dbUid, dbPwd);

                DataSource dataSource = DataSourceBuilder.create().driverClassName("com.mysql.cj.jdbc.Driver").url(dbUrl).username(dbUid)
                        .password(dbPwd).build();

                try (Connection c = dataSource.getConnection()) {
                    tenantDatabaseUpgradeService.upgradeAllTenants();
                    LOG.info("Tenant '{}' added.", identifier);
                }

                return new ResponseEntity<Object>(String.format("Tenant `%s` database successfully created", identifier), HttpStatus.OK);

            } catch (Exception e) {
                LOG.error("TenantsApiResource:createDatabase - Exception: {}", e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<Object>("An error has occured.", HttpStatus.INTERNAL_SERVER_ERROR);
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
        Environment environment = context.getEnvironment();
        String value = environment.getProperty(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

}
