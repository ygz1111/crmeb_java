package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "SecondHandProductRequest", description = "Second hand product publish request")
public class SecondHandProductRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "product name", required = true)
    private String storeName;

    @ApiModelProperty(value = "product description")
    private String storeInfo;

    @ApiModelProperty(value = "category id")
    private String cateId;

    @ApiModelProperty(value = "original price")
    private BigDecimal price;

    @ApiModelProperty(value = "item condition")
    private String itemCondition;

    @ApiModelProperty(value = "original purchase price")
    private BigDecimal otPrice;

    @ApiModelProperty(value = "stock quantity")
    private Integer stock;

    @ApiModelProperty(value = "unit name")
    private String unitName;

    @ApiModelProperty(value = "image url (uploaded)")
    private String image;

    @ApiModelProperty(value = "detail images urls, comma separated")
    private String sliderImage;

    @ApiModelProperty(value = "product detail content (HTML)")
    private String content;
}
