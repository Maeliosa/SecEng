package service.vaxapp.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import service.vaxapp.repository.MyUserDetailsService;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@Configuration
@EnableWebSecurity
public class AccessControlConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(myUserDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    // Allow access to all static resources (CSS, JS, images, etc.)
                    .antMatchers("/styles/**", "/js/**", "/images/**", "/fonts/**", "/scripts/**").permitAll()
                    // Allow access to public pages
                    .antMatchers("/", "/login", "/complete-appointment/", "/dashboard","/make-appointment", "/find-user","/mark-answer", "/question", "/cancel-appointment/", "/logout", "/profile/**", "/register", "/profile", "/error", "/404", "/ask-a-question", "/forum", "/stats", "/verify-mfa", "/enable-mfa", "/mfasetup").permitAll()
                    // Restrict access to admin pages
                    .antMatchers("/admin/**").hasRole("ADMIN")
                    // Restrict access to user pages
                    .antMatchers("/user/**").authenticated()
                    // Any other requests require authentication
                    .anyRequest().authenticated()
                .and()
                .formLogin()
                    .loginPage("/login")
                    .loginProcessingUrl("/profile/") // URL to submit login form (should match the form's action)
                    .defaultSuccessUrl("/profile", true)
                    .permitAll()
                .and()
                .logout()
                    .permitAll()
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        // Register the filter using addFilterAfter instead of addFilterBefore
        http.addFilterAfter(new SameSiteFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Custom filter to set SameSite attribute Swith enhanced logging
    @Component
    public static class SameSiteFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            filterChain.doFilter(request, response);

            // Log to confirm the filter is executing
            System.out.println("Executing SameSiteFilter...");

            // Log all Set-Cookie headers before modification
            Collection<String> headers = response.getHeaders("Set-Cookie");
            if (headers != null) {
                for (String header : headers) {
                    System.out.println("Original Set-Cookie header: " + header);
                }

                // Modify each Set-Cookie header to add SameSite=Lax if not already present
                boolean firstHeader = true;
                for (String header : headers) {
                    if (!header.toLowerCase().contains("samesite")) {
                        header += "; SameSite=Lax";
                    }

                    // Set or add the header back to the response
                    if (firstHeader) {
                        response.setHeader("Set-Cookie", header);
                        firstHeader = false;
                    } else {
                        response.addHeader("Set-Cookie", header);
                    }

                    // Log the updated Set-Cookie header
                    System.out.println("Updated Set-Cookie header: " + header);
                }
            }
        }
    }


}

