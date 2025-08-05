package com.deckerpw.birthdays.api

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

class Database(val ip: String) {

    fun getBirthdays(): List<Birthday> {
        val response = getData("birthdays")
        val list = mutableListOf<Birthday>()
        val array = JSONArray(response)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val birthday = Birthday(
                id = item.getInt("id"),
                name = item.getString("name"),
                date = LocalDateTime.parse(item.getString("date"))
            )
            list.add(birthday)
        }
        return list
    }

    fun getBirthdayVideo(id: Int): String {
        val response = getData("birthdays/$id/video")
        val item = JSONObject(response)
        if (item.has("error")) {
            throw DatabaseException(item.getString("error"))
        }
        return item.getString("video")
    }

    fun getFullBirthday(id: Int, password: String): Birthday {
        val response = postData("birthdays/$id/admin", "{ \"password\": \"$password\" }")
        val item = JSONObject(response)
        if (item.has("error")) {
            throw DatabaseException(item.getString("error"))
        }
        return Birthday(
            id = item.getInt("id"),
            name = item.getString("name"),
            date = LocalDateTime.parse(item.getString("date")),
            video = item.getString("video")
        )
    }

    fun deleteBirthday(id: Int, password: String) {
        val response = postData("birthdays/$id/admin/delete", "{ \"password\": \"$password\" }")
        val item = JSONObject(response)
        if (item.has("error")) {
            throw DatabaseException(item.getString("error"))
        }
    }

    fun addBirthday(birthday: Birthday, password: String) {
        val data =
            "{ \"name\": \"${birthday.name}\", \"date\": \"${birthday.date}\", \"video\": \"${birthday.video ?: ""}\", \"password\": \"$password\" }"
        val response = postData("birthdays", data)
        val item = JSONObject(response)
        if (item.has("error")) {
            throw DatabaseException(item.getString("error"))
        }
    }

    fun changeBirthdayVideo(id: Int, video: String, password: String) {
        val response = postData(
            "birthdays/$id/admin/video",
            "{ \"video\": \"$video\", \"password\": \"$password\" }"
        )
        val item = JSONObject(response)
        if (item.has("error")) {
            throw DatabaseException(item.getString("error"))
        }
    }

    private fun getData(endpoint: String): String {
        val url = URL("$ip/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun postData(endpoint: String, data: String): String {
        val url = URL("$ip/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.outputStream.use { outputStream ->
            outputStream.write(data.toByteArray())
        }

        val text = connection.inputStream.bufferedReader().use { it.readText() }
        return text
    }
}

class DatabaseException(message: String) : Exception(message)

