package com.sky.vo;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class OrderVO extends Orders implements Serializable {

    //订单菜品信息
    private String orderDishes;

    //订单详情
    private List<OrderDetail> orderDetailList;


    // ✅ 计算 orderDishes（根据 orderDetailList 拼接）
    public String getOrderDishes() {
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            return "";
        }
        return orderDetailList.stream()
                .map(od ->  od.getDishNumber()+ "份" + od.getName()) // dishName x 数量
                .collect(Collectors.joining(", "));
    }



}
