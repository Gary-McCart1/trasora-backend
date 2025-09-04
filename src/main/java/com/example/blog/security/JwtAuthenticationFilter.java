package com.example.blog.security;

import com.example.blog.service.CustomUserDetailsService;
import com.example.blog.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JWT validation for OPTIONS and public auth endpoints
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/signup") ||
                path.startsWith("/api/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtil.extractTokenFromCookie(request);
        System.out.println("JWT Token from cookie: " + token);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(token)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("Authenticated user: " + userDetails.getUsername());
                System.out.println("Authorities: " + userDetails.getAuthorities());
            }
        }

        filterChain.doFilter(request, response);
    }
}
