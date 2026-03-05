# 에러 메시지 한글화 - QA #8

## 문제 요약

### #8 [LOW] 에러 메시지 영어 노출

**현상**: 로그인 실패 시 "Invalid email or password", 회원가입 중복 시 "Email already in use" 영어 메시지 노출
**원인**: 백엔드 UseCase에서 영어 에러 메시지를 하드코딩
**파일**:
- `eolma-user/src/main/java/com/eolma/user/application/usecase/LoginUseCase.java`
- `eolma-user/src/main/java/com/eolma/user/application/usecase/RegisterMemberUseCase.java`

## 구현 계획

### 변경 내용
- [x] LoginUseCase: "Invalid email or password" → "이메일 또는 비밀번호가 올바르지 않습니다"
- [x] LoginUseCase: "Account is not active" → "비활성화된 계정입니다"
- [x] RegisterMemberUseCase: "Email already in use" → "이미 사용 중인 이메일입니다"
- [x] RegisterMemberUseCase: "Nickname already in use" → "이미 사용 중인 닉네임입니다"
