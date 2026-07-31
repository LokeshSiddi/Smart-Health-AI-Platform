package com.fitness.gateway.filter;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain filterChain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        RegisterRequest request = getUserDetails(token);

        if(userId == null) {
            userId = request.getKeycloakId();
        }

        if(userId != null && token != null) {
            String finalUserId = userId;
            return userService.validateUser(userId)
                    .flatMap(exist -> {
                        if(!exist) {
                            // Register User

                            if (request != null) {
                                return userService.registerUser(request).then(Mono.empty());
                            } else {
                                return Mono.empty();
                            }
                        } else {
                            log.info("User Already Exist, Skipping Sync.");
                            return Mono.empty();
                        }
                    })
                    .then(Mono.defer(() -> {
                        ServerHttpRequest mutatedRequest = (ServerHttpRequest) exchange.getRequest().mutate()
                                .header("X-User-ID", finalUserId)
                                .build();
                        return filterChain.filter(exchange.mutate().request(mutatedRequest).build());
                    }));
        }

        return filterChain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {

        try {
            String tokenWithoutBearer = token.replace("Bearer ", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();


            RegisterRequest request = new RegisterRequest();

            request.setKeycloakId(claims.getStringClaim("sub"));
            request.setEmail(claims.getStringClaim("email"));
            request.setFirstName(claims.getStringClaim("given_name"));
            request.setLastName(claims.getStringClaim("family_name"));
            request.setPassword("password1");

            return request;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
