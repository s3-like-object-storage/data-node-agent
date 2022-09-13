package org.max.object.storage.data.agent.util;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.lang.invoke.MethodHandles;
import org.max.object.storage.data.agent.api.ErrorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RestUtils() {
        throw new AssertionError("Can't instantiate utility-only class");
    }

    public static Single<ServerResponse> processError(Throwable ex, ServerRequest request, ServerResponse response) {
        LOG.error("Internal server error", ex);

        ErrorData error = new ErrorData("INTERNAL_ERROR", "Internal server error occurred");

        response.headers().contentType(MediaType.APPLICATION_JSON);

        return response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(error);
    }


}
