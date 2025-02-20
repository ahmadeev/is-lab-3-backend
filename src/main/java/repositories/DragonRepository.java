package repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import objects.Dragon;

@Repository
public interface DragonRepository extends CrudRepository<Dragon, Long> {

}
