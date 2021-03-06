package com.ajax.springcourse.car.model.mapper;

import com.ajax.springcourse.car.model.dto.CarCreateDto;
import com.ajax.springcourse.car.model.dto.CarReadDto;
import com.ajax.springcourse.car.model.dto.CarUpdateDto;
import com.ajax.springcourse.grpc.Car;

public class GrpcCarDtoMapper {
    public static Car.CarReadDto mapToGrpcCarReadDto(CarReadDto dto) {
        return Car.CarReadDto.newBuilder()
                .setId(dto.getId())
                .setBrand(dto.getBrand())
                .setModel(dto.getModel())
                .setSeats(dto.getSeats())
                .setDescription(dto.getDescription())
                .build();
    }

    public static CarCreateDto mapToCarCreateDto(Car.CarCreateDto grpcCarCreateDto) {
        CarCreateDto carCreateDto = new CarCreateDto();
        carCreateDto.setBrand(grpcCarCreateDto.getBrand());
        carCreateDto.setModel(grpcCarCreateDto.getModel());
        carCreateDto.setSeats(grpcCarCreateDto.getSeats());
        carCreateDto.setDescription(grpcCarCreateDto.getDescription());
        return carCreateDto;
    }

    public static CarUpdateDto mapToCarUpdateDto(Car.CarUpdateDto grpcCarUpdateDto) {
        CarUpdateDto carUpdateDto = new CarUpdateDto();
        carUpdateDto.setId(grpcCarUpdateDto.getId());
        carUpdateDto.setDescription(grpcCarUpdateDto.getDescription());
        return carUpdateDto;
    }
}
