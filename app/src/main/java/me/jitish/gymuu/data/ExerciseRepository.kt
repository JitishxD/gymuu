package me.jitish.gymuu.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ExerciseRepository(private val context: Context) {
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    suspend fun loadExercises() = withContext(Dispatchers.IO) {
        if (_exercises.value.isNotEmpty()) return@withContext

        // Exercise JSON is loaded from app assets and parsed once at startup.
        val json = context.assets.open(EXERCISE_ASSET).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val parsed = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Exercise(
                        exerciseId = item.optString("exerciseId"),
                        name = item.optString("name"),
                        gifUrl = item.optString("gifUrl"),
                        bodyParts = item.optJSONArray("bodyParts").toStringList(),
                        equipments = item.optJSONArray("equipments").toStringList(),
                        targetMuscles = item.optJSONArray("targetMuscles").toStringList(),
                        secondaryMuscles = item.optJSONArray("secondaryMuscles").toStringList(),
                        instructions = item.optJSONArray("instructions").toStringList()
                    )
                )
            }
        }.sortedBy { it.name }

        _exercises.value = parsed
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    companion object {
        private const val EXERCISE_ASSET = "exercises-paginated-indexed.json"
    }
}
