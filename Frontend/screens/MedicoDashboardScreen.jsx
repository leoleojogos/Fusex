import React, { useState, useMemo } from 'react';
import { 
  Users, 
  CalendarCheck2, 
  CheckCircle2, 
  XCircle, 
  Search, 
  FileEdit, 
  Clock, 
  Stethoscope, 
  AlertCircle,
  X,
  Filter
} from 'lucide-react';
import StatCard from '../components/StatCard.jsx';

export default function MedicoDashboardScreen({
  appointments = [],
  formatDateTime,
  onFinalizarAppointment,
  isLoading,
  errorMessage
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('TODOS');
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [modalStatus, setModalStatus] = useState('REALIZADO');
  const [modalObservacao, setModalObservacao] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');

  // Filtering
  const filteredAppointments = useMemo(() => {
    return appointments.filter((item) => {
      const matchesSearch = 
        !searchTerm.trim() ||
        item.nomeBeneficiario?.toLowerCase().includes(searchTerm.toLowerCase().trim()) ||
        String(item.id).includes(searchTerm.trim());

      const matchesStatus = 
        statusFilter === 'TODOS' || item.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [appointments, searchTerm, statusFilter]);

  // Statistics
  const stats = useMemo(() => {
    const agendados = appointments.filter((a) => a.status === 'AGENDADO').length;
    const realizados = appointments.filter((a) => a.status === 'REALIZADO').length;
    const cancelados = appointments.filter((a) => a.status === 'CANCELADO' || a.status === 'NAO_COMPARECEU').length;
    return {
      total: appointments.length,
      agendados,
      realizados,
      cancelados
    };
  }, [appointments]);

  const openFinalizarModal = (appointment) => {
    setSelectedAppointment(appointment);
    setModalStatus(appointment.status === 'REALIZADO' ? 'REALIZADO' : 'REALIZADO');
    setModalObservacao(appointment.observacao || '');
    setActionError('');
  };

  const closeModal = () => {
    setSelectedAppointment(null);
    setModalObservacao('');
    setActionError('');
  };

  const handleSaveAtendimento = async (e) => {
    e.preventDefault();
    if (!selectedAppointment) return;

    setIsSubmitting(true);
    setActionError('');
    try {
      await onFinalizarAppointment(selectedAppointment.id, modalStatus, modalObservacao);
      closeModal();
    } catch (err) {
      setActionError(err.message || 'Erro ao atualizar atendimento.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusChipClass = (status) => {
    switch (status) {
      case 'AGENDADO':
        return 'status-chip ok';
      case 'REALIZADO':
        return 'status-chip ok';
      case 'CANCELADO':
      case 'NAO_COMPARECEU':
        return 'status-chip canceled';
      default:
        return 'status-chip';
    }
  };

  return (
    <section className="view-grid">
      {/* Hero Banner for Doctor */}
      <div className="hero-panel" style={{ background: 'linear-gradient(135deg, #0f4c81 0%, #1e70b8 100%)', color: '#fff' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Stethoscope size={28} style={{ color: '#60a5fa' }} />
          <div>
            <h2 style={{ margin: 0, color: '#fff' }}>Agenda Médica & Pacientes</h2>
            <p style={{ margin: '4px 0 0 0', opacity: 0.9 }}>
              Gerencie seus atendimentos do dia, acompanhe os pacientes agendados e registre a evolução médica.
            </p>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="stats-grid">
        <StatCard
          title="Consultas Agendadas"
          value={`${stats.agendados}`}
          hint="pendentes de atendimento"
          icon={<CalendarCheck2 size={18} />}
          tone="blue"
        />
        <StatCard
          title="Atendimentos Realizados"
          value={`${stats.realizados}`}
          hint="consultas concluídas"
          icon={<CheckCircle2 size={18} />}
          tone="green"
        />
        <StatCard
          title="Ausências / Cancelados"
          value={`${stats.cancelados}`}
          hint="não compareceu ou cancelado"
          icon={<XCircle size={18} />}
          tone="red"
        />
        <StatCard
          title="Total de Pacientes"
          value={`${stats.total}`}
          hint="registrados na sua agenda"
          icon={<Users size={18} />}
          tone="blue"
        />
      </div>

      {/* Main Patient Table Panel */}
      <section className="panel table-panel">
        <div className="panel-title-row" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3>Lista de Pacientes & Consultas</h3>
            <p style={{ fontSize: '0.85rem', color: '#56738f', margin: 0 }}>
              Exibindo {filteredAppointments.length} de {appointments.length} consultas
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
            {/* Search Input */}
            <div style={{ position: 'relative', minWidth: '240px' }}>
              <input
                type="text"
                placeholder="Buscar paciente ou ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{
                  paddingLeft: '34px',
                  width: '100%',
                  fontSize: '0.88rem'
                }}
              />
              <Search 
                size={16} 
                style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#56738f' }} 
              />
            </div>

            {/* Filter Select */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Filter size={15} style={{ color: '#56738f' }} />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                style={{
                  padding: '9px 12px',
                  borderRadius: '10px',
                  border: '1px solid #c7d9eb',
                  background: '#fff',
                  fontSize: '0.88rem',
                  outline: 'none',
                  color: '#12243a'
                }}
              >
                <option value="TODOS">Todos os Status</option>
                <option value="AGENDADO">Agendado</option>
                <option value="REALIZADO">Realizado</option>
                <option value="NAO_COMPARECEU">Não Compareceu</option>
                <option value="CANCELADO">Cancelado</option>
              </select>
            </div>
          </div>
        </div>

        {errorMessage && (
          <div style={{ padding: '12px 16px', background: '#fdeeee', color: '#b83a3a', borderRadius: '8px', marginBottom: '16px' }}>
            <AlertCircle size={16} style={{ display: 'inline', marginRight: '6px' }} />
            {errorMessage}
          </div>
        )}

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Data & Hora</th>
                <th>Paciente</th>
                <th>Status</th>
                <th>Observações Médicas</th>
                <th style={{ textAlign: 'right' }}>Ações</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '30px' }}>
                    Carregando a sua agenda de atendimentos...
                  </td>
                </tr>
              ) : filteredAppointments.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '30px' }}>
                    Nenhum paciente ou consulta encontrada.
                  </td>
                </tr>
              ) : (
                filteredAppointments.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Clock size={14} color="#1f7ec8" />
                        {formatDateTime(item.dataHora)}
                      </strong>
                      <small style={{ color: '#68829c' }}>Agendamento #{item.id}</small>
                    </td>
                    <td>
                      <strong style={{ fontSize: '0.95rem', color: '#0f4c81' }}>
                        {item.nomeBeneficiario || 'Paciente Desconhecido'}
                      </strong>
                      <small style={{ color: '#56738f' }}>ID Paciente: #{item.beneficiarioId}</small>
                    </td>
                    <td>
                      <span className={getStatusChipClass(item.status)}>
                        {item.status === 'AGENDADO' && 'AGENDADO'}
                        {item.status === 'REALIZADO' && '✓ REALIZADO'}
                        {item.status === 'NAO_COMPARECEU' && '✕ NÃO COMPARECEU'}
                        {item.status === 'CANCELADO' && '✕ CANCELADO'}
                      </span>
                    </td>
                    <td style={{ maxWidth: '280px' }}>
                      <p style={{ margin: 0, fontSize: '0.88rem', color: item.observacao ? '#12243a' : '#889cb0', fontStyle: item.observacao ? 'normal' : 'italic' }}>
                        {item.observacao || 'Sem observações registradas.'}
                      </p>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {item.status !== 'CANCELADO' && (
                        <button
                          className="btn btn-ghost"
                          style={{ padding: '6px 12px', fontSize: '0.82rem' }}
                          onClick={() => openFinalizarModal(item)}
                        >
                          <FileEdit size={14} />
                          Evoluir / Finalizar
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Clinical Record Modal */}
      {selectedAppointment && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(15, 30, 50, 0.6)',
            backdropFilter: 'blur(4px)',
            zIndex: 999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
          }}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '16px',
              maxWidth: '520px',
              width: '100%',
              boxShadow: '0 20px 40px rgba(0,0,0,0.25)',
              overflow: 'hidden',
              animation: 'fadeUp 0.25s ease'
            }}
          >
            {/* Modal Header */}
            <div
              style={{
                padding: '18px 24px',
                background: 'linear-gradient(135deg, #0f4c81, #1e70b8)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <FileEdit size={20} />
                <h3 style={{ margin: 0, color: '#fff', fontSize: '1.1rem' }}>Evolução Clínica do Atendimento</h3>
              </div>
              <button
                onClick={closeModal}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#fff',
                  cursor: 'pointer',
                  padding: '4px',
                  borderRadius: '6px'
                }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleSaveAtendimento} style={{ padding: '24px' }}>
              <div style={{ marginBottom: '16px', background: '#f4f8fc', padding: '12px 16px', borderRadius: '10px' }}>
                <p style={{ margin: 0, fontSize: '0.85rem', color: '#56738f' }}>Paciente:</p>
                <strong style={{ fontSize: '1.05rem', color: '#0f4c81' }}>{selectedAppointment.nomeBeneficiario}</strong>
                <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem', color: '#56738f' }}>
                  Data: {formatDateTime(selectedAppointment.dataHora)} (Consulta #{selectedAppointment.id})
                </p>
              </div>

              {actionError && (
                <div style={{ padding: '10px', background: '#fdeeee', color: '#b83a3a', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                  {actionError}
                </div>
              )}

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '6px', color: '#12243a' }}>
                  Status da Consulta:
                </label>
                <select
                  value={modalStatus}
                  onChange={(e) => setModalStatus(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #c7d9eb',
                    outline: 'none',
                    fontSize: '0.9rem'
                  }}
                >
                  <option value="REALIZADO">REALIZADO (Consulta concluída)</option>
                  <option value="NAO_COMPARECEU">NÃO COMPARECEU (Paciente ausente)</option>
                  <option value="AGENDADO">AGENDADO (Manter em aberto)</option>
                </select>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '6px', color: '#12243a' }}>
                  Observações Médicas / Prontuário Resumido:
                </label>
                <textarea
                  rows={4}
                  placeholder="Escreva aqui a conduta médica, receita emitida ou orientações prestadas ao paciente..."
                  value={modalObservacao}
                  onChange={(e) => setModalObservacao(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #c7d9eb',
                    outline: 'none',
                    fontSize: '0.9rem',
                    fontFamily: 'inherit',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={closeModal}
                  disabled={isSubmitting}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Salvando...' : 'Salvar Evolução'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
