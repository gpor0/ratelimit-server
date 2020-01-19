package com.github.gpor0;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * author: gpor0@github.com
 */
@Path("/api")
public class ApiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ApiResource.class);
    //we will remove old keys automatically to prevent memory leaks
    private static final Cache<String, ClientRequestPool> CLIENT_ACCESS_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    @Inject
    protected Config config;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@QueryParam("clientId") final String clientId) {
        LOG.debug("Received new request from client {}", clientId);

        int maxRequests = config.getValue("maxRequests", Integer.class);
        int window = config.getValue("window", Integer.class);
        TimeUnit unit = config.getValue("unit", TimeUnit.class);

        if (clientId == null || clientId.isEmpty()) {
            return Response.status(400).build();
        }

        CLIENT_ACCESS_CACHE.asMap().computeIfAbsent(clientId, k -> new ClientRequestPool(maxRequests, window, unit));
        boolean canProceed = CLIENT_ACCESS_CACHE.getIfPresent(clientId).canProceed();

        if (!canProceed) {
            LOG.info("Returning service unavailable to {}", clientId);
        }
        return Response.status(canProceed ? 200 : 503).build();
    }

    private static class ClientRequestPool {

        private int maxRequests;

        private Cache<UUID, Integer> pool;

        public ClientRequestPool(int maxRequests, int window, TimeUnit unit) {
            this.maxRequests = maxRequests;
            this.pool = CacheBuilder.newBuilder()
                    .expireAfterAccess(window, unit)
                    .build();
        }

        public synchronized boolean canProceed() {
            pool.cleanUp();
            boolean canProceed = pool.size() < maxRequests;
            if (canProceed) {
                pool.put(UUID.randomUUID(), 1);
            }
            return canProceed;
        }

    }
}
