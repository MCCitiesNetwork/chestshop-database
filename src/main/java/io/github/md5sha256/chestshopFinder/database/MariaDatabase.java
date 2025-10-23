package io.github.md5sha256.chestshopFinder.database;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

public class MariaDatabase {

    public SqlSessionFactory buildSessionFactory(@Nonnull DatabaseSettings settings) {
        DataSource dataSource = new PooledDataSource("", settings.url(), settings.username(), settings.password());
        Environment environment = new Environment("production", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(MariaChestshopMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

}
