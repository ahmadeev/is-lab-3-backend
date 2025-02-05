package utils;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.ejb.Singleton;
import services.DragonService;

@Singleton
@Startup
public class DatabaseFunctionInitializer {

    @Inject
    private DragonService dragonService;

    private static final String dropFun1 = "DROP FUNCTION IF EXISTS delete_dragons_by_head(FLOAT, DOUBLE PRECISION);";
    private static final String dropFun2 = "DROP FUNCTION IF EXISTS count_dragons_by_wingspan(BIGINT);";
    private static final String dropFun3 = "DROP FUNCTION IF EXISTS get_dragons_by_character(TEXT);";
    private static final String dropFun4 = "DROP FUNCTION IF EXISTS find_dragon_in_deepest_cave();";
    private static final String dropFun5 = "DROP FUNCTION IF EXISTS kill_dragon(BIGINT);";

    // select delete_dragons_by_head(eyeCount, toothCount);
    // не каскадно
    private static final String fun1 = """
        CREATE OR REPLACE FUNCTION delete_dragons_by_head(head_eyes double precision, head_tooth double precision)
        RETURNS integer AS
        $$
        DECLARE
            deleted_count integer;
        BEGIN
            -- Выполнение DELETE запроса
            DELETE FROM dragon
            WHERE dragon_head_id IN (
                SELECT id FROM dragon_head
                WHERE eyes_count = head_eyes AND tooth_count = head_tooth
            );

            -- Получаем количество затронутых строк
            GET DIAGNOSTICS deleted_count = ROW_COUNT;

            -- Возвращаем количество удаленных строк
            RETURN deleted_count;
        END;
        $$ LANGUAGE plpgsql;
    """;

    // select count_dragons_by_wingspan(wingspan);
    private static final String fun2 = """
        CREATE OR REPLACE FUNCTION count_dragons_by_wingspan(wingspan_value BIGINT)
        RETURNS INT AS $$
        BEGIN
            RETURN (SELECT COUNT(*) FROM dragon WHERE wingspan = wingspan_value);
        END;
        $$ LANGUAGE plpgsql;
    """;

    // select get_dragons_by_character('CUNNING');
    // сравнение с помощью CASE, потому что в системе нет enum классов
    private static final String fun3 = """
            CREATE OR REPLACE FUNCTION get_dragons_by_character(character_value TEXT)
            RETURNS SETOF dragon AS $$
            BEGIN
                RETURN QUERY
                SELECT * FROM dragon
                WHERE
                    CASE dragon.character
                        WHEN 'CUNNING' THEN 1
                        WHEN 'GOOD' THEN 2
                        WHEN 'CHAOTIC_EVIL' THEN 3
                    END
                    <
                    CASE character_value
                        WHEN 'CUNNING' THEN 1
                        WHEN 'GOOD' THEN 2
                        WHEN 'CHAOTIC_EVIL' THEN 3
                    END;
            END;
            $$ LANGUAGE plpgsql;
    """;

    // select find_dragon_in_deepest_cave();
    // ищет по минимальному coordinates.y
    private static final String fun4 = """
            CREATE OR REPLACE FUNCTION find_dragon_in_deepest_cave()
            RETURNS SETOF dragon AS $$
            BEGIN
                RETURN QUERY
                SELECT d.*
                FROM dragon d
                JOIN coordinates c ON d.coordinates_id = c.id
                ORDER BY c.y ASC
                LIMIT 1;
            END;
            $$ LANGUAGE plpgsql;
    """;

    // select kill_dragon(1);
    // назначает убийцу даже в случае не null убийцы у дракона
    private static final String fun5 = """
        CREATE OR REPLACE FUNCTION kill_dragon(dragon_id BIGINT)
        RETURNS BIGINT AS $$
        DECLARE
            existing_killer_id BIGINT;
        BEGIN
            -- Ищем случайного убийцу
            SELECT id INTO existing_killer_id FROM person ORDER BY RANDOM() LIMIT 1;

            -- Если убийца не найден, создаем нового и получаем его id
            IF existing_killer_id IS NULL THEN
                INSERT INTO person(name, eye_color, hair_color, location, birthday, height, owner_id)
                VALUES ('Unknown Slayer', 'BLACK', NULL, CAST(ROW(0, 0, 0) AS location), CURRENT_DATE, 180, 0)
                RETURNING id INTO existing_killer_id;
            END IF;

            -- Назначаем убийцу дракону
            UPDATE dragon SET person_id = existing_killer_id WHERE id = dragon_id;

            -- Возвращаем id убийцы
            RETURN existing_killer_id;
        END;
        $$ LANGUAGE plpgsql;
    """;

    @PostConstruct
    public void init() {
        System.out.println("=============== Database functions are getting updated ===============");

        dragonService.executeNativeQuery(dropFun1);
        dragonService.executeNativeQuery(dropFun2);
        dragonService.executeNativeQuery(dropFun3);
        dragonService.executeNativeQuery(dropFun4);
        dragonService.executeNativeQuery(dropFun5);

        dragonService.executeNativeQuery(fun1);
        dragonService.executeNativeQuery(fun2);
        dragonService.executeNativeQuery(fun3);
        dragonService.executeNativeQuery(fun4);
        dragonService.executeNativeQuery(fun5);

        System.out.println("=============== Database functions successfully updated ===============");
    }
}
