package me.jitish.gymuu.data.routine

internal fun defaultSets(count: Int, reps: String, newId: () -> String): List<WorkoutSet> {
    return List(count.coerceIn(1, 10)) { index ->
        WorkoutSet(id = newId(), setNo = index + 1, reps = reps.ifBlank { "10" })
    }
}

internal fun normalizeRest(rest: String): String {
    val clean = rest.trim()
    if (clean.isBlank()) return "1:30"
    return if (clean.contains(":")) clean else "$clean:00"
}

internal fun Any?.safeString(): String = this as? String ?: ""

