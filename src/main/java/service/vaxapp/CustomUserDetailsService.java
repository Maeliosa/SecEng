package service.vaxapp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import service.vaxapp.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       User user = userRepository.findByEmail(username)
               .orElseThrow(() -> new UsernameNotFoundException("not found"+ username));



       Collection<SimpleGrantedAuthority> authorities = user.getClass().stream()
               .map(role -> new SimpleGrantedAuthority("role"+ role))
               .collect(Collectors.toList());

       return new org.springframework.security.core.userdetails.User(
               user.getEmail(),
               user.getPassword(),
               authorities
       );
    }



}
