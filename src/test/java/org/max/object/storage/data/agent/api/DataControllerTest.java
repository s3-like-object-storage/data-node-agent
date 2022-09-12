
package org.max.object.storage.data.agent.api;

import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.max.object.storage.data.agent.Main;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class DataControllerTest {

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
    public void uploadFile() {

        WebClientResponse response = webClient.post()
            .path("/data/file")
            .submit("hello binary data".getBytes(StandardCharsets.UTF_8))
            .await();

        assertThat(response.status().code()).isEqualTo(201);
        assertThat(response.headers().value("Location")).isNotEmpty();

        String fileIdStr = response.headers().value("Location").get();

        byte[] fileData = webClient.get()
            .path("/data/file/" + fileIdStr)
            .request(byte[].class)
            .await();

        assertThat(fileData).isNotEmpty();

        assertThat(new String(fileData)).isEqualTo("hello binary data");
    }
}
