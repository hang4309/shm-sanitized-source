package com.example.shm.monitor.controller;

import com.example.shm.common.Result;
import com.example.shm.monitor.dto.ModuleDataQueryDTO;
import com.example.shm.monitor.service.MonitorModuleService;
import com.example.shm.vendorsource.vo.VendorReadingVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The six business module data APIs:
 *
 *   POST /api/data/displacement/latest|history
 *   POST /api/data/acceleration/latest|history
 *   POST /api/data/strain/latest|history
 *   POST /api/data/vibration/latest|history
 *   POST /api/data/stress/latest|history
 *   POST /api/data/deflection/latest|history
 *
 * The module key is validated against the fixed six-module whitelist;
 * literal routes (/api/data/os265, /api/data/fiber) take precedence
 * over this template by Spring's path matching rules.
 */
@RestController
@RequestMapping("/api/data/{moduleKey}")
public class MonitorModuleDataController {

    private final MonitorModuleService monitorModuleService;

    public MonitorModuleDataController(MonitorModuleService monitorModuleService) {
        this.monitorModuleService = monitorModuleService;
    }

    @PostMapping("/latest")
    public Result<VendorReadingVO> latest(@PathVariable("moduleKey") String moduleKey,
                                          @RequestBody(required = false) ModuleDataQueryDTO dto) {
        return Result.success(monitorModuleService.getLatest(moduleKey, dto));
    }

    @PostMapping("/history")
    public Result<List<VendorReadingVO>> history(@PathVariable("moduleKey") String moduleKey,
                                                 @RequestBody(required = false) ModuleDataQueryDTO dto) {
        return Result.success(monitorModuleService.getHistory(moduleKey, dto));
    }
}
