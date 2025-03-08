package com.sky.controller.admin;

import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "订单管理接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(String page, String pageSize, String number, String beginTime, String endTime,
                                              String phone, String status) {
        log.info("page:{},pageSize:{},number:{},beginTime:{},endTime:{},phone:{},status:{}", page, pageSize, number, beginTime, endTime, phone, status);
        PageResult pageResult = orderService.conditionSearch(page, pageSize, number, beginTime, endTime, phone, status);
        return Result.success(pageResult);
    }


    /**
     * 查询订单详细
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详细")
    public Result<OrderVO> details(@PathVariable Long id) {
        log.info("查询订单详细:{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }


    /**
     * 各个状态的订单数量
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量")
    public Result<OrderStatisticsVO> statistics() {

        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        log.info("各个状态的订单数量:{}", orderStatisticsVO);
        return Result.success(orderStatisticsVO);
    }
}
