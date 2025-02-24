package repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import objects.utils.ImportHistoryUnit;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ImportHistoryRepository {
    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void save(ImportHistoryUnit unit) {
        em.persist(unit);
    }

    @Transactional
    public List<ImportHistoryUnit> findAll(long userId, int page, int pageSize, String filterValue, String filterCol, String sortBy, String sortDir) {
        // в будущем необходимо добавить id и ownerId
        Map<String, String> columnNames = Map.ofEntries(
                Map.entry("id", "ih.id"),
                Map.entry("status", "ih.status"),
                Map.entry("user_id", "ih.user_id"),
                Map.entry("rows_added", "ih.rows_added")
        );

        // столбцы, содержащие числовые значения
        Set<String> numericColumns = Set.of(
                "ID",
                "User ID",
                "Rows added"
        );

        // Базовый JPQL-запрос
        String baseQuery = """
        SELECT ih
        FROM ImportHistoryUnit ih
        LEFT JOIN FETCH ih.user u
        WHERE 1=1
        """;

        if (userId > 0) {
            baseQuery += " AND u.id = :userId";
        }

        // Фильтрация
        boolean hasFilter = false;
        if (filterCol != null && !filterCol.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            if (numericColumns.contains(filterCol)) {
                // точное
                baseQuery += " AND " + columnNames.get(filterCol) + " = :filterValue";
                // частичное
                // baseQuery += " AND FUNCTION('STR', " + columnNames.get(filterCol) + ") LIKE :filterValue"; // плохо
                // baseQuery += " AND CONCAT('', " + columnNames.get(filterCol) + ") LIKE :filterValue"; // плохо
            } else {
                baseQuery += " AND LOWER(%" + columnNames.get(filterCol) + "%) LIKE LOWER(:filterValue)";
            }
            hasFilter = true;
        }

        // Сортировка
        if (sortDir != null && !sortDir.isEmpty()) {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "ih.id") + " " + sortDir.toUpperCase();
        } else {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "ih.id") + " ASC";
        }

        // Создание TypedQuery
        TypedQuery<ImportHistoryUnit> query = em.createQuery(baseQuery, ImportHistoryUnit.class);

        if (userId > 0) {
            query.setParameter("userId", userId);
        }

        // Применение фильтра
        if (hasFilter) {
            query.setParameter("filterValue", filterValue);
        }

        // Пагинация
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        // Выполнение запроса
        return query.getResultList();
    }
}
