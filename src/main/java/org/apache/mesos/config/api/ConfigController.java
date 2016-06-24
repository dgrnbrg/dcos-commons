package org.apache.mesos.config.api;

import java.util.Collection;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.mesos.config.ConfigStore;
import org.apache.mesos.config.Configuration;
import org.apache.mesos.config.ConfigurationFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A read-only API for accessing active and inactive configurations from persistent storage.
 *
 * @param <T> The configuration type which is being stored by the framework.
 */
@Path("/v1/configurations")
public class ConfigController<T extends Configuration> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private final ConfigStore<T> configStore;
    private final ConfigurationFactory<T> configFactory;

    public ConfigController(ConfigStore<T> configStore, ConfigurationFactory<T> configFactory) {
        this.configStore = configStore;
        this.configFactory = configFactory;
    }

    /**
     * Produces an ID listing of all stored configurations.
     */
    @GET
    public Response getConfigurationIds() {
        try {
            Collection<UUID> configNames = configStore.list();
            JSONArray configArray = new JSONArray(configNames);
            return Response.ok(configArray.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            logger.error("Failed to fetch list of configuration ids", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Produces the content of the provided configuration ID, or returns an error if that ID doesn't
     * exist or the data couldn't be read.
     */
    @Path("/{configurationId}")
    @GET
    public Response getConfiguration(@PathParam("configurationId") String configurationId) {
        try {
            logger.info("Attempting to fetch config with id '{}'", configurationId);
            return fetchConfig(UUID.fromString(configurationId));
        } catch (Exception ex) {
            // Warning instead of Error: Subject to user input
            logger.warn(String.format(
                    "Failed to fetch requested configuration with id '%s'", configurationId), ex);
            return Response.serverError().build();
        }
    }

    /**
     * Produces the ID of the current target configuration, or returns an error if reading that
     * data failed.
     */
    @Path("/targetId")
    @GET
    public Response getTargetId() {
        try {
            JSONObject configObj = new JSONObject(configStore.getTargetConfig());
            return Response.ok(configObj.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            logger.error("Failed to fetch target configuration", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Produces the content of the current target configuration, or returns an error if reading that
     * data failed.
     */
    @Path("/target")
    @GET
    public Response getTarget() {
        UUID targetId;
        try {
            targetId = configStore.getTargetConfig();
        } catch (Exception ex) {
            logger.error("Failed to fetch ID of target configuration", ex);
            return Response.serverError().build();
        }
        try {
            return fetchConfig(targetId);
        } catch (Exception ex) {
            logger.error(String.format("Failed to fetch target configuration '%s'", targetId), ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns an HTTP response containing the content of the requested configuration.
     */
    private Response fetchConfig(UUID id) throws Exception {
        JSONObject configObj = new JSONObject(configStore.fetch(id, configFactory));
        return Response.ok(configObj.toString(), MediaType.APPLICATION_JSON).build();
    }
}
