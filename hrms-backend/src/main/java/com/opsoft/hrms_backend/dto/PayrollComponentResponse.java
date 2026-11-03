
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;

public class PayrollComponentResponse {

    private Long id;
    private String componentName;
    private String componentType;
    private BigDecimal amount;

    public PayrollComponentResponse() {
    }

    public PayrollComponentResponse(
            Long id,
            String componentName,
            String componentType,
            BigDecimal amount) {

        this.id = id;
        this.componentName = componentName;
        this.componentType = componentType;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
