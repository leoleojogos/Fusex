package br.com.fusex28gac.fusex_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class BeneficiarioRequest {

    @NotBlank(message = "Informe o nome completo")
    private String nomeCompleto;

    @NotBlank(message = "Informe o CPF")
    private String cpf;

    @NotBlank(message = "Informe o PRECCP")
    private String preccp;

    @NotNull(message = "Informe a data de nascimento")
    private LocalDateTime dataNascimento;

    @NotBlank(message = "Informe o tipo do beneficiário")
    private String tipo;
}
