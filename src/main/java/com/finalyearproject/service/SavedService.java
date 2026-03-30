package com.finalyearproject.service;

import com.finalyearproject.model.SavedItem;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.SavedRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SavedService {
    private final SavedRepository savedRepository;

    public SavedService(SavedRepository savedRepository) {
        this.savedRepository = savedRepository;
    }

    public int countSaved(User user) {
        return savedRepository.countByUser(user);
    }
    public List<SavedItem> getSavedItemsForUser(User user) {
        return savedRepository.findByUser(user);
    }
}