package com.example.shm.vendorsource.mapper;

import com.example.shm.vendorsource.entity.VendorSourceReadingEntity;
import com.example.shm.vendorsource.vo.VendorReadingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VendorSourceReadingMapper {

    int insertReading(VendorSourceReadingEntity entity);

    VendorReadingVO selectLatest(@Param("sourceType") String sourceType,
                                 @Param("deviceNo") String deviceNo,
                                 @Param("channelNo") String channelNo,
                                 @Param("sensorId") String sensorId,
                                 @Param("moduleKey") String moduleKey);

    List<VendorReadingVO> selectHistory(@Param("sourceType") String sourceType,
                                        @Param("deviceNo") String deviceNo,
                                        @Param("channelNo") String channelNo,
                                        @Param("sensorId") String sensorId,
                                        @Param("moduleKey") String moduleKey,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("limit") Integer limit);
}
