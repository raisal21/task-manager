package id.raisal.taskmanager.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Web slice: CORS for /api/**. A small test controller keeps this test apart from the boards. */
@WebMvcTest(CorsTest.PingController.class)
@Import({CorsConfig.class, CorsTest.PingController.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173")
class CorsTest {

    @RestController
    static class PingController {

        @GetMapping("/api/ping")
        String ping() {
            return "pong";
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void allowsTheConfiguredOrigin() throws Exception {
        mvc.perform(get("/api/ping").header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void answersThePreflightRequestOfAPatch() throws Exception {
        mvc.perform(options("/api/ping")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE"));
    }

    @Test
    void rejectsAnOriginThatIsNotConfigured() throws Exception {
        mvc.perform(get("/api/ping").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
