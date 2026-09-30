package com.example.shm.fiber.controller;

import com.example.shm.common.Result;
import com.example.shm.vendorsource.dto.VendorReadingQueryDTO;
import com.example.shm.vendorsource.service.VendorSourceService;
import com.example.shm.vendorsource.vo.VendorReadingVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Legacy fiber compatibility queries. Deprecated alias that reads the
 * unified vendor source readings; fiber is not a business module.
 */
@RestController
@RequestMapping("/api/data/fiber")
public class FiberDataController {

    private final VendorSourceService vendorSourceService;

    public FiberDataController(VendorSourceService vendorSourceService) {
        this.vendorSourceService = vendorSourceService;
    }

    @PostMapping("/latest")
    public Result<VendorReadingVO> latest(@RequestBody VendorReadingQueryDTO dto) {
        return Result.success(vendorSourceService.getLatest(dto));
    }

    @PostMapping("/history")
    public Result<List<VendorReadingVO>> history(@RequestBody VendorReadingQueryDTO dto) {
        return Result.success(vendorSourceService.getHistory(dto));
    }
}
