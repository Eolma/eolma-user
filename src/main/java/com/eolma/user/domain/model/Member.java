package com.eolma.user.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Member(String email, String passwordHash, String nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Member createSocialMember(String email) {
        Member member = new Member();
        member.email = email;
        member.role = MemberRole.USER;
        member.status = MemberStatus.ACTIVE;
        member.createdAt = Instant.now();
        member.updatedAt = Instant.now();
        return member;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String nickname, String profileImage) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public boolean hasPassword() {
        return this.passwordHash != null;
    }

    public boolean hasNickname() {
        return this.nickname != null;
    }
}
