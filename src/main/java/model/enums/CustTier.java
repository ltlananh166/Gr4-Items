package model.enums;
public enum CustTier {
    STANDARD,
            SILVER,
            GOLD,
            PLANTIUM;
    
    public static CustTier fromValue(String value){
        if(value == null || value.trim().isEmpty()) {
            return STANDARD;
            
        }
        return CustTier.valueOf(value.trim().toUpperCase());
    }
}