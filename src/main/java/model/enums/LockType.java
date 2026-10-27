
package model.enums;

public enum LockType {
    NO_LOCK,
    FILE_LOCK,
    SYNCHRONIZED,
    REENTRANT_LOCK,
    OPTIMISTIC_LOCK;
    
    public boolean protectsStock() {
         return this != NO_LOCK;
    }
}
