package br.com.fusex28gac.fusex_backend.service;

import br.com.fusex28gac.fusex_backend.dto.AgendamentoRequest;
import br.com.fusex28gac.fusex_backend.dto.AgendamentoResponse;
import br.com.fusex28gac.fusex_backend.model.Agendamento;
import br.com.fusex28gac.fusex_backend.model.Beneficiario;
import br.com.fusex28gac.fusex_backend.model.HorarioDisponivel;
import br.com.fusex28gac.fusex_backend.model.Medico;
import br.com.fusex28gac.fusex_backend.model.StatusAgendamento;
import br.com.fusex28gac.fusex_backend.model.StatusHorario;
import br.com.fusex28gac.fusex_backend.repository.AgendamentoRepository;
import br.com.fusex28gac.fusex_backend.repository.BeneficiarioRepository;
import br.com.fusex28gac.fusex_backend.repository.HorarioDisponivelRepository;
import br.com.fusex28gac.fusex_backend.repository.MedicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final HorarioDisponivelRepository horarioDisponivelRepository;
    private final MedicoRepository medicoRepository;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            BeneficiarioRepository beneficiarioRepository,
            HorarioDisponivelRepository horarioDisponivelRepository,
            MedicoRepository medicoRepository
    ) {
        this.agendamentoRepository = agendamentoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.horarioDisponivelRepository = horarioDisponivelRepository;
        this.medicoRepository = medicoRepository;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        if (request.getBeneficiarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o beneficiario");
        }

        if (request.getHorarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o horario");
        }

        if (request.getMedicoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o medico");
        }

        Beneficiario beneficiario = beneficiarioRepository.findById(request.getBeneficiarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiario nao encontrado"));

        if (Boolean.FALSE.equals(beneficiario.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beneficiario inativo");
        }

        Medico medico = medicoRepository.findById(request.getMedicoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medico nao encontrado"));

        if (Boolean.FALSE.equals(medico.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medico inativo");
        }

        HorarioDisponivel horario = buscarHorarioDisponivel(request.getHorarioId());
        horario.setStatus(StatusHorario.AGENDADO);

        Agendamento agendamento = new Agendamento();
        agendamento.setBeneficiario(beneficiario);
        agendamento.setMedico(medico);
        agendamento.setHorario(horario);
        agendamento.setDataHora(horario.getDataHora());
        agendamento.setObservacao(request.getObservacao());
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        return toResponse(agendamentoRepository.save(agendamento));
    }

    public List<AgendamentoResponse> listarTodos() {
        return agendamentoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AgendamentoResponse buscarPorId(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));

        return toResponse(agendamento);
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agendamento ja esta cancelado");
        }

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta ja realizada. Nao pode ser cancelada");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        liberarHorario(agendamento.getHorario());

        return toResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse remarcar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));

        if (request.getHorarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o novo horario");
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agendamento cancelado, nao pode ser remarcado");
        }

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta realizada nao pode ser remarcada");
        }

        if (agendamento.getHorario() != null && request.getHorarioId().equals(agendamento.getHorario().getId())) {
            agendamento.setObservacao(request.getObservacao());
            return toResponse(agendamentoRepository.save(agendamento));
        }

        HorarioDisponivel novoHorario = buscarHorarioDisponivel(request.getHorarioId());
        liberarHorario(agendamento.getHorario());
        novoHorario.setStatus(StatusHorario.AGENDADO);

        agendamento.setHorario(novoHorario);
        agendamento.setDataHora(novoHorario.getDataHora());
        agendamento.setObservacao(request.getObservacao());

        return toResponse(agendamentoRepository.save(agendamento));
    }

    public List<AgendamentoResponse> listarPorBeneficiario(Long beneficiarioId) {
        if (beneficiarioId == null || !beneficiarioRepository.existsById(beneficiarioId)) {
            return List.of();
        }

        return agendamentoRepository.findByBeneficiarioId(beneficiarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgendamentoResponse> listarPorMedico(Long medicoId) {
        if (medicoId == null || !medicoRepository.existsById(medicoId)) {
            return List.of();
        }

        return agendamentoRepository.findByMedicoId(medicoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AgendamentoResponse finalizar(Long id, StatusAgendamento status, String observacao) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agendamento cancelado nao pode ser alterado");
        }

        if (status != null) {
            agendamento.setStatus(status);
        }

        if (observacao != null) {
            agendamento.setObservacao(observacao);
        }

        return toResponse(agendamentoRepository.save(agendamento));
    }

    private AgendamentoResponse toResponse(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getBeneficiario().getId(),
                agendamento.getBeneficiario().getNomeCompleto(),
                agendamento.getHorario() == null ? null : agendamento.getHorario().getId(),
                agendamento.getDataHora(),
                agendamento.getStatus(),
                agendamento.getObservacao(),
                agendamento.getMedico() == null ? null : agendamento.getMedico().getId(),
                agendamento.getMedico() == null ? null : agendamento.getMedico().getNome(),
                agendamento.getMedico() == null ? null : agendamento.getMedico().getEspecialidade().name()
        );
    }

    private HorarioDisponivel buscarHorarioDisponivel(Long horarioId) {
        HorarioDisponivel horario = horarioDisponivelRepository.buscarPorIdComLock(horarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horario nao encontrado"));

        if (horario.getDataHora().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao e permitido agendar horario no passado");
        }

        if (horario.getStatus() != StatusHorario.DISPONIVEL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horario indisponivel");
        }

        return horario;
    }

    private void liberarHorario(HorarioDisponivel horario) {
        if (horario != null && horario.getStatus() == StatusHorario.AGENDADO) {
            horario.setStatus(StatusHorario.DISPONIVEL);
        }
    }
}
