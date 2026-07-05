//package com.example.demo.controller;
//
//import com.example.demo.auth.dto.NewUser;
//import com.example.demo.model.AppUser;
//import com.example.demo.service.UserService;
//import jakarta.validation.Valid;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.auth.dto.LoginRequest;
//import com.example.demo.auth.dto.RefreshTokenRequest;
//import com.example.demo.auth.dto.TokenResponse;
//import com.example.demo.service.AuthService;
//
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@RestController
//@RequestMapping("/auth")
//public class AuthController {
//
//    private final AuthService authService;
//    private final UserService userSerice;
//
//    public AuthController(AuthService authService, UserService userSerice) {
//        this.authService = authService;
//        this.userSerice = userSerice;
//    }
//
//    @PostMapping("/login")
//    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
//        return authService.login(request.username(), request.password());
//    }
//
//    @PostMapping("/refresh")
//    public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
//        return authService.refresh(request.refreshToken());
//    }
//
//    @DeleteMapping("/delete")
//    public Mono<String> deleteProfile(@RequestParam(value = "token", required = false) String token){
//        return authService.deleteUser(token);
//    }
//
//
//    @PostMapping("/new_user")
//    public Mono<TokenResponse> new_user(@Valid @RequestBody NewUser req){
//        if (req.role() != "USER" && req.role() != "SUPPLIER"){
//            return Mono.error(new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Недопустимая роль. Доступны только USER и SUPPLIER"
//            ));
//        }
//        return authService.createUser(new AppUser(req.username(), req.password(), req.role()));
//    }
//}
