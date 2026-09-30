package com.example.shm.vendorsource.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified vendor-source reading persisted to vendor_source_reading.
 *
 * measuredValue/rawValue/intensity all carry the OS265 intensity/energy
 * measured value; wavelength is an auxiliary reference only and must
 * never be promoted to the primary business value.
 */
public class VendorSourceReadingEntity {

    private Long id;
    private String sourceType;
    private String deviceNo;
    private String channelNo;
    private String sensorId;
    private String moduleKey;

    private BigDecimal measuredValue;
    private BigDecimal rawValue;
    private BigDecimal intensity;
    private BigDecimal wavelength;
    private BigDecimal wavelengthShift;

    private LocalDateTime collectTime;

    private String sourceFile;
    /**
     * MD5 of sourceFile path, stored as CHAR(32). Computed by the backend.
     */
    private String sourceFileHash;
    private Long sourceOffset;
    private Long sourceLine;

    private LocalDateTime createdAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    public String getChannelNo() { return channelNo; }
    public void setChannelNo(String channelNo) { this.channelNo = channelNo; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public BigDecimal getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(BigDecimal measuredValue) { this.measuredValue = measuredValue; }
    public BigDecimal getRawValue() { return rawValue; }
    public void setRawValue(BigDecimal rawValue) { this.rawValue = rawValue; }
    public BigDecimal getIntensity() { return intensity; }
    public void setIntensity(BigDecimal intensity) { this.intensity = intensity; }
    public BigDecimal getWavelength() { return wavelength; }
    public void setWavelength(BigDecimal wavelength) { this.wavelength = wavelength; }
    public BigDecimal getWavelengthShift() { return wavelengthShift; }
    public void setWavelengthShift(BigDecimal wavelengthShift) { this.wavelengthShift = wavelengthShift; }
    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public String getSourceFileHash() { return sourceFileHash; }
    public void setSourceFileHash(String sourceFileHash) { this.sourceFileHash = sourceFileHash; }
    public Long getSourceOffset() { return sourceOffset; }
    public void setSourceOffset(Long sourceOffset) { this.sourceOffset = sourceOffset; }
    public Long getSourceLine() { return sourceLine; }
    public void setSourceLine(Long sourceLine) { this.sourceLine = sourceLine; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
