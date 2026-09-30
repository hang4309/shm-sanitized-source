package com.example.shm.fiber.controller;

import com.example.shm.common.Result;
import com.example.shm.vendorsource.dto.VendorReadingUploadDTO;
import com.example.shm.vendorsource.service.VendorSourceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy fiber compatibility upload. Deprecated alias of the canonical
 * OS265 endpoint: it delegates to the unified vendor source layer and
 * never persists a second copy of the record.
 */
@RestController
@RequestMapping("/api/sensor/fiber")
public class FiberSensorController {

    private final VendorSourceService vendorSourceService;

    public FiberSensorController(VendorSourceService vendorSourceService) {
        this.vendorSourceService = vendorSourceService;
    }

    @PostMapping("/raw/upload")
    public Result<Void> uploadRaw(@RequestBody VendorReadingUploadDTO dto) {
        vendorSourceService.ingestLegacyFiber(dto);
        return Result.success();
    }
}
