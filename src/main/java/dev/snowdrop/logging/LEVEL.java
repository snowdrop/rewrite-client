package dev.snowdrop.logging;

public enum LEVEL {
    TRACE("CYAN", org.jboss.logmanager.Level.TRACE),
    DEBUG("CYAN", org.jboss.logmanager.Level.DEBUG),
    INFO("GREEN", org.jboss.logmanager.Level.INFO),
    WARN("YELLOW", org.jboss.logmanager.Level.WARN),
    ERROR("RED", org.jboss.logmanager.Level.ERROR),
    FATAL("RED", org.jboss.logmanager.Level.FATAL);

    private final String color;
    private final org.jboss.logmanager.Level jbossLevel;

    LEVEL(String color, org.jboss.logmanager.Level jbossLevel) {
        this.color = color;
        this.jbossLevel = jbossLevel;
    }

    public org.jboss.logmanager.Level toJbossLevel() {
        return jbossLevel;
    }
}
