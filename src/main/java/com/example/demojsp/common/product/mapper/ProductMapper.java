package com.example.demojsp.common.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 수신/여신 상품 TB_PRODUCT (mapper/common/ProductMapper.xml) */
@Mapper
public interface ProductMapper {

    List<ProductRow> selectAll();

    ProductRow selectById(@Param("productId") long productId);

    int update(ProductRow row);

    int insert(ProductRow row);
}
