package com.finalyearproject.service;

import com.finalyearproject.model.SystemSettings;
import com.finalyearproject.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

   public SystemSettings getSettings() {
        return repository.findAll().stream().findFirst()
                .orElse(new SystemSettings()); // returns empty settings if none exist
    }

    public SystemSettings saveSettings(SystemSettings settings) {
        return repository.save(settings);
    }
}