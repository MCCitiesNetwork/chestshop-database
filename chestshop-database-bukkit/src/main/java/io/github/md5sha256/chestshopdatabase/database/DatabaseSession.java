package io.github.md5sha256.chestshopdatabase.database;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;

public class DatabaseSession implements Closeable, AutoCloseable {

    private final SqlSession session;
    private final Class<? extends ChestshopMapper> chestShopMapperClass;
    private final Class<? extends PreferenceMapper> preferenceMapperClass;

    public DatabaseSession(@NotNull SqlSessionFactory factory,
                           @NotNull Class<? extends ChestshopMapper> chestShopMapperClass,
                           @NotNull Class<? extends PreferenceMapper> preferenceMapperClass) {
        this.session = factory.openSession();
        this.chestShopMapperClass = chestShopMapperClass;
        this.preferenceMapperClass = preferenceMapperClass;
    }

    @NotNull
    public SqlSession session() {
        return this.session;
    }

    @NotNull
    public ChestshopMapper chestshopMapper() {
        return this.session.getMapper(this.chestShopMapperClass);
    }

    @NotNull
    public PreferenceMapper preferenceMapper() {
        return this.session.getMapper(this.preferenceMapperClass);
    }

    @Override
    public void close() {
        if (this.session != null) {
            session.close();
        }
    }
}
