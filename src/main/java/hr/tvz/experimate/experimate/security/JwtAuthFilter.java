package hr.tvz.experimate.experimate.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;

    private final Set<String> PUBLIC_PATH_PREFIXES = Set.of("/api/auth");

    private final Map<String, String> PUBLIC_EXACT_ENDPOINTS = Map.of(
            "POST", "/api/user"
    );

    public JwtAuthFilter(JwtService jwtService,
                         AppUserDetailsService userDetailsService,
                         @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (isPublicEndpoint(request)) {
            log.debug("PUBLIC ENDPOINT: [{}]", request.getServletPath());
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Authorization header not present");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (JwtException e) {
            log.debug(e.getMessage());
            resolver.resolveException(request, response, null, new BadCredentialsException("JWT invalid.", e));
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(token, userDetails)) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null,
                            userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns true if the request targets a public endpoint that should bypass JWT validation.
     * Prefix entries (e.g. /api/auth) match any subpath; exact entries match only the specific path and method.
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        boolean prefixMatch = PUBLIC_PATH_PREFIXES.stream()
                .anyMatch(path::startsWith);

        boolean exactMatch = PUBLIC_EXACT_ENDPOINTS.entrySet().stream()
                .anyMatch(e -> e.getValue().equals(path) && e.getKey().equalsIgnoreCase(method));

        return prefixMatch || exactMatch;
    }
}
