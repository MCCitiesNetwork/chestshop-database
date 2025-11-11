package io.github.md5sha256.chestshopdatabase.database;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mapper
public interface MariaPreferenceMapper extends PreferenceMapper {

    @Override
    @Nullable
    @Select("""
            SELECT visible
            FROM PreviewPreference
            WHERE player_id = CAST(#{player_id} AS UUID);
            """)
    Boolean selectPreference(@NotNull @Param("player_id") UUID player);

    @Override
    @Select("""
            INSERT INTO PreviewPreference
                (player_id, visible)
            VALUES (CAST(#{player_id} AS UUID), #{visible})
            ON DUPLICATE KEY UPDATE visible = VALUES(visible);
            """)
    void insertPreference(@NotNull @Param("player_id") UUID player,
                          @Nullable @Param("visible") Boolean visible);

    @Flush
    void flushSession();

}
