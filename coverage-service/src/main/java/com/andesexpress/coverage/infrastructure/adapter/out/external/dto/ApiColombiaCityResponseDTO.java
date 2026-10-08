package com.andesexpress.coverage.infrastructure.adapter.out.external.dto;

import lombok.Data;

/** Municipio de la lista /City. La lista trae departmentId pero no el objeto department. */
@Data
public class ApiColombiaCityResponseDTO {
    private Integer id;
    private String name;
    private Integer departmentId;
}
