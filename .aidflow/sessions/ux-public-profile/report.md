# ux-public-profile - Report

## Summary
`GET /api/v1/members/{id}` 공개 프로필 조회 API를 추가하여, 인증 없이 사용자의 닉네임/프로필 이미지를 조회할 수 있도록 했다.

## Plan Completion
- [x] 1: PublicMemberResponse record 생성
- [x] 2: MemberController에 GET /{id} 엔드포인트 추가
- [x] 3: SecurityConfig permitAll에 GET /api/v1/members/* 추가
- [x] 4: ./gradlew build 확인 (컴파일 성공, Testcontainers 통합테스트는 Docker 환경 이슈로 기존과 동일하게 실패)

## Changed Files
| File | Change Type | Description |
|------|-------------|-------------|
| adapter/in/web/dto/PublicMemberResponse.java | new | id, nickname, profileImage 필드 + from(Member) 팩토리 메서드 |
| adapter/in/web/MemberController.java | modified | GET /{id} 엔드포인트 추가 (GetMemberProfileUseCase 재사용) |
| config/SecurityConfig.java | modified | GET /api/v1/members/* permitAll 추가 |

## Key Decisions
- 기존 `GetMemberProfileUseCase`를 재사용하여 새 UseCase 생성 없이 구현
- `PublicMemberResponse`를 별도 DTO로 분리하여 민감 정보(email, role, status) 노출 방지
- SecurityConfig에서 `HttpMethod.GET`으로 제한하여 GET만 공개, PUT 등은 인증 유지

## Issues & Observations
- eolma-gateway의 JwtAuthFilter PUBLIC_PATHS에도 `GET /api/v1/members/**` 추가 필요 (별도 gateway 세션에서 처리 예정)
- `/{id}`와 `/me` 경로 충돌 없음 (Spring MVC가 구체적 경로 우선 매칭)
- nickname이 null인 경우 그대로 null 반환 (소셜 가입 후 닉네임 미설정 케이스)

## Duration
- Started: 2026-03-05T10:23:14.010Z
- Completed: 2026-03-05
- Commits: pending (git commit 대기 중)
