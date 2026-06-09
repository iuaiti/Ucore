package com.ultra.umenu.module;

public enum ModuleType {
    UQUEST("UQuest"),
    UBOSS("UBoss"),
    UCRATE("UCrate"),
    UCOLLECTION("UCollection"),
    UITEM("UItem"),
    USKILL("USkill");

    private final String pluginName;

    ModuleType(String pluginName) {
        this.pluginName = pluginName;
    }

    public String pluginName() {
        return pluginName;
    }
}
