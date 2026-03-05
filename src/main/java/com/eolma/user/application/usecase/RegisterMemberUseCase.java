package com.eolma.user.application.usecase;

import com.eolma.common.event.DomainEvent;
import com.eolma.common.event.EventType;
import com.eolma.common.event.payload.UserRegisteredEvent;
import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.application.port.out.EventPublisher;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eolma.common.logging.StructuredLogger.kv;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterMemberUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterMemberUseCase.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public RegisterMemberUseCase(MemberRepository memberRepository,
                                  PasswordEncoder passwordEncoder,
                                  EventPublisher eventPublisher) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Member execute(String email, String password, String nickname) {
        if (memberRepository.existsByEmail(email)) {
            throw new EolmaException(ErrorType.DUPLICATE_EMAIL,
                    "이미 사용 중인 이메일입니다");
        }

        if (memberRepository.existsByNickname(nickname)) {
            throw new EolmaException(ErrorType.INVALID_REQUEST,
                    "이미 사용 중인 닉네임입니다");
        }

        Member member = Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(nickname)
                .build();

        Member saved = memberRepository.save(member);
        log.info("Member registered: {} {}", kv("memberId", saved.getId()), kv("email", saved.getEmail()));

        publishUserRegisteredEvent(saved);

        return saved;
    }

    private void publishUserRegisteredEvent(Member member) {
        UserRegisteredEvent payload = new UserRegisteredEvent(
                member.getId(),
                member.getEmail(),
                member.getNickname()
        );

        DomainEvent<UserRegisteredEvent> event = DomainEvent.create(
                EventType.USER_REGISTERED,
                "user-service",
                String.valueOf(member.getId()),
                "Member",
                payload
        );

        eventPublisher.publish(event);
    }
}
