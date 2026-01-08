package com.example.budgettracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        // Return 204 No Content instead of 500 error
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

/**
 * Redirect controller for backward compatibility
 * Redirects old root-level HTML paths to new /html/ directory
 */
@Controller
class HtmlRedirectController {

    @GetMapping("/")
    public String redirectRoot() {
        return "redirect:/html/login.html";
    }

    @GetMapping("/index.html")
    public String redirectIndex() {
        return "redirect:/html/login.html";
    }

    @GetMapping("/login.html")
    public String redirectLogin() {
        return "redirect:/html/login.html";
    }

    @GetMapping("/signup.html")
    public String redirectSignup() {
        return "redirect:/html/signup.html";
    }

    @GetMapping("/oversave-dashboard.html")
    public String redirectDashboard() {
        return "redirect:/html/oversave-dashboard.html";
    }

    @GetMapping("/transactions_page.html")
    public String redirectTransactions() {
        return "redirect:/html/transactions_page.html";
    }

    @GetMapping("/budgets_page.html")
    public String redirectBudgets() {
        return "redirect:/html/budgets_page.html";
    }

    @GetMapping("/goals_page.html")
    public String redirectGoals() {
        return "redirect:/html/goals_page.html";
    }

    @GetMapping("/subscriptions_page.html")
    public String redirectSubscriptions() {
        return "redirect:/html/subscriptions_page.html";
    }

    @GetMapping("/shopping_page.html")
    public String redirectShopping() {
        return "redirect:/html/shopping_page.html";
    }
}