package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import jpabook.jpashop.service.OrderSearch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /* 주문 조회 - 엔티티 직접노출(지양) */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();        // LAZY 초기화
            order.getDelivery().getAddress();   // LAZY 초기화
            List<OrderItem> orderItems = order.getOrderItems(); // LAZY 초기화
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /* 주문 조회 - 엔티티를 DTO로 변환 */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                                .map(o -> new OrderDto(o))
                                .collect(toList());
        return collect;
    }

    /* 주문 조회 - 엔티티를 DTO로 변환 - 페치 조인 최적화 */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /* 주문 조회 - 엔티티를 DTO로 변환 - 페이징과 한계 돌파 */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /* 주문 조회 - JPA에서 DTO 직접조회 */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /* 주문 조회 - JPA에서 DTO 직접조회(컬렉션 조회 최적화) */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /* 주문 조회 - JPA에서 DTO 직접조회(플랫 데이터 최적화) */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                         mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;     // 엔티티 의존을 완전히 끊어야 한다.

        public OrderDto(Order o) {
            orderId = o.getId();
            name = o.getMember().getName();
            orderDate = o.getOrderDate();
            orderStatus = o.getStatus();
            address = o.getDelivery().getAddress();
            orderItems = o.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
