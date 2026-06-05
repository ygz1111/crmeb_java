package com.zbkj.front.service;

import com.zbkj.common.request.SecondHandProductRequest;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.IndexProductResponse;
import java.util.Map;

public interface SecondHandService {

    CommonPage<IndexProductResponse> getMyList(PageParamRequest pageParamRequest);

    Map<String, Object> publish(SecondHandProductRequest request);

    Boolean delete(Integer productId);
}
