package com.itzi.itzi.partnership.repository;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.partnership.domain.Partnership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {
    boolean existsBySenderAndReceiver(User sender, User receiver);

}
