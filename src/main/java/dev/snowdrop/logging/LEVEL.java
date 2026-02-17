package dev.snowdrop.logging;

/**
 * Log level enumeration mapping color names to JBoss log manager levels.
 */
public enum LEVEL {
    /** Trace level, displayed in cyan. */
    TRACE("CYAN", org.jboss.logmanager.Level.TRACE),
    /** Debug level, displayed in cyan. */
    DEBUG("CYAN", org.jboss.logmanager.Level.DEBUG),
    /** Info level, displayed in green. */
    INFO("GREEN", org.jboss.logmanager.Level.INFO),
    /** Warn level, displayed in yellow. */
    WARN("YELLOW", org.jboss.logmanager.Level.WARN),
    /** Error level, displayed in red. */
    ERROR("RED", org.jboss.logmanager.Level.ERROR),
    /** Fatal level, displayed in red. */
    FATAL("RED", org.jboss.logmanager.Level.FATAL);

    private final String color;
    private final org.jboss.logmanager.Level jbossLevel;

    LEVEL(String color, org.jboss.logmanager.Level jbossLevel) {
        this.color = color;
        this.jbossLevel = jbossLevel;
    }

    /**
     * Converts this level to the corresponding JBoss log manager level.
     *
     * @return the JBoss log manager level
     */
    public org.jboss.logmanager.Level toJbossLevel() {
        return jbossLevel;
    }
}