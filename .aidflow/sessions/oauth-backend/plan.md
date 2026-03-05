# OAuth 소셜 로그인 백엔드 구현

## Background
현재 이메일/비밀번호 기반 JWT 인증만 지원한다. Google/Kakao OAuth 소셜 로그인을 추가하여 사용자 편의성을 높인다. 같은 이메일로 다른 방식 가입 시 중복 계정을 방지하고, 기존 계정과의 연결 기능을 제공한다.

## Objective
eolma-user 서비스에 OAuth 소셜 로그인(Google, Kakao) 처리 로직을 구현한다. 헥사고날 아키텍처 패턴을 따라 도메인 모델, UseCase, Port/Adapter를 추가한다.

## Requirements

### Functional Requirements
- FR-1: Google OAuth 인가 코드로 토큰 교환 후 사용자 정보 조회
- FR-2: Kakao OAuth 인가 코드로 토큰 교환 후 사용자 정보 조회
- FR-3: 소셜 로그인으로 신규 가입 시 닉네임 없이 Member 생성, JWT 발급 + nicknameRequired=true 반환
- FR-4: 닉네임 설정 API (소셜 가입 후 닉네임 미설정 사용자 대상)
- FR-5: 동일 이메일 다른 provider 존재 시 기존 provider 목록 + linkToken 반환 (ACCOUNT_EXISTS_DIFFERENT_PROVIDER)
- FR-6: linkToken + 비밀번호(LOCAL 계정인 경우)로 계정 연결 후 JWT 발급
- FR-7: 인증된 사용자의 연결된 소셜 계정 목록 조회
- FR-8: 이미 연결된 소셜로 재로그인 시 정상 JWT 발급

### Non-Functional Requirements
- NFR-1: linkToken은 Redis에 5분 TTL로 저장, 1회 사용 후 삭제
- NFR-2: OAuth provider 통신 실패 시 OAUTH_PROVIDER_ERROR(502) 반환
- NFR-3: 기존 JWT 토큰 구조 유지 (AccessToken 30분, RefreshToken 7일)

## Out of Scope
- 소셜 계정 연결 해제 기능
- 프론트엔드 구현
- Gateway 경로 설정 (oauth-gateway 세션에서 처리)

## Technical Approach

### OAuth 플로우 (공식 문서 기반)
프론트엔드에서 인가 코드를 받아 백엔드에 전달하면, 백엔드가 Provider와 토큰 교환 + 사용자 정보 조회를 수행한다.

**Google OAuth 2.0:**
- 토큰 교환: `POST https://oauth2.googleapis.com/token` (code, client_id, client_secret, redirect_uri, grant_type=authorization_code)
- 사용자 정보: `GET https://www.googleapis.com/oauth2/v2/userinfo` (Authorization: Bearer {access_token})
- 응답: { id, email, name, picture }

**Kakao OAuth 2.0:**
- 토큰 교환: `POST https://kauth.kakao.com/oauth/token` (grant_type=authorization_code, client_id, redirect_uri, code, client_secret)
- 사용자 정보: `GET https://kapi.kakao.com/v2/user/me` (Authorization: Bearer {access_token})
- 응답: { id, kakao_account: { profile: { nickname, profile_image_url }, email } }

### Affected Files

**DB 스키마:**
- `src/main/resources/schema.sql` - member.password_hash/nickname nullable, social_account 테이블 추가

**도메인 모델 (수정):**
- `domain/model/Member.java` - passwordHash/nickname nullable, hasPassword(), hasNickname(), createSocialMember()

**도메인 모델 (신규):**
- `domain/model/AuthProvider.java` - enum: LOCAL, GOOGLE, KAKAO
- `domain/model/SocialAccount.java` - JPA entity (id, member, provider, providerId, createdAt)
- `domain/repository/SocialAccountRepository.java`

**Output Port (신규):**
- `application/port/out/OAuthProviderPort.java` - interface
- `application/port/out/dto/OAuthTokenResponse.java` - record
- `application/port/out/dto/OAuthUserInfo.java` - record

**Output Adapter (신규):**
- `adapter/out/oauth/GoogleOAuthAdapter.java` - RestClient, Google 공식 엔드포인트
- `adapter/out/oauth/KakaoOAuthAdapter.java` - RestClient, Kakao 공식 엔드포인트
- `adapter/out/oauth/OAuthProviderRegistry.java` - Map<AuthProvider, OAuthProviderPort>
- `adapter/out/redis/OAuthLinkTokenStore.java` - Redis, 기존 RefreshTokenStore 패턴

