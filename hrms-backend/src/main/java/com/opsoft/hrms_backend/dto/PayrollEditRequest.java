
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class PayrollEditRequest {

    private String changeReason;

    private List<ComponentRequest> components;

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public List<ComponentRequest> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentRequest> components) {
        this.components = components;
    }

    public static class ComponentRequest {

        private String componentName;

        private String componentType;

        private BigDecimal amount;

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
}
