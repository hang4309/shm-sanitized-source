package com.example.shm.vendorsource.controller;

import com.example.shm.common.Result;
import com.example.shm.vendorsource.dto.VendorReadingUploadDTO;
import com.example.shm.vendorsource.service.VendorSourceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canonical OS265 vendor source ingestion endpoint.
 */
@RestController
@RequestMapping("/api/sensor/os265")
public class Os265SensorController {

    private final VendorSourceService vendorSourceService;

    public Os265SensorController(VendorSourceService vendorSourceService) {
        this.vendorSourceService = vendorSourceService;
    }

    @PostMapping("/raw/upload")
    public Result<Void> uploadRaw(@RequestBody VendorReadingUploadDTO dto) {
        vendorSourceService.ingestCanonical(dto);
        return Result.success();
    }
}
