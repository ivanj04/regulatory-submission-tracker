package com.ivanjiang.regtracker.repository;

import com.ivanjiang.regtracker.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
