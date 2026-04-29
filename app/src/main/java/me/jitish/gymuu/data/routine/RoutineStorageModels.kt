package me.jitish.gymuu.data.routine

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.time.Instant

private const val STORAGE_VERSION = 1

@Keep
internal data class PersistedAppState(
    @SerializedName("version") val version: Int,
    @SerializedName("exportedAt") val exportedAt: String,
    @SerializedName("routines") val routines: List<StoredRoutine?>? = null,
    @SerializedName("customExercises") val customExercises: List<StoredCustomExercise?>? = null
) {
    companion object {
        fun fromDomain(routines: List<Routine>, customExercises: List<CustomExercise>): PersistedAppState {
            return PersistedAppState(
                version = STORAGE_VERSION,
                exportedAt = Instant.now().toString(),
                routines = routines.map(StoredRoutine::fromDomain),
                customExercises = customExercises.map(StoredCustomExercise::fromDomain)
            )
        }
    }
}

@Keep
internal data class StoredRoutine(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("days") val days: List<StoredWorkoutDay?>? = null
) {
    companion object {
        fun fromDomain(routine: Routine): StoredRoutine {
            return StoredRoutine(
                id = routine.id,
                name = routine.name,
                days = routine.days.map(StoredWorkoutDay::fromDomain)
            )
        }
    }
}

@Keep
internal data class StoredWorkoutDay(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("exercises") val exercises: List<StoredRoutineExercise?>? = null
) {
    companion object {
        fun fromDomain(day: WorkoutDay): StoredWorkoutDay {
            return StoredWorkoutDay(
                id = day.id,
                name = day.name,
                exercises = day.exercises.map(StoredRoutineExercise::fromDomain)
            )
        }
    }
}

@Keep
internal data class StoredRoutineExercise(
    @SerializedName("id") val id: String? = null,
    @SerializedName("exerciseId") val exerciseId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("gifUrl") val gifUrl: String? = null,
    @SerializedName("sets") val sets: List<StoredWorkoutSet?>? = null,
    @SerializedName("rest") val rest: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("source") val source: String? = null
) {
    companion object {
        fun fromDomain(exercise: RoutineExercise): StoredRoutineExercise {
            return StoredRoutineExercise(
                id = exercise.id,
                exerciseId = exercise.exerciseId,
                name = exercise.name,
                gifUrl = exercise.gifUrl,
                sets = exercise.sets.map(StoredWorkoutSet::fromDomain),
                rest = exercise.rest,
                notes = exercise.notes,
                source = exercise.source.name
            )
        }
    }
}

@Keep
internal data class StoredWorkoutSet(
    @SerializedName("id") val id: String? = null,
    @SerializedName("setNo") val setNo: Int? = null,
    @SerializedName("reps") val reps: String? = null,
    @SerializedName("weight") val weight: String? = null,
    @SerializedName("completed") val completed: Boolean? = null
) {
    companion object {
        fun fromDomain(set: WorkoutSet): StoredWorkoutSet {
            return StoredWorkoutSet(
                id = set.id,
                setNo = set.setNo,
                reps = set.reps,
                weight = set.weight,
                completed = set.completed
            )
        }
    }
}

@Keep
internal data class StoredCustomExercise(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("sets") val sets: Int? = null,
    @SerializedName("reps") val reps: String? = null,
    @SerializedName("rest") val rest: String? = null
) {
    companion object {
        fun fromDomain(exercise: CustomExercise): StoredCustomExercise {
            return StoredCustomExercise(
                id = exercise.id,
                name = exercise.name,
                sets = exercise.sets,
                reps = exercise.reps,
                rest = exercise.rest
            )
        }
    }
}
