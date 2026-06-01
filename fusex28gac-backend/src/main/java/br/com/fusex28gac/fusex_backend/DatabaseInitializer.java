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

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
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
    }
}
