# eolma-user Development Guide

## 서비스 개요

회원 인증/인가를 담당하는 서비스. JWT 기반 토큰 발급 및 검증, 회원 프로필 관리.

- 포트: 8081
- 프레임워크: Spring MVC (Servlet)
- DB: PostgreSQL (`eolma_user`), JPA
- 캐시: Redis (Refresh Token 저장)

## 핵심 도메인

### Member 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK (Auto Increment) |
| email | String | 고유, 로그인 ID |
| passwordHash | String | BCrypt 해시 |
| nickname | String | 고유, 표시 이름 |
| profileImage | String | 프로필 이미지 URL (nullable) |
| role | MemberRole | USER, ADMIN |
| status | MemberStatus | ACTIVE, SUSPENDED, WITHDRAWN |

### 상태 전환
- 가입 시: ACTIVE
- ACTIVE -> SUSPENDED (관리자), ACTIVE -> WITHDRAWN (탈퇴)

## API 엔드포인트

### 인증 (AuthController)
| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/register` | X | 회원가입 |
| POST | `/api/v1/auth/login` | X | 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | X | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | O | 로그아웃 (Refresh Token 삭제) |

### 회원 (MemberController)
| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| GET | `/api/v1/members/me` | O | 내 프로필 조회 |
| PUT | `/api/v1/members/me` | O | 프로필 수정 |

## JWT 인증 흐름

1. **로그인**: email + password -> AccessToken(30분) + RefreshToken(7일) 발급
2. **요청**: `Authorization: Bearer {accessToken}` -> Gateway에서 검증
3. **갱신**: AccessToken 만료 시 RefreshToken으로 재발급
4. **로그아웃**: Redis에서 RefreshToken 삭제

### 토큰 설정
- Access Token: 30분 TTL, JWT Claims에 memberId, email, role 포함
- Refresh Token: 7일 TTL, Redis `refresh_token:{memberId}` 키에 저장

## UseCase 목록

| UseCase | 설명 | 이벤트 |
|---------|------|--------|
| RegisterMemberUseCase | 이메일/닉네임 중복 검증, 비밀번호 해싱, 회원 생성 | USER_REGISTERED 발행 |
| LoginUseCase | 계정 확인, 비밀번호 검증, 토큰 발급, Redis 저장 | - |
| RefreshTokenUseCase | Refresh Token 검증, 새 토큰 쌍 발급 | - |
| LogoutUseCase | Redis에서 Refresh Token 삭제 | - |
| UpdateMemberProfileUseCase | 닉네임/프로필 이미지 수정 | - |
| GetMemberProfileUseCase | 회원 정보 조회 | - |

## Kafka 이벤트

**발행:**
- `eolma.user.events` 토픽으로 `USER_REGISTERED` 이벤트 발행
- KafkaEventPublisher가 EventPublisher 포트 구현

**수신:** 없음 (Phase 1)

## 주의사항

- Gateway와 동일한 JWT Secret을 사용해야 함
- 비밀번호는 BCrypt로 해싱, 평문 저장/로그 금지
- RefreshToken은 DB가 아닌 Redis에 저장 (TTL 자동 만료)
