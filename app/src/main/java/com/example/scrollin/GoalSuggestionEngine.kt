package com.example.scrollin

class GoalSuggestionEngine(private val pointsManager: PointsManager) {

    fun getSuggestedGoals(userLevel: Int, completedGoals: List<JourneyGoal>): List<JourneyGoal> {
        val suggestions = mutableListOf<JourneyGoal>()

        // Based on user level, suggest appropriate goals
        when (userLevel) {
            1 -> {
                suggestions.add(createGoal("Complete 5 push-ups", 10, Difficulty.EASY, Category.PHYSICAL))
                suggestions.add(createGoal("Meditate for 3 minutes", 10, Difficulty.EASY, Category.MENTAL))
            }
            2 -> {
                suggestions.add(createGoal("Complete 15 push-ups", 20, Difficulty.MEDIUM, Category.PHYSICAL))
                suggestions.add(createGoal("Read for 20 minutes", 25, Difficulty.MEDIUM, Category.PRODUCTIVITY))
            }
        }

        // Based on completion patterns
        val mostCompletedCategory = completedGoals
            .groupBy { it.category }
            .maxByOrNull { it.value.size }?.key

        if (mostCompletedCategory != null) {
            when (mostCompletedCategory) {
                Category.PHYSICAL -> suggestions.add(createGoal("Go for a 15-min walk", 20, Difficulty.MEDIUM, Category.PHYSICAL))
                Category.MENTAL -> suggestions.add(createGoal("Listen to a podcast for 15 mins", 20, Difficulty.MEDIUM, Category.MENTAL))
                Category.PRODUCTIVITY -> suggestions.add(createGoal("Organize your workspace", 20, Difficulty.MEDIUM, Category.PRODUCTIVITY))
                else -> {}
            }
        }

        return suggestions.distinctBy { it.title }.take(2)
    }

    private fun createGoal(title: String, points: Int, difficulty: Difficulty, category: Category): JourneyGoal {
        return JourneyGoal(
            id = "suggestion_${title.hashCode()}",
            title = title,
            points = points,
            difficulty = difficulty,
            category = category,
            type = GoalType.GENERAL
        )
    }
}
