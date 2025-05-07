package com.kmhoon.chatserver.member.controller;

import com.kmhoon.chatserver.common.auth.JwtTokenProvider;
import com.kmhoon.chatserver.member.domain.Member;
import com.kmhoon.chatserver.member.dto.MemberListResDto;
import com.kmhoon.chatserver.member.dto.MemberLoginReqDto;
import com.kmhoon.chatserver.member.dto.MemberLoginResDto;
import com.kmhoon.chatserver.member.dto.MemberSaveReqDto;
import com.kmhoon.chatserver.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@RequestBody MemberSaveReqDto reqDto) {
        Member member = memberService.create(reqDto);
        return new ResponseEntity<>(member.getId(), HttpStatus.CREATED);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginReqDto reqDto) {
        // email, password 검증
        Member member = memberService.login(reqDto);

        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());

        // 일치할경우 access 토큰 발행
        MemberLoginResDto resDto = MemberLoginResDto.builder()
                .id(member.getId())
                .token(jwtToken)
                .build();

        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<?> memberList() {
        List<MemberListResDto> dtos = memberService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }
}
