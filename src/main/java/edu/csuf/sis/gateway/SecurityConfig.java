package edu.csuf.sis.gateway;

import edu.csuf.sis.identity.IAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * The API Gateway from the component diagram: checks each request's session token through IAuth
 * and only lets each role reach its own portal's API.
 */
@Configuration
public class SecurityConfig {

    /** The portals load scripts, styles and data only from this server. */
    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain apiGateway(HttpSecurity http, IAuth auth) throws Exception {
        http
                // Tokens travel in the Authorization header, which browsers never add on their own,
                // so cross-site request forgery does not apply.
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new SessionTokenFilter(auth), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, denied) ->
                                ApiErrorHandler.write(response, HttpStatus.UNAUTHORIZED, "Please sign in."))
                        .accessDeniedHandler((request, response, denied) ->
                                ApiErrorHandler.write(response, HttpStatus.FORBIDDEN,
                                        "Your account cannot use this part of the system.")))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/professor/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // The portal pages themselves are public; every bit of data they show comes from /api.
                        .anyRequest().permitAll());
        return http.build();
    }
}
