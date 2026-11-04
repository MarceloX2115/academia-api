package com.academia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem o contexto completo (Controller -> Service -> Repository -> H2 em memória).
 * Cada teste é revertido ao final (@Transactional), então os dados não vazam entre os testes.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed=false"
})
@AutoConfigureMockMvc
@Transactional
class AcademiaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------- helpers

    private long criarPlano(String nome, boolean ativo) throws Exception {
        String json = """
                {"nome":"%s","descricao":"teste","valorMensal":100.00,"duracaoMeses":3,"ativo":%s}
                """.formatted(nome, ativo);
        return idDaResposta(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long criarAluno(String nome, String email, String cpf) throws Exception {
        String json = """
                {"nome":"%s","email":"%s","cpf":"%s","telefone":"86999990000"}
                """.formatted(nome, email, cpf);
        return idDaResposta(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String jsonMatricula(long alunoId, long planoId) {
        return """
                {"alunoId":%d,"planoId":%d}
                """.formatted(alunoId, planoId);
    }

    private long criarMatricula(long alunoId, long planoId) throws Exception {
        return idDaResposta(post("/api/matriculas")
                .contentType(MediaType.APPLICATION_JSON).content(jsonMatricula(alunoId, planoId)));
    }

    private long idDaResposta(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        MvcResult result = mockMvc.perform(req).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertNotNull(id);
        return id;
    }

    // ------------------------------------------------------------------ testes

    @Test
    void deveCriarMatriculaComVencimentoEValorCalculados() throws Exception {
        long planoId = criarPlano("Trimestral Teste", true);
        long alunoId = criarAluno("Ana Souza", "ana@teste.com", "12345678901");

        mockMvc.perform(post("/api/matriculas")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMatricula(alunoId, planoId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.valorTotal").value(300.00))
                .andExpect(jsonPath("$.alunoNome").value("Ana Souza"))
                .andExpect(jsonPath("$.planoNome").value("Trimestral Teste"));
    }

    @Test
    void deveImpedirMatriculaDuplicada() throws Exception {
        long planoId = criarPlano("Mensal Teste", true);
        long alunoId = criarAluno("Bruno Lima", "bruno@teste.com", "22222222222");
        criarMatricula(alunoId, planoId);

        mockMvc.perform(post("/api/matriculas")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMatricula(alunoId, planoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já possui uma matrícula ativa")));
    }

    @Test
    void deveImpedirMatriculaEmPlanoInativo() throws Exception {
        long planoId = criarPlano("Plano Inativo", false);
        long alunoId = criarAluno("Carla Mendes", "carla@teste.com", "33333333333");

        mockMvc.perform(post("/api/matriculas")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMatricula(alunoId, planoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inativo")));
    }

    @Test
    void deveRetornar404ParaRegistroInexistente() throws Exception {
        mockMvc.perform(get("/api/alunos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/api/planos/999999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/matriculas/999999")).andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaDadosInvalidos() throws Exception {
        String invalido = """
                {"nome":"","email":"nao-e-email","cpf":"123"}
                """;
        mockMvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void deveImpedirAlunoComEmailDuplicado() throws Exception {
        criarAluno("Ana Souza", "repetido@teste.com", "11111111111");

        String json = """
                {"nome":"Outra Pessoa","email":"repetido@teste.com","cpf":"99999999999"}
                """;
        mockMvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void deveFiltrarMatriculasPorNomeEStatus() throws Exception {
        long planoId = criarPlano("Plano Filtro", true);
        long anaId = criarAluno("Ana Souza", "ana2@teste.com", "44444444444");
        long brunoId = criarAluno("Bruno Lima", "bruno2@teste.com", "55555555555");
        criarMatricula(anaId, planoId);
        long matriculaBruno = criarMatricula(brunoId, planoId);

        mockMvc.perform(patch("/api/matriculas/" + matriculaBruno + "/cancelar")).andExpect(status().isOk());

        mockMvc.perform(get("/api/matriculas").param("nomeAluno", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].alunoNome").value("Ana Souza"));

        mockMvc.perform(get("/api/matriculas").param("status", "CANCELADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].alunoNome").value("Bruno Lima"));

        mockMvc.perform(get("/api/matriculas").param("nomeAluno", "ana").param("status", "CANCELADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/matriculas").param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveFiltrarAlunosPorNome() throws Exception {
        criarAluno("Ana Souza", "ana3@teste.com", "66666666666");
        criarAluno("Bruno Lima", "bruno3@teste.com", "77777777777");

        mockMvc.perform(get("/api/alunos").param("nome", "bru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Bruno Lima"));
    }

    @Test
    void deveImpedirExclusaoDeAlunoEPlanoComMatriculaEPermitirAposRemoverMatricula() throws Exception {
        long planoId = criarPlano("Plano Exclusao", true);
        long alunoId = criarAluno("Diego Alves", "diego@teste.com", "88888888888");
        long matriculaId = criarMatricula(alunoId, planoId);

        // aluno e plano possuem dependência -> 409
        mockMvc.perform(delete("/api/alunos/" + alunoId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/planos/" + planoId)).andExpect(status().isConflict());

        // matrícula ativa não pode ser excluída -> 409; precisa cancelar antes
        mockMvc.perform(delete("/api/matriculas/" + matriculaId)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/matriculas/" + matriculaId + "/cancelar")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/matriculas/" + matriculaId + "/cancelar")).andExpect(status().isConflict());

        // agora tudo pode ser removido -> 204
        mockMvc.perform(delete("/api/matriculas/" + matriculaId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/alunos/" + alunoId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/planos/" + planoId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/alunos/" + alunoId)).andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarAlunoERenovarMatricula() throws Exception {
        long planoId = criarPlano("Plano Renovacao", true);
        long alunoId = criarAluno("Elisa Costa", "elisa@teste.com", "10101010101");
        long matriculaId = criarMatricula(alunoId, planoId);

        String atualizado = """
                {"nome":"Elisa Costa Lima","email":"elisa@teste.com","cpf":"10101010101"}
                """;
        mockMvc.perform(put("/api/alunos/" + alunoId).contentType(MediaType.APPLICATION_JSON).content(atualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Elisa Costa Lima"));

        mockMvc.perform(patch("/api/matriculas/" + matriculaId + "/renovar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.valorTotal").value(600.00));
    }

    @Test
    void deveRetornar400ParaDiasInvalidosNaConsultaDeVencimento() throws Exception {
        mockMvc.perform(get("/api/matriculas/vencendo").param("dias", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/matriculas/vencendo").param("dias", "30"))
                .andExpect(status().isOk());
    }
}
