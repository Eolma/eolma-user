package com.eolma.user.application.port.out;

import com.eolma.user.domain.model.Member;

public interface TokenProvider {

    String createAccessToken(Member member);

    String createRefreshToken(Member member);

    String getMemberIdFromToken(String token);

    boolean validateToken(String token);
}
