package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;



    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放Begin到End范围内的每天的日期
        List<LocalDate> dateList = getBetweenDates(begin, end);

        // 创建集合，用于存放日期对应的营业额数据
        List<Double> turnoverList = new ArrayList<>();

        for(LocalDate date : dateList){
            // 获取日期对应的营业额数据
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);


            // select sum(amount) from orders where order_time >= ? and order_time < ? and status = 5
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            log.info("营业额查询条件为：{}",map);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);

        }

       return  TurnoverReportVO.builder()
               .dateList(StringUtils.join(dateList,","))
               .turnoverList(StringUtils.join(turnoverList,","))
               .build();
    }


    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放Begin到End范围内的每天的日期
        List<LocalDate> dateList = getBetweenDates(begin, end);

        // 每日新增用户数量 select count(id) from user where create_time >= ? and create_time < ?
        List<Integer> newUserList = new ArrayList<>();
        // 每日总用户数量 select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();

            // 查询总用户数量
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);

            // 查询新增用户数量
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();

    }


    /**
     * 统计指定时间区间内订单数量统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getBetweenDates(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 获取日期对应的订单总数 select count(id) from orders where order_time >= ? and order_time < ?
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            Integer totalOrderCount = orderMapper.countByMap(map);

            // 获取日期对应的有效订单数量 select count(id) from orders where order_time >= ? and order_time < ? and status = 5
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map);

            orderCountList.add(totalOrderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        // 时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        // 订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0)
            orderCompletionRate = validOrderCount.doubleValue()/totalOrderCount.doubleValue();


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    /**
     * 指定时间区间内的销量排名前十
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        // 获取商品名称列表
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        // 获取销量列表
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }


    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1.查询数据库 获取营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 2.通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");


        try {
            // 基于模板文件创建一个Excel文件
            XSSFWorkbook workbook = new XSSFWorkbook(in);

            // 获取Sheet页
            XSSFSheet sheet = workbook.getSheet("Sheet1");

            // 填充数据 -时间
            sheet.getRow(1).getCell(1).setCellValue("时间: "+dateBegin+"至"+dateEnd);

            // 填充数据 -营业额
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());

            // 填充数据 -订单完成率
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());

            // 填充数据 -新增用户数
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 填充数据 -有效订单数
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());

            // 填充数据 -平均客单价
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据、
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);

                // 查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // 获取到单元格，设置值
                XSSFRow row = sheet.getRow(7 + i);

                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }

            // 3.使用工具类将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);

            // 关闭流
            out.close();
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }





    }


    /**
     * 获取两个日期之间的所有日期
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getBetweenDates(LocalDate begin, LocalDate end) {
        List<LocalDate> result = new ArrayList<>();
        result.add(begin);
        while (!begin.equals(end)) {
            // 日期加1
            begin = begin.plusDays(1);
            result.add(begin);
        }
        return result;
    }
}
