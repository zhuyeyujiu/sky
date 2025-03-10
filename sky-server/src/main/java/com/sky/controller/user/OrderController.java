package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;




    /**
     * 处理用户下单
     * @param ordersSubmitDTO
     */
    @PostMapping("submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }


    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        //log.info("生成预支付交易单：{}", orderPaymentVO);
        // 模拟支付成功
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }


    /**
     * 查询订单信息
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单信息")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id){
        log.info("查询订单信息：{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }


    /**
     * 查询历史订单
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> historyOrders(String page, String pageSize, String status){
        log.info("pageNum:{},pageSize:{},status:{}",page,pageSize,status);
        PageResult pageResult = orderService.historyOrders(page, pageSize, status);
        return Result.success(pageResult);
    }



    /**
     * 取消订单
     * @param id
     */
    @ApiOperation("取消订单")
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id){
        log.info("取消订单：{}", id);
        orderService.cancel(id);
        return Result.success();
    }


    /**
     * 再来一单
     */
    @ApiOperation("再来一单")
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable Long id){
        log.info("再来一单：{}", id);
        orderService.repetition(id);
        return Result.success();
    }


    /**
     * 催单
     */
    @ApiOperation("催单")
    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable Long id){
        log.info("催单：{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
