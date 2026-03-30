package com.finalyearproject.repository;

import com.finalyearproject.model.SavedItem;
import com.finalyearproject.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedRepository extends JpaRepository<SavedItem, Long> {
    int countByUser(User user);
    List<SavedItem> findByUser(User user);
}
