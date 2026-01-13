package com.wms.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static String baseUrl;
    private static String seleniumUrl;

    // ================= SETUP =================

    @BeforeAll
    static void setUpDriver() throws Exception {

        // ✅ Jenkins HOST için doğru adresler
        baseUrl = System.getenv().getOrDefault(
                "BACKEND_URL",
                "http://wms-backend:8080"

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

        // ✅ Selenium 4 → /wd/hub YOK
        driver = new RemoteWebDriver(
                new URL(seleniumUrl),
                options
        );

        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void loginBeforeEachTest() {
        loginAsAdmin();
    }

    // ================= TESTS =================

    @Test
    @Order(1)
    void productPage_shouldLoadSuccessfully() {
        driver.get(baseUrl + "/products");

        WebElement table = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productsTable"))
        );
        WebElement createBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("createProductBtn"))
        );

        assertNotNull(table);
        assertNotNull(createBtn);
    }

    @Test
    @Order(2)
    void createProduct_shouldAppearInTable() {
        driver.get(baseUrl + "/products");

        wait.until(ExpectedConditions.elementToBeClickable(By.id("createProductBtn")))
                .click();

        WebElement modal = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productModal"))
        );
        assertTrue(modal.isDisplayed());

        driver.findElement(By.id("sku")).sendKeys("E2E-PRD-001");
        driver.findElement(By.id("name")).sendKeys("E2E Product");
        driver.findElement(By.id("unit")).sendKeys("PCS");
        driver.findElement(By.id("unitPrice")).sendKeys("100");
        driver.findElement(By.id("minStockLevel")).sendKeys("5");
        driver.findElement(By.id("category")).sendKeys("Test");

        driver.findElement(By.cssSelector("#productForm button[type='submit']")).click();

        wait.until(ExpectedConditions.invisibilityOf(modal));

        WebElement body = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productsBody"))
        );

        assertTrue(
                body.getText().contains("E2E-PRD-001"),
                "Oluşturulan ürün tabloda görünmüyor!"
        );
    }

    @Test
    @Order(3)
    void productSearch_shouldFilterResults() {
        driver.get(baseUrl + "/products");

        WebElement searchInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("searchInput"))
        );

        searchInput.clear();
        searchInput.sendKeys("E2E");

        WebElement body = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productsBody"))
        );

        assertTrue(
                body.getText().toLowerCase().contains("e2e"),
                "Arama sonucu beklenen ürünü içermiyor!"
        );
    }

    // ================= HELPERS =================

    private void loginAsAdmin() {
        driver.get(baseUrl + "/login");

        WebElement email = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        WebElement password = driver.findElement(By.id("password"));
        WebElement submit = driver.findElement(By.cssSelector("button[type='submit']"));

        email.clear();
        password.clear();

        email.sendKeys("admin@wms.com");
        password.sendKeys("Admin123!");
        submit.click();

        Boolean tokenExists = wait.until(d -> {
            Object token = ((JavascriptExecutor) d)
                    .executeScript("return localStorage.getItem('token');");
            return token != null && !token.toString().isEmpty();
        });

        assertTrue(tokenExists, "Login başarısız – token oluşmadı!");
    }
}
