package com.academia.mapper;

import com.academia.dto.PlanoRequest;
import com.academia.dto.PlanoResponse;
import com.academia.model.Plano;
import org.springframework.stereotype.Component;

/**
 * Conversão manual entre a entidade Plano e seus DTOs.
 */
@Component
public class PlanoMapper {

    public Plano toEntity(PlanoRequest request) {
        Plano plano = new Plano();
        updateEntity(plano, request);
        return plano;
    }

    public void updateEntity(Plano plano, PlanoRequest request) {
        plano.setNome(request.nome().trim());
        plano.setDescricao(request.descricao());
        plano.setValorMensal(request.valorMensal());
        plano.setDuracaoMeses(request.duracaoMeses());
        // Se "ativo" não for informado: novo plano nasce ativo; em atualização, mantém o valor atual.
        if (request.ativo() != null) {
            plano.setAtivo(request.ativo());
        }
    }

    public PlanoResponse toResponse(Plano plano) {
        return new PlanoResponse(
                plano.getId(),
                plano.getNome(),
                plano.getDescricao(),
                plano.getValorMensal(),
                plano.getDuracaoMeses(),
                plano.isAtivo());
    }
}
