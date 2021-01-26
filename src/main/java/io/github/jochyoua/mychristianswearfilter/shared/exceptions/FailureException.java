package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The type Failure exception.
 */
public class FailureException extends Exception {
    YamlConfiguration language;
    String message;

    /**
     * Instantiates a new Failure exception.
     *
     * @param language the language that is being used
     * @param message  the string that will added to the message
     */
    public FailureException(YamlConfiguration language, String message) {
        this.language = language;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", message);
    }
}