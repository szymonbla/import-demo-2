package com.example.importdemo.configuration;

import java.util.concurrent.locks.Lock;

public interface LockProvider {
    Lock getLock(String key);
}
