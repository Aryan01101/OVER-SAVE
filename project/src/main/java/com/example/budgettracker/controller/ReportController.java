package com.example.budgettracker.controller;



import com.example.budgettracker.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/data")
    public Map<String, Object> getReportData(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {

        Map<String, Object> totals = reportService.generateReport(userId, startDate, endDate);

        return Map.of(
                "userId", userId,
                "startDate", startDate,
                "endDate", endDate,
                "totals", totals
        );
    }

    @GetMapping(value = "/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getReportChart(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) throws Exception {

        Map<String, Object> report = reportService.generateReport(userId, startDate, endDate);
        Map<String, BigDecimal> expenseByCategory = (Map<String, BigDecimal>) report.get("expenseByCategory");

        BigDecimal totalIncome = (BigDecimal) report.get("totalIncome");
        BigDecimal totalExpense = (BigDecimal) report.get("totalExpense");
        BigDecimal balance = (BigDecimal) report.get("balance");


        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        expenseByCategory.forEach((category, amount) -> {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                dataset.setValue(category, amount);
            }
        });


        JFreeChart chart = ChartFactory.createPieChart(
                "Expense Breakdown",
                dataset,
                false, false, false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setCircular(true);
        plot.setLabelGap(0.02);
        plot.setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);


        int width = 800;
        int height = 500;
        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = combinedImage.createGraphics();


        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, width, height);


        g2.setPaint(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString("Budget Report", 20, 30);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.drawString("Period: " + startDate + " to " + endDate, 20, 55);


        BufferedImage chartImage = chart.createBufferedImage(400, 400);
        g2.drawImage(chartImage, 20, 80, null);


        g2.setPaint(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        int x = 440;
        int y = 100;
        g2.drawString("Total Income: $" + totalIncome.setScale(2, RoundingMode.HALF_UP), x, y);
        g2.drawString("Total Expense: $" + totalExpense.setScale(2, RoundingMode.HALF_UP), x, y + 30);
        g2.drawString("Balance: $" + balance.setScale(2, RoundingMode.HALF_UP), x, y + 60);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        y += 100;
        g2.drawString("Expense Details:", x, y);
        for (Map.Entry<String, BigDecimal> entry : expenseByCategory.entrySet()) {
            y += 25;
            g2.drawString("- " + entry.getKey() + ": $" + entry.getValue().setScale(2, RoundingMode.HALF_UP), x, y);
        }

        g2.dispose();


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combinedImage, "png", baos);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(baos.toByteArray());
    }


}
