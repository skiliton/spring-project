package com.ajax.springcourse.car.repository.reactive

import com.ajax.springcourse.car.model.Car
import com.ajax.springcourse.car.repository.RedisCarRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import javax.annotation.PostConstruct

@Component
@Profile("redis_reactive")
class RedisReactiveCarRepository @Autowired constructor(
    val template: ReactiveRedisTemplate<String, String>
) : ReactiveCarRepository {

    companion object {
        const val ID = "id"
        const val BRAND = "brand"
        const val MODEL = "model"
        const val SEATS = "seats"
        const val DESCRIPTION = "description"
    }

    private lateinit var hashOperations: ReactiveHashOperations<String, String, String>
    private lateinit var setOperations: ReactiveSetOperations<String, String>

    @PostConstruct
    fun init() {
        hashOperations = template.opsForHash()
        setOperations = template.opsForSet()
    }

    override fun findByModel(model: String): Flux<Car> = findHashIdsByModel(model)
        .map(hashOperations::entries)
        .flatMap(this::entryFluxToCarMono)

    override fun findById(id: String): Mono<Car> = hashOperations
        .entries(getCarHashKey(id))
        .`as`(this::entryFluxToCarMono)
        .filter{it!=Car()}


    override fun save(car: Car): Mono<Car> = Mono
        .just(car)
        .flatMap(this::removeModelIndexIfModelChanged)
        .flatMap(this::createIdIfNotExists)
        .flatMap(this::createModelIndexIfNotExists)
        .flatMap(this::saveAsHash)

    override fun findAll(): Flux<Car> = getAllHashKeys()
        .map(hashOperations::entries)
        .flatMap(this::entryFluxToCarMono)

    override fun deleteAll(): Mono<Unit> = deleteAllCarHashes()
        .then(deleteAllCarIndexes())
        .then(Mono.empty())

    private fun deleteAllCarHashes() = template.delete(getAllHashKeys())

    private fun deleteAllCarIndexes() = template.delete(getAllModelIndexes())

    private fun removeModelIndexIfModelChanged(car: Car): Mono<Car> = Mono
        .just(car)
        .filter {it.id!=null}
        .flatMap(this::findOldCar)
        .filter { oldCar -> oldCar.model != car.model }
        .flatMap(this::deleteModelIndex)
        .then(Mono.just(car))

    private fun findOldCar(car: Car): Mono<Car> = hashOperations
        .entries(getCarHashKey(car.id!!))
        .reduce(Car(), this::accumulateIntoCar)

    private fun saveAsHash(car: Car): Mono<Car> = hashOperations
        .putAll(
            getCarHashKey(car.id!!),
            carToEntries(car)
        )
        .then(Mono.just(car))


    private fun createIdIfNotExists(car: Car): Mono<Car> = Mono
        .just(car)
        .filter{it.id==null}
        .map {
            car.id = UUID.randomUUID().toString()
            car
        }.defaultIfEmpty(car)


    private fun carToEntries(car: Car): MutableMap<String, String> {
        val entries: MutableMap<String, String> = mutableMapOf()
        entries[RedisCarRepository.ID] = car.id ?: ""
        entries[RedisCarRepository.BRAND] = car.brand
        entries[RedisCarRepository.MODEL] = car.model
        entries[RedisCarRepository.SEATS] = car.seats.toString()
        entries[RedisCarRepository.DESCRIPTION] = car.description
        return entries
    }

    private fun createModelIndexIfNotExists(car: Car): Mono<Car> = setOperations
        .add(
            getModelIndexKey(car.model),
            getCarHashKey(car.id!!))
        .then(Mono.just(car))

    private fun deleteModelIndex(car: Car) = setOperations
        .remove(
            getModelIndexKey(car.model),
            getCarHashKey(car.id!!))
        .then(Mono.just(car))

    private fun getAllHashKeys(): Flux<String> = template
        .keys(getCarHashKey("*"))

    private fun getAllModelIndexes(): Flux<String> = template
        .keys(getModelIndexKey("*"))

    private fun findHashIdsByModel(model: String): Flux<String> = setOperations
        .members(getModelIndexKey(model))
        .map(this::getCarHashKey)

    private fun getModelIndexKey(model: String): String = "indexes:cars:model:$model"

    private fun getCarHashKey(id: String): String = "cars:$id"

    private fun accumulateIntoCar(car: Car, entry: Map.Entry<String, String>): Car {
        when (entry.key) {
            ID -> car.id = entry.value
            BRAND -> car.brand = entry.value
            MODEL -> car.model = entry.value
            SEATS -> car.seats = entry.value.toInt()
            DESCRIPTION -> car.description = entry.value
        }
        return car
    }

    private fun entryFluxToCarMono(entries: Flux<Map.Entry<String, String>>): Mono<Car> = entries
        .reduceWith(::Car, this::accumulateIntoCar)
}