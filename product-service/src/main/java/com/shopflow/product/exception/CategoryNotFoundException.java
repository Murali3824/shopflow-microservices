package com.shopflow.product.exception;

public class CategoryNotFoundException extends ResourceNotFoundException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}