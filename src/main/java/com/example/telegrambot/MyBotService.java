package com.example.telegrambot;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MyBotService {

    // Generics класс
    public static class CarInfo<T> {
        private final T brand;
        private final String type;
        private final Map<String, List<String>> models;

        public CarInfo(T brand, String type, Map<String, List<String>> models) {
            this.brand = brand;
            this.type = type;
            this.models = models;
        }

        public T getBrand() {
            return brand;
        }

        public String getType() {
            return type;
        }

        public Map<String, List<String>> getModels() {
            return models;
        }

        // Optional
        public Optional<List<String>> getModels(String category) {
            return Optional.ofNullable(models.get(category));
        }
    }

    // Collections и Map
    private static final Map<String, List<String>> BMW_CATEGORIES = new HashMap<>();
    private static final Map<String, List<String>> DODGE_CATEGORIES = new HashMap<>();

    static {
        // BMW категории
        BMW_CATEGORIES.put("седан", Arrays.asList("3 Series", "5 Series", "7 Series"));
        BMW_CATEGORIES.put("внедорожник", Arrays.asList("X3", "X5", "X7"));
        BMW_CATEGORIES.put("купе", Arrays.asList("2 Series", "4 Series", "8 Series"));
        BMW_CATEGORIES.put("пикап", Collections.singletonList("не доступно"));
        BMW_CATEGORIES.put("маслкар", Arrays.asList("M3", "M5", "M8"));

        // Dodge категории
        DODGE_CATEGORIES.put("седан", Arrays.asList("Charger", "Challenger"));
        DODGE_CATEGORIES.put("внедорожник", Collections.singletonList("Durango"));
        DODGE_CATEGORIES.put("купе", Collections.singletonList("Challenger Coupe"));
        DODGE_CATEGORIES.put("пикап", Collections.singletonList("Ram"));
        DODGE_CATEGORIES.put("маслкар", Arrays.asList("Charger SRT Hellcat", "Challenger SRT Demon"));
    }

    // HashMap и TreeMap
    private static final Map<String, CarInfo<String>> CARS_MAP = new HashMap<>();
    private static final TreeMap<String, String> CAR_TYPES_TREE = new TreeMap<>();

    static {
        // Инициализация HashMap
        CARS_MAP.put("BMW", new CarInfo<>("BMW", "German", BMW_CATEGORIES));
        CARS_MAP.put("Dodge", new CarInfo<>("Dodge", "American", DODGE_CATEGORIES));

        // Инициализация TreeMap
        CAR_TYPES_TREE.put("BMW", "German Luxury");
        CAR_TYPES_TREE.put("Dodge", "American Muscle");
    }

    // Stream API методы
    public static List<String> getAllCategories() {
        return BMW_CATEGORIES.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getAvailableModels(String brand, String category) {
        return Optional.ofNullable(CARS_MAP.get(brand))
                .flatMap(carInfo -> carInfo.getModels(category))
                .orElse(Collections.emptyList());
    }

    // Lambda выражения
    public static final Function<String, List<String>> BRAND_MODEL_FINDER = brand ->
            Optional.ofNullable(CARS_MAP.get(brand))
                    .flatMap(carInfo -> carInfo.getModels("маслкар"))
                    .orElse(Arrays.asList("No models found"));

    // Stream с фильтрацией
    public static List<String> filterModelsByKeyword(String keyword) {
        return CARS_MAP.values().stream()
                .flatMap(carInfo -> carInfo.getModels().values().stream())
                .flatMap(List::stream)
                .filter(model -> model.toLowerCase().contains(keyword.toLowerCase()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public static Map<String, List<String>> getCarCategories(String brand) {
        return Optional.ofNullable(CARS_MAP.get(brand))
                .map(CarInfo::getModels)
                .orElse(Collections.emptyMap());
    }


    // Новые методы для интерактивности
    public static List<String> getAllBrands() {
        return new ArrayList<>(CARS_MAP.keySet());
    }

    public static String getCarDescription(String brand) {
        return CAR_TYPES_TREE.getOrDefault(brand, "Unknown brand");
    }

    public static Map<String, Integer> getModelCounts() {
        return CARS_MAP.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getModels().values().stream()
                                .mapToInt(List::size)
                                .sum()
                ));
    }

    // Получить случайную модель
    public static Optional<String> getRandomModel() {
        List<String> allModels = CARS_MAP.values().stream()
                .flatMap(carInfo -> carInfo.getModels().values().stream())
                .flatMap(List::stream)
                .filter(model -> !model.equals("не доступно"))
                .collect(Collectors.toList());

        if (allModels.isEmpty()) {
            return Optional.empty();
        }

        Random random = new Random();
        return Optional.of(allModels.get(random.nextInt(allModels.size())));
    }

    // Поиск по частичному совпадению
    public static List<String> searchModelsPartial(String partialName) {
        return CARS_MAP.values().stream()
                .flatMap(carInfo -> carInfo.getModels().values().stream())
                .flatMap(List::stream)
                .filter(model -> model.toLowerCase().contains(partialName.toLowerCase()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Получить топ моделей
    public static List<String> getTopModels(int limit) {
        return CARS_MAP.values().stream()
                .flatMap(carInfo -> carInfo.getModels().values().stream())
                .flatMap(List::stream)
                .filter(model -> !model.equals("не доступно"))
                .limit(limit)
                .sorted()
                .collect(Collectors.toList());
    }

    // Проверить, является ли модель маслкаром
    public static boolean isMuscleCar(String model) {
        if (model == null) return false;
        String lowerModel = model.toLowerCase();
        return lowerModel.contains("m") ||
                lowerModel.contains("srt") ||
                lowerModel.contains("hellcat") ||
                lowerModel.contains("demon");
    }

    // Получить бренд модели
    public static Optional<String> getBrandOfModel(String model) {
        return CARS_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().getModels().values().stream()
                        .flatMap(List::stream)
                        .anyMatch(m -> m.equals(model)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // Получить категорию модели
    public static Optional<String> getCategoryOfModel(String brand, String model) {
        return Optional.ofNullable(CARS_MAP.get(brand))
                .flatMap(carInfo -> carInfo.getModels().entrySet().stream()
                        .filter(entry -> entry.getValue().contains(model))
                        .map(Map.Entry::getKey)
                        .findFirst());
    }

    // Получить описание модели
    public static String getModelDescription(String brand, String model) {
        Optional<String> category = getCategoryOfModel(brand, model);
        if (category.isPresent()) {
            return String.format("%s %s - %s %s",
                    brand, model, category.get(),
                    isMuscleCar(model) ? "(Muscle Car)" : "");
        }
        return "Описание не найдено";
    }


    // Получить все модели бренда
    public static List<String> getAllModels(String brand) {
        return Optional.ofNullable(CARS_MAP.get(brand))
                .map(carInfo -> carInfo.getModels().values().stream()
                        .flatMap(List::stream)
                        .filter(model -> !model.equals("не доступно"))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // Получить статистику по категориям
    public static Map<String, Long> getCategoryStats() {
        return CARS_MAP.values().stream()
                .flatMap(carInfo -> carInfo.getModels().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingLong(entry -> entry.getValue().size())
                ));
    }

    // Сериализация Map
    public static String serializeMap() {
        return CARS_MAP.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().getType())
                .collect(Collectors.joining(";"));
    }
}
