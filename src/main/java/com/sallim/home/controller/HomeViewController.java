package com.sallim.home.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    // 로그인 안 한 사용자: 랜딩 페이지(index) / 로그인된 사용자: 대시보드로 바로 리다이렉트
    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthentiㄹcationToken)) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    // 랜딩 페이지 상단 메뉴(기능/보안/문의) - 현재는 빈 페이지, 페이지 내 콘텐츠는 추후 ajax로 채울 예정
    @GetMapping("/features")
    public String features() {
        return "home/features";
    }

    @GetMapping("/security")
    public String security() {
        return "home/security";
    }

    @GetMapping("/contact")
    public String contact() {
        return "home/contact";
    }
}
