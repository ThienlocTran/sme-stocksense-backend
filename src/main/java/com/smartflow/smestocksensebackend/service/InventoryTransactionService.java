package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;

public interface InventoryTransactionService {

    /**
     * Ghi nhận giao dịch biến động tồn kho (log lịch sử kho).
     * Kế thừa T73, track biến động kho phục vụ đối soát.
     *
     * @param productId ID sản phẩm
     * @param warehouseId ID kho hàng
     * @param transactionType Loại giao dịch (ví dụ: NHAP_KHO, XUAT_KHO)
     * @param quantity Số lượng thay đổi (có thể dương/âm)
     * @param quantityBefore Số lượng trước khi thay đổi
     * @param quantityAfter Số lượng sau khi thay đổi
     * @param importReceipt Phiếu nhập liên kết (nếu có)
     * @param note Ghi chú cho giao dịch
     * @return Bản ghi giao dịch kho đã lưu
     */
    InventoryTransaction recordTransaction(
            Long productId,
            Long warehouseId,
            InventoryTransactionType transactionType,
            Integer quantity,
            Integer quantityBefore,
            Integer quantityAfter,
            ImportReceipt importReceipt,
            String note
    );
}
