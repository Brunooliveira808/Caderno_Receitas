package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.AuthRequestDTO;
import ChefBot_Telegram.dto.AuthResponseDTO;
import ChefBot_Telegram.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso e retornar token JWT")
    void shouldRegisterNewUser() {
        AuthRequestDTO dto = new AuthRequestDTO("chef@teste.com", "senha123", "Chef Bruno");
        ResponseEntity<?> response = authController.register(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuthResponseDTO);

        AuthResponseDTO auth = (AuthResponseDTO) response.getBody();
        assertNotNull(auth.token());
        assertEquals("chef@teste.com", auth.email());
        assertEquals("Chef Bruno", auth.name());
    }

    @Test
    @DisplayName("Deve autenticar com sucesso usuário cadastrado")
    void shouldLoginSuccessfully() {
        authController.register(new AuthRequestDTO("maria@teste.com", "segredo456", "Dona Maria"));

        ResponseEntity<?> response = authController.login(new AuthRequestDTO("maria@teste.com", "segredo456", null));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthResponseDTO auth = (AuthResponseDTO) response.getBody();
        assertNotNull(auth);
        assertNotNull(auth.token());
    }

    @Test
    @DisplayName("Deve rejeitar login com senha incorreta")
    void shouldRejectInvalidPassword() {
        authController.register(new AuthRequestDTO("pedro@teste.com", "correta123", "Pedro"));

        ResponseEntity<?> response = authController.login(new AuthRequestDTO("pedro@teste.com", "errada456", null));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Deve rejeitar registro duplicado com conflito 409")
    void shouldRejectDuplicateEmail() {
        authController.register(new AuthRequestDTO("duplo@teste.com", "senha123", "User 1"));
        ResponseEntity<?> response = authController.register(new AuthRequestDTO("duplo@teste.com", "outrasenha", "User 2"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
