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
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.exercise.ExerciseRepository
import me.jitish.gymuu.data.routine.CreateExerciseDraft
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.RoutineExercisePastePosition
import me.jitish.gymuu.data.routine.RoutineRepository
import me.jitish.gymuu.data.routine.WorkoutDay

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val exerciseRepository = ExerciseRepository(application)
    private val routineRepository = RoutineRepository(application)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(ExerciseCategory.ALL)
    private val routineExerciseClipboard = MutableStateFlow<List<RoutineExercise>>(emptyList())

    val uiState: StateFlow<GymUiState> = combine(
        combine(
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
        },
        routineExerciseClipboard
    ) { state, copiedExercises ->
        state.copy(copiedExercises = copiedExercises)
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
    fun swapWithBuiltInExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: Exercise) = routineRepository.swapWithBuiltInExercise(routineId, dayId, routineExerciseId, exercise)
    fun swapWithCustomExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: CustomExercise) = routineRepository.swapWithCustomExercise(routineId, dayId, routineExerciseId, exercise)
    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) = routineRepository.removeExercise(routineId, dayId, routineExerciseId)
    fun removeExercises(routineId: String, dayId: String, routineExerciseIds: Set<String>) = routineRepository.removeExercises(routineId, dayId, routineExerciseIds)
    fun copyExercises(exercises: List<RoutineExercise>) {
        routineExerciseClipboard.value = exercises
    }
    fun clearCopiedExercises() {
        routineExerciseClipboard.value = emptyList()
    }
    fun pasteCopiedExercises(
        routineId: String,
        dayId: String,
        anchorExerciseId: String? = null,
        position: RoutineExercisePastePosition = RoutineExercisePastePosition.END
    ): Int {
        val copiedExercises = routineExerciseClipboard.value
        routineRepository.pasteExercisesToDay(
            routineId = routineId,
            dayId = dayId,
            exercises = copiedExercises,
            anchorExerciseId = anchorExerciseId,
            position = position
        )
        routineExerciseClipboard.value = emptyList()
        return copiedExercises.size
    }
    fun moveExercise(routineId: String, dayId: String, routineExerciseId: String, offset: Int) = routineRepository.moveExercise(routineId, dayId, routineExerciseId, offset)
    fun addSet(routineId: String, dayId: String, routineExerciseId: String) = routineRepository.addSet(routineId, dayId, routineExerciseId)
    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) = routineRepository.removeSet(routineId, dayId, routineExerciseId, setId)
    fun updateSet(routineId: String, dayId: String, routineExerciseId: String, setId: String, reps: String? = null, weight: String? = null, completed: Boolean? = null) = routineRepository.updateSet(routineId, dayId, routineExerciseId, setId, reps, weight, completed)
    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) = routineRepository.updateRest(routineId, dayId, routineExerciseId, rest)
    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) = routineRepository.updateNotes(routineId, dayId, routineExerciseId, notes)
    fun upsertCustomExercise(draft: CreateExerciseDraft) = routineRepository.upsertCustomExercise(draft)
    fun deleteCustomExercise(exerciseId: String) = routineRepository.deleteCustomExercise(exerciseId)
    fun exportRoutineBackup(): String = routineRepository.exportBackup()
    fun importRoutineBackup(json: String): Result<String> = routineRepository.importBackup(json)
}

data class GymUiState(
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val customExercises: List<CustomExercise> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ExerciseCategory = ExerciseCategory.ALL,
    val copiedExercises: List<RoutineExercise> = emptyList()
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

    fun builtInSectionGroups(filteredExercises: List<Exercise> = filteredBuiltInExercises()): Map<ExerciseCategory, List<Exercise>> {
        if (selectedCategory != ExerciseCategory.ALL && selectedCategory != ExerciseCategory.CUSTOM) {
            return if (filteredExercises.isEmpty()) {
                emptyMap()
            } else {
                linkedMapOf(selectedCategory to filteredExercises)
            }
        }

        val grouped = filteredExercises
            .groupBy { exercise -> ExerciseCategory.sectionOrder.firstOrNull { exercise.matchesCategory(it) } ?: ExerciseCategory.CORE }

        return linkedMapOf<ExerciseCategory, List<Exercise>>().apply {
            ExerciseCategory.sectionOrder.forEach { category ->
                grouped[category]?.takeIf { it.isNotEmpty() }?.let { put(category, it) }
            }
        }
    }

    fun visibleBuiltInSections(sectionGroups: Map<ExerciseCategory, List<Exercise>>, limit: Int = Int.MAX_VALUE): Map<ExerciseCategory, List<Exercise>> {
        var remaining = limit.coerceAtLeast(0)
        if (remaining == 0) return emptyMap()

        val visibleSections = linkedMapOf<ExerciseCategory, List<Exercise>>()
        sectionGroups.forEach { (category, exercises) ->
            if (remaining == 0) return@forEach

            val visibleExercises = exercises.take(remaining)
            if (visibleExercises.isNotEmpty()) {
                visibleSections[category] = visibleExercises
                remaining -= visibleExercises.size
            }
        }

        return visibleSections
    }

    fun builtInSections(limit: Int = Int.MAX_VALUE): Map<ExerciseCategory, List<Exercise>> {
        return visibleBuiltInSections(builtInSectionGroups(), limit)
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
