# 🏠 살림 (Sallim)

> 한국 가정을 위한 가계부 웹 애플리케이션

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 4.1.x, Spring Security, JPA |
| Database | PostgreSQL |
| Frontend | Thymeleaf, Thymeleaf Layout Dialect, Tabler UI, ApexCharts |
| Auth | JWT (jjwt) |
| Infra | AWS (예정), GitHub Actions CI/CD (예정) |

## 📐 ERD

![ERD](docs/erd.png)

## ✅ 주요 기능

- [x] 회원가입 / 로그인 (JWT + HttpOnly Cookie 기반 인증)
- [x] 카테고리 관리
- [x] 계좌 관리
- [x] 결제수단 관리
- [x] 거래내역 관리
- [x] 대시보드
- [ ] 거래내역 OCR 기능 추가(영수증 인식)
- [ ] 이메일 인증
- [ ] AI 챗봇 (자연어 기반 지출 조회)
- [ ] OAuth2 소셜 로그인

## 📝 트러블슈팅 & 기술적 의사결정

개발 과정의 문제 해결 및 의사결정 기록은 Notion에 정리했습니다.

👉 [Notion 바로가기](https://app.notion.com/p/e608ceb9043383b0a55481b8298473e1?source=copy_link)

## 📖 API 명세

Swagger UI를 통해 확인 가능합니다. (예정)

## 🚀 배포 링크
https://sallim.dev

**데모 계정**
- ID: demo
- PW: demo1234!

> 데모 계정 데이터는 언제든 초기화될 수 있습니다.