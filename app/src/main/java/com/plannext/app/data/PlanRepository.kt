package com.plannext.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PlanRepository private constructor(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _plans = MutableStateFlow(loadPlans())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    fun snapshot(): List<Plan> = _plans.value

    fun getPlan(id: Long): Plan? = snapshot().firstOrNull { it.id == id }

    @Synchronized
    fun upsert(plan: Plan) {
        val updated = snapshot()
            .filterNot { it.id == plan.id }
            .plus(plan)
            .sortedBy { it.startAtMillis }
        persist(updated)
    }

    @Synchronized
    fun setCompleted(id: Long, isCompleted: Boolean = true) {
        val updated = snapshot().map { plan ->
            if (plan.id == id) plan.copy(isCompleted = isCompleted) else plan
        }
        persist(updated)
    }

    @Synchronized
    fun delete(id: Long) {
        persist(snapshot().filterNot { it.id == id })
    }

    private fun persist(plans: List<Plan>) {
        preferences.edit().putString(PLANS_KEY, serialize(plans)).apply()
        _plans.value = plans
    }

    private fun loadPlans(): List<Plan> {
        val raw = preferences.getString(PLANS_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val plan = item.toPlanOrNull() ?: continue
                    add(plan)
                }
            }.sortedBy { it.startAtMillis }
        }.getOrDefault(emptyList())
    }

    private fun serialize(plans: List<Plan>): String {
        val array = JSONArray()
        plans.forEach { plan ->
            array.put(
                JSONObject()
                    .put("id", plan.id)
                    .put("title", plan.title)
                    .put("notes", plan.notes)
                    .put("startAtMillis", plan.startAtMillis)
                    .put("endAtMillis", plan.endAtMillis)
                    .put("leadMinutes", plan.leadMinutes)
                    .put("category", plan.category.name)
                    .put("isCompleted", plan.isCompleted),
            )
        }
        return array.toString()
    }

    private fun JSONObject.toPlanOrNull(): Plan? = runCatching {
        Plan(
            id = getLong("id"),
            title = getString("title"),
            notes = optString("notes", ""),
            startAtMillis = getLong("startAtMillis"),
            endAtMillis = getLong("endAtMillis"),
            leadMinutes = getInt("leadMinutes"),
            category = PlanCategory.fromStored(optString("category")),
            isCompleted = optBoolean("isCompleted", false),
        )
    }.getOrNull()

    companion object {
        private const val PREFERENCES_NAME = "plannext_plans"
        private const val PLANS_KEY = "plans_json"

        @Volatile
        private var instance: PlanRepository? = null

        fun getInstance(context: Context): PlanRepository =
            instance ?: synchronized(this) {
                instance ?: PlanRepository(context).also { instance = it }
            }
    }
}

