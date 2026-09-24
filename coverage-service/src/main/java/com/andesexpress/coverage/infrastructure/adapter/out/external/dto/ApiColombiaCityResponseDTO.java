package com.andesexpress.coverage.infrastructure.adapter.out.external.dto;

import lombok.Data;

@Data
public class ApiColombiaCityResponseDTO {
    private Integer id;
    private String name;
    private DepartmentDTO department;

    @Data
    public static class DepartmentDTO {
        private String name;
    }
}