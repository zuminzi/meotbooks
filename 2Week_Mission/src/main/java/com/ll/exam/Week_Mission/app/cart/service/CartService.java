package com.ll.exam.Week_Mission.app.cart.service;

import com.ll.exam.Week_Mission.app.cart.entity.CartItem;
import com.ll.exam.Week_Mission.app.cart.repository.CartItemRepository;
import com.ll.exam.Week_Mission.app.member.entity.Member;
import com.ll.exam.Week_Mission.app.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {
    private final CartItemRepository cartItemRepository;

    public CartItem addItem(Member buyer, Product product) {
        CartItem existingCartItem = cartItemRepository.findByBuyerIdAndProductId(buyer.getId(), product.getId()).orElse(null);

        if (existingCartItem!=null) {
            return existingCartItem;
        }

        CartItem cartItem = CartItem.builder()
                .buyer(buyer)
                .product(product)
                .build();

        cartItemRepository.save(cartItem);

        return cartItem;
    }

    @Transactional
    public boolean removeItem(Member buyer, Product product) {
        CartItem existingCartItem = cartItemRepository.findByBuyerIdAndProductId(buyer.getId(), product.getId()).orElse(null);

        if (existingCartItem!=null) {
            cartItemRepository.delete(existingCartItem);
            return true;
        }

        return false;
    }

    // CartServiceTests용 메서드
    public boolean hasItem(Member buyer, Product product) {
        return cartItemRepository.existsByBuyerIdAndProductId(buyer.getId(), product.getId());
    }
}
