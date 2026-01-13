package com.wms.e2e;

import com.wms.entity.User;
import com.wms.enums.Role;
import com.wms.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogoutE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static String baseUrl;
    private static String seleniumUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ================= SETUP =================

    @BeforeAll
    static void setUpDriver() throws Exception {

        baseUrl = System.getenv().getOrDefault(
                "BACKEND_URL",
                "http://localhost:8089"
        );

        seleniumUrl = System.getenv().getOrDefault(
                "SELENIUM_URL",
                "http://localhost:4444"
        );

        System.out.println("🌐 App URL      : " + baseUrl);
        System.out.println("🔗 Selenium URL : " + seleniumUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new RemoteWebDriver(
                new URL(seleniumUrl),
                options
        );

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }



    @BeforeEach
    void prepareUser() {
        userRepository.findByEmail("admin@wms.com")
                .ifPresent(userRepository::delete);

        User admin = new User();
        admin.setEmail("admin@wms.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(Role.ROLE_ADMIN);
        admin.setFullName("Test Admin");
        admin.setActive(true);

        userRepository.saveAndFlush(admin);
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ================= TEST =================

    @Test
    @Order(1)
    void logout_shouldClearToken_andRedirectToLogin() {

        loginAsAdmin();

        WebElement logoutButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("logout"))
        );
        logoutButton.click();

        wait.until(ExpectedConditions.urlContains("/login"));

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/login"),
                "Logout sonrası login sayfasına yönlenmedi!");

        Object token = ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('token');");

        assertTrue(token == null || token.toString().isEmpty(),
                "Logout sonrası token hala duruyor!");
    }

    // ================= HELPERS =================

    private void loginAsAdmin() {

        driver.get(baseUrl + "/login");

        WebElement email = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        WebElement password = driver.findElement(By.id("password"));
        WebElement submit = driver.findElement(By.cssSelector("button[type='submit']"));

        email.sendKeys("admin@wms.com");
        password.sendKeys("Admin123!");
        submit.click();

        Boolean tokenExists = wait.until(d -> {
            Object token = ((JavascriptExecutor) d)
                    .executeScript("return localStorage.getItem('token');");
            return token != null && !token.toString().isEmpty();
        });

        assertTrue(tokenExists, "Login sonrası token oluşmadı!");
    }
}
