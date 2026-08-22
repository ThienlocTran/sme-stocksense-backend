package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void sendsToSelectedEmployeeWithBusinessContent() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendAssignmentNotification(assignment());

        MimeMessage sent = sentMessage();
        assertEquals("employee@example.com", sent.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(sent.getSubject().contains("SME StockSense"));
        
        String content = sent.getContent().toString();
        assertTrue(content.contains("Số lượng AI gợi ý"));
        assertTrue(content.contains("70"));
        assertTrue(content.contains("Số lượng yêu cầu"));
        assertTrue(content.contains("50"));
        assertTrue(content.contains("Mo StockSense va tao phieu nhap."));
        assertTrue(content.contains("tạo phiếu nhập kho thủ công"));
        assertFalse(content.contains("app password"));
    }

    @Test
    void missingRecipientEmailBlocked() {
        AiPurchaseRequest assignment = assignment();
        assignment.getReceiver().setEmail(" ");

        assertThrows(BadRequestException.class, () -> service.sendAssignmentNotification(assignment));
    }

    @Test
    void mailFailurePropagatesForCallerToTrackStatus() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class, () -> service.sendAssignmentNotification(assignment()));
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
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
