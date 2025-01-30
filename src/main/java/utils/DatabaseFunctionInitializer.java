package utils;


import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import services.DragonService;

@Singleton
@Startup
public class DatabaseFunctionInitializer {

    @Inject
    private DragonService dragonService;

    private static final String fun1 = """
            CREATE OR REPLACE FUNCTION delete_dragons_by_head(head_eyes FLOAT, head_tooth DOUBLE PRECISION)
            RETURNS VOID AS $$
            BEGIN
                DELETE FROM dragon
                WHERE head_id IN (
                    SELECT id FROM dragon_head
                    WHERE eyes_count = head_eyes AND tooth_count = head_tooth
                );
            END;
            $$ LANGUAGE plpgsql;
    """;

    private static final String fun2 = """
        CREATE OR REPLACE FUNCTION count_dragons_by_wingspan(wingspan_value BIGINT)
        RETURNS INT AS $$
        BEGIN
            RETURN (SELECT COUNT(*) FROM dragon WHERE wingspan = wingspan_value);
        END;
        $$ LANGUAGE plpgsql;
    """;

    private static final String fun3 = """
            CREATE OR REPLACE FUNCTION get_dragons_by_character(character_value TEXT)
            RETURNS SETOF dragon AS $$
            BEGIN
                RETURN QUERY
                SELECT * FROM dragon
                WHERE character < character_value::dragon_character;
            END;
            $$ LANGUAGE plpgsql;
    """;

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

    private static final String fun5 = """
        CREATE OR REPLACE FUNCTION kill_dragon(dragon_id BIGINT)
        RETURNS VOID AS $$
        DECLARE
            existing_killer BIGINT;
        BEGIN
            -- Ищем случайного убийцу среди существующих людей
            SELECT id INTO existing_killer FROM person ORDER BY RANDOM() LIMIT 1;

            -- Если убийца не найден, создаем нового
            IF existing_killer IS NULL THEN
                INSERT INTO person(name, eye_color, hair_color, location, birthday, height)
                VALUES ('Unknown Slayer', 'BLACK', NULL, ROW(0, 0, 0)::location, CURRENT_DATE, 180)
                RETURNING id INTO existing_killer;
            END IF;

            -- Назначаем убийцу дракону
            UPDATE dragon SET killer_id = existing_killer WHERE id = dragon_id;
        END;
        $$ LANGUAGE plpgsql;
    """;

    @PostConstruct
    public void init() {
        System.out.println("=============== Database functions are getting updated ===============");

        dragonService.executeNativeQuery(fun1);
        dragonService.executeNativeQuery(fun2);
        dragonService.executeNativeQuery(fun3);
        dragonService.executeNativeQuery(fun4);
        dragonService.executeNativeQuery(fun5);

        System.out.println("=============== Database functions successfully updated ===============");
    }
}
