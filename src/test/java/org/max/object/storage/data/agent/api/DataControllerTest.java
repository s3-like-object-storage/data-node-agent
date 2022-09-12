
package org.max.object.storage.data.agent.api;

import java.util.concurrent.TimeUnit;

import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.max.object.storage.data.agent.Main;
import org.max.object.storage.data.agent.domain.FileData;

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
    public void checkGreetingsWorksFine() {

        WebClientResponse response = webClient.put()
            .path("/data/file/133")
            .submit(new FileData("133", "Hello from file with id 133") /*"{\"greeting\" : \"Hola\"}"*/)
            .await();
        assertThat(response.status().code(), is(204));

        FileData fileData = webClient.get()
                .path("/data/file/133")
                .request(FileData.class)
                .await();
        assertThat(fileData.getData(), is("Hello from file with id 133"));
    }
}
