package com.sallim.member.controller;

import com.sallim.global.jwt.JwtProvider;
import com.sallim.member.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;

@Controller
@RequiredArgsConstructor
//@RequestMapping("/")
class AuthController {
    Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthService service;
    private final JwtProvider jwtProvider; // maxAge를 토큰 만료시간이랑 통일하려고 주입

    @GetMapping("/login")
    public String loginPage() {
        // 이미 인증된 사용자면 대시보드로 바로 보냄
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        logger.info("로그인 페이지 접근");
        return "member/login";
    }

    // 로그인 처리
    @PostMapping("/api/members/login")
    public String login(String memberId, String password,
                        RedirectAttributes rAttr,
                        HttpServletResponse res) {
        logger.info("로그인 시도(컨트롤러 진입) - memberId: {}", memberId);

        if (memberId.isEmpty() || password.isEmpty()) {
            rAttr.addFlashAttribute("msg", "아이디/비밀번호를 입력해주세요.");
            logger.warn("아이디 혹은 비밀번호가 없습니다.");
            return "redirect:/login";
        }

        try {
            String token = service.login(memberId, password);

            ResponseCookie cookie = ResponseCookie.from("token", token)
                    .httpOnly(true)
                    .path("/") // 사이트 전체에서 유효해야 함 (기존 "/login" 버그 수정)
                    .sameSite("Strict") // cross-site 요청엔 쿠키 안 실림 → CSRF 방어
                    .maxAge(Duration.ofMillis(jwtProvider.getExpiration())) // 토큰 만료시간과 동일하게
                    .build();
            res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            rAttr.addFlashAttribute("msg", "안녕하세요 " + memberId + "님");
            return "redirect:/dashboard";

        } catch (IllegalArgumentException e) {
            rAttr.addFlashAttribute("msg", e.getMessage());
            return "redirect:/login";
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/") // 로그인 때와 반드시 동일해야 삭제됨
                .sameSite("Strict") // 로그인 때와 반드시 동일해야 함
                .maxAge(0) // 즉시 만료
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        logger.info("로그아웃 처리");
        return "redirect:/login";
    }

}
