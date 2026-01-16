package org.zerock.com.example.user;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1) 로그인만 필요한 경로
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns(
                        "/cart/**",
                        "/checkout/**",
                        "/order/**",
                        "/board/write/**"
                )
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );

        // 2) 관리자 전용 경로
        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
    }
}
