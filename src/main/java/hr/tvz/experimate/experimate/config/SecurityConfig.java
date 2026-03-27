package hr.tvz.experimate.experimate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration      //For Spring to know this is the configuration class
@EnableWebSecurity      //For Spring to know this class will override the default security filter chain
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(request -> request.anyRequest().permitAll());

        return http.build();
    }
}
