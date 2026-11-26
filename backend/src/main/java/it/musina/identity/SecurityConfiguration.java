package it.musina.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("pbkdf2", Map.of("pbkdf2",
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean UserDetailsService userDetailsService(IdentityRepository repository) {
        return username -> repository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Credenziali non valide"));
    }

    @Bean LoginThrottle loginThrottle() { return new LoginThrottle(Clock.systemUTC()); }

    @Bean SecurityFilterChain security(HttpSecurity http, UserDetailsService users,
                                      PasswordEncoder passwords, LoginThrottle throttle,
                                      IdentityRepository repository) throws Exception {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        http.authenticationProvider(provider)
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Accesso richiesto"))
                        .accessDeniedHandler((req, res, ex) -> error(res, 403, "Operazione non consentita")))
                .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> {
                            throttle.succeeded(req.getRemoteAddr());
                            res.setStatus(204);
                        })
                        .failureHandler((req, res, ex) -> {
                            throttle.failed(req.getRemoteAddr());
                            error(res, 401, "Credenziali non valide");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutRequestMatcher(req -> req.getMethod().equals("POST")
                                && req.getServletPath().equals("/api/auth/logout")
                                && SecurityContextHolder.getContext().getAuthentication() != null
                                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof MusinaPrincipal)
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                                              FilterChain chain) throws ServletException, IOException {
                        if (req.getServletPath().equals("/api/auth/login") && req.getMethod().equals("POST")
                                && throttle.blocked(req.getRemoteAddr())) {
                            res.setHeader("Retry-After", "900");
                            error(res, 429, "Troppi tentativi. Riprova più tardi");
                            return;
                        }
                        chain.doFilter(req, res);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                                              FilterChain chain) throws ServletException, IOException {
                        var auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.getPrincipal() instanceof MusinaPrincipal user
                                && repository.findById(user.id()).filter(MusinaPrincipal::enabled).isEmpty()) {
                            SecurityContextHolder.clearContext();
                            var session = req.getSession(false);
                            if (session != null) { session.invalidate(); }
                            error(res, 401, "Accesso richiesto");
                            return;
                        }
                        chain.doFilter(req, res);
                    }
                }, AuthorizationFilter.class);
        return http.build();
    }

    static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
