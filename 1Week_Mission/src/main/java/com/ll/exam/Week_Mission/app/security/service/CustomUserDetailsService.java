package com.ll.exam.Week_Mission.app.security.service;

import com.ll.exam.Week_Mission.app.member.entity.Member;
import com.ll.exam.Week_Mission.app.member.exception.MemberNotFoundException;
import com.ll.exam.Week_Mission.app.member.repository.MemberRepository;
import com.ll.exam.Week_Mission.app.security.dto.MemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username).orElseThrow(() -> {
            throw new MemberNotFoundException(username + "멤버가 존재하지 않습니다.");
        });

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (member.getNickname() == null) {
            authorities.add(new SimpleGrantedAuthority("GENERAL"));

        } else {
            authorities.add(new SimpleGrantedAuthority("AUTHOR"));
        }
        return new MemberContext(member, authorities);
    }
}
