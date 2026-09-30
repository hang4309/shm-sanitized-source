package com.example.shm.monitor.service.impl;

import com.example.shm.common.BusinessException;
import com.example.shm.monitor.MonitorModules;
import com.example.shm.monitor.dto.ModuleDataQueryDTO;
import com.example.shm.monitor.service.MonitorModuleService;
import com.example.shm.vendorsource.dto.VendorReadingQueryDTO;
import com.example.shm.vendorsource.service.VendorSourceService;
import com.example.shm.vendorsource.vo.VendorReadingVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitorModuleServiceImpl implements MonitorModuleService {

    private final VendorSourceService vendorSourceService;

    public MonitorModuleServiceImpl(VendorSourceService vendorSourceService) {
        this.vendorSourceService = vendorSourceService;
    }

    @Override
    public VendorReadingVO getLatest(String moduleKey, ModuleDataQueryDTO dto) {
        return vendorSourceService.getLatest(toVendorQuery(moduleKey, dto));
    }

    @Override
    public List<VendorReadingVO> getHistory(String moduleKey, ModuleDataQueryDTO dto) {
        return vendorSourceService.getHistory(toVendorQuery(moduleKey, dto));
    }

    private VendorReadingQueryDTO toVendorQuery(String moduleKey, ModuleDataQueryDTO dto) {
        if (!MonitorModules.isBusinessModule(moduleKey)) {
            throw new BusinessException(404, "未知监测模块: " + moduleKey);
        }
        VendorReadingQueryDTO query = new VendorReadingQueryDTO();
        query.setModuleKey(moduleKey);
        if (dto != null) {
            query.setSensorId(dto.getSensorId());
            query.setDeviceNo(dto.getDeviceNo());
            query.setChannelNo(dto.getChannelNo());
            query.setStartTime(dto.getStartTime());
            query.setEndTime(dto.getEndTime());
            query.setLimit(dto.getLimit());
        }
        return query;
    }
}
