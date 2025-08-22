package com.itzi.itzi.agreement.repository;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    List<Agreement> findByStatusAndPostIsNull(Status status);
}
