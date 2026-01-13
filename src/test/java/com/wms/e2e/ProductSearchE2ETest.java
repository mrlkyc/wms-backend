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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSearchE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static String baseUrl;
    private static String seleniumUrl;

    // ================= SETUP =================

    @BeforeAll
    static void setUpDriver() throws Exception {
        baseUrl = System.getProperty("app.url", "http://localhost:8089");
        seleniumUrl = System.getProperty("selenium.remote.url", "http://localhost:4444");

        System.out.println("🌐 App URL      : " + baseUrl);
        System.out.println("🔗 Selenium URL : " + seleniumUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new RemoteWebDriver(
                new URL(seleniumUrl + "/wd/hub"),
                options
        );

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
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

    // ================= TEST =================

    @Test
    @Order(1)
    void productSearch_shouldFilterResultsCorrectly() {
        String keyword = "test";

        driver.get(baseUrl + "/products");

        WebElement searchInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("searchInput"))
        );
        searchInput.clear();
        searchInput.sendKeys(keyword);

        WebElement tableBody = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productsBody"))
        );

        List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
        assertFalse(rows.isEmpty(), "Arama sonrası ürün listesi boş!");

        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            assertFalse(cells.isEmpty(), "Ürün satırında hücre bulunamadı!");

            String rowText = row.getText().toLowerCase();
            assertTrue(
                    rowText.contains(keyword.toLowerCase()),
                    "Filtrelenen sonuç aranan kelimeyi içermiyor: " + rowText
            );
        }
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
