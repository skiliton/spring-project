package com.ajax.springcourse.car.service;

import com.ajax.springcourse.car.exception.CarNotFoundException;
import com.ajax.springcourse.car.model.Car;
import com.ajax.springcourse.car.model.dto.CarCreateDto;
import com.ajax.springcourse.car.model.dto.CarReadDto;
import com.ajax.springcourse.car.model.dto.CarUpdateDto;
import com.ajax.springcourse.car.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile({"redis", "mongo"})
public class CarServiceImpl implements CarService {

    private final CarRepository repository;

    @Autowired
    public CarServiceImpl(CarRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CarReadDto> findAll() {
        return repository
                .findAll()
                .stream()
                .map(CarReadDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<CarReadDto> findByModel(String model) {
        return repository
                .findByModel(model)
                .stream()
                .map(CarReadDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public CarReadDto findById(String id) {
        return new CarReadDto(repository
                .findById(id)
                .orElseThrow(
                    ()->new CarNotFoundException("Couldn't find car with id="+id)
                ));
    }

    @Override
    public CarReadDto create(CarCreateDto carCreateDto) {
        Car car = carCreateDto.mapToCar();
        return new CarReadDto(repository.save(car));
    }

    @Override
    public CarReadDto update(CarUpdateDto carDto) {
        Car car = repository
                .findById(carDto.getId())
                .orElseThrow(
                    ()->new CarNotFoundException("Couldn't find car with id="+carDto.getId())
                );
        return new CarReadDto(repository.save(carDto.projectOnto(car)));
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
