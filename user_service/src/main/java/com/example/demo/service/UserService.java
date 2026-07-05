package com.example.demo.service;

import com.example.demo.auth.dto.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;

import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Mono<AppUser> findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
    }

    public Mono<Void> deleteUser(AppUser user){
       return appUserRepository.delete(user);
    }

    public Mono<AppUser> saveUser(AppUser user){
        return appUserRepository.save(user);
    }

}
