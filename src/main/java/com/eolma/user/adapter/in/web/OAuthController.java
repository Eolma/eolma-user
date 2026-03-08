package com.eolma.user.adapter.in.web;

import com.eolma.user.adapter.in.web.dto.*;
import com.eolma.user.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private final OAuthLoginUseCase oAuthLoginUseCase;
    private final LinkAccountUseCase linkAccountUseCase;
    private final SetNicknameUseCase setNicknameUseCase;
    private final GetLinkedAccountsUseCase getLinkedAccountsUseCase;

    public OAuthController(OAuthLoginUseCase oAuthLoginUseCase,
                            LinkAccountUseCase linkAccountUseCase,
                            SetNicknameUseCase setNicknameUseCase,
                            GetLinkedAccountsUseCase getLinkedAccountsUseCase) {
        this.oAuthLoginUseCase = oAuthLoginUseCase;
        this.linkAccountUseCase = linkAccountUseCase;
        this.setNicknameUseCase = setNicknameUseCase;
        this.getLinkedAccountsUseCase = getLinkedAccountsUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<OAuthLoginResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        OAuthLoginResponse response = oAuthLoginUseCase.execute(
                request.provider(), request.code(), request.redirectUri());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link")
    public ResponseEntity<LoginResponse> linkAccount(@Valid @RequestBody OAuthLinkRequest request) {
        LoginResponse response = linkAccountUseCase.execute(
                request.linkToken(), request.provider(),
                request.code(), request.redirectUri(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/nickname")
    public ResponseEntity<LoginResponse> setNickname(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody SetNicknameRequest request) {
        LoginResponse response = setNicknameUseCase.execute(memberId, request.nickname());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<LinkedAccountResponse>> getLinkedAccounts(
            @AuthenticationPrincipal String memberId) {
        List<LinkedAccountResponse> accounts = getLinkedAccountsUseCase.execute(memberId);
        return ResponseEntity.ok(accounts);
    }
}
