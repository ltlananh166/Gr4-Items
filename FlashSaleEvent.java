/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import model.enums.SaleStatus;

public class FlashSaleEvent extends BaseEntity {
    private String eventId;
    private String name;
    private String startTime;
    private String endTime;
    private SaleStatus status = SaleStatus.UPCOMING;
    private String unlockTime;
    
    public FlashSaleEvent() {}
    
    public FlashSaleEvent(String eventId, String name, String startTime, String endTime, String status) {
        this(eventId, name, startTime, endTime, status, "");
    }
    
    public FlashSaleEvent(String eventId, String name, String startTime, String endTime, String status, String unlockTime) {
        this.eventId = eventId;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SaleStatus.fromValue(status);
        this.unlockTime = unlockTime;
    }
    
    @Override
    public String getId() {
        return eventId;   
    }
    
    @Override
    public String toCsvLine(){
        String safeUnlock = (unlockTime != null) ? unlockTime : "";
        return String.format("%s, %s, %s, %s, %s, %s", eventId, name, startTime, endTime, getStatus(), safeUnlock);
    }
    
    public static FlashSaleEvent fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        String status = parts[4].trim();
        String unlockTime = (parts.length >= 6) ? parts[5]. trim() : "";
        return new FlashSaleEvent(
            parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                status,
                unlockTime
        );  
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status.name();
    }

    public SaleStatus getSaleStatus(){
        return status;
    }
    
    public void setStatus(SaleStatus status) {
        if (status == null) {
            throw  new IllegalArgumentException("Trạng thái sự kiện không được để trống");
        }
        this.status = status;
    }
    
    public String getUnlockTime() {
        return unlockTime;
    }

    public void setUnlockTime(String unlockTime) {
        this.unlockTime = unlockTime;
    }
    
}
