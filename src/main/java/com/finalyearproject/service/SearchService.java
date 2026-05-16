package com.finalyearproject.service;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Message;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.CourseRepository;
import com.finalyearproject.repository.MessageRepository;
import com.finalyearproject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final MessageRepository messageRepository;

    public SearchService(UserRepository userRepository,
                         CourseRepository courseRepository,
                         MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.messageRepository = messageRepository;
    }

    public Map<String, Object> search(String query, User currentUser) {
        String q = query.trim();
        Map<String, Object> results = new LinkedHashMap<>();

        List<User> users = userRepository.search(q);
        List<Course> courses = courseRepository.search(q);
        List<Message> messages = messageRepository.search(q, currentUser);

        results.put("users", users);
        results.put("courses", courses);
        results.put("messages", messages);
        results.put("total", users.size() + courses.size() + messages.size());
        results.put("query", q);

        return results;
    }
}
