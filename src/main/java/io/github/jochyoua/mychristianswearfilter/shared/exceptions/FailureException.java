package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

public class FailureException extends Exception {
    YamlConfiguration language;
    String message;

    public FailureException(YamlConfiguration language, String message) {
        this.language = language;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", message);
    }
}