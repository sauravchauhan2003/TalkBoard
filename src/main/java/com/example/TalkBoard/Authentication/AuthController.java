package com.example.TalkBoard.Authentication;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private MyUserRepository repository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public String register(@RequestHeader String email,
                           @RequestHeader String username,
                           @RequestHeader String password,
                           HttpServletResponse response) {

        System.out.println("Registering user:");
        System.out.println("Email: " + email);
        System.out.println("Username: " + username);

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Missing one or more parameters";
        }

        if (repository.existsByEmail(email) || repository.existsByUsername(username)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Username or email already in use";
        }

        MyUser myUser = new MyUser();
        myUser.setEmail(email);
        myUser.setPassword(password);
        myUser.setUsername(username);
        myUser.setActivated(false);
        repository.save(myUser);

        String jwt = jwtUtil.generateToken(myUser);
        verificationService.storeEmailandlink(email);


        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println("JWT issued: " + jwt);
        return jwt;
    }

    @PostMapping("/login")
    public String login(@RequestHeader String username,
                        @RequestHeader String password,
                        HttpServletResponse response) {

        System.out.println("Login attempt for username: " + username);

        if (!repository.existsByUsername(username)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Wrong username";
        }

        MyUser user = repository.getByUsername(username).get();

        if (!user.getPassword().equals(password)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Incorrect password";
        }

        if (!user.isActivated()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Check your email for activating your account";
        }

        String jwt = jwtUtil.generateToken(user);
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println("Login successful. JWT: " + jwt);
        return jwt;
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String token,
                         HttpServletResponse response) {

        System.out.println("Verification token received: " + token);

        String result = verificationService.verify(token);

        if ("Invalid link".equals(result)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            System.out.println("Verification failed: Invalid token");
            return "Invalid Link";
        }

        repository.getByEmail(result).get().setActivated(true);
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println("Account verified for email: " + result);
        return "Account activated. You may now log in with your username and password";
    }
}
