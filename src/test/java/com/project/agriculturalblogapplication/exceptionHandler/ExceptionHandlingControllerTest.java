package com.project.agriculturalblogapplication.exceptionHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.project.agriculturalblogapplication.enums.CropSeason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExceptionHandlingControllerTest {

    private MockMvc mockMvc;

    @RestController
    static class ProbeController {
        @GetMapping("/probe")
        String probe(@RequestParam(required = false) CropSeason season,
                     @RequestParam(defaultValue = "0") int pageNo,
                     @RequestParam(required = false) String q) {
            return "ok";
        }

        @GetMapping("/probe/required")
        String required(@RequestParam String q) {
            return q;
        }

        @GetMapping("/probe/missing")
        String missing() {
            throw new ApplicationException(HttpStatus.NOT_FOUND, "Not here");
        }

        @GetMapping("/probe/broken")
        String broken() {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "Broken");
        }

        @GetMapping("/probe/header")
        String header(@RequestHeader("X-Probe") String probe) {
            return probe;
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new ExceptionHandlingController())
                .build();
        logs.start();
        handlerLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logs);
    }

    private final Logger handlerLogger = (Logger) LoggerFactory.getLogger(ExceptionHandlingController.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @Test
    void notFoundIsNotLoggedAsAnError() throws Exception {
        mockMvc.perform(get("/probe/missing")).andExpect(status().isNotFound());
        assertThat(logs.list).noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
    }

    @Test
    void serverErrorIsStillLoggedAsAnError() throws Exception {
        mockMvc.perform(get("/probe/broken")).andExpect(status().isInternalServerError());
        assertThat(logs.list).anyMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void badEnumValueIsA400ThatListsTheChoices() throws Exception {
        mockMvc.perform(get("/probe").param("season", "WINTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid value for 'season'. Use one of: RABI, KHARIF_1, KHARIF_2, YEAR_ROUND."));
    }

    @Test
    void badNumberIsA400ThatNamesTheParameterWithoutEchoingTheValue() throws Exception {
        mockMvc.perform(get("/probe").param("pageNo", "<script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'pageNo'."));
    }

    @Test
    void missingRequiredParameterIsA400ThatNamesIt() throws Exception {
        mockMvc.perform(get("/probe/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'q'."));
    }

    @Test
    void otherClientErrorsNoLongerSayServerError() throws Exception {
        mockMvc.perform(get("/probe/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request."));
    }
}
