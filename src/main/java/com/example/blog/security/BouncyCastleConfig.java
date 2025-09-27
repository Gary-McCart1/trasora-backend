package com.example.blog.security;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;
import java.security.Security;

@Configuration
public class BouncyCastleConfig {

    @PostConstruct
    public void addBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("Bouncy Castle provider added");
        }
    }
}
