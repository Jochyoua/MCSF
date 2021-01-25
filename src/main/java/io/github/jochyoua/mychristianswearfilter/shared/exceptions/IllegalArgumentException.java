package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class IllegalArgumentException extends  Exception {
    YamlConfiguration language;

    public IllegalArgumentException(YamlConfiguration language) {
        this.language = language;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", language.getString("variables.error.incorrectargs"));
    }
}