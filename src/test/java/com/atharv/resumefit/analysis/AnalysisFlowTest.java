package com.atharv.resumefit.analysis;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisFlowTest {

    private static final String JD = "We need a Java engineer with Spring Boot, MySQL, Docker and Kubernetes.";

    @Autowired
    private MockMvc mvc;

    private String registerAndGetToken(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private MockMultipartFile resume() {
        return new MockMultipartFile("file", "resume.txt", "text/plain",
                "Java developer. Spring Boot, MySQL and Git.".getBytes());
    }

    @Test
    void protectedEndpointsNeedLogin() throws Exception {
        mvc.perform(get("/api/analyses")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        registerAndGetToken("dup@example.com");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"password123\",\"fullName\":\"Other\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        registerAndGetToken("login@example.com");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shortPasswordFailsValidation() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"short@example.com\",\"password\":\"123\",\"fullName\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAnalyzeListAndDelete() throws Exception {
        String token = registerAndGetToken("flow@example.com");

        String created = mvc.perform(multipart("/api/analyses").file(resume())
                        .param("jobTitle", "Java Developer")
                        .param("jobDescription", JD)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fitScore").isNumber())
                .andExpect(jsonPath("$.matchedSkills").isArray())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/analyses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/analyses/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnalyses").value(1));

        mvc.perform(delete("/api/analyses/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/analyses/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersAnalyses() throws Exception {
        String owner = registerAndGetToken("owner@example.com");
        String other = registerAndGetToken("other@example.com");

        String created = mvc.perform(multipart("/api/analyses").file(resume())
                        .param("jobTitle", "Java Developer")
                        .param("jobDescription", JD)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/analyses/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        String token = registerAndGetToken("badfile@example.com");
        MockMultipartFile exe = new MockMultipartFile("file", "resume.exe", "application/octet-stream", "abc".getBytes());
        mvc.perform(multipart("/api/analyses").file(exe)
                        .param("jobTitle", "Dev")
                        .param("jobDescription", JD)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
