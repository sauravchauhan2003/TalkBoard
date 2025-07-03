package com.example.TalkBoard.Authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private VerificationService verificationService;
    @Autowired
    private MyUserRepository repository;
    @Autowired
    private JWTUtil jwtUtil;
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestHeader String email,
                                   @RequestHeader String username,
                                   @RequestHeader String password,
                                   HttpServletResponse response){
        if(email.isEmpty()||username.isEmpty()||password.isEmpty()){
            return ResponseEntity.badRequest().body("Missing one or more paramters");
        }
        if(repository.existsByEmail(email)|| repository.existsByUsername(username)){
            return ResponseEntity.badRequest().body("Username or email already in use");
        }
        else{
            MyUser myUser=new MyUser();
            myUser.setEmail(email);
            myUser.setPassword(password);
            myUser.setUsername(username);
            myUser.setActivated(false);
            repository.save(myUser);
            String jwt=jwtUtil.generateToken(myUser);
            Cookie cookie = new Cookie("token", jwt);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60*10);
            response.addCookie(cookie);
            verificationService.storeEmailandlink(email);
            return ResponseEntity.ok("User registered and JWT set in cookie");
        }

    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader String username,
                                   @RequestHeader String password,
                                   HttpServletResponse response){
        if(repository.existsByUsername(username)){
            MyUser user=repository.getByUsername(username).get();
            if(user.getPassword().equals(password)){
                if(!user.isActivated()){
                    return ResponseEntity.badRequest().body("Check your email for activating your account");
                }
                else{
                    String jwt=jwtUtil.generateToken(user);
                    Cookie cookie = new Cookie("token", jwt);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(24 * 60 * 60*10);
                    response.addCookie(cookie);
                    return ResponseEntity.ok().build();
                }
            }
            else return ResponseEntity.badRequest().body("Incorrect password");
        }
        else{
            return ResponseEntity.badRequest().body("Wrong username");
        }

    }
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token){
        if(verificationService.verify(token).equals("Invalid link")){
            return ResponseEntity.badRequest().body("Invalid Link");
        }
        else{
            repository.getByEmail(verificationService.verify(token)).get().setActivated(true);
            return  ResponseEntity.ok("Account activated.You may now log in with your username and password");
        }
    }


}
