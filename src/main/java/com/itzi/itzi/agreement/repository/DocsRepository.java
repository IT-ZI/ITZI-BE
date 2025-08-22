package com.itzi.itzi.agreement.repository;

import com.itzi.itzi.agreement.domain.Docs;
import com.itzi.itzi.agreement.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocsRepository extends JpaRepository<Docs, Long> {

    List<Docs> findByStatusAndPostIsNull(Status status);
}
