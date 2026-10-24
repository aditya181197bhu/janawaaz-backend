package com.stavya.janawaaz.controller;

import com.stavya.janawaaz.dto.MetaDtos.FileUploadResponse;
import com.stavya.janawaaz.dto.MetaDtos.QuestionResponse;
import com.stavya.janawaaz.dto.MetaDtos.VolunteerResponse;
import com.stavya.janawaaz.dto.MetaDtos.WardResponse;
import com.stavya.janawaaz.service.FileStorageService;
import com.stavya.janawaaz.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;
    private final FileStorageService fileStorageService;

    @GetMapping("/wards")
    public List<WardResponse> wards() {
        return metaService.wards();
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions() {
        return metaService.questions();
    }

    @GetMapping("/volunteers/{id}")
    public VolunteerResponse volunteer(@PathVariable Long id) {
        return metaService.volunteer(id);
    }

    @PostMapping("/files")
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("kind") FileStorageService.Kind kind) {
        return new FileUploadResponse(fileStorageService.store(file, kind));
    }
}
