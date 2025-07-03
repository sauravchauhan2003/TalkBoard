package com.example.TalkBoard.Authentication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MyUserRepository extends JpaRepository<MyUser,Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<MyUser> getByUsername(String username);
    Optional<MyUser> getByEmail(String email);
}
