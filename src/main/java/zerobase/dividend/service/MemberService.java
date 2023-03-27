package zerobase.dividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerobase.dividend.exception.impl.AlreadyExistUserException;
import zerobase.dividend.exception.impl.NoUserException;
import zerobase.dividend.exception.impl.WrongPasswordException;
import zerobase.dividend.model.Auth;
import zerobase.dividend.persist.MemberRepository;
import zerobase.dividend.persist.entity.MemberEntity;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(NoUserException::new);

    }

    public MemberEntity register(Auth.SignUp member) {
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());

        if(exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        return this.memberRepository.save(member.toEntity());
    }

    public MemberEntity authenticate(Auth.SignIn member) {
        MemberEntity user = this.memberRepository.findByUsername(member.getUsername())
                .orElseThrow(NoUserException::new);
        if(!this.passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            log.warn("wrong password -> " + member.getUsername());
            throw new WrongPasswordException();
        }
        log.info("authentication success");
        return user;
    }
}
