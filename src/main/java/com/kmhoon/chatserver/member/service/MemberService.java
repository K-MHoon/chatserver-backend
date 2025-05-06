package com.kmhoon.chatserver.member.service;

import com.kmhoon.chatserver.member.domain.Member;
import com.kmhoon.chatserver.member.dto.MemberListResDto;
import com.kmhoon.chatserver.member.dto.MemberLoginReqDto;
import com.kmhoon.chatserver.member.dto.MemberSaveReqDto;
import com.kmhoon.chatserver.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    public Member create(MemberSaveReqDto reqDto) {
        // 이미 가입되어 있는 이메일 검증
        if (memberRepository.findByEmail(reqDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        Member newMember = Member.builder()
                .name(reqDto.getName())
                .email(reqDto.getEmail())
                .password(passwordEncoder.encode(reqDto.getPassword()))
                .build();
        Member member = memberRepository.save(newMember);

        return member;
    }

    public Member login(MemberLoginReqDto reqDto) {
        Member member = memberRepository.findByEmail(reqDto.getEmail()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(reqDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

    public List<MemberListResDto> findAll() {
        return memberRepository.findAll().stream()
                .map(m -> MemberListResDto.builder()
                        .id(m.getId())
                        .email(m.getEmail())
                        .name(m.getName())
                        .build())
                .toList();
    }
}
