package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

public class CommandDisabledException extends Exception {
    YamlConfiguration language;

    public CommandDisabledException(YamlConfiguration language) {
        this.language = language;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.disabled");
    }
}

