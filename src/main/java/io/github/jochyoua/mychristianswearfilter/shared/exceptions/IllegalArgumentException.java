package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class IllegalArgumentException extends Exception {
    YamlConfiguration language;

    /**
     * Instantiates a new Illegal argument exception.
     *
     * @param language the language that will be used
     */
    public IllegalArgumentException(YamlConfiguration language) {
        this.language = language;
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(language.getString("variables.failure")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(language.getString("variables.error.incorrectargs")));
    }
}