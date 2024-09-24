package io.proj3ct.Spring.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RestaurantsRepository extends CrudRepository<Restaurants, Long> {
    List<Restaurants> findByDistrict(String district);
    List<Restaurants> findByConcept(String concept);
    List<Restaurants> findByCuisineType(String cuisineType);

}