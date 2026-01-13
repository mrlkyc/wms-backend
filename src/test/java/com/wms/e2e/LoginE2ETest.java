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
class LoginE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static final String BASE_URL = "http://host.docker.internal:8089";
    private static final String SELENIUM_URL = "http://localhost:4444/wd/hub";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ================= SETUP =================

    @BeforeAll
    static void startDriver() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new RemoteWebDriver(new URL(SELENIUM_URL), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @BeforeEach
    void prepareTestUser() {
        userRepository.findByEmail("admin@wms.com")
                .ifPresent(userRepository::delete);

        User admin = new User();
        admin.setEmail("admin@wms.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setFullName("Test Admin");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
    }

    @AfterAll
    static void stopDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ================= TESTS =================

    @Test
    @Order(1)
    void loginPage_shouldLoad() {
        driver.get(BASE_URL + "/login");

        WebElement email = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        WebElement password = driver.findElement(By.id("password"));
        WebElement button = driver.findElement(By.cssSelector("button[type='submit']"));

        assertNotNull(email);
        assertNotNull(password);
        assertNotNull(button);
    }

    @Test
    @Order(2)
    void validLogin_shouldRedirectToAdminPage() {
        driver.get(BASE_URL + "/login");

        driver.findElement(By.id("email")).sendKeys("admin@wms.com");
        driver.findElement(By.id("password")).sendKeys("Admin123!");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // token oluştu mu?
        Boolean tokenExists = wait.until(d ->
                ((JavascriptExecutor) d)
                        .executeScript("return localStorage.getItem('token') != null;")
                        .equals(true)
        );

        assertTrue(tokenExists);

        wait.until(ExpectedConditions.urlContains("/admin"));
        assertTrue(driver.getCurrentUrl().contains("/admin"));
    }

    @Test
    @Order(3)
    void invalidLogin_shouldShowErrorMessage() {
        driver.get(BASE_URL + "/login");

        driver.findElement(By.id("email")).sendKeys("wrong@test.com");
        driver.findElement(By.id("password")).sendKeys("wrongpass");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebElement error = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("message"))
        );

        assertTrue(error.isDisplayed());
        assertFalse(error.getText().isBlank());
    }
}
