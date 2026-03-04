package com.eolma.user.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 50)
        String nickname,

        @Size(max = 500)
        String profileImage
) {
}
