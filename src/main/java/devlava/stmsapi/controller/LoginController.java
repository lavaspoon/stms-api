package devlava.stmsapi.controller;

import devlava.stmsapi.dto.LoginResponse;
import devlava.stmsapi.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/login")
    public LoginResponse login(@RequestParam String skid) {
        return loginService.login(skid);
    }
}
