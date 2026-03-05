package com.eolma.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetNicknameRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        String nickname
) {
}
