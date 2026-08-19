package br.com.fusex28gac.fusex_backend.service;

import br.com.fusex28gac.fusex_backend.model.Usuario;
import br.com.fusex28gac.fusex_backend.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;


    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cleanName = username != null ? username.trim() : "";
        Usuario usuario = usuarioRepository.findByLogin(cleanName)
                .orElseGet(() -> usuarioRepository.findAll().stream()
                        .filter(u -> u.getLogin().equalsIgnoreCase(cleanName) ||
                                     (u.getMedico() != null && u.getMedico().getCrm().equalsIgnoreCase(cleanName)))
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado"))
                );

        if(Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new UsernameNotFoundException("Usuario inativo");
        }

        return User.builder()
                .username(usuario.getLogin())
                .password(usuario.getSenhaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())))
                .build();
    }
}
