package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

public class NoPermissionException extends Exception {
    YamlConfiguration language;

    /**
     * Instantiates a new No permission exception.
     *
     * @param language the language that will be used
     */
    public NoPermissionException(YamlConfiguration language) {
        this.language = language;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.noperm");
    }
}