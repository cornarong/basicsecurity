package io.security.basicsecurity.security.configs;

import io.security.basicsecurity.security.common.FormAuthenticationDetailsSource;
import io.security.basicsecurity.security.handler.FormAccessDeniedHandler;
import io.security.basicsecurity.security.provider.FormAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
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
@Order(1) // config 실행 순서
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

//    @Autowired
//    private final UserDetailsService userDetailsService;
    @Autowired
    private FormAuthenticationDetailsSource authenticationDetailsSource;
    @Autowired
    private AuthenticationSuccessHandler formAuthenticationSuccessHandler;
    @Autowired
    private AuthenticationFailureHandler formAuthenticationFailureHandler;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(userDetailsService);
        // 직접 만든 FormAuthenticationProvider를 사용해서 인증 처리를 하게 된다.
        auth.authenticationProvider(authenticationProvider());
    }

    //  정적 자원 관리 (WebIgnore 설정)
    // StaticResourceLocation 클래스에서 기본으로 css,js,images 등과 같은 정적 파일들의 경로에 대해서는 "보안필터를 거치지 않고" 통과되도록 해준다.
    // 정적 파일을 경로.permitAll()으로 하면 안되나요? -> 가능하다. 하지만 permitAll()을 "보안필터의 검사를 받는다"는 차이가 있다.
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/favicon.ico", "/resources/**", "/error")
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 인가 정책
        http
                .authorizeRequests()
                .antMatchers("/", "/users", "user/login/**", "/login*").permitAll()
                .antMatchers("/mypage").hasRole("USER")
                .antMatchers("/messages").hasRole("MANAGER")
                .antMatchers("/config").hasRole("ADMIN")
                .anyRequest().authenticated()
        // 인증 정책
        .and()
                .formLogin() // formLogin방식
                    .loginPage("/login") // 로그인 페이지
                    .usernameParameter("username") // form의 id 파라미터명
                    .passwordParameter("password") // form의 password 파라미터명
                    .loginProcessingUrl("/login_proc") // form의 action 경로
                    .defaultSuccessUrl("/") // 로그인 성공시 url
//                    .failureUrl("/login") // 로그인 실패시 url
                    .authenticationDetailsSource(authenticationDetailsSource) // 인증 부가 기능
                    .successHandler(formAuthenticationSuccessHandler) // 1. 성공시 custom success 핸들러를 호출한다.
//                    .successHandler(new AuthenticationSuccessHandler() { // 2. 성공시 success 핸들러를 호출한다. 추가로 사용해보자
//                        // 로그인 성공시 authentication 정보를 매개변수로 -
//                        @Override
//                        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//                            RequestCache requestCache = new HttpSessionRequestCache();
//                            SavedRequest savedRequest = requestCache.getRequest(request, response); // savedRequest 안에 사용자가 가고자 했던 정보가 들어 있다.
//                            String redirectUrl = savedRequest.getRedirectUrl();
//
//                            System.out.println("authentication : " + authentication.getName());
//                            // 인증에 성공하면 세션에 저장되어 있던 이전 정보(가고자 했던 경로)를 꺼내와서 이동 시킨다.
//                            response.sendRedirect(redirectUrl);
//                        }
//                    })
                    .failureHandler(formAuthenticationFailureHandler) // 1. 실패시 custom failure 핸들러를 호출한다.
//                    .failureHandler(new AuthenticationFailureHandler() { // 2. 실패시 fail 핸들러를 호출한다. 추가로 사용해보자
//                        // 로그인 실패시 exception 정보를 매개변수로 -
//                        @Override
//                        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//                            System.out.println("exception : " + exception.getMessage());
//                            response.sendRedirect("/loginPage");
//                        }
//                    });
                    .permitAll()
        .and()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler());


        http
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
//                .rememberMe()
//                    .rememberMeParameter("remember")
//                    .tokenValiditySeconds(3600)
//                    .alwaysRemember(false)
//                    .userDetailsService(userDetailsService)
//                    .and()
                .sessionManagement()
                    .sessionFixation().changeSessionId() // 기본 설정 되어 있음. 요청 할 떄 마다 세션 ID를 새로 공급 받아 공격자로부터 세션을 공유하지 못하도록 방어한다.
                    .maximumSessions(1)
                    // 동시 세션 제어
                    .maxSessionsPreventsLogin(false); // default : false -> 기존 사용중인 사용자는 세션을 만료시키고 새로 로그인한 사용자에게 세션이 주어준다.
                    // 세션 고정 보호

        // 동시 세션 제어 -> 관련 블로그 글 정리 : https://cornarong.tistory.com/82
        http.sessionManagement()
                .maximumSessions(1) // 최대 허용 가능 세션 수, -1 : 무제한 로그인 세션 허용
                .maxSessionsPreventsLogin(false) // 동시 로그인 차단, false : 기존 세션 만료(default)
//                .invalidSessionUrl("/invalid") // 세션이 유효하지 않을 대 이동 할 페이지
                .expiredUrl("/expired"); // 세션이 만료된 경우 이동 할 페이지
        // 세션 고정 보호
        http.sessionManagement()
                .sessionFixation().changeSessionId(); // 기본 값 (서블릿 3.1 이상의 기본 값)
                // 새로운 세션 할당, 기존 세션의 모든 어트리뷰트가 새로운 세션으로 이동한다. (서블릿 3.1 이하의 기본 값)
//                .sessionFixation().migrateSession()
                // 새로운 세션 생성, 기존 세션의 모든 어트리뷰트는 새로운 세션으로 옮겨지지 않는다.
//                .sessionFixation().newSession() // 새로운 세션 생성
                // 설정해제, 공격에 방치된다.
//                .sessionFixation().none();
        // 세션 정책
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED); // 스프링 시큐리티가 필요 시 생성(기본 값)
//                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) //  스프링 시큐리티가 항상 세션 생성
//                .sessionCreationPolicy(SessionCreationPolicy.NEVER) // 스프링 시큐리티가 생성하지 않지만 이미 존재하면 사용
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 스프링 시큐리티가 생성하지도 않고 존재해도 사용하지 않음

/*        // 예외 처리
        http
                .exceptionHandling()
                // 인증 예외
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        response.sendRedirect("/login"); // 우리가 직접 만든 로그인페이지로 이동
                    }
                })
                // 인가 예외
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        response.sendRedirect("/denied");
                    }
                });*/
    }

    // 인증 거부 처리 빈 등록
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        FormAccessDeniedHandler accessDeniedHandler = new FormAccessDeniedHandler();
        accessDeniedHandler.setErrorPage("/denied");
        return accessDeniedHandler;
    }

    // 패스워드 인코더(passwordEncoder) 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new FormAuthenticationProvider(passwordEncoder());
    }

}
