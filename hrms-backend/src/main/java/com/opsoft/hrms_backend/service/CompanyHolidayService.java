
package com.opsoft.hrms_backend.service;

import com.opsoft.hrms_backend.dto.CompanyHolidayRequest;
import com.opsoft.hrms_backend.dto.CompanyHolidayResponse;
import com.opsoft.hrms_backend.entity.CompanyHoliday;
import com.opsoft.hrms_backend.repository.CompanyHolidayRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CompanyHolidayService {

    private final CompanyHolidayRepository holidayRepository;

    public CompanyHolidayService(
            CompanyHolidayRepository holidayRepository) {

        this.holidayRepository = holidayRepository;
    }

    // 1. Add a new company holiday
    @Transactional
    public CompanyHolidayResponse addHoliday(
            CompanyHolidayRequest request) {

        validateRequest(request);

        if (holidayRepository.existsByHolidayDate(
                request.getHolidayDate())) {

            throw new IllegalArgumentException(
                    "A holiday already exists on this date");
        }

        CompanyHoliday holiday = new CompanyHoliday();

        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setHolidayName(request.getHolidayName().trim());
        holiday.setDescription(normalizeDescription(
                request.getDescription()));
        holiday.setIsActive(true);

        CompanyHoliday savedHoliday =
                holidayRepository.save(holiday);

        return convertToResponse(savedHoliday);
    }

    // 2. Get all holidays, including inactive holidays
    public List<CompanyHolidayResponse> getAllHolidays() {

        return holidayRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
        (CompanyHoliday holiday) -> holiday.getHolidayDate()
                ))
                .map(this::convertToResponse)
                .toList();
    }

    // 3. Get only active holidays
    public List<CompanyHolidayResponse> getActiveHolidays() {

        return holidayRepository
                .findByIsActiveTrueOrderByHolidayDateAsc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // 4. Update an existing holiday
    @Transactional
    public CompanyHolidayResponse updateHoliday(
            Long holidayId,
            CompanyHolidayRequest request) {

        validateRequest(request);

        CompanyHoliday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Company holiday not found"));

        CompanyHoliday existingHoliday =
                holidayRepository.findByHolidayDate(
                        request.getHolidayDate()).orElse(null);

        if (existingHoliday != null
                && !existingHoliday.getId().equals(holidayId)) {

            throw new IllegalArgumentException(
                    "Another holiday already exists on this date");
        }

        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setHolidayName(request.getHolidayName().trim());
        holiday.setDescription(normalizeDescription(
                request.getDescription()));

        CompanyHoliday updatedHoliday =
                holidayRepository.save(holiday);

        return convertToResponse(updatedHoliday);
    }

    // 5. Deactivate a holiday
    @Transactional
    public CompanyHolidayResponse deactivateHoliday(Long holidayId) {

        CompanyHoliday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Company holiday not found"));

        if (Boolean.FALSE.equals(holiday.getIsActive())) {
            throw new IllegalArgumentException(
                    "This holiday is already inactive");
        }

        holiday.setIsActive(false);

        CompanyHoliday updatedHoliday =
                holidayRepository.save(holiday);

        return convertToResponse(updatedHoliday);
    }

    // Validate holiday request
    private void validateRequest(CompanyHolidayRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Holiday details are required");
        }

        if (request.getHolidayDate() == null) {
            throw new IllegalArgumentException(
                    "Holiday date is required");
        }

        if (request.getHolidayName() == null
                || request.getHolidayName().isBlank()) {

            throw new IllegalArgumentException(
                    "Holiday name is required");
        }

        if (request.getHolidayName().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Holiday name cannot exceed 150 characters");
        }

        if (request.getDescription() != null
                && request.getDescription().length() > 500) {

            throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters");
        }
    }

    // Normalize optional description
    private String normalizeDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    // Convert entity into response DTO
    private CompanyHolidayResponse convertToResponse(
            CompanyHoliday holiday) {

        CompanyHolidayResponse response =
                new CompanyHolidayResponse();

        response.setId(holiday.getId());
        response.setHolidayDate(holiday.getHolidayDate());
        response.setHolidayName(holiday.getHolidayName());
        response.setDescription(holiday.getDescription());
        response.setIsActive(holiday.getIsActive());
        response.setCreatedAt(holiday.getCreatedAt());

        return response;
    }
}
