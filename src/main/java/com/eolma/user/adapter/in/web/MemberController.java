package com.eolma.user.adapter.in.web;

import com.eolma.user.adapter.in.web.dto.MemberResponse;
import com.eolma.user.adapter.in.web.dto.UpdateProfileRequest;
import com.eolma.user.application.usecase.GetMemberProfileUseCase;
import com.eolma.user.application.usecase.UpdateMemberProfileUseCase;
import com.eolma.user.domain.model.Member;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final GetMemberProfileUseCase getMemberProfileUseCase;
    private final UpdateMemberProfileUseCase updateMemberProfileUseCase;

    public MemberController(GetMemberProfileUseCase getMemberProfileUseCase,
                            UpdateMemberProfileUseCase updateMemberProfileUseCase) {
        this.getMemberProfileUseCase = getMemberProfileUseCase;
        this.updateMemberProfileUseCase = updateMemberProfileUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyProfile(@AuthenticationPrincipal Long memberId) {
        Member member = getMemberProfileUseCase.execute(memberId);
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @PutMapping("/me")
    public ResponseEntity<MemberResponse> updateMyProfile(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateProfileRequest request) {
        Member member = updateMemberProfileUseCase.execute(
                memberId, request.nickname(), request.profileImage());
        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
