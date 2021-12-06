package io.security.basicsecurity;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Configuration
@EnableWebSecurity // 필수 설정
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private final UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .csrf().disable(); // csrf설정은 기본적으로 활성화 되어있음 사용하지 않을 경우에만 선언해주면 된다.

        // 인가 정책
        http
                .authorizeRequests()
                .anyRequest().authenticated();
//                .anyRequest().permitAll();
        // 인증 정책
        http
                .formLogin() // formLogin방식
//                    .loginPage("/loginPage") // 로그인 페이지
                    .permitAll()
                    .defaultSuccessUrl("/") // 로그인 성공시 url
                    .failureUrl("/loginPage") // 로그인 실패시 url
                    .usernameParameter("userId") // form의 id 파라미터명
                    .passwordParameter("passwd") // form의 password 파라미터명
                    .loginProcessingUrl("/login_proc") // form의 action 경로

                    .successHandler(new AuthenticationSuccessHandler() { // 성공시 success 핸들러를 호출한다. 추가로 사용해보자
                        // 로그인 성공시 authentication 정보를 매개변수로 -
                        @Override
                        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                            System.out.println("authentication : " + authentication.getName());
                            response.sendRedirect("/");
                        }
                    })
                    .failureHandler(new AuthenticationFailureHandler() { // 실패시 fail 핸들러를 호출한다. 추가로 사용해보자
                        // 로그인 실패시 exception 정보를 매개변수로 -
                        @Override
                        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                            System.out.println("exception : " + exception.getMessage());
                            response.sendRedirect("/loginPage");
                        }
                    })
                    .and()
                .logout()
                    .logoutUrl("/logout") // 시큐리티는 원칙적으로 logout 처리를 post 방식으로 처리해야 한다.
                    .logoutSuccessUrl("/login")
                    .addLogoutHandler(new LogoutHandler() {
                        @Override
                        public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
                            HttpSession session = request.getSession();
                            session.invalidate();
                        }
                    })
                    .logoutSuccessHandler(new LogoutSuccessHandler() {
                        @Override
                        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                            response.sendRedirect("/login");
                        }
                    })
                    .deleteCookies("remember-me")
                    .and()
                .rememberMe()
                    .rememberMeParameter("remember")
                    .tokenValiditySeconds(3600)
                    .alwaysRemember(false)
                    .userDetailsService(userDetailsService)
                    .and()
                .sessionManagement()
                    .sessionFixation().changeSessionId() // 기본 설정 되어 있음. 요청 할 떄 마다 세션 ID를 새로 공급 받아 공격자로부터 세션을 공유하지 못하도록 방어한다.
                    .maximumSessions(1)
                    // 동시 세션 제어
                    .maxSessionsPreventsLogin(false) // default : false -> 기존 사용중인 사용자는 세션을 만료시키고 새로 로그인한 사용자에게 세션이 주어준다.
                    // 세션 고정 보호
        ;
    }
}