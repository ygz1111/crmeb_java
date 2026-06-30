package com.zbkj.front.controller;

import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderPayRequest;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.PayConfigResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.service.service.OrderPayService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.WeChatPayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 微信缓存表 前端控制器*/
@Slf4j
@RestController
@RequestMapping("api/front/pay")
@Api(tags = "支付管理")
public class PayController {

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private OrderPayService orderPayService;

    @Autowired
    private StoreOrderService storeOrderService;



    @ApiOperation(value = "获取支付配置")
    @RequestMapping(value = "/get/config", method = RequestMethod.GET)
    public CommonResult<PayConfigResponse> getPayConfig() {
        return CommonResult.success(orderPayService.getPayConfig());
    }

    /**
     * 订单支付
     */
    @ApiOperation(value = "订单支付")
    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public CommonResult<OrderPayResultResponse> payment(@RequestBody @Validated OrderPayRequest orderPayRequest, HttpServletRequest request) {
        String ip = CrmebUtil.getClientIp(request);
        return CommonResult.success(orderPayService.payment(orderPayRequest, ip));
    }

    /**
     * 查询支付结果
     *
     * @param orderNo |订单编号|String|必填
     */
    @ApiOperation(value = "查询支付结果")
    @RequestMapping(value = "/queryPayResult", method = RequestMethod.GET)
    public CommonResult<Boolean> queryPayResult(@RequestParam String orderNo) {
        return CommonResult.success(weChatPayService.queryPayResult(orderNo));
    }

    /**
     * 模拟支付（大学期末项目专用）
     * 直接标记订单为已支付，跳过真实支付流程
     */
    @ApiOperation(value = "模拟支付")
    @RequestMapping(value = "/mock", method = RequestMethod.POST)
    public CommonResult<Map<String, Object>> mockPay(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        if (orderNo == null || orderNo.isEmpty()) {
            return CommonResult.failed("订单号不能为空");
        }
        // 用 orderId 字段查订单
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreOrder> lqw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        lqw.eq(StoreOrder::getOrderId, orderNo);
        StoreOrder order = storeOrderService.getOne(lqw);
        if (order == null) {
            return CommonResult.failed("订单不存在");
        }
        if (order.getPaid()) {
            return CommonResult.failed("订单已支付");
        }
        order.setPaid(true);
        order.setPayTime(cn.hutool.core.date.DateUtil.date());
        storeOrderService.updateById(order);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("orderNo", orderNo);
        return CommonResult.success(result);
    }
}
