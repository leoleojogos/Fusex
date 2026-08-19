package br.com.fusex28gac.fusex_backend.service;

import br.com.fusex28gac.fusex_backend.dto.LoginResponse;
import br.com.fusex28gac.fusex_backend.model.Beneficiario;
import br.com.fusex28gac.fusex_backend.model.PerfilUsuario;
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public LoginResponse login(String login, String senha){
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe login e senha");
        }

        String loginLimpo = login.trim();
        String senhaLimpa = senha.trim();
        String loginNumerico = loginLimpo.replaceAll("\\D", "");

        // Primeiro, verifica se é um usuário do sistema (Admin / Operador / Medico)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(loginLimpo);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findAll().stream()
                    .filter(u -> {
                        if (u.getLogin() != null && u.getLogin().equalsIgnoreCase(loginLimpo)) return true;
                        if (u.getMedico() != null && u.getMedico().getCrm() != null) {
                            String crm = u.getMedico().getCrm();
                            if (crm.equalsIgnoreCase(loginLimpo)) return true;
                            if (!loginNumerico.isBlank() && crm.replaceAll("\\D", "").equals(loginNumerico)) return true;
                        }
                        return false;
                    })
                    .findFirst();
        }
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            boolean senhaOk = passwordEncoder.matches(senha, usuario.getSenhaHash()) ||
                              passwordEncoder.matches(senhaLimpa, usuario.getSenhaHash());
            if (!senhaOk) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha inválida");
            }
            if (Boolean.FALSE.equals(usuario.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro inativo");
            }
            Long idRetorno = (usuario.getPerfil() == PerfilUsuario.MEDICO && usuario.getMedico() != null)
                    ? usuario.getMedico().getId()
                    : usuario.getId();

            System.out.println(">>> [AuthService] Login efetuado com SUCESSO! Nome: " + usuario.getNome() + " | Perfil: " + usuario.getPerfil() + " | Retorno ID: " + idRetorno);

            return new LoginResponse(
                    idRetorno,
                    usuario.getNome(),
                    StatusCadastro.VALIDADO,
                    usuario.getPerfil().name()
            );
        }

        // Se não for usuário do sistema, tenta encontrar como Beneficiário (Paciente)
        String documento = normalizarDocumento(login);
        if (documento != null) {
            Optional<Beneficiario> user = repository.findByCpfOrPreccp(documento, documento);
            if (user.isPresent()) {
                Beneficiario beneficiario = user.get();
                validarSenha(beneficiario, senha);

                StatusCadastro status = beneficiario.getStatusCadastro();
                if (status == StatusCadastro.PENDENTE_VALIDACAO) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Seu cadastro foi recebido e ainda aguarda validação pelo FUSEX. Tente novamente após a aprovação");
                }
                if (status == StatusCadastro.REJEITADO) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Seu cadastro não foi aprovado. Entre em contato com o atendimento para mais informações");
                }
                if (status == StatusCadastro.INATIVO || Boolean.FALSE.equals(beneficiario.getAtivo())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Cadastro inativo. Procure o FUSEX para regularizar o acesso.");
                }
                if (status != StatusCadastro.VALIDADO) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro ainda não validado pelo FUSEX");
                }

                return new LoginResponse(
                        beneficiario.getId(),
                        beneficiario.getNomeCompleto(),
                        beneficiario.getStatusCadastro(),
                        "PACIENTE"
                );
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário ou documento não encontrado");
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
