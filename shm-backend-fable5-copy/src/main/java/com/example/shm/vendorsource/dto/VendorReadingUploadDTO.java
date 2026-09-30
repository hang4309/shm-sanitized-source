package com.example.shm.vendorsource.dto;

import java.math.BigDecimal;

/**
 * Normalized vendor reading upload payload.
 *
 * The canonical producer is the OS265 file collector. Legacy fiber
 * compatibility uploads bind to the same shape; missing fields are
 * defaulted only inside the compatibility adapter.
 */
public class VendorReadingUploadDTO {

    private String sourceType;
    private String deviceNo;
    private String channelNo;
    /**
     * Legacy alias for channelNo kept for fiber-era payload compatibility.
     */
    private String fiberNo;
    private String sensorId;
    private String moduleKey;

    private BigDecimal rawValue;
    private BigDecimal measuredValue;
    private BigDecimal intensity;
    private BigDecimal wavelength;
    private BigDecimal wavelengthShift;

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String collectTime;

    private String sourceFile;
    private Long sourceOffset;
    private Long sourceLine;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    public String getChannelNo() { return channelNo; }
    public void setChannelNo(String channelNo) { this.channelNo = channelNo; }
    public String getFiberNo() { return fiberNo; }
    public void setFiberNo(String fiberNo) { this.fiberNo = fiberNo; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public BigDecimal getRawValue() { return rawValue; }
    public void setRawValue(BigDecimal rawValue) { this.rawValue = rawValue; }
    public BigDecimal getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(BigDecimal measuredValue) { this.measuredValue = measuredValue; }
    public BigDecimal getIntensity() { return intensity; }
    public void setIntensity(BigDecimal intensity) { this.intensity = intensity; }
    public BigDecimal getWavelength() { return wavelength; }
    public void setWavelength(BigDecimal wavelength) { this.wavelength = wavelength; }
    public BigDecimal getWavelengthShift() { return wavelengthShift; }
    public void setWavelengthShift(BigDecimal wavelengthShift) { this.wavelengthShift = wavelengthShift; }
    public String getCollectTime() { return collectTime; }
    public void setCollectTime(String collectTime) { this.collectTime = collectTime; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public Long getSourceOffset() { return sourceOffset; }
    public void setSourceOffset(Long sourceOffset) { this.sourceOffset = sourceOffset; }
    public Long getSourceLine() { return sourceLine; }
    public void setSourceLine(Long sourceLine) { this.sourceLine = sourceLine; }
}
