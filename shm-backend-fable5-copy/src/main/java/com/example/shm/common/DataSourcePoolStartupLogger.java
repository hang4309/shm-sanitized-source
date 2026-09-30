package com.example.shm.common;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Logs the effective HikariCP pool settings once at startup so the
 * configured limits are visible without any per-request noise.
 */
@Component
public class DataSourcePoolStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSourcePoolStartupLogger.class);

    private final DataSource dataSource;

    public DataSourcePoolStartupLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dataSource instanceof HikariDataSource hikari) {
            log.info("HikariCP pool configured: maximumPoolSize={} connectionTimeoutMs={} jdbcUrl={}",
                    hikari.getMaximumPoolSize(), hikari.getConnectionTimeout(), hikari.getJdbcUrl());
        } else {
            log.info("DataSource is not HikariCP: {}", dataSource.getClass().getName());
        }
    }
}
