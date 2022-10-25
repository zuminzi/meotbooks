package com.ll.exam.Week_Mission.app.order.service;

import com.ll.exam.Week_Mission.app.cart.entity.CartItem;
import com.ll.exam.Week_Mission.app.cart.service.CartService;
import com.ll.exam.Week_Mission.app.exception.DataNotFoundException;
import com.ll.exam.Week_Mission.app.exception.PaymentFailedException;
import com.ll.exam.Week_Mission.app.member.entity.Member;
import com.ll.exam.Week_Mission.app.member.service.MemberService;
import com.ll.exam.Week_Mission.app.order.entity.Order;
import com.ll.exam.Week_Mission.app.order.entity.OrderItem;
import com.ll.exam.Week_Mission.app.order.repository.OrderRepository;
import com.ll.exam.Week_Mission.app.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final MemberService memberService;
    private final CartService cartService;
    private final OrderRepository orderRepository;

    public List<Order> findAllByBuyerId(long actorId){
       return orderRepository.findAllByBuyerId(actorId);
    }

    /* CartItem -> OrderItem */
    @Transactional
    public Order createFromCart(Member buyer) {

        List<CartItem> cartItems = cartService.findByBuyerId(buyer);

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // 주문 가능한 아이템들 주문 품목에 추가
            if (product.isOrderable()) {
                orderItems.add(new OrderItem(product));
            }
            // Todo: 주문 불가능한 아이템 있을 시 장바구니 페이지로 리다이렉트
            // 주문 가능한 아이템, 불가능한 아이템 구별없이 장바구니에서 삭제
            cartService.removeItem(buyer, product);
        }

        return create(buyer, orderItems);
    }

    /* OrderItem -> Order */
    @Transactional
    public Order create(Member buyer, List<OrderItem> orderItems) {
        Order order = Order
                .builder()
                .buyer(buyer)
                .build();

        orderItemsToOrder(order, orderItems);

        makeName(order);

        orderRepository.save(order);

        order.setReadyStatus(true);
        return order;
    }

    public void orderItemsToOrder (Order order, List<OrderItem> orderItems){
        for (OrderItem orderItem : orderItems) {
            // orderItem의 order 속성에 order 저장
            orderItem.setOrder(order);

            // order의 orderItems 리스트 속성에 orderItem 저장
           order.getOrderItems().add(orderItem);
        }
    }

    public void makeName(Order order) {
        String name = order.getOrderItems().get(0).getProduct().getSubject();

        if ( order.getOrderItems().size() > 1 ) {
            name += " 외 %d권".formatted(order.getOrderItems().size() - 1);
        }

        order.setName(name);

    }

    /* Order -> PaymentDone */
    @Transactional
    public void payByRestCashOnly(Order order) {
        Member buyer = order.getBuyer();

        long restCash = buyer.getRestCash();
        int payPrice = order.calculatePayPrice();

        if (payPrice > restCash) {
            throw new PaymentFailedException("예치금이 부족합니다.");
        }

        memberService.addCash(buyer, payPrice * -1, "주문__%d__사용__예치금".formatted(order.getId()));

        order.setPaymentDone();
        orderRepository.save(order);
    }

    /* Order -> PaymentDone */
    /*
    Case 1.토스페이먼츠
    Case 2.토스페이먼츠+예치금
    */
    @Transactional
    public void payByTossPayments(Order order, long useRestCash) {
        Member buyer = order.getBuyer();
        int payPrice = order.calculatePayPrice();

        long pgPayPrice = payPrice - useRestCash;

        memberService.addCash(buyer, pgPayPrice, "주문__%d__충전__토스페이먼츠".formatted(order.getId()));
        memberService.addCash(buyer, pgPayPrice * -1, "주문__%d__사용__토스페이먼츠".formatted(order.getId()));

        if ( useRestCash > 0 ) {
            memberService.addCash(buyer, useRestCash * -1, "주문__%d__사용__예치금".formatted(order.getId()));
        }

        order.setPaymentDone();
        orderRepository.save(order);
    }

    @Transactional
    public void refund(Order order) {
        int payPrice = order.getPayPrice();
        memberService.addCash(order.getBuyer(), payPrice, "주문__%d__환불__예치금".formatted(order.getId()));

        order.setRefundDone();
        orderRepository.save(order);
    }

    public Order findById(long id) {
        return orderRepository.findById(id).orElseThrow(()-> new DataNotFoundException("해당 주문이 존재하지 않습니다"));
    }

    public boolean actorCanSee(Member actor, Order order) {
        return actor.getId().equals(order.getBuyer().getId());
    }

    public boolean actorCanPayment(Member actor, Order order) {return actorCanSee(actor, order);}
}