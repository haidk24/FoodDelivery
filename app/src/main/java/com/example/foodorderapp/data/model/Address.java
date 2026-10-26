package com.example.foodorderapp.data.model;

public class Address {
    private String street;
    private String ward;
    private String district;
    private String city;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Address() {} // Bắt buộc

    // Cần tạo các Getter/Setter (Alt + Insert -> Getter and Setter)

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }
}
