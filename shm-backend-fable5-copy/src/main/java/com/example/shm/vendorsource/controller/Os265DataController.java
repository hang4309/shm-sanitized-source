package com.example.shm.vendorsource.controller;

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
 * Query endpoints over the unified OS265/vendor source readings.
 */
@RestController
@RequestMapping("/api/data/os265")
public class Os265DataController {

    private final VendorSourceService vendorSourceService;

    public Os265DataController(VendorSourceService vendorSourceService) {
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
