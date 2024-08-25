package service.vaxapp.repository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import service.vaxapp.model.User;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import org.owasp.encoder.Encode;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        org.springframework.security.core.userdetails.User.UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(email);
        builder.password(user.getPPS());
        builder.roles(user.getRole());  // Ensure the role is set correctly
        return builder.build();
    }




    public static class InputSanitizationUtil {

        public static String sanitizeInput(String input) {
            return Encode.forHtml(input);
        }
    }


    @Service
    public class ValidationService {

        public String sanitizeInput(String input) {
            return Encode.forHtml(input);
        }
    }


}

