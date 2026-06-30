package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SecondHandProductRequest;
import com.zbkj.common.response.IndexProductResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.front.service.SecondHandService;
import com.zbkj.service.util.MlServiceClient;
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

    @ApiOperation(value = "update second hand product")
    @RequestMapping(value = "/secondhand/update", method = RequestMethod.POST)
    public CommonResult<Boolean> update(@RequestBody @Validated SecondHandProductRequest request) {
        return CommonResult.success(secondHandService.update(request));
    }

    @ApiOperation(value = "get my second hand product list")
    @RequestMapping(value = "/secondhand/mylist", method = RequestMethod.GET)
    public CommonResult<CommonPage<IndexProductResponse>> getMyList(@Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(secondHandService.getMyList(pageParamRequest));
    }

    @ApiOperation(value = "get public second hand product list (no login)")
    @RequestMapping(value = "/secondhand/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<IndexProductResponse>> getPublicList(@Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(secondHandService.getPublicList(pageParamRequest));
    }

    @ApiOperation(value = "delete second hand product")
    @RequestMapping(value = "/secondhand/delete/{id}", method = RequestMethod.POST)
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(secondHandService.delete(id));
    }
    
    @ApiOperation(value = "get my publish statistics")
    @RequestMapping(value = "/secondhand/stats", method = RequestMethod.GET)
    public CommonResult<Map<String, Integer>> getMyStats() {
        return CommonResult.success(secondHandService.getMyPublishStats());
    }

    @ApiOperation(value = "AI image identification proxy")
    @RequestMapping(value = "/secondhand/identify", method = RequestMethod.POST)
    public CommonResult<Map<String, Object>> identify(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        if (image == null || image.isEmpty()) {
            return CommonResult.failed("image is required");
        }
        log.info("AI identify request received, image length: {}", image.length());
        try {
            Map<String, Object> result = MlServiceClient.identifyFromDataUri(image);
            log.info("AI identify result: {}", result);
            if (result != null) {
                return CommonResult.success(result);
            }
            return CommonResult.failed("AI identification returned null");
        } catch (Exception e) {
            log.error("AI identify proxy error", e);
            return CommonResult.failed("AI service error: " + e.getMessage());
        }
    }
}