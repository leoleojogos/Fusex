package br.com.fusex28gac.fusex_backend;

import br.com.fusex28gac.fusex_backend.model.Beneficiario;
import br.com.fusex28gac.fusex_backend.model.StatusCadastro;
import br.com.fusex28gac.fusex_backend.model.Usuario;
import br.com.fusex28gac.fusex_backend.model.PerfilUsuario;
import br.com.fusex28gac.fusex_backend.repository.BeneficiarioRepository;
import br.com.fusex28gac.fusex_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

import br.com.fusex28gac.fusex_backend.model.EspecialidadeMedica;
import br.com.fusex28gac.fusex_backend.model.Medico;
import br.com.fusex28gac.fusex_backend.repository.MedicoRepository;

import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Remove a restrição CHECK antiga da tabela usuarios caso tenha sido criada pelo Hibernate anteriormente
        try {
            jdbcTemplate.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_perfil_check");
        } catch (Exception e) {
            // Ignora caso a tabela ou constraint não exista
        }
        // Valida beneficiários pendentes
        List<Beneficiario> beneficiarios = beneficiarioRepository.findAll();
        boolean updatedBeneficiarios = false;
        for (Beneficiario b : beneficiarios) {
            if (b.getStatusCadastro() == StatusCadastro.PENDENTE_VALIDACAO) {
                b.setStatusCadastro(StatusCadastro.VALIDADO);
                beneficiarioRepository.save(b);
                updatedBeneficiarios = true;
            }
        }
        if (updatedBeneficiarios) {
            System.out.println(">>> [DatabaseInitializer] Todos os beneficiários pendentes foram validados automaticamente para teste.");
        }

        // Se a tabela de usuários estiver vazia, cria um usuário admin padrão
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNome("Admin");
            admin.setLogin("admin");
            admin.setSenhaHash(passwordEncoder.encode("admin"));
            admin.setPerfil(PerfilUsuario.ADMIN);
            admin.setAtivo(true);
            usuarioRepository.save(admin);
            System.out.println(">>> [DatabaseInitializer] Nenhum usuário encontrado. Criado usuário administrador padrão: login 'admin' e senha 'admin'.");
        } else {
            // Criptografa senhas dos usuários que estão em texto plano
            List<Usuario> usuarios = usuarioRepository.findAll();
            boolean updatedUsuarios = false;
            for (Usuario u : usuarios) {
                String hash = u.getSenhaHash();
                if (hash != null && !hash.startsWith("$2a$") && !hash.startsWith("$2b$") && !hash.startsWith("$2y$")) {
                    u.setSenhaHash(passwordEncoder.encode(hash));
                    usuarioRepository.save(u);
                    updatedUsuarios = true;
                    System.out.println(">>> [DatabaseInitializer] Senha em texto plano do usuário '" + u.getLogin() + "' foi criptografada com BCrypt.");
                }
            }
            if (updatedUsuarios) {
                System.out.println(">>> [DatabaseInitializer] Senhas em texto plano foram atualizadas no banco.");
            }
        }

        // Se a tabela de médicos estiver vazia, cria um médico demonstrativo inicial
        if (medicoRepository.count() == 0) {
            Medico m = new Medico();
            m.setNome("Dr. Jorge Silva");
            m.setCrm("123456");
            m.setEspecialidade(EspecialidadeMedica.CLINICO_GERAL);
            m.setAtivo(true);
            medicoRepository.save(m);
            System.out.println(">>> [DatabaseInitializer] Criado médico demonstrativo inicial: Dr. Jorge Silva | CRM: 123456");
        }

        // Garante que todo médico cadastrado possua uma conta de usuário vinculada (login = Nome gerado, senha = CRM)
        List<Medico> medicos = medicoRepository.findAll();
        for (Medico m : medicos) {
            String loginDesejado = gerarLoginPeloNome(m.getNome(), m.getCrm());
            String senhaInicial = m.getCrm().trim();

            Usuario u = usuarioRepository.findByMedicoId(m.getId())
                    .orElseGet(() -> usuarioRepository.findByLogin(loginDesejado).orElse(new Usuario()));

            u.setNome(m.getNome());
            u.setLogin(loginDesejado);
            u.setSenhaHash(passwordEncoder.encode(senhaInicial));
            u.setPerfil(PerfilUsuario.MEDICO);
            u.setMedico(m);
            u.setAtivo(m.getAtivo());
            usuarioRepository.save(u);
            System.out.println(">>> [DatabaseInitializer] Conta do médico '" + m.getNome() + "' pronta -> Usuário/Login: '" + loginDesejado + "' | Senha (CRM): '" + senhaInicial + "'");
        }
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
}
