package io.github.jochyoua.mychristianswearfilter.shared.exceptions;

import org.bukkit.configuration.file.YamlConfiguration;

public class NoPermissionException extends Exception {
    YamlConfiguration language;
    public NoPermissionException(YamlConfiguration language) {
        this.language = language;
    }

    @Override
    public String getMessage() {
        return language.getString("variables.noperm");
    }
}