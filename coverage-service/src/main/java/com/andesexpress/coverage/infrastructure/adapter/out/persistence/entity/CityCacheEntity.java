package com.andesexpress.coverage.infrastructure.adapter.out.persistence.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class CityCacheEntity {

    private String pk;
    private String sk;
    private String gsiPk;
    private String gsiSk;
    private String cityName;
    private String departmentName;
    private Long ttl;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    @DynamoDbAttribute("GSI_PK")
    @DynamoDbSecondaryPartitionKey(indexNames = "GSI_Department")
    public String getGsiPk() { return gsiPk; }
    public void setGsiPk(String gsiPk) { this.gsiPk = gsiPk; }

    @DynamoDbAttribute("GSI_SK")
    @DynamoDbSecondarySortKey(indexNames = "GSI_Department")
    public String getGsiSk() { return gsiSk; }
    public void setGsiSk(String gsiSk) { this.gsiSk = gsiSk; }

    @DynamoDbAttribute("cityName")
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    @DynamoDbAttribute("departmentName")
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    @DynamoDbAttribute("ttl")
    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }
}