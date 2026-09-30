package com.example.shm.vendorsource.dto;

/**
 * Query payload for unified vendor source readings.
 *
 * All filters are optional; the service applies a safe default/maximum
 * limit. Business module endpoints pin moduleKey server-side.
 */
public class VendorReadingQueryDTO {

    private String sourceType;
    private String deviceNo;
    private String channelNo;
    private String sensorId;
    private String moduleKey;

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String endTime;
    private Integer limit;

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
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
