package com.itzi.itzi.agreement.repository;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    List<Agreement> findByStatusAndPostIsNull(Status status);

    // ✅ 특정 유저(sender or receiver)가 포함된 협약서 조회
    @EntityGraph(attributePaths = {"sender","receiver"})
    List<Agreement> findByStatusAndSenderUserIdOrStatusAndReceiverUserId(
            Status status1, Long senderId,
            Status status2, Long receiverId
    );
}

