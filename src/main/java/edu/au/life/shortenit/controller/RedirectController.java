package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    @GetMapping("/s/{code}")
    public RedirectView redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = urlService.getOriginalUrl(code, request);
        return new RedirectView(originalUrl);
    }

}

// When user visit to sample URL, returns a RedirectView object that Spring MVC converts to an HTTP redirect