**UseCase (신규):**
- `application/usecase/OAuthLoginUseCase.java` - 핵심 분기 로직
- `application/usecase/LinkAccountUseCase.java` - 계정 연결
- `application/usecase/SetNicknameUseCase.java` - 닉네임 설정 + JWT 재발급
- `application/usecase/GetLinkedAccountsUseCase.java` - 연결 목록 조회

**Input Adapter (신규):**
- `adapter/in/web/OAuthController.java`
- `adapter/in/web/dto/OAuthLoginRequest.java`
- `adapter/in/web/dto/OAuthLoginResponse.java`
- `adapter/in/web/dto/OAuthLinkRequest.java`
- `adapter/in/web/dto/SetNicknameRequest.java`
- `adapter/in/web/dto/LinkedAccountResponse.java`

**설정 (수정/신규):**
- `config/OAuthConfig.java` - @ConfigurationProperties(prefix = "oauth")
- `config/SecurityConfig.java` - OAuth 공개 경로 추가
- `src/main/resources/application.yml` - oauth.google/kakao 설정
- `src/main/resources/application-dev.yml` - 개발 환경 client-id/secret
- `build.gradle.kts` - 필요 시 추가 의존성

## Implementation Items
- [x] 1. DB 스키마 변경: member.password_hash/nickname nullable, social_account 테이블
- [x] 2. AuthProvider enum 생성
- [x] 3. SocialAccount 엔티티 + SocialAccountRepository 생성
- [x] 4. Member 엔티티 수정: nullable 필드, hasPassword(), hasNickname(), createSocialMember()
- [x] 5. OAuthProviderPort 인터페이스 + DTO (OAuthTokenResponse, OAuthUserInfo)
- [x] 6. GoogleOAuthAdapter 구현 (RestClient, 공식 엔드포인트)
- [x] 7. KakaoOAuthAdapter 구현 (RestClient, 공식 엔드포인트)
- [x] 8. OAuthProviderRegistry 구현
- [x] 9. OAuthLinkTokenStore 구현 (Redis, TTL 5분)
- [x] 10. OAuthLoginUseCase 구현 (신규가입/기존로그인/연결요구 분기)
- [x] 11. LinkAccountUseCase 구현 (비밀번호 검증 포함)
- [x] 12. SetNicknameUseCase 구현 (닉네임 설정 + JWT 재발급)
- [x] 13. GetLinkedAccountsUseCase 구현
- [x] 14. OAuthController + DTO 구현
- [x] 15. OAuthConfig 설정 클래스 생성
- [x] 16. SecurityConfig에 OAuth 공개 경로 추가
- [x] 17. application.yml / application-dev.yml 설정 추가
- [x] 18. 단위 테스트: OAuthLoginUseCase 핵심 분기
- [x] 19. 통합 테스트: OAuth 전체 플로우 (Mock Provider)

## Acceptance Criteria
- [x] AC-1: POST /api/v1/auth/oauth/login으로 provider+code 전송 시 JWT 또는 linkInfo 반환
- [x] AC-2: 신규 소셜 가입 시 nicknameRequired=true 포함된 응답
- [x] AC-3: POST /api/v1/auth/oauth/nickname으로 닉네임 설정 후 JWT 재발급
- [x] AC-4: 동일 이메일 다른 provider 시 ACCOUNT_EXISTS_DIFFERENT_PROVIDER + linkToken
- [x] AC-5: POST /api/v1/auth/oauth/link로 계정 연결 성공 (LOCAL은 비밀번호 필수)
- [x] AC-6: 연결된 소셜로 재로그인 시 정상 JWT 발급
- [x] AC-7: GET /api/v1/auth/oauth/accounts로 연결된 소셜 목록 조회

## Notes
- eolma-common의 oauth-common 세션 완료 후 진행 (ErrorType 의존성)
- Spring Security OAuth2 Client 미사용 (기존 Stateless JWT 아키텍처 유지)
- RestClient 사용 (Spring Boot 3.4 기본 제공, 추가 의존성 불필요)
- nickname nullable 변경 시 기존 RegisterMemberUseCase에는 영향 없음 (일반 가입은 nickname 필수 유지)
