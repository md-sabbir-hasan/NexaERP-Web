package com.nexaerp.vendorbill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorBillItemRepository extends JpaRepository<VendorBillItem, Long> {
    List<VendorBillItem> findByVendorBillId(Long billId);
}
