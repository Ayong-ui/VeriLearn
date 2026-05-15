package com.verilearn.workflow;

import com.verilearn.workflow.service.LocalDemoPageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalDemoPageController {

    private final LocalDemoPageService localDemoPageService;

    public LocalDemoPageController(LocalDemoPageService localDemoPageService) {
        this.localDemoPageService = localDemoPageService;
    }

    @GetMapping(value = "/demo/local-page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> renderPage() {
        return ResponseEntity.ok(localDemoPageService.buildPage());
    }
}
