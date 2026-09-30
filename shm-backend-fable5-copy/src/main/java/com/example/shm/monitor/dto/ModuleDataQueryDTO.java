package com.example.shm.monitor.dto;

/**
 * Query payload for a business module's latest/history endpoints.
 * The module key comes from the request path, never from the body.
 */
public class ModuleDataQueryDTO {

    private String sensorId;
    private String deviceNo;
    private String channelNo;

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String endTime;
    private Integer limit;

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    public String getChannelNo() { return channelNo; }
    public void setChannelNo(String channelNo) { this.channelNo = channelNo; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
