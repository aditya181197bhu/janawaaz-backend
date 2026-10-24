package com.stavya.janawaaz.controller;

import com.stavya.janawaaz.dto.ComplaintDtos.*;
import com.stavya.janawaaz.entity.ComplaintStatus;
import com.stavya.janawaaz.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tickets: created by volunteers; searched and edited by volunteers and admin. Not exposed to Janta. */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public List<ComplaintResponse> create(@Valid @RequestBody CreateComplaintRequest request) {
        return complaintService.create(request);
    }

    @GetMapping
    public List<ComplaintResponse> search(@RequestParam(required = false) Integer wardId,
                                          @RequestParam(required = false) ComplaintStatus status) {
        return complaintService.search(wardId, status);
    }

    @GetMapping("/{ticketNo}")
    public ComplaintResponse get(@PathVariable String ticketNo) {
        return complaintService.get(ticketNo);
    }

    @PutMapping("/{ticketNo}")
    public ComplaintResponse edit(@PathVariable String ticketNo, @RequestBody EditComplaintRequest request) {
        return complaintService.edit(ticketNo, request);
    }

    @PatchMapping("/{ticketNo}/status")
    public ComplaintResponse updateStatus(@PathVariable String ticketNo,
                                          @Valid @RequestBody UpdateStatusRequest request) {
        return complaintService.updateStatus(ticketNo, request);
    }
}
