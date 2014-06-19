package test;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.openqa.selenium.support.ui.Select;
import test.SilentHtmlUnitDriver;

public class JournalSelectionTest extends TestCase {
    private WebDriver driver;
    private String baseUrl = System.getProperty("selenium_test_url");
    private StringBuffer verificationErrors = new StringBuffer();

    @Before
    public void setUp() throws Exception {
        driver = new SilentHtmlUnitDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Test
    public void testJournalSelection() throws Exception {

        // login
        driver.get(baseUrl+ "/");
        driver.findElement(By.id("login-item")).click();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("admin@mire.com");
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("admin01");
        WebElement loginBox = driver.findElement(By.id("aspect_eperson_PasswordLogin_div_login"));
        loginBox.findElement(By.id("aspect_eperson_PasswordLogin_field_submit")).click();

        // begin submission with a known manuscript (using the URL interface to set the fields)
        driver.get(baseUrl + "/handle/10255/3/submit");
        driver.findElement(By.id("xmlui_submit_publication_article_status_accepted")).click();

        driver.findElement(By.id("aspect_submission_StepTransformer_field_prism_publicationName")).sendKeys("test");
        driver.findElement(By.name("manu_accepted-cb")).click();

        driver.findElement(By.name("license_accept")).click();
        driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();

        // assert the the page is correct and the correct manuscript metadata was imported
        assertEquals("Dryad Submission - Dryad", driver.getTitle());
        assertTrue("Input journal", idContains("aspect_submission_StepTransformer_field_prism_publicationName", "test"));

        // remove the partial submission
        driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_cancel")).click();
        driver.findElement(By.id("aspect_submission_submit_SaveOrRemoveStep_field_submit_remove")).click();
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }


    private boolean idContains(String cssID, String targetText) {
        return driver.findElement(By.id(cssID)).getText().contains(targetText);
    }


    private boolean cssClassContains(String cssClass, String targetText) {
        return driver.findElement(By.cssSelector(cssClass)).getText().contains(targetText);
    }


    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
