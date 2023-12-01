package com.dailycodebuffer.Productservice.service;

import com.dailycodebuffer.Productservice.model.ProductRequest;
import com.dailycodebuffer.Productservice.model.ProductResponse;

public interface ProductService {
    long addProduct(ProductRequest productRequest);

    ProductResponse getProductById(long productId);

    void reduceQuantity(long productId, long quantity);
}
