package br.com.fusex28gac.fusex_backend.dto;

import br.com.fusex28gac.fusex_backend.model.EspecialidadeMedica;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicoRequest {
    private String login;
    private String senha;
    private String nome;
    private String crm;
    private EspecialidadeMedica especialidade;
    private Boolean ativo;
}
