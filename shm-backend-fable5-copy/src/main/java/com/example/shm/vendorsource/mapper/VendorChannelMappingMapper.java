package com.example.shm.vendorsource.mapper;

import com.example.shm.vendorsource.entity.VendorChannelMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VendorChannelMappingMapper {

    VendorChannelMappingEntity selectEnabledByChannel(@Param("sourceType") String sourceType,
                                                      @Param("deviceNo") String deviceNo,
                                                      @Param("channelNo") String channelNo);

    VendorChannelMappingEntity selectEnabledBySensorId(@Param("sourceType") String sourceType,
                                                       @Param("sensorId") String sensorId);
}
