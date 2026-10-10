package com.smartshop.backend.controller;

import com.smartshop.backend.dto.FAQDto;
import com.smartshop.backend.dto.FAQRequest;
import com.smartshop.backend.service.FAQService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FAQController {

    private final FAQService faqService;

    /** PUBLIC: get all active FAQs */
    @GetMapping
    public ResponseEntity<List<FAQDto>> getActiveFAQs() {
        return ResponseEntity.ok(faqService.getAllActiveFAQs());
    }

    /** PUBLIC: get FAQs by category */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FAQDto>> getFAQsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(faqService.getFAQsByCategory(category.toUpperCase()));
    }

    /** PUBLIC: get single FAQ */
    @GetMapping("/{id}")
    public ResponseEntity<FAQDto> getFAQById(@PathVariable Long id) {
        return ResponseEntity.ok(faqService.getFAQById(id));
    }

    /** ADMIN: get all FAQs (active + inactive) */
    @GetMapping("/admin/all")
    public ResponseEntity<List<FAQDto>> getAllFAQs() {
        return ResponseEntity.ok(faqService.getAllFAQs());
    }

    /** ADMIN: create FAQ */
    @PostMapping("/admin")
    public ResponseEntity<FAQDto> createFAQ(@Valid @RequestBody FAQRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(faqService.createFAQ(request));
    }

    /** ADMIN: update FAQ */
    @PutMapping("/admin/{id}")
    public ResponseEntity<FAQDto> updateFAQ(
            @PathVariable Long id,
            @Valid @RequestBody FAQRequest request) {
        return ResponseEntity.ok(faqService.updateFAQ(id, request));
    }

    /** ADMIN: delete FAQ */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteFAQ(@PathVariable Long id) {
        faqService.deleteFAQ(id);
        return ResponseEntity.noContent().build();
    }
}
