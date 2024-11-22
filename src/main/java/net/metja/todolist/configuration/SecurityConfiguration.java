package net.metja.todolist.configuration;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Value("${CORS_HOSTS:http://localhost,https://localhost}")
    private String corsHosts;
    private static Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);
    private PersistentTokenRepository persistentTokenRepository;
    private DatabaseManager databaseManager;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        for(UserAccount userAccount: this.databaseManager.getUsers()) {
            logger.info("Adding user "+userAccount.getUsername()+" with roles: "+userAccount.getRoles());
            manager.createUser(User.builder()
                    .username(userAccount.getUsername())
                    .password(userAccount.getPassword())
                    .roles(userAccount.getRoles().toArray(new String[0]))
                .build());
        }
        return manager;
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        logger.debug("CORS hosts: "+this.corsHosts);
        StringTokenizer st = new StringTokenizer(this.corsHosts, ",");
        List<String> corsHosts = new LinkedList<>();
        while(st.hasMoreTokens()) {
            corsHosts.add(st.nextToken());
        }
        configuration.setAllowedOrigins(corsHosts);
        configuration.setAllowedMethods(Arrays.asList("GET","POST"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf((csrf) -> csrf.disable())
            .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/password/generate/**").permitAll()
                .requestMatchers("/webjars/patternfly__patternfly/**").permitAll()
                .requestMatchers("/webjars/jquery/**").permitAll()
                .requestMatchers("/fontawesome-free-5.10.2-web/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/js/**").permitAll()
                .requestMatchers("/login.html").permitAll()
                .requestMatchers("/api/v1/hello/**").permitAll()
                .requestMatchers("/api/v1/items").hasAnyRole("ADMIN","USER","VIEW")
                .requestMatchers("/api/v1/items/**").hasAnyRole("ADMIN","USER")
                .requestMatchers("/api/v1/new").hasAnyRole("ADMIN","USER")
                .anyRequest().authenticated()
            )
            .rememberMe((rememberMe) -> rememberMe
                .tokenRepository(this.persistentTokenRepository)
                .tokenValiditySeconds(1209600)
            )
            .formLogin((formLogin) -> formLogin
                .loginPage("/login.html")
                .loginProcessingUrl("/authentication")
                .failureUrl("/login.html?error=true")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout((logout) -> logout
                .invalidateHttpSession(true)
                .deleteCookies()
                .logoutSuccessUrl("/"));

        return http.build();
    }

    @Autowired
    public void setPersistentTokenRepository(PersistentTokenRepository persistentTokenRepository) {
        this.persistentTokenRepository = persistentTokenRepository;
    }

    @Autowired
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}