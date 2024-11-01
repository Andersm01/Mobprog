package com.example.myapplication.screens

data class Recipe(
    val name: String,
    val ingredients: List<String>,
    val description: List<String>,
    val time: Int
)
