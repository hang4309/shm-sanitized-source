package com.example.shm.vendorsource.service.impl;

import com.example.shm.common.BusinessException;
import com.example.shm.vendorsource.dto.VendorReadingQueryDTO;
import com.example.shm.vendorsource.dto.VendorReadingUploadDTO;
import com.example.shm.vendorsource.entity.VendorChannelMappingEntity;
import com.example.shm.vendorsource.entity.VendorSourceReadingEntity;
import com.example.shm.vendorsource.mapper.VendorChannelMappingMapper;
import com.example.shm.vendorsource.mapper.VendorSourceReadingMapper;
import com.example.shm.vendorsource.service.VendorSourceService;
import com.example.shm.vendorsource.vo.VendorReadingVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;

@Service
public class VendorSourceServiceImpl implements VendorSourceService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    public static final String SOURCE_TYPE_OS265 = "OS265";
    /** Explicit compatibility markers for legacy fiber uploads without source metadata. */
    public static final String LEGACY_SOURCE_FILE = "legacy-fiber-endpoint";
    public static final long LEGACY_SOURCE_LINE = 0L;

    private final VendorSourceReadingMapper readingMapper;
    private final VendorChannelMappingMapper mappingMapper;

    public VendorSourceServiceImpl(VendorSourceReadingMapper readingMapper,
                                   VendorChannelMappingMapper mappingMapper) {
        this.readingMapper = readingMapper;
        this.mappingMapper = mappingMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ingestCanonical(VendorReadingUploadDTO dto) {
        if (dto == null) throw new BusinessException("请求体不能为空");
        if (!hasText(dto.getSourceType())) {
            // The canonical endpoint is the OS265 adapter, so OS265 is a safe default here.
            dto.setSourceType(SOURCE_TYPE_OS265);
        }
        // Collector-originated records must carry real file origin metadata.
        if (!hasText(dto.getSourceFile())) {
            throw new BusinessException("sourceFile 不能为空：OS265 采集记录必须携带来源文件");
        }
        if (dto.getSourceLine() == null) {
            throw new BusinessException("sourceLine 不能为空：OS265 采集记录必须携带来源行号");
        }
        persist(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ingestLegacyFiber(VendorReadingUploadDTO dto) {
        if (dto == null) throw new BusinessException("请求体不能为空");
        if (!hasText(dto.getSourceType())) {
            dto.setSourceType(SOURCE_TYPE_OS265);
        }
        // Compatibility records are explicitly marked; they are not real OS265 file-origin records.
        if (!hasText(dto.getSourceFile())) {
            dto.setSourceFile(LEGACY_SOURCE_FILE);
        }
        if (dto.getSourceLine() == null) {
            dto.setSourceLine(LEGACY_SOURCE_LINE);
        }
        persist(dto);
    }

    @Override
    public VendorReadingVO getLatest(VendorReadingQueryDTO dto) {
        VendorReadingQueryDTO query = normalizeQuery(dto);
        return readingMapper.selectLatest(
                query.getSourceType(), query.getDeviceNo(), query.getChannelNo(),
                query.getSensorId(), query.getModuleKey());
    }

    @Override
    public List<VendorReadingVO> getHistory(VendorReadingQueryDTO dto) {
        VendorReadingQueryDTO query = normalizeQuery(dto);
        LocalDateTime start = parseNullable(query.getStartTime(), "startTime");
        LocalDateTime end = parseNullable(query.getEndTime(), "endTime");
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("startTime 不能晚于 endTime");
        }
        return readingMapper.selectHistory(
                query.getSourceType(), query.getDeviceNo(), query.getChannelNo(),
                query.getSensorId(), query.getModuleKey(),
                start, end, normalizeLimit(query.getLimit()));
    }

    private void persist(VendorReadingUploadDTO dto) {
        String channelNo = firstText(dto.getChannelNo(), dto.getFiberNo());
        if (!hasText(dto.getSensorId())) throw new BusinessException("sensorId 不能为空");
        if (!hasText(dto.getDeviceNo())) throw new BusinessException("deviceNo 不能为空");
        if (!hasText(channelNo)) throw new BusinessException("channelNo 不能为空");
        if (!hasText(dto.getCollectTime())) throw new BusinessException("collectTime 不能为空");

        // The OS265 intensity/energy value is the primary business value;
        // measuredValue/rawValue/intensity are mutual fallbacks of the same quantity.
        BigDecimal measured = firstDecimal(dto.getMeasuredValue(), dto.getRawValue(), dto.getIntensity());
        if (measured == null) {
            throw new BusinessException("measuredValue/rawValue/intensity 至少需要一个测值");
        }

        VendorSourceReadingEntity entity = new VendorSourceReadingEntity();
        entity.setSourceType(dto.getSourceType().trim());
        entity.setDeviceNo(dto.getDeviceNo().trim());
        entity.setChannelNo(channelNo.trim());
        entity.setSensorId(dto.getSensorId().trim());
        entity.setMeasuredValue(measured);
        entity.setRawValue(firstDecimal(dto.getRawValue(), measured));
        entity.setIntensity(firstDecimal(dto.getIntensity(), measured));
        entity.setWavelength(dto.getWavelength());
        entity.setWavelengthShift(dto.getWavelengthShift());
        entity.setCollectTime(parseRequired(dto.getCollectTime(), "collectTime"));
        entity.setSourceFile(dto.getSourceFile().trim());
        // source_file_hash is owned by the backend: MD5 of the sourceFile path.
        entity.setSourceFileHash(md5Hex(entity.getSourceFile()));
        entity.setSourceOffset(dto.getSourceOffset());
        entity.setSourceLine(dto.getSourceLine());
        entity.setModuleKey(resolveModuleKey(entity, dto.getModuleKey()));

        try {
            if (readingMapper.insertReading(entity) != 1) {
                throw new BusinessException("统一数据源读数入库失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("duplicate reading already exists（相同来源记录已存在）");
        }
    }

    /**
     * Module assignment is mapping-driven. Order:
     * 1. vendor_channel_mapping by sourceType + deviceNo + channelNo
     * 2. vendor_channel_mapping by sourceType + sensorId
     * 3. moduleKey carried by the payload (the collector only sends the
     *    validated CH2 -> strain mapping)
     * 4. null / unassigned — never fabricated for unvalidated channels.
     */
    private String resolveModuleKey(VendorSourceReadingEntity entity, String payloadModuleKey) {
        VendorChannelMappingEntity mapping = mappingMapper.selectEnabledByChannel(
                entity.getSourceType(), entity.getDeviceNo(), entity.getChannelNo());
        if (mapping == null) {
            mapping = mappingMapper.selectEnabledBySensorId(entity.getSourceType(), entity.getSensorId());
        }
        if (mapping != null && hasText(mapping.getModuleKey())) {
            return mapping.getModuleKey().trim();
        }
        if (hasText(payloadModuleKey)) {
            return payloadModuleKey.trim();
        }
        return null;
    }

    private VendorReadingQueryDTO normalizeQuery(VendorReadingQueryDTO dto) {
        VendorReadingQueryDTO query = dto == null ? new VendorReadingQueryDTO() : dto;
        query.setSourceType(trimToNull(query.getSourceType()));
        query.setDeviceNo(trimToNull(query.getDeviceNo()));
        query.setChannelNo(trimToNull(query.getChannelNo()));
        query.setSensorId(trimToNull(query.getSensorId()));
        query.setModuleKey(trimToNull(query.getModuleKey()));
        return query;
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("MD5 摘要算法不可用");
        }
    }

    private LocalDateTime parseRequired(String value, String field) {
        try {
            return LocalDateTime.parse(value.trim(), FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(field + " 格式错误，必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private LocalDateTime parseNullable(String value, String field) {
        if (!hasText(value)) return null;
        return parseRequired(value, field);
    }

    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    private static BigDecimal firstDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
