package com.sky.takeout.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ShopStatusService {
    private static final String KEY = "sky:shop:status";
    private final StringRedisTemplate redis;
    private final AtomicInteger fallback = new AtomicInteger(1);
    public ShopStatusService(StringRedisTemplate redis) { this.redis = redis; }
    public int getStatus() { try { String value=redis.opsForValue().get(KEY); if(value!=null)return Integer.parseInt(value); } catch(Exception ignored) {} return fallback.get(); }
    public void setStatus(int status) { fallback.set(status); try { redis.opsForValue().set(KEY, String.valueOf(status)); } catch(Exception ignored) {} }
}
