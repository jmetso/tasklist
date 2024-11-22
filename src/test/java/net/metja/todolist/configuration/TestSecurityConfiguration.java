package net.metja.todolist.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-11-19
 */
@Configuration
@EnableWebSecurity()
@EnableGlobalMethodSecurity(securedEnabled = true)
public class TestSecurityConfiguration{

    private static final Logger logger = LoggerFactory.getLogger(TestSecurityConfiguration.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        logger.info("Users: user,admin,view");
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withDefaultPasswordEncoder().username("user").password("{noop}user").roles("USER").build());
        manager.createUser(User.withDefaultPasswordEncoder().username("admin").password("{noop}admin").roles("ADMIN").build());
        manager.createUser(User.withDefaultPasswordEncoder().username("view").password("{noop}view").roles("VIEW").build());
        return manager;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost", "https://localhost"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
            .csrf((csrf) -> csrf.disable())
            .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                .requestMatchers("/api/v1/password/generate/**").permitAll()
                .requestMatchers("/api/v1/items").hasAnyRole("ADMIN", "USER", "VIEW")
                .requestMatchers("/api/v1/items/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/api/v1/new").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/api/v1/logout").hasAnyRole("ADMIN", "USER", "VIEW")
                .requestMatchers("/*").authenticated()
            )
            .rememberMe((rememberMe) -> rememberMe.tokenValiditySeconds(1209600))
            .httpBasic(withDefaults())
            .formLogin(withDefaults());

        return http.build();
    }

}
