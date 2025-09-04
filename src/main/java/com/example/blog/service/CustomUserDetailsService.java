package com.example.blog.service;

import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser user;

        if (login.contains("@")) {
            user = userRepository.findByEmail(login)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + login));
        } else {
            user = userRepository.findByUsername(login)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + login));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                getAuthorities(user) // <-- here
        );
    }


    private boolean isEmail(String input) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return input.matches(emailRegex);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(AppUser user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }


}
