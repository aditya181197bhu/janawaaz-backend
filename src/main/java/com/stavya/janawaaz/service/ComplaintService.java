package com.stavya.janawaaz.service;

import com.stavya.janawaaz.dto.ComplaintDtos.*;
import com.stavya.janawaaz.entity.*;
import com.stavya.janawaaz.exception.ApiException;
import com.stavya.janawaaz.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final MetaService metaService;
    private final LocationService locationService;

    @Transactional
    public List<ComplaintResponse> create(CreateComplaintRequest req) {
        Ward ward = metaService.ward(req.wardId());
        Volunteer volunteer = metaService.volunteerEntity(req.volunteerId());
        LocationService.Check location = locationService.check(ward, req.latitude(), req.longitude());
        Instant now = Instant.now();

        List<ComplaintResponse> created = new ArrayList<>();
        for (ComplaintCategory category : new LinkedHashSet<>(req.categories())) {
            Complaint c = Complaint.builder()
                    .ward(ward)
                    .volunteer(volunteer)
                    .category(category)
                    .description(trimToNull(req.description()))
                    .landmark(trimToNull(req.landmark()))
                    .residentName(trimToNull(req.residentName()))
                    .residentMobile(trimToNull(req.residentMobile()))
                    .photoUrl(req.photoUrl())
                    .latitude(req.latitude())
                    .longitude(req.longitude())
                    .distanceFromWardMeters(location.distanceMeters())
                    .locationStatus(location.status())
                    .status(ComplaintStatus.REPORTED)
                    .createdAt(now)
                    .build();
            c.getUpdates().add(ComplaintUpdate.builder()
                    .complaint(c).status(ComplaintStatus.REPORTED).createdAt(now).build());
            complaintRepository.save(c);
            c.setTicketNo(ticketNo(ward.getId(), c.getId()));
            created.add(toResponse(c));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> search(Integer wardId, ComplaintStatus status) {
        return complaintRepository.search(wardId, status).stream().map(ComplaintService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse get(String ticketNo) {
        return toResponse(find(ticketNo));
    }

    @Transactional
    public ComplaintResponse updateStatus(String ticketNo, UpdateStatusRequest req) {
        Complaint c = find(ticketNo);
        if (req.status().ordinal() <= c.getStatus().ordinal()) {
            throw new ApiException("A ticket can only move forward: Reported, then In progress, then Resolved",
                    HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        c.setStatus(req.status());
        if (req.status() == ComplaintStatus.RESOLVED) {
            c.setResolvedAt(now);
        }
        c.getUpdates().add(ComplaintUpdate.builder()
                .complaint(c).status(req.status()).note(trimToNull(req.note())).createdAt(now).build());
        return toResponse(c);
    }

    @Transactional
    public ComplaintResponse edit(String ticketNo, EditComplaintRequest req) {
        Complaint c = find(ticketNo);
        if (req.category() != null) {
            c.setCategory(req.category());
        }
        if (req.description() != null) {
            c.setDescription(trimToNull(req.description()));
        }
        if (req.landmark() != null) {
            c.setLandmark(trimToNull(req.landmark()));
        }
        return toResponse(c);
    }

    private Complaint find(String ticketNo) {
        return complaintRepository.findByTicketNoIgnoreCase(ticketNo.trim())
                .orElseThrow(() -> new ApiException("No complaint found with number " + ticketNo,
                        HttpStatus.NOT_FOUND));
    }

    public static String ticketNo(int wardId, long complaintId) {
        return String.format("JA-W%02d-C%04d", wardId, complaintId);
    }

    static ComplaintResponse toResponse(Complaint c) {
        Ward w = c.getWard();
        return new ComplaintResponse(c.getTicketNo(), w.getId(), w.getLocalityEn(), w.getLocalityHi(),
                c.getCategory(), c.getDescription(), c.getLandmark(), c.getResidentName(),
                mask(c.getResidentMobile()), c.getPhotoUrl(), c.getLatitude(), c.getLongitude(),
                c.getLocationStatus(), c.getStatus(), c.getCreatedAt(), c.getResolvedAt(), updates(c));
    }

    private static List<UpdateResponse> updates(Complaint c) {
        return c.getUpdates().stream()
                .map(u -> new UpdateResponse(u.getStatus(), u.getNote(), u.getCreatedAt()))
                .toList();
    }

    private static String mask(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return mobile;
        }
        return "XXXXXX" + mobile.substring(mobile.length() - 4);
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
