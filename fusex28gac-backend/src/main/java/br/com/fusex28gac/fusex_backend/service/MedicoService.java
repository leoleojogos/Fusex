package br.com.fusex28gac.fusex_backend.service;

import br.com.fusex28gac.fusex_backend.dto.MedicoRequest;
import br.com.fusex28gac.fusex_backend.dto.MedicoResponse;
import br.com.fusex28gac.fusex_backend.model.EspecialidadeMedica;
import br.com.fusex28gac.fusex_backend.model.Medico;
import br.com.fusex28gac.fusex_backend.repository.MedicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import br.com.fusex28gac.fusex_backend.model.PerfilUsuario;
import br.com.fusex28gac.fusex_backend.model.Usuario;
import br.com.fusex28gac.fusex_backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public MedicoService(
            MedicoRepository medicoRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MedicoResponse criar(MedicoRequest request) {
        validarRequest(request);

        String crm = normalizarCrm(request.getCrm());

        if (medicoRepository.findByCrm(crm).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CRM ja cadastrado");
        }

        String loginDesejado = (request.getLogin() != null && !request.getLogin().isBlank())
                ? request.getLogin().trim()
                : gerarLoginPeloNome(request.getNome(), crm);
        String senhaDesejada = (request.getSenha() != null && !request.getSenha().isBlank())
                ? request.getSenha()
                : crm;

        if (usuarioRepository.findByLogin(loginDesejado).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login de acesso '" + loginDesejado + "' já está em uso.");
        }

        Medico medico = new Medico();
        medico.setNome(request.getNome().trim());
        medico.setCrm(crm);
        medico.setEspecialidade(request.getEspecialidade());
        medico.setAtivo(request.getAtivo() == null ? true : request.getAtivo());

        medico = medicoRepository.save(medico);

        // Criar usuário para acesso do médico
        Usuario usuario = new Usuario();
        usuario.setNome(medico.getNome());
        usuario.setLogin(loginDesejado);
        usuario.setSenhaHash(passwordEncoder.encode(senhaDesejada));
        usuario.setPerfil(PerfilUsuario.MEDICO);
        usuario.setMedico(medico);
        usuario.setAtivo(medico.getAtivo());
        usuarioRepository.save(usuario);

        return toResponse(medico);
    }

    public List<MedicoResponse> listarTodos() {
        return medicoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MedicoResponse buscarPorId(Long id) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medico não encontrado"));

        return toResponse(medico);
    }

    public List<MedicoResponse> listarPorEspecialidade(EspecialidadeMedica especialidade) {
        return medicoRepository.findByEspecialidadeAndAtivoTrue(especialidade)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MedicoResponse atualizar(Long id, MedicoRequest request) {
        validarRequest(request);

        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medico não encontrado"));

        String crm = normalizarCrm(request.getCrm());

        medicoRepository.findByCrm(crm)
                .filter(medicoEncontrado -> !medicoEncontrado.getId().equals(id))
                .ifPresent(medicoEncontrado -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CRM já cadastrado");
                });

        medico.setNome(request.getNome().trim());
        medico.setCrm(crm);
        medico.setEspecialidade(request.getEspecialidade());
        medico.setAtivo(request.getAtivo() == null ? true : request.getAtivo());

        medico = medicoRepository.save(medico);

        // Atualizar usuario correspondente caso exista
        Usuario usuario = usuarioRepository.findByMedicoId(id).orElse(null);
        if (usuario != null) {
            usuario.setNome(medico.getNome());
            usuario.setAtivo(medico.getAtivo());

            if (request.getLogin() != null && !request.getLogin().isBlank()) {
                usuario.setLogin(request.getLogin().trim());
            }

            if (request.getSenha() != null && !request.getSenha().isBlank()) {
                usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
            }

            usuarioRepository.save(usuario);
        }

        return toResponse(medico);
    }

    public MedicoResponse desativar(Long id) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medico não encontrado"));

        medico.setAtivo(false);
        medicoRepository.save(medico);

        usuarioRepository.findByMedicoId(id).ifPresent(u -> {
            u.setAtivo(false);
            usuarioRepository.save(u);
        });

        return toResponse(medico);
    }

    private void validarRequest(MedicoRequest request) {
        if(request.getNome() == null || request.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do medico");
        }

        if (request.getCrm() == null || request.getCrm().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o CRM do medico");
        }

        if (request.getEspecialidade() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a especialidade do medico");
        }
    }

    private String normalizarCrm(String crm) {
        return crm.trim().toUpperCase();
    }

    private String gerarLoginPeloNome(String nome, String crm) {
        if (nome == null || nome.isBlank()) return crm.trim();
        String limpo = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("^(dr\\.?|dra\\.?)\\s*", "")
                .replaceAll("[^a-z0-9]", "");
        return limpo.isBlank() ? crm.trim() : limpo;
    }

    private MedicoResponse toResponse(Medico medico) {
        String login = usuarioRepository.findByMedicoId(medico.getId())
                .map(Usuario::getLogin)
                .orElse(medico.getCrm());

        return new MedicoResponse(
                medico.getId(),
                medico.getNome(),
                medico.getCrm(),
                login,
                medico.getEspecialidade(),
                medico.getAtivo()
        );
    }
}
