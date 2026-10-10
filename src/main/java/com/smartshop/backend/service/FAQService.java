package com.smartshop.backend.service;

import com.smartshop.backend.dto.FAQDto;
import com.smartshop.backend.dto.FAQRequest;
import com.smartshop.backend.entity.FAQ;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class FAQService {

    private final FAQRepository faqRepository;

    public List<FAQDto> getAllActiveFAQs() {
        return faqRepository.findByStatusOrderBySortOrderAscCreatedAtAsc("ACTIVE")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<FAQDto> getAllFAQs() {
        return faqRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<FAQDto> getFAQsByCategory(String category) {
        return faqRepository.findByCategoryAndStatusOrderBySortOrderAsc(category, "ACTIVE")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public FAQDto getFAQById(Long id) {
        return toDTO(faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ", "id", id)));
    }

    public FAQDto createFAQ(FAQRequest request) {
        FAQ faq = FAQ.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return toDTO(faqRepository.save(faq));
    }

    public FAQDto updateFAQ(Long id, FAQRequest request) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ", "id", id));
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        if (request.getCategory() != null) faq.setCategory(request.getCategory());
        if (request.getStatus() != null) faq.setStatus(request.getStatus());
        if (request.getSortOrder() != null) faq.setSortOrder(request.getSortOrder());
        return toDTO(faqRepository.save(faq));
    }

    public void deleteFAQ(Long id) {
        faqRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("FAQ", "id", id));
        faqRepository.deleteById(id);
    }

    private FAQDto toDTO(FAQ faq) {
        return FAQDto.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .category(faq.getCategory())
                .status(faq.getStatus())
                .sortOrder(faq.getSortOrder())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
