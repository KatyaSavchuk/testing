package io.proj3ct.Spring.service;

import io.proj3ct.Spring.model.Restaurants;
import io.proj3ct.Spring.model.RestaurantsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class RestaurantService {
    @Autowired
    private RestaurantsRepository restaurantsRepository;

    public List<Restaurants> getRestaurantsByDistrict(String district) {
        return restaurantsRepository.findByDistrict(district);
    }

    public List<Restaurants> getRestaurantsByConcept(String concept) {
        return restaurantsRepository.findByConcept(concept);
    }

    public Restaurants getRandomRestaurant() {
        List<Restaurants> restaurantsList = (List<Restaurants>) restaurantsRepository.findAll();
        if (restaurantsList.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return restaurantsList.get(random.nextInt(restaurantsList.size()));
    }

    private String formatRestaurantInfo(Restaurants restaurant) {
        StringBuilder sb = new StringBuilder();
        sb.append("Назва: ").append(restaurant.getName()).append("\n");
        sb.append("Адреса: ").append(restaurant.getAddress()).append("\n");
        sb.append("Район: ").append(restaurant.getDistrict()).append("\n");
        sb.append("Тип кухні: ").append(restaurant.getCuisineType()).append("\n");
        sb.append("Концепція: ").append(restaurant.getConcept()).append("\n");
        sb.append("Графік роботи: ").append(restaurant.getWorkingHours()).append("\n");
        sb.append("Середній чек: ").append(restaurant.getAverageCheck()).append("\n");
        sb.append("Посилання на меню: ").append(restaurant.getMenuLink()).append("\n");
        sb.append("Посилання на відгуки: ").append(restaurant.getReviewsLink()).append("\n");
        return sb.toString();
    }
    public List<Restaurants> getRestaurantsByCuisineType(String cuisineType) {
        return restaurantsRepository.findByCuisineType(cuisineType);
    }

}
