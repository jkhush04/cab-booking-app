package com.cabbooking.security;



import com.cabbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{

       com.cabbooking.entity.User user= userRepository.findByEmail(email)
                 .orElseThrow(()->new UsernameNotFoundException("User not found: "+email));

         return new org.springframework.security.core.userdetails.User(
                 user.getEmail(),
                 user.getPassword(),
                 List.of(new SimpleGrantedAuthority(("ROLE_"+user.getRole().name())))
         );
    }
}
