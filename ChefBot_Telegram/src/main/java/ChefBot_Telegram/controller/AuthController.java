package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.AuthRequestDTO;
import ChefBot_Telegram.dto.AuthResponseDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ChefBot_Telegram.service.UserService userService;

    public AuthController(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ChefBot_Telegram.service.UserService userService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequestDTO dto) {
        if (dto.email() == null || dto.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-mail é obrigatório."));
        }
        if (dto.password() == null || dto.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha deve ter no mínimo 6 caracteres."));
        }

        String email = dto.email().trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Este e-mail já está cadastrado."));
        }

        String name = (dto.name() != null && !dto.name().isBlank()) ? dto.name().trim() : "Chef";
        String hash = passwordEncoder.encode(dto.password());

        AppUser user = new AppUser(email, hash, name);
        AppUser saved = appUserRepository.save(user);

        String token = jwtService.generateToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(
                token,
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getTelegramChatId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO dto) {
        if (dto.email() == null || dto.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-mail e senha são obrigatórios."));
        }

        String email = dto.email().trim().toLowerCase();
        Optional<AppUser> userOpt = appUserRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(dto.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "E-mail ou senha inválidos."));
        }

        AppUser user = userOpt.get();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getTelegramChatId()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AppUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
        }
        // Recarregar do banco para pegar atualizações (ex: vínculo com Telegram)
        return appUserRepository.findById(user.getId())
                .map(u -> ResponseEntity.ok(new AuthResponseDTO(
                        null,
                        u.getId(),
                        u.getEmail(),
                        u.getName(),
                        u.getTelegramChatId()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/link-code")
    public ResponseEntity<?> generateLinkCode(@AuthenticationPrincipal AppUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
        }
        try {
            String code = userService.generateLinkCode(user.getId());
            return ResponseEntity.ok(Map.of("code", code, "expiresInMinutes", 15));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
