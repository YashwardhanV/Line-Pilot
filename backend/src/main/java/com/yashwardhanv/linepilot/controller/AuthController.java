package com.yashwardhanv.linepilot.controller;

import com.yashwardhanv.linepilot.dto.AuthUserResponse;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.exception.ResourceNotFoundException;
import com.yashwardhanv.linepilot.repository.UserAccountRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "basicAuth")
public class AuthController {

    private final UserAccountRepository userRepository;

    public AuthController(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public AuthUserResponse me(Principal principal) {
        UserAccount user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return new AuthUserResponse(user.getUsername(), user.getDisplayName(), user.getRole().name());
    }
}
