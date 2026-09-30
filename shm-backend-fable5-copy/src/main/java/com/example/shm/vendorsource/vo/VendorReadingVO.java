package com.example.shm.vendorsource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified vendor reading exposed to the six business modules and to the
 * legacy fiber compatibility endpoints.
 */
public class VendorReadingVO {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime collectTime;

    private String sourceFile;
    private Long sourceLine;

    /**
     * Legacy alias: fiber-era consumers read fiberNo, which is the channel number.
     */
    public String getFiberNo() { return channelNo; }

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
    public Long getSourceLine() { return sourceLine; }
    public void setSourceLine(Long sourceLine) { this.sourceLine = sourceLine; }
}
