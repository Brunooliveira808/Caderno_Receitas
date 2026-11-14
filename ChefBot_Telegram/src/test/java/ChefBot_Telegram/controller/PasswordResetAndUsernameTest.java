package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.AuthRequestDTO;
import ChefBot_Telegram.dto.AuthResponseDTO;
import ChefBot_Telegram.dto.ForgotPasswordRequestDTO;
import ChefBot_Telegram.dto.ResetPasswordRequestDTO;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.PasswordResetTokenRepository;
import ChefBot_Telegram.security.JwtService;
import ChefBot_Telegram.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetAndUsernameTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(appUserRepository, passwordResetTokenRepository, passwordEncoder, jwtService, userService);
    }

    @Test
    @DisplayName("Deve cadastrar com @username e validar confirmação de senha")
    void testRegisterWithUsernameAndConfirmPassword() {
        // Senhas diferentes
        AuthRequestDTO mismatchDto = new AuthRequestDTO("user@test.com", "chefbruno", "senha123", "outrasenha", "Bruno");
        ResponseEntity<?> mismatchResp = authController.register(mismatchDto);
        assertEquals(HttpStatus.BAD_REQUEST, mismatchResp.getStatusCode());

        // Sucesso
        AuthRequestDTO validDto = new AuthRequestDTO("user@test.com", "chefbruno", "senha123", "senha123", "Bruno");
        ResponseEntity<?> resp = authController.register(validDto);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        AuthResponseDTO body = (AuthResponseDTO) resp.getBody();
        assertNotNull(body);
        assertEquals("chefbruno", body.username());
    }

    @Test
    @DisplayName("Deve solicitar token de recuperação de senha e redefinir senha com sucesso")
    void testForgotPasswordAndResetPassword() {
        // 1. Cadastra usuário
        AuthRequestDTO reg = new AuthRequestDTO("esqueci@test.com", "chefesqueci", "senhaOriginal123", "senhaOriginal123", "Esquecido");
        authController.register(reg);

        // 2. Solicita recuperação usando username
        ResponseEntity<?> forgotResp = authController.forgotPassword(new ForgotPasswordRequestDTO("chefesqueci"));
        assertEquals(HttpStatus.OK, forgotResp.getStatusCode());
        Map<?, ?> forgotMap = (Map<?, ?>) forgotResp.getBody();
        assertNotNull(forgotMap);
        String resetToken = (String) forgotMap.get("resetToken");
        assertNotNull(resetToken);

        // 3. Tenta redefinir com senhas diferentes (deve falhar)
        ResponseEntity<?> failReset = authController.resetPassword(new ResetPasswordRequestDTO(resetToken, "novaSenha123", "diferente123"));
        assertEquals(HttpStatus.BAD_REQUEST, failReset.getStatusCode());

        // 4. Redefine com sucesso
        ResponseEntity<?> successReset = authController.resetPassword(new ResetPasswordRequestDTO(resetToken, "novaSenha123", "novaSenha123"));
        assertEquals(HttpStatus.OK, successReset.getStatusCode());

        // 5. Login com a nova senha
        ResponseEntity<?> loginResp = authController.login(new AuthRequestDTO("chefesqueci", null, "novaSenha123", null, null));
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
    }
}
