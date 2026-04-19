package com.example.app.repository;

import com.example.app.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
