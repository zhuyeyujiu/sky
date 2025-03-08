package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理业务异常(地址簿为空 购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = new ShoppingCart();
        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        // 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now()); // 下单时间
        orders.setPayStatus(Orders.UN_PAID); // 订单状态
        orders.setStatus(Orders.PENDING_PAYMENT); // 待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 订单号
        orders.setPhone(addressBook.getPhone()); // 手机号
        orders.setConsignee(addressBook.getConsignee()); // 收货人
        orders.setUserId(userId); // 用户id
        orders.setAddress(addressBook.getDetail());// 收货地址

        orderMapper.insert(orders);


        List<OrderDetail> orderDetailList = new ArrayList<OrderDetail>();
        // 向订单明细表插入n条数据
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail(); // 订单明细对象
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 封装VO并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        // 模拟支付成功
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 获取订单信息
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        Orders orders = orderMapper.getOrderById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        log.info("订单信息：{}", orderVO);
        return orderVO;
    }


    /**
     * 历史订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult historyOrders(String page,String pageSize, String status) {
        if(page != null && pageSize != null){
            Integer pageNum = Integer.parseInt(page);
            Integer pageSizeNum = Integer.parseInt(pageSize);
            PageHelper.startPage(pageNum, pageSizeNum);

            Long userId = BaseContext.getCurrentId();

            Orders orders = Orders.builder()
                    .userId(userId)
                    .build();

            if(status != null){
                orders.setStatus(Integer.parseInt(status));
            }


            Page<OrderVO> pageResult = orderMapper.page(orders, null, null);

            log.info("分页查询结果：{}", pageResult);

            return new PageResult(pageResult.getTotal(),pageResult.getResult());
        }

        return null;

    }


    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) {
        // 查询当前订单状态
        Orders orders = orderMapper.getOrderById(id);

        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }else if(orders.getStatus() == Orders.TO_BE_CONFIRMED){
            // 未接单时可以无条件取消订单
            orders.setStatus(Orders.CANCELLED);
            orders.setCancelTime(LocalDateTime.now());
            orderMapper.update(orders);
            // TODO 状态为接单时 需要与商家协商取消订单
        }else{
            // 其他情况暂时不接受订单取消操作
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    }


    /**
     * 再来一单
     * @param id
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        Orders orders = orderMapper.getOrderById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);

        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }else{
            orders.setStatus(Orders.PENDING_PAYMENT);
            orders.setId(null);
            orders.setNumber(String.valueOf(System.currentTimeMillis()));
            orders.setOrderTime(LocalDateTime.now());
            orders.setPayStatus(Orders.UN_PAID);
            orders.setCheckoutTime(null);
            orders.setCancelTime(null);
            orders.setRejectionReason(null);
            orderMapper.insert(orders);
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setId(null);
                orderDetail.setOrderId(orders.getId());
            }
            orderDetailMapper.insertBatch(orderDetailList);
        }

    }


    /**
     * 条件查询
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @param phone
     * @param status
     * @return
     */
    @Override
    public PageResult conditionSearch(String page, String pageSize, String number, String beginTime, String endTime, String phone, String status) {
        if(page != null && pageSize != null){
            Integer pageNum = Integer.parseInt(page);
            Integer pageSizeNum = Integer.parseInt(pageSize);

            Orders orders = Orders.builder()
                    .number(number)
                    .phone(phone)
                    .build();

            if(status != null){
                orders.setStatus(Integer.parseInt(status));
            }


            Page<OrderVO> pageResult = orderMapper.page(orders, beginTime, endTime);


            log.info("分页查询结果：{}", pageResult);

            return new PageResult(pageResult.getTotal(),pageResult.getResult());
        }

        return null;
    }


    /**
     * 各个状态的订单数量
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        List<Orders> ordersList = orderMapper.list();
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        Integer toBeConfirmed = 0;
        Integer confirmed = 0;
        Integer deliveryInProgress = 0;

        for (Orders orders : ordersList) {
            if(orders.getStatus() == Orders.TO_BE_CONFIRMED)
                toBeConfirmed++;
            else if(orders.getStatus() == Orders.CONFIRMED)
                confirmed++;
            else if(orders.getStatus() == Orders.DELIVERY_IN_PROGRESS)
                deliveryInProgress++;
        }
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;

    }

}
