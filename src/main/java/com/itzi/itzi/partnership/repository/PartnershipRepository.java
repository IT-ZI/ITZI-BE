package com.itzi.itzi.partnership.repository;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.partnership.domain.AcceptedStatus;
import com.itzi.itzi.partnership.domain.Partnership;
import com.itzi.itzi.partnership.domain.SendStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    @EntityGraph(attributePaths = {
            "sender","receiver","sender.orgProfile","sender.store","receiver.orgProfile","receiver.store"
    })
    List<Partnership> findBySenderUserId(Long senderId);

    @EntityGraph(attributePaths = {
            "sender","receiver","sender.orgProfile","sender.store","receiver.orgProfile","receiver.store"
    })
    List<Partnership> findByReceiverUserId(Long receiverId);

    boolean existsBySenderAndReceiver(User sender, User receiver);

    // 보낸 문의 중 특정 상태만 조회
    List<Partnership> findBySenderUserIdAndSendStatus(Long userId, SendStatus sendStatus);

    // 받은 문의 중 특정 응답 상태만 조회
    List<Partnership> findByReceiverUserIdAndSendStatus(Long userId, SendStatus sendStatus);

    // ✅ Accepted 상태의 partnership 조회 (sender/receiver 둘 다 포함)
    @EntityGraph(attributePaths = {
            "sender","receiver","sender.orgProfile","sender.store","receiver.orgProfile","receiver.store"
    })
    List<Partnership> findByAcceptedStatusAndSenderUserIdOrAcceptedStatusAndReceiverUserId(
            AcceptedStatus status1, Long senderId,
            AcceptedStatus status2, Long receiverId
    );
}

