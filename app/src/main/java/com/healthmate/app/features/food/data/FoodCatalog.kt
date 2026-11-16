package com.healthmate.app.features.food.data

/** A food in the built-in list, with its approximate calories for one serving. */
data class FoodItem(
    val name: String,
    val serving: String,
    val kcalPerServing: Int,
)

/**
 * Common foods with approximate calories per serving.
 * General foods are based on typical food-composition table values; Sri Lankan
 * dishes vary a lot by recipe, so they are rounded averages. The app shows
 * them as estimates and the user can add a custom food with their own value.
 */
object FoodCatalog {
    val items: List<FoodItem> = listOf(
        // Sri Lankan
        FoodItem("Rice and curry", "1 plate (rice + 2 curries)", 650),
        FoodItem("White rice", "1 cup cooked", 240),
        FoodItem("Red rice", "1 cup cooked", 215),
        FoodItem("String hoppers", "10 pieces", 280),
        FoodItem("Plain hopper", "1 hopper", 110),
        FoodItem("Egg hopper", "1 hopper", 180),
        FoodItem("Pol roti", "1 roti", 220),
        FoodItem("Kottu roti (chicken)", "1 plate", 800),
        FoodItem("Paratha (godamba roti)", "1 piece", 280),
        FoodItem("Dhal curry", "1 cup", 230),
        FoodItem("Chicken curry", "1 cup", 300),
        FoodItem("Fish curry", "1 cup", 220),
        FoodItem("Coconut sambol", "2 tablespoons", 90),
        FoodItem("Milk rice (kiribath)", "2 pieces", 330),
        FoodItem("Fish bun", "1 bun", 250),
        FoodItem("Vegetable roti", "1 piece", 200),
        FoodItem("Milk tea with sugar", "1 cup", 70),
        // General
        FoodItem("Bread (white)", "1 slice", 80),
        FoodItem("Boiled egg", "1 large egg", 78),
        FoodItem("Omelette", "2 eggs", 190),
        FoodItem("Banana", "1 medium", 105),
        FoodItem("Apple", "1 medium", 95),
        FoodItem("Orange", "1 medium", 62),
        FoodItem("Papaya", "1 cup pieces", 62),
        FoodItem("Full cream milk", "1 glass (250 ml)", 150),
        FoodItem("Plain yoghurt", "1 cup", 150),
        FoodItem("Oats porridge", "1 cup cooked", 165),
        FoodItem("Grilled chicken breast", "100 g", 165),
        FoodItem("Fried chicken", "1 piece", 280),
        FoodItem("Fried rice", "1 cup", 240),
        FoodItem("Noodles (cooked)", "1 cup", 220),
        FoodItem("Pizza", "1 slice", 285),
        FoodItem("Burger", "1 burger", 350),
        FoodItem("Soft drink", "1 can (330 ml)", 140),
        FoodItem("Black coffee", "1 cup", 2),
    )

    /** Foods whose name contains [query] (case does not matter). */
    fun search(query: String): List<FoodItem> {
        val text = query.trim()
        return if (text.isEmpty()) items else items.filter { it.name.contains(text, ignoreCase = true) }
    }
}
