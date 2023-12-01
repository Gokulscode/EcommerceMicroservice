package com.dailycodebuffer.Productservice.service;

import com.dailycodebuffer.Productservice.entity.Product;
import com.dailycodebuffer.Productservice.exception.ProductServiceCustomException;
import com.dailycodebuffer.Productservice.model.ProductRequest;
import com.dailycodebuffer.Productservice.model.ProductResponse;
import com.dailycodebuffer.Productservice.repository.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ProductServiceImpl implements ProductService{
    @Autowired
    private ProductRepository productRepository;

    @Override
    public long addProduct(ProductRequest productRequest) {
        log.info("Adding the product..");
        Product product=Product.builder()
                        .productName(productRequest.getName())
                        .quantity(productRequest.getQuantity())
                        .price(productRequest.getPrice())
                        .build();

        productRepository.save(product);
        log.info("product is created :");
        return product.getProductId();
    }

    @Override
    public ProductResponse getProductById(long productId) {
        log.info("get product by id..");
        Product p=productRepository.findById(productId).orElseThrow(()->new ProductServiceCustomException("product with gioven product_id not found","PRODUCT_NOT_FOUND")) ;
        ProductResponse r=new ProductResponse();
        BeanUtils.copyProperties(p,r);
        return r;
    }

    @Override
    public void reduceQuantity(long productId, long quantity) {
        log.info("Reduce Quantity {} for Id :{}",quantity,productId);
        Product product=productRepository.findById(productId).orElseThrow(()->new ProductServiceCustomException("product with given id not found","PRODUCT_NOT_FOUND"));
        if(product.getQuantity()<quantity){
            throw new ProductServiceCustomException("Product doesnot have sufficient quantity","INSUFFICIENT_QUANTITY");
        }
        product.setQuantity(product.getQuantity()-quantity);
        productRepository.save(product);
        log.info("product quantity updated sucessfully");
    }
}
