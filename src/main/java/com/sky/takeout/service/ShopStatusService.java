package com.sky.takeout.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopStatusService {
    private final JdbcTemplate jdbc;
    public ShopStatusService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public int getStatus() { return jdbc.queryForList("SELECT status FROM shop WHERE id=1", Integer.class).stream().findFirst().orElse(1); }
    public void setStatus(int status) { jdbc.update("INSERT INTO shop(id,status) VALUES (1,?) ON DUPLICATE KEY UPDATE status=?", status, status); }
}
