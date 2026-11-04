package com.academia.config;

import com.academia.dto.AlunoRequest;
import com.academia.dto.AlunoResponse;
import com.academia.dto.MatriculaRequest;
import com.academia.dto.PlanoRequest;
import com.academia.dto.PlanoResponse;
import com.academia.repository.PlanoRepository;
import com.academia.service.AlunoService;
import com.academia.service.MatriculaService;
import com.academia.service.PlanoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carrega dados de exemplo na primeira execução (útil para demonstração).
 * Desative com app.seed=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "false")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final PlanoRepository planoRepository;
    private final PlanoService planoService;
    private final AlunoService alunoService;
    private final MatriculaService matriculaService;

    public DataLoader(PlanoRepository planoRepository, PlanoService planoService,
                      AlunoService alunoService, MatriculaService matriculaService) {
        this.planoRepository = planoRepository;
        this.planoService = planoService;
        this.alunoService = alunoService;
        this.matriculaService = matriculaService;
    }

    @Override
    public void run(String... args) {
        if (planoRepository.count() > 0) {
            return;   // já possui dados
        }
        PlanoResponse mensal = planoService.criar(
                new PlanoRequest("Mensal", "Acesso livre por 1 mês", new BigDecimal("89.90"), 1, true));
        planoService.criar(
                new PlanoRequest("Trimestral", "Acesso livre por 3 meses", new BigDecimal("79.90"), 3, true));
        planoService.criar(
                new PlanoRequest("Anual", "Acesso livre por 12 meses", new BigDecimal("69.90"), 12, true));
        planoService.criar(
                new PlanoRequest("Plano Legado", "Plano descontinuado (inativo)", new BigDecimal("59.90"), 1, false));

        AlunoResponse ana = alunoService.criar(new AlunoRequest(
                "Ana Souza", "ana@email.com", "11111111111", "(86) 99999-1111", LocalDate.of(1995, 3, 14)));
        alunoService.criar(new AlunoRequest(
                "Bruno Lima", "bruno@email.com", "22222222222", "(86) 99999-2222", LocalDate.of(1990, 7, 2)));
        alunoService.criar(new AlunoRequest(
                "Carla Mendes", "carla@email.com", "33333333333", "(86) 99999-3333", LocalDate.of(2000, 11, 25)));

        matriculaService.criar(new MatriculaRequest(ana.id(), mensal.id(), null));

        log.info("Dados de exemplo carregados (4 planos, 3 alunos, 1 matrícula).");
    }
}
