package model;

public abstract class BaseEntity {

    public abstract String getId();

    public abstract String toCsvLine();
}
