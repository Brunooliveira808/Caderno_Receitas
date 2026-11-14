package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.AuthRequestDTO;
import ChefBot_Telegram.dto.AuthResponseDTO;
import ChefBot_Telegram.dto.ForgotPasswordRequestDTO;
import ChefBot_Telegram.dto.ResetPasswordRequestDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.PasswordResetToken;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.PasswordResetTokenRepository;
import ChefBot_Telegram.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ChefBot_Telegram.service.UserService userService;

    public AuthController(
            AppUserRepository appUserRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ChefBot_Telegram.service.UserService userService) {
        this.appUserRepository = appUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequestDTO dto) {
        if (dto.email() == null || dto.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-mail é obrigatório."));
        }
        if (dto.username() == null || dto.username().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nome de usuário (@username) é obrigatório."));
        }
        if (dto.password() == null || dto.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha deve ter no mínimo 6 caracteres."));
        }
        if (dto.confirmPassword() != null && !dto.password().equals(dto.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "As senhas não coincidem."));
        }

        String email = dto.email().trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Este e-mail já está cadastrado."));
        }

        String username = dto.username().trim().toLowerCase().replaceAll("[^a-z0-9_.]", "");
        if (username.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nome de usuário deve ter pelo menos 3 caracteres alfanuméricos."));
        }
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Este @username já está em uso. Escolha outro."));
        }

        String name = (dto.name() != null && !dto.name().isBlank()) ? dto.name().trim() : username;
        String hash = passwordEncoder.encode(dto.password());

        AppUser user = new AppUser(email, username, hash, name);
        AppUser saved = appUserRepository.save(user);

        String token = jwtService.generateToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(
                token,
                saved.getId(),
                saved.getEmail(),
                saved.getUsername(),
                saved.getName(),
                saved.getTelegramChatId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO dto) {
        if (dto.email() == null && dto.username() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-mail ou nome de usuário é obrigatório."));
        }
        if (dto.password() == null || dto.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha é obrigatória."));
        }

        String loginIdentifier = (dto.email() != null && !dto.email().isBlank()) ? dto.email().trim().toLowerCase() : dto.username().trim().toLowerCase();
        
        Optional<AppUser> userOpt = appUserRepository.findByEmailIgnoreCase(loginIdentifier);
        if (userOpt.isEmpty()) {
            userOpt = appUserRepository.findByUsernameIgnoreCase(loginIdentifier);
        }

        if (userOpt.isEmpty() || !passwordEncoder.matches(dto.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciais inválidas."));
        }

        AppUser user = userOpt.get();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getTelegramChatId()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO dto) {
        if (dto.identifier() == null || dto.identifier().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Informe seu e-mail ou @username."));
        }

        String identifier = dto.identifier().trim().toLowerCase();
        Optional<AppUser> userOpt = appUserRepository.findByEmailIgnoreCase(identifier);
        if (userOpt.isEmpty()) {
            userOpt = appUserRepository.findByUsernameIgnoreCase(identifier);
        }

        if (userOpt.isEmpty()) {
            // Retorna sucesso neutro para evitar enumeração de contas
            return ResponseEntity.ok(Map.of(
                    "message", "Se o usuário existir, o token de recuperação foi gerado com sucesso.",
                    "note", "Em modo dev: verifique o console ou resposta para o token."
            ));
        }

        AppUser user = userOpt.get();
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        log.info("🔑 [RECUPERAÇÃO DE SENHA] Token gerado para {}: {}", user.getEmail(), token);

        return ResponseEntity.ok(Map.of(
                "message", "Código de recuperação gerado com sucesso!",
                "resetToken", token, // Facilitador de desenvolvimento / modo simulado
                "expiresInMinutes", 60
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO dto) {
        if (dto.token() == null || dto.token().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token de recuperação é obrigatório."));
        }
        if (dto.newPassword() == null || dto.newPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "A nova senha deve ter no mínimo 6 caracteres."));
        }
        if (dto.confirmPassword() != null && !dto.newPassword().equals(dto.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "As senhas não coincidem."));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(dto.token().trim());
        if (tokenOpt.isEmpty() || tokenOpt.get().isUsed() || tokenOpt.get().isExpired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token inválido ou expirado."));
        }

        PasswordResetToken resetToken = tokenOpt.get();
        AppUser user = resetToken.getAppUser();
        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        appUserRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("✅ Senha alterada com sucesso para o usuário {}", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso! Você já pode entrar com a nova senha."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AppUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
        }
        return appUserRepository.findById(user.getId())
                .map(u -> ResponseEntity.ok(new AuthResponseDTO(
                        null,
                        u.getId(),
                        u.getEmail(),
                        u.getUsername(),
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
