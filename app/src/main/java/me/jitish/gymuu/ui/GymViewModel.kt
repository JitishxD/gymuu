package me.jitish.gymuu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.CreateExerciseDraft
import me.jitish.gymuu.data.CustomExercise
import me.jitish.gymuu.data.Exercise
import me.jitish.gymuu.data.ExerciseRepository
import me.jitish.gymuu.data.Routine
import me.jitish.gymuu.data.RoutineRepository
import me.jitish.gymuu.data.WorkoutDay

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val exerciseRepository = ExerciseRepository(application)
    private val routineRepository = RoutineRepository(application)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(ExerciseCategory.ALL)

    val uiState: StateFlow<GymUiState> = combine(
        exerciseRepository.exercises,
        routineRepository.routines,
        routineRepository.customExercises,
        searchQuery,
        selectedCategory
    ) { exercises, routines, customExercises, search, category ->
        GymUiState(
            exercises = exercises,
            routines = routines,
            customExercises = customExercises,
            searchQuery = search,
            selectedCategory = category
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GymUiState()
    )

    init {
        viewModelScope.launch {
            exerciseRepository.loadExercises()
        }
    }

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onCategorySelected(category: ExerciseCategory) {
        selectedCategory.value = category
    }

    fun createRoutine(name: String) = routineRepository.createRoutine(name)
    fun updateRoutineName(routineId: String, name: String) = routineRepository.updateRoutineName(routineId, name)
    fun deleteRoutine(routineId: String) = routineRepository.deleteRoutine(routineId)
    fun addDay(routineId: String) = routineRepository.addDay(routineId)
    fun updateDayName(routineId: String, dayId: String, name: String) = routineRepository.updateDayName(routineId, dayId, name)
    fun removeDay(routineId: String, dayId: String) = routineRepository.removeDay(routineId, dayId)
    fun addBuiltInExercise(routineId: String, dayId: String, exercise: Exercise) = routineRepository.addBuiltInExercise(routineId, dayId, exercise)
    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) = routineRepository.addCustomExercise(routineId, dayId, exercise)
    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) = routineRepository.removeExercise(routineId, dayId, routineExerciseId)
    fun addSet(routineId: String, dayId: String, routineExerciseId: String) = routineRepository.addSet(routineId, dayId, routineExerciseId)
    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) = routineRepository.removeSet(routineId, dayId, routineExerciseId, setId)
    fun updateSet(routineId: String, dayId: String, routineExerciseId: String, setId: String, reps: String? = null, weight: String? = null, completed: Boolean? = null) = routineRepository.updateSet(routineId, dayId, routineExerciseId, setId, reps, weight, completed)
    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) = routineRepository.updateRest(routineId, dayId, routineExerciseId, rest)
    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) = routineRepository.updateNotes(routineId, dayId, routineExerciseId, notes)
    fun upsertCustomExercise(draft: CreateExerciseDraft) = routineRepository.upsertCustomExercise(draft)
    fun deleteCustomExercise(exerciseId: String) = routineRepository.deleteCustomExercise(exerciseId)
}

data class GymUiState(
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val customExercises: List<CustomExercise> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ExerciseCategory = ExerciseCategory.ALL
) {
    fun routine(routineId: String): Routine? = routines.firstOrNull { it.id == routineId }
    fun day(routineId: String, dayId: String): WorkoutDay? = routine(routineId)?.days?.firstOrNull { it.id == dayId }

    fun filteredCustomExercises(): List<CustomExercise> {
        if (selectedCategory != ExerciseCategory.ALL && selectedCategory != ExerciseCategory.CUSTOM) return emptyList()
        val query = searchQuery.trim().lowercase()
        return customExercises.filter { query.isBlank() || it.name.lowercase().contains(query) }
    }

    fun filteredBuiltInExercises(): List<Exercise> {
        if (selectedCategory == ExerciseCategory.CUSTOM) return emptyList()
        val query = searchQuery.trim().lowercase()
        return exercises.filter { exercise ->
            val categoryMatch = selectedCategory == ExerciseCategory.ALL || exercise.matchesCategory(selectedCategory)
            val searchMatch = query.isBlank() || exercise.searchableText().contains(query)
            categoryMatch && searchMatch
        }
    }

    fun builtInSections(limit: Int = Int.MAX_VALUE): Map<ExerciseCategory, List<Exercise>> {
        var remaining = limit.coerceAtLeast(0)
        if (remaining == 0) return emptyMap()

        val grouped = filteredBuiltInExercises()
            .groupBy { exercise -> ExerciseCategory.sectionOrder.firstOrNull { exercise.matchesCategory(it) } ?: ExerciseCategory.CORE }
            .toSortedMap(compareBy { ExerciseCategory.sectionOrder.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })

        val visibleSections = linkedMapOf<ExerciseCategory, List<Exercise>>()
        grouped.forEach { (category, exercises) ->
            if (remaining == 0) return@forEach

            val visibleExercises = exercises.take(remaining)
            if (visibleExercises.isNotEmpty()) {
                visibleSections[category] = visibleExercises
                remaining -= visibleExercises.size
            }
        }

        return visibleSections
    }
}

enum class ExerciseCategory(val label: String) {
    ALL("ALL"),
    CUSTOM("CUSTOM"),
    CHEST("CHEST"),
    BACK("BACK"),
    LEGS_GLUTES("LEGS/GLUTES"),
    BICEPS("BICEPS"),
    TRICEPS("TRICEPS"),
    SHOULDERS("SHOULDERS"),
    CORE("CORE");

    companion object {
        val sectionOrder = listOf(CHEST, BACK, SHOULDERS, LEGS_GLUTES, BICEPS, TRICEPS, CORE)
    }
}

private fun Exercise.searchableText(): String {
    return listOf(name, bodyParts.joinToString(" "), equipments.joinToString(" "), targetMuscles.joinToString(" "), secondaryMuscles.joinToString(" "))
        .joinToString(" ")
        .lowercase()
}

private fun Exercise.matchesCategory(category: ExerciseCategory): Boolean {
    val body = bodyParts.map { it.lowercase() }
    val target = targetMuscles.map { it.lowercase() }
    val secondary = secondaryMuscles.map { it.lowercase() }

    // Category filtering follows the requested body-part and muscle mapping from the exercise database.
    return when (category) {
        ExerciseCategory.ALL -> true
        ExerciseCategory.CUSTOM -> false
        ExerciseCategory.CHEST -> body.contains("chest") || target.contains("pectorals") || secondary.contains("chest")
        ExerciseCategory.BACK -> body.contains("back") || target.any { it in listOf("lats", "upper back", "spine") }
        ExerciseCategory.LEGS_GLUTES -> body.any { it in listOf("upper legs", "lower legs") } || target.any { it in listOf("glutes", "quads", "hamstrings", "calves", "abductors") }
        ExerciseCategory.BICEPS -> target.contains("biceps") || secondary.contains("biceps")
        ExerciseCategory.TRICEPS -> target.contains("triceps") || secondary.contains("triceps")
        ExerciseCategory.SHOULDERS -> body.contains("shoulders") || target.contains("delts") || secondary.contains("shoulders")
        ExerciseCategory.CORE -> body.contains("waist") || target.contains("abs") || secondary.any { it in listOf("core", "obliques") }
    }
}
