package com.bank.transaction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web configuration to handle browser-specific resource requests.
 * Browsers and dev tools automatically request certain resources which cause
 * 404 errors
 * in API-only applications. This controller returns 204 No Content to suppress
 * the errors.
 */
@Configuration
public class WebConfig {

    @RestController
    static class BrowserResourceController {

        @GetMapping("favicon.ico")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void favicon() {
            // Return 204 No Content for favicon requests
        }

        @GetMapping(".well-known/appspecific/com.chrome.devtools.json")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void chromeDevTools() {
            // Return 204 No Content for Chrome DevTools requests
        }
    }
}
