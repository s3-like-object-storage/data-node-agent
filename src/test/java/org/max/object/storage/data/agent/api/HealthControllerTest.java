
package org.max.object.storage.data.agent.api;

import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.max.object.storage.data.agent.Main;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class HealthControllerTest {

    private static WebServer webServer;
    private static WebClient webClient;

    @BeforeAll
    public static void setUp() {
        webServer = Main.startServer().await();
        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JacksonSupport.create())
                .build();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void checkHealthEndpointWorksFine() {
        WebClientResponse response = webClient.get()
                .path("health")
                .request()
                .await();
        assertThat(response.status().code(), is(200));
    }

}
