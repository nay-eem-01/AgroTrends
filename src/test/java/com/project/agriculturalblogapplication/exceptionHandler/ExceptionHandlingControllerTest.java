package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.service.ErrorCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
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

        @GetMapping("/probe/header")
        String header(@RequestHeader("X-Probe") String probe) {
            return probe;
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new ExceptionHandlingController(mock(ErrorCodeService.class)))
                .build();
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
