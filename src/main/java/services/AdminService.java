package services;

import auth.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

@Named(value = "adminService")
@ApplicationScoped
public class AdminService {

    @PersistenceContext
    protected EntityManager em;

    @PostConstruct
    private void init() {
        System.out.println("AdminService initialized");
    }

    @Transactional
    public void createUser(long userId) {
        AdminRequest adminRequest = em.find(AdminRequest.class, userId);
        if (adminRequest == null) return;
        try {
            em.persist(new User(
                    adminRequest.getName(),
                    adminRequest.getPassword(),
                    adminRequest.getRole()
            ));
            deleteUserById(userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public AdminRequest getUserById(long id) {
        return em.find(AdminRequest.class, id);
    }

    @Transactional
    public List<AdminRequestDTO> getUserList(int page, int pageSize, String filterValue, String filterCol, String sortBy, String sortDir) {
        // в будущем необходимо добавить id и ownerId
        Map<String, String> columnNames = Map.ofEntries(
                Map.entry("Name", "ar.name")
        );

        // Базовый JPQL-запрос
        String baseQuery = """
            SELECT new auth.AdminRequestDTO(ar.id, ar.name)
            FROM AdminRequest ar
            WHERE 1=1
        """;

        // Фильтрация
        boolean hasFilter = false;
        if (filterCol != null && !filterCol.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            baseQuery += " AND LOWER(" + columnNames.get(filterCol) + ") LIKE LOWER(:filterValue)";
            hasFilter = true;
        }

        // Сортировка
        if (sortDir != null && !sortDir.isEmpty()) {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "ar.id") + " " + sortDir.toUpperCase();
        } else {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "ar.id") + "ASC";
        }

        // Создание TypedQuery
        TypedQuery<AdminRequestDTO> query = em.createQuery(baseQuery, AdminRequestDTO.class);

        // Применение фильтра
        if (hasFilter) {
            query.setParameter("filterValue", "%" + filterValue + "%");
        }

        // Пагинация
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        // Выполнение запроса
        return query.getResultList();
    }

    @Transactional
    public boolean deleteUserById(long id) {
        AdminRequest adminRequest = em.find(AdminRequest.class, id);

        if (adminRequest == null) return false;

        em.remove(adminRequest);
        return true;
    }

    public AdminRequest createEntityFromDTO(UserDTO userDTO) {
        return new AdminRequest(
                userDTO.getName(),
                userDTO.getPassword(),
                userDTO.getIsAdmin().equals("true") ? Roles.ADMIN : Roles.USER
        );
    }
}

