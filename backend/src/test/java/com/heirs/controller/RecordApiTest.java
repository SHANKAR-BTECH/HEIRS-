package com.heirs.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heirs.TestRecords;
import com.heirs.repository.RecordRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(resolver = com.heirs.TestDatabaseProfileResolver.class)
@Transactional
class RecordApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired RecordRepository repository;
  Long id;

  @BeforeEach
  void prepare() {
    id = repository.saveAndFlush(TestRecords.entity("REF/1")).getId();
  }

  @Test
  void getsPageWithStableContract() throws Exception {
    mvc.perform(get("/api/records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").isNumber())
        .andExpect(jsonPath("$.content[0].category").value("Policy"))
        .andExpect(jsonPath("$.content[0].status").value("Active"))
        .andExpect(jsonPath("$.content[0].publishedDate").value("2026-03-12"))
        .andExpect(jsonPath("$.content[0].keywords").isString())
        .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(20));
    mvc.perform(get("/api/records?page=1&size=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void getsByIdAndHandlesNotFound() throws Exception {
    mvc.perform(get("/api/records/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.referenceNumber").value("REF/1"));
    mvc.perform(get("/api/records/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.path").value("/api/records/999999"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "?q=digital",
        "?category=Policy",
        "?status=Active",
        "?year=2026",
        "?q=digital&category=Policy&status=Active&year=2026&department=Higher%20Education%20Department"
      })
  void searchesOptionalAndCombinedFilters(String query) throws Exception {
    mvc.perform(get(java.net.URI.create("/api/records/search" + query)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void exposesCategoriesAndDatabaseHealth() throws Exception {
    mvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].code").value("POLICY"))
        .andExpect(jsonPath("$[0].name").value("Policy"));
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.database").value("UP"));
  }

  @Test
  void crudLifecycle() throws Exception {
    var response =
        mvc.perform(
                post("/api/records")
                    .contentType("application/json")
                    .content(json.writeValueAsString(TestRecords.create("REF/2"))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn();
    long newId = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            put("/api/records/" + newId)
                .contentType("application/json")
                .content(json.writeValueAsString(TestRecords.update("REF/2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Revised Policy"))
        .andExpect(jsonPath("$.status").value("Archived"));
    mvc.perform(delete("/api/records/" + newId))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/api/records/" + newId)).andExpect(status().isNotFound());
  }

  @Test
  void duplicateReferenceReturnsConflict() throws Exception {
    mvc.perform(
            post("/api/records")
                .contentType("application/json")
                .content(json.writeValueAsString(TestRecords.create("REF/1"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Reference number")));
  }

  @Test
  void missingRequiredFieldsReturnFieldErrors() throws Exception {
    mvc.perform(post("/api/records").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.title").exists())
        .andExpect(jsonPath("$.fieldErrors.referenceNumber").exists())
        .andExpect(jsonPath("$.fieldErrors.department").exists())
        .andExpect(jsonPath("$.fieldErrors.category").exists())
        .andExpect(jsonPath("$.fieldErrors.status").exists());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/records?page=-1",
        "/api/records?size=0",
        "/api/records?size=101",
        "/api/records/abc",
        "/api/records/0",
        "/api/records/search?category=Unknown",
        "/api/records/search?status=Unknown",
        "/api/records/search?year=1800",
        "/api/records/search?year=2101",
        "/api/records/search?year=no"
      })
  void invalidParametersReturn400(String url) throws Exception {
    mvc.perform(get(url))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{",
        "{\"category\":\"Unknown\"}",
        "{\"status\":2}",
        "{\"publishedDate\":\"2026-02-30\"}",
        "{\"extra\":true}"
      })
  void malformedBodiesReturn400(String body) throws Exception {
    mvc.perform(post("/api/records").contentType("application/json").content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validatesLengthsAndYear() throws Exception {
    var body = json.valueToTree(TestRecords.create("REF/2"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body)
        .put("title", "x".repeat(256))
        .put("publicationYear", 2101)
        .put("referenceNumber", " ");
    mvc.perform(post("/api/records").contentType("application/json").content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.title").exists())
        .andExpect(jsonPath("$.fieldErrors.publicationYear").exists())
        .andExpect(jsonPath("$.fieldErrors.referenceNumber").exists());
  }

  @Test
  void corsAllowsConfiguredOriginsOnly() throws Exception {
    for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173"}) {
      mvc.perform(
              options("/api/records")
                  .header("Origin", origin)
                  .header("Access-Control-Request-Method", "POST")
                  .header("Access-Control-Request-Headers", "content-type"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }
    mvc.perform(
            options("/api/records")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void unknownRouteAndWrongMethodHaveConsistentErrors() throws Exception {
    mvc.perform(get("/api/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    mvc.perform(patch("/api/records/" + id))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
  }
}
