package com.example.budgettracker.controller;

import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.repository.CashFlowRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class DataExportController {

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @GetMapping(value = "/transactions", produces = "text/csv")
    public void exportTransactions(HttpServletResponse response,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate)
            throws Exception {

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");

        LocalDateTime start = (startDate != null) ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = (endDate != null) ? endDate : LocalDateTime.now();

        List<CashFlow> records = cashFlowRepository.findByUserIdAndDateRange(userId, start, end);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Type,Description,Category,Amount,Date");
            for (CashFlow c : records) {
                writer.printf("%d,%s,%s,%s,%.2f,%s%n",
                        c.getCashFlowId(),
                        c.getType(),
                        sanitize(c.getDescription()),
                        (c.getCategory() != null ? c.getCategory().getName() : ""),
                        c.getAmount(),
                        c.getOccurredAt());
            }
        }
    }

    private String sanitize(String text) {
        return text == null ? "" : text.replace(",", " ");
    }
}
