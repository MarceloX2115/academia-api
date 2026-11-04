package com.academia.service;

import com.academia.dto.MatriculaRequest;
import com.academia.dto.MatriculaResponse;
import com.academia.exception.RecursoNaoEncontradoException;
import com.academia.exception.RegraNegocioException;
import com.academia.mapper.MatriculaMapper;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Plano;
import com.academia.model.StatusMatricula;
import com.academia.repository.AlunoRepository;
import com.academia.repository.MatriculaRepository;
import com.academia.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private PlanoRepository planoRepository;
    @Spy
    private MatriculaMapper mapper = new MatriculaMapper();

    @InjectMocks
    private MatriculaService service;

    private Aluno aluno;
    private Plano plano;

    @BeforeEach
    void setUp() {
        aluno = new Aluno();
        aluno.setId(1L);
        aluno.setNome("Ana Souza");

        plano = new Plano();
        plano.setId(10L);
        plano.setNome("Trimestral");
        plano.setValorMensal(new BigDecimal("100.00"));
        plano.setDuracaoMeses(3);
        plano.setAtivo(true);
    }

    @Test
    void criar_deveCalcularVencimentoEValorTotal() {
        LocalDate inicio = LocalDate.now();
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(10L)).thenReturn(Optional.of(plano));
        when(matriculaRepository.existsByAlunoIdAndPlanoIdAndStatus(1L, 10L, StatusMatricula.ATIVA)).thenReturn(false);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> {
            Matricula m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        MatriculaResponse resposta = service.criar(new MatriculaRequest(1L, 10L, inicio));

        assertEquals(100L, resposta.id());
        assertEquals(StatusMatricula.ATIVA, resposta.status());
        assertEquals(inicio.plusMonths(3), resposta.dataVencimento());
        assertEquals(0, new BigDecimal("300.00").compareTo(resposta.valorTotal()));
    }

    @Test
    void criar_deveImpedirMatriculaEmPlanoInativo() {
        plano.setAtivo(false);
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(10L)).thenReturn(Optional.of(plano));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.criar(new MatriculaRequest(1L, 10L, null)));

        assertTrue(ex.getMessage().contains("inativo"));
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void criar_deveImpedirMatriculaDuplicada() {
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(10L)).thenReturn(Optional.of(plano));
        when(matriculaRepository.existsByAlunoIdAndPlanoIdAndStatus(1L, 10L, StatusMatricula.ATIVA)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.criar(new MatriculaRequest(1L, 10L, null)));

        assertTrue(ex.getMessage().contains("já possui uma matrícula ativa"));
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void criar_deveImpedirDataDeInicioNoPassado() {
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(10L)).thenReturn(Optional.of(plano));

        assertThrows(RegraNegocioException.class,
                () -> service.criar(new MatriculaRequest(1L, 10L, LocalDate.now().minusDays(1))));
    }

    @Test
    void criar_deveFalharQuandoAlunoNaoExiste() {
        when(alunoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.criar(new MatriculaRequest(1L, 10L, null)));
    }

    @Test
    void cancelar_deveImpedirCancelarMatriculaJaCancelada() {
        Matricula m = matriculaCom(StatusMatricula.CANCELADA);
        when(matriculaRepository.findWithAlunoAndPlanoById(100L)).thenReturn(Optional.of(m));

        assertThrows(RegraNegocioException.class, () -> service.cancelar(100L));
    }

    @Test
    void excluir_deveImpedirExclusaoDeMatriculaAtiva() {
        Matricula m = matriculaCom(StatusMatricula.ATIVA);
        when(matriculaRepository.findWithAlunoAndPlanoById(100L)).thenReturn(Optional.of(m));

        assertThrows(RegraNegocioException.class, () -> service.excluir(100L));
        verify(matriculaRepository, never()).delete(any(Matricula.class));
    }

    @Test
    void renovar_deveEstenderVencimentoAPartirDeHojeQuandoVencida() {
        Matricula m = matriculaCom(StatusMatricula.VENCIDA);
        m.setDataVencimento(LocalDate.now().minusDays(5));
        m.setValorTotal(new BigDecimal("300.00"));
        when(matriculaRepository.findWithAlunoAndPlanoById(100L)).thenReturn(Optional.of(m));
        when(matriculaRepository.existsByAlunoIdAndPlanoIdAndStatusAndIdNot(1L, 10L, StatusMatricula.ATIVA, 100L))
                .thenReturn(false);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));

        MatriculaResponse resposta = service.renovar(100L);

        assertEquals(StatusMatricula.ATIVA, resposta.status());
        assertEquals(LocalDate.now().plusMonths(3), resposta.dataVencimento());
        assertEquals(0, new BigDecimal("600.00").compareTo(resposta.valorTotal()));
    }

    private Matricula matriculaCom(StatusMatricula status) {
        Matricula m = new Matricula();
        m.setId(100L);
        m.setAluno(aluno);
        m.setPlano(plano);
        m.setDataInicio(LocalDate.now().minusMonths(1));
        m.setDataVencimento(LocalDate.now().plusMonths(2));
        m.setValorTotal(new BigDecimal("300.00"));
        m.setStatus(status);
        return m;
    }
}
