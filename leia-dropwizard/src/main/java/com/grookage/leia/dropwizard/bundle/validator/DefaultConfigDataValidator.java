package com.grookage.leia.dropwizard.bundle.validator;

import com.grookage.concierge.core.engine.validator.ConfigDataValidator;
import com.grookage.concierge.models.config.ConfigKey;

public class DefaultConfigDataValidator implements ConfigDataValidator {
    @Override
    public void validate(ConfigKey configKey, Object configData) {
        if (configKey == null || configData == null) {
            throw new IllegalArgumentException("ConfigKey or ConfigData cannot be null");
        }
    }
}
