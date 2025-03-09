package repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import utils.FileUploadData;

import java.util.List;

@ApplicationScoped
public class FileDataRepository {
    @PersistenceContext
    private EntityManager em;

    public List<FileUploadData> getByImportHistoryUnitId(long id) {
        return em.createQuery("select f from FileUploadData f where f.importHistoryUnit.id = :importHistoryUnitId", FileUploadData.class)
                .setParameter("importHistoryUnitId", (long) id)
                .getResultList();
    }
}
