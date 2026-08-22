package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiPurchaseAssignmentEmailServiceImplTest {

    @Mock JavaMailSender mailSender;

    AiPurchaseAssignmentEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiPurchaseAssignmentEmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "stocksense@example.com");
    }

    @Test
    void sendsToSelectedEmployeeWithBusinessContent() {
        service.sendAssignmentNotification(assignment());

        SimpleMailMessage sent = sentMessage();
        assertEquals("employee@example.com", sent.getTo()[0]);
        assertTrue(sent.getSubject().contains("SME StockSense"));
        assertTrue(sent.getText().contains("Số lượng AI gợi ý: 70"));
        assertTrue(sent.getText().contains("Số lượng yêu cầu: 50"));
        assertTrue(sent.getText().contains("Mo StockSense va tao phieu nhap."));
        assertTrue(sent.getText().contains("tạo phiếu nhập kho thủ công"));
        assertFalse(sent.getText().contains("app password"));
    }

    @Test
    void missingRecipientEmailBlocked() {
        AiPurchaseRequest assignment = assignment();
        assignment.getReceiver().setEmail(" ");

        assertThrows(BadRequestException.class, () -> service.sendAssignmentNotification(assignment));
    }

    @Test
    void mailFailurePropagatesForCallerToTrackStatus() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class, () -> service.sendAssignmentNotification(assignment()));
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private AiPurchaseRequest assignment() {
        Employee receiver = new Employee();
        receiver.setFullName("Nhan vien A");
        receiver.setEmail("employee@example.com");

        Product product = new Product();
        product.setCode("SP001");
        product.setName("Laptop");

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("K001");
        warehouse.setName("Kho chinh");

        AiPurchaseRequest assignment = new AiPurchaseRequest();
        assignment.setCode("YCAI-1");
        assignment.setReceiver(receiver);
        assignment.setProduct(product);
        assignment.setWarehouse(warehouse);
        assignment.setHorizonDays((short) 7);
        assignment.setAiSuggestedQuantity(70);
        assignment.setRequestedQuantity(50);
        assignment.setContent("Mo StockSense va tao phieu nhap.");
        return assignment;
    }
}
