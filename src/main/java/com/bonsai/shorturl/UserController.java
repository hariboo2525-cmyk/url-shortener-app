package com.bonsai.shorturl;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password) {
        try {
            userService.registerNewUser(username, password);
            return "redirect:/login?reg_success";
        } catch (IllegalArgumentException e) {
            return "redirect:/register?error";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}