package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SecondHandProductRequest;
import com.zbkj.common.response.IndexProductResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.front.service.SecondHandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/front")
@Api(tags = "second hand product")
public class SecondHandController {

    @Autowired
    private SecondHandService secondHandService;

    @ApiOperation(value = "publish second hand product")
    @RequestMapping(value = "/secondhand/publish", method = RequestMethod.POST)
    public CommonResult<Map<String, Object>> publish(@RequestBody @Validated SecondHandProductRequest request) {
        return CommonResult.success(secondHandService.publish(request));
    }

    @ApiOperation(value = "get my second hand product list")
    @RequestMapping(value = "/secondhand/mylist", method = RequestMethod.GET)
    public CommonResult<CommonPage<IndexProductResponse>> getMyList(@Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(secondHandService.getMyList(pageParamRequest));
    }

    @ApiOperation(value = "delete second hand product")
    @RequestMapping(value = "/secondhand/delete/{id}", method = RequestMethod.POST)
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(secondHandService.delete(id));
    }
}