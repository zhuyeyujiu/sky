package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);


    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    OrderVO getOrderDetail(Long id);


    /**
     * 历史订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    PageResult historyOrders(String pageNum, String pageSize, String status);


    /**
     * 取消订单
     * @param id
     */
    void cancel(Long id);


    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);


    /**
     * 条件搜索订单
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @param phone
     * @param status
     * @return
     */
    PageResult conditionSearch(String page, String pageSize, String number, String beginTime, String endTime, String phone, String status);


    /**
     * 各个状态的订单数量
     * @return
     */
    OrderStatisticsVO statistics();
}
