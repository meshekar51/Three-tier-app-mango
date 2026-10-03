package com.demo.tasks;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TaskController {

    private static final String COLS = "id, title, done, created_at";

    private final JdbcTemplate jdbc;

    public TaskController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        String db;
        try (Connection c = jdbc.getDataSource().getConnection()) {
            db = c.isValid(2) ? "connected" : "down";
        } catch (Exception e) {
            db = "down";
        }
        Map<String, String> body = new LinkedHashMap<>(); // keeps JSON key order stable
        body.put("status", "ok");
        body.put("db", db);
        return body;
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT " + COLS + " FROM tasks ORDER BY id");
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        String title = body == null || body.get("title") == null ? "" : body.get("title").toString().trim();
        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
        }
        if (title.length() > 200) title = title.substring(0, 200);

        Map<String, Object> row = jdbc.queryForMap(
                "INSERT INTO tasks (title) VALUES (?) RETURNING " + COLS, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @PatchMapping("/tasks/{id}")
    public ResponseEntity<?> update(@PathVariable long id,
                                    @RequestBody(required = false) Map<String, Object> body) {
        boolean done = body != null && Boolean.TRUE.equals(body.get("done"));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "UPDATE tasks SET done = ? WHERE id = ? RETURNING " + COLS, done, id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not found"));
        }
        return ResponseEntity.ok(rows.get(0));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        int deleted = jdbc.update("DELETE FROM tasks WHERE id = ?", id);
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not found"));
        }
        return ResponseEntity.noContent().build();
    }
}
