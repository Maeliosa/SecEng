package service.vaxapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import service.vaxapp.LoginAttemptService;
import org.springframework.security.authentication.LockedException;

import java.util.ArrayList;
import java.util.List;


@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException{
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (loginAttemptService.isBlocked(email)){
            throw new LockedException("User account is locked. Tryagain later.");
        }
        try{
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (user == null) {
                throw new UsernameNotFoundException("Error not found");
            }
            if(!password.equals(user.getPassword())) {
                throw new BadCredentialsException("Invalid password.");
            }
            loginAttemptService.loginSucceeded(email);
            return new UsernamePasswordAuthenticationToken(email, password,user.getAuthorities());
        } catch (BadCredentialsException | UsernameNotFoundException e){
            loginAttemptService.loginFailed(email);
            throw e;
        }
    }



    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);

    }
}
