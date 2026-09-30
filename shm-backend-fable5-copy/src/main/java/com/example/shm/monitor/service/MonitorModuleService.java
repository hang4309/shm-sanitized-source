package com.example.shm.monitor.service;

import com.example.shm.monitor.dto.ModuleDataQueryDTO;
import com.example.shm.vendorsource.vo.VendorReadingVO;

import java.util.List;

/**
 * Business layer over the unified vendor source. Every module reads
 * the same normalized readings, selected by its moduleKey mapping.
 */
public interface MonitorModuleService {

    VendorReadingVO getLatest(String moduleKey, ModuleDataQueryDTO dto);

    List<VendorReadingVO> getHistory(String moduleKey, ModuleDataQueryDTO dto);
}
