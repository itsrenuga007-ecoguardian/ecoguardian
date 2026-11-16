
package com.ecoguardian.ecoguardian;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    // ==============================
    // SUBMIT NEW REPORT
    // ==============================
    @PostMapping
    public Report submitReport(@RequestBody Report report) {

        System.out.println("New Report Received!");
        System.out.println("Location: " + report.getLocation());
        System.out.println("Status: " + report.getStatus());
        System.out.println("Description: " + report.getDescription());

        report.setTaskStatus("Pending");

        return reportRepository.save(report);
    }

    // ==============================
    // GET ALL REPORTS
    // ==============================
    @GetMapping
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    // ==============================
    // ASSIGN WORKER
    // ==============================
    @PutMapping("/{id}/assign")
    public Report assignWorker(
            @PathVariable Long id,
            @RequestParam String worker) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Report not found"));

        System.out.println("Assigning Worker: " + worker);
        System.out.println("Report ID: " + id);

        report.setAssignedWorker(worker);
        report.setTaskStatus("Assigned");

        return reportRepository.save(report);
    }

    // ==============================
    // START TASK
    // ==============================
    @PutMapping("/{id}/start")
    public Report startTask(@PathVariable Long id) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Report not found"));

        if (report.getAssignedWorker() == null ||
                report.getAssignedWorker().isBlank()) {
            throw new IllegalStateException(
                    "Cannot start a task without an assigned worker");
        }

        if ("Completed".equalsIgnoreCase(report.getTaskStatus())) {
            throw new IllegalStateException(
                    "This task is already completed");
        }

        report.setTaskStatus("In Progress");

        return reportRepository.save(report);
    }

    // ==============================
    // COMPLETE TASK
    // ==============================
    @PutMapping("/{id}/complete")
    public Report completeTask(@PathVariable Long id) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Report not found"));

        System.out.println("Completing Report ID: " + id);

        report.setTaskStatus("Completed");

        return reportRepository.save(report);
    }
}
