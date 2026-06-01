package br.com.fusex28gac.fusex_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/beneficiarios/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horarios/disponiveis").permitAll()
                .requestMatchers(HttpMethod.GET, "/agendamentos/beneficiario/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/agendamentos").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/cancelamento").permitAll()
                .requestMatchers(HttpMethod.GET, "/medicos/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/medicos").hasAnyRole("ADMIN", "OPERADOR_FUSEX")
                        .requestMatchers(HttpMethod.PUT, "/medicos/**").hasAnyRole("ADMIN", "OPERADOR_FUSEX")
                        .requestMatchers(HttpMethod.PATCH, "/medicos/**").hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.GET, "/beneficiarios").hasAnyRole("ADMIN", "OPERADOR_FUSEX")
                        .requestMatchers(HttpMethod.PATCH, "/beneficiarios/*/validacao")
                            .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.POST, "/agendamentos")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.GET, "/agendamentos/**")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/cancelamento")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/remarcacao")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.POST, "/horarios")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.POST, "/horarios/geracao")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.GET, "/horarios")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.PATCH, "/horarios/*/bloqueio")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.PATCH, "/horarios/bloqueio-intervalo")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")

                        .requestMatchers(HttpMethod.PATCH, "/horarios/*/desbloqueio")
                        .hasAnyRole("ADMIN", "OPERADOR_FUSEX")


                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
