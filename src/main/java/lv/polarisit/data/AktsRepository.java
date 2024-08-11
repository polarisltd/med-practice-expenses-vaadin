package lv.polarisit.data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AktsRepository extends JpaRepository<Akts, Long> {

    @Query("select c from Akts c " +
            "where lower(c.personName) like lower(concat('%', :searchTerm, '%')) " +
            "or to_char(c.docDate, 'YYYYMMDD') like lower(concat('%', :searchTerm, '%'))")
    List<Akts> search(@Param("searchTerm") String searchTerm);
}
