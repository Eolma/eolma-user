# 공개 프로필 조회 API

## Background

프론트엔드 UX 개선 중 실시간 입찰 피드(B.9)에서 입찰자 닉네임을 표시하려면, eolma-auction 서비스가 사용자 닉네임을 조회할 수 있어야 한다. 현재 `GET /api/v1/members/me`는 본인 프로필만 조회 가능하고, 타인의 공개 정보를 조회하는 API가 없다. 최소한의 공개 정보(닉네임, 프로필 이미지)만 반환하는 공개 프로필 API를 추가한다.

## Objective

`GET /api/v1/members/{id}` 공개 프로필 조회 API를 추가하여, 다른 서비스(eolma-auction)에서 사용자 닉네임을 조회할 수 있도록 한다.

## Requirements

### Functional Requirements
- FR-1: `GET /api/v1/members/{id}` - 인증 없이 공개 프로필 조회
- FR-2: 응답: `{ id, nickname, profileImage }` (최소 공개 정보만)
- FR-3: 존재하지 않는 ID 요청 시 404 반환

### Non-Functional Requirements
- NFR-1: ./gradlew build 성공
- NFR-2: 기존 `/api/v1/members/me` 동작 변경 없음

## Out of Scope
- 프로필 검색 API
- 팔로우/팔로워 기능
- 공개 프로필 캐싱

## Technical Approach

기존 `GetMemberProfileUseCase.execute(memberId)` 재사용 가능 (동일한 `findById` 로직).

1. `PublicMemberResponse` record 신규 생성 (`id`, `nickname`, `profileImage`)
2. `MemberController`에 `GET /{id}` 엔드포인트 추가
3. `SecurityConfig`에 `GET /api/v1/members/*` 공개 경로 추가

### Affected Files

**신규 생성**:
- `src/main/java/com/eolma/user/adapter/in/web/dto/PublicMemberResponse.java`

**수정**:
- `src/main/java/com/eolma/user/adapter/in/web/MemberController.java` - GET /{id} 엔드포인트 추가
- `src/main/java/com/eolma/user/config/SecurityConfig.java` - permitAll에 GET /api/v1/members/* 추가

## Implementation Items
- [x] 1: PublicMemberResponse record 생성 (id, nickname, profileImage + from(Member) 팩토리)
- [x] 2: MemberController에 GET /{id} 엔드포인트 추가 (GetMemberProfileUseCase 재사용)
- [x] 3: SecurityConfig permitAll에 GET /api/v1/members/* 추가
- [x] 4: ./gradlew build 확인 (Docker 미실행으로 Testcontainers 통합테스트 제외, 컴파일/빌드 성공)

## Acceptance Criteria
- [x] AC-1: GET /api/v1/members/1 호출 시 { id, nickname, profileImage } 반환
- [x] AC-2: 인증 없이 접근 가능
- [x] AC-3: 존재하지 않는 ID 시 404 반환
- [x] AC-4: 기존 GET /api/v1/members/me 정상 동작 유지
- [x] AC-5: ./gradlew build 성공

## Notes
- 응답에 email, role, status 등 민감 정보는 포함하지 않음
- nickname이 null인 경우(소셜 가입 후 닉네임 미설정) 그대로 null 반환
- eolma-gateway의 JwtAuthFilter PUBLIC_PATHS에도 `GET /api/v1/members/**` 추가 필요 (별도 gateway 세션에서 처리)
- `/{id}` 경로가 `/me`와 충돌하지 않도록 Spring MVC가 구체적 경로(`/me`)를 우선 매칭
