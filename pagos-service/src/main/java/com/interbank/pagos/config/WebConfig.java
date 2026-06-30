package com.interbank.pagos.config;

import com.interbank.pagos.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protegemos las rutas del microservicio de pagos
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/ping-auth", "/pagos/**");
    }
}
