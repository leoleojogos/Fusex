package br.com.fusex28gac.fusex_backend.service;

import br.com.fusex28gac.fusex_backend.dto.LoginResponse;
import br.com.fusex28gac.fusex_backend.model.Beneficiario;
import br.com.fusex28gac.fusex_backend.model.StatusCadastro;
import br.com.fusex28gac.fusex_backend.model.Usuario;
import br.com.fusex28gac.fusex_backend.repository.BeneficiarioRepository;
import br.com.fusex28gac.fusex_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private BeneficiarioRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse login(String login, String senha){
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe login e senha");
        }

        // Primeiro, verifica se é um usuário do sistema (Admin / Operador)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(login);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha inválida");
            }
            if (Boolean.FALSE.equals(usuario.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro inativo");
            }
            return new LoginResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    StatusCadastro.VALIDADO,
                    usuario.getPerfil().name()
            );
        }

        // Se não for usuário do sistema, tenta encontrar como Beneficiário (Paciente)
        String documento = normalizarDocumento(login);
        if (documento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um documento válido ou usuário cadastrado");
        }

        Optional<Beneficiario> user = repository.findByCpfOrPreccp(documento, documento);

        if(user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        Beneficiario beneficiario = user.get();

        validarSenha(beneficiario, senha);

        StatusCadastro status = beneficiario.getStatusCadastro();

        if (status == StatusCadastro.PENDENTE_VALIDACAO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu cadastro foi recebido e ainda aguarda validação pelo FUSEX. Tente novamente após a aprovação"
            );
        }

        if(status == StatusCadastro.REJEITADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu cadastro não foi aprovado. Entre em contato com o atendimento para mais informações"
            );
        }

        if (status == StatusCadastro.INATIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cadastro inativo. Procure o FUSEX para regularizar o acesso."
            );
        }

        if (Boolean.FALSE.equals(beneficiario.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro inativo");
        }

        if (beneficiario.getStatusCadastro() != StatusCadastro.VALIDADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro ainda não validado pelo FUSEX");
        }

        return new LoginResponse(
                beneficiario.getId(),
                beneficiario.getNomeCompleto(),
                beneficiario.getStatusCadastro(),
                "PACIENTE"
        );
    }

    private void validarSenha(Beneficiario beneficiario, String senha){
        String senhaHash = beneficiario.getSenhaHash();

        if (senhaHash != null && passwordEncoder.matches(senha, senhaHash)) {
            return;
        }

        String senhaInicial = gerarSenhaInicial(beneficiario.getCpf());

        if(senhaHash == null && senhaInicial.equals(senha)) {
            beneficiario.setSenhaHash(passwordEncoder.encode(senhaInicial));
            repository.save(beneficiario);
            return;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha inválida");
    }

    private String gerarSenhaInicial(String cpf) {
        String documento = normalizarDocumento(cpf);

        if(documento == null || documento.length() < 6) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha inicial indisponível para este cadastro");
        }

        return documento.substring(0, 6);
    }

    private String normalizarDocumento(String valor){
        if (valor == null){
            return null;
        }
        String normalizado = valor.replaceAll("\\D", "");
        return normalizado.isBlank() ? null : normalizado;
    }
}
