package com.dailycodebuffer.OrderService.service;

import com.dailycodebuffer.OrderService.entity.Order;
import com.dailycodebuffer.OrderService.exception.CustomException;
import com.dailycodebuffer.OrderService.external.client.PaymentService;
import com.dailycodebuffer.OrderService.external.client.ProductService;
import com.dailycodebuffer.OrderService.external.client.request.PaymentRequest;
import com.dailycodebuffer.OrderService.external.client.response.PaymentResponse;
import com.dailycodebuffer.OrderService.external.client.response.ProductResponse;
import com.dailycodebuffer.OrderService.model.OrderRequest;
import com.dailycodebuffer.OrderService.model.OrderResponse;
import com.dailycodebuffer.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RestTemplate restTemplate;
    @Override
    public Long placeOrder(OrderRequest orderRequest) {
        //1 -->save the orderentity in the order table
        //2 -->call productservice ->block prodects reduce the quantity
        //3 -->payment service --> payment status


        log.info("placing order request :{}",orderRequest);
        productService.reduceQuantity(orderRequest.getProductId(),orderRequest.getQuantity());
        log.info("creating order with status created ");
        Order order=Order.builder()
                .amount(orderRequest.getTotalAmount())
                .productId(orderRequest.getProductId())
                .orderStatus("CREATED")
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity()).build();
        order=orderRepository.save(order);

        log.info("Calling Payment API");
        PaymentRequest pr= PaymentRequest
                .builder()
                .orderId(order.getId())
                .referenceNumber("REF-001")
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus=null;
        try {
            paymentService.doPayment(pr);
            log.info("payment done successfully changing order status to placed");
            orderStatus="PLACED";

        }catch (Exception e){
            log.info("Error occured on payment API{}",e);
            orderStatus="PAYMENT_FAILED";

        }
        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("order placed successfully with order id:{}",order.getId());

        return order.getId();

        
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("get order info based on orderid:{}",orderId);
        Order order=  orderRepository.findById(orderId).orElseThrow(()->new CustomException("Order not found for the orderId","NOT_FOUND",404));
        log.info("Invoking product service to get the product service details using resttemplate call");
        ProductResponse productResponse=restTemplate.getForObject("http://PRODUCT-SERVICE/product/"+order.getProductId(),ProductResponse.class);
        OrderResponse.ProductDetails productDetails=OrderResponse.ProductDetails.builder()
                .productName(productResponse.getProductName())
                .productId(productResponse.getProductId())
                .build();
        log.info("Invoking payment service to get the payment service details for the orderId using resttemplate call");
        PaymentResponse paymentResponse=restTemplate.getForObject("http://PAYMENT-SERVICE/payment/order/"+order.getId(), PaymentResponse.class);
        OrderResponse.PaymentDetails paymentDetails=OrderResponse.PaymentDetails.builder()
                .paymentId(paymentResponse.getPaymentId())
                .paymentDate(paymentResponse.getPaymentDate()).build();
        OrderResponse orderResponse=OrderResponse
                .builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .orderDate(order.getOrderDate()).build();
        return orderResponse;
    }
}
