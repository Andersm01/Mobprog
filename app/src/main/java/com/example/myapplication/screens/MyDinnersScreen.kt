
package com.example.myapplication.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyDinnersScreen(navController: NavHostController) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "my_dinners") {
                    composable("my_dinners") {
                        MyDinnersScreenContent(navController)  // Pass the navController
                    }
                }
            }
        }
    }
}

@Composable
fun MyDinnersScreenContent(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var savedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var publishedRecipe by remember { mutableStateOf<Recipe?>(null) }

    // Fetch saved recipes
    LaunchedEffect(Unit) {
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("saved_recipes")
                .get()
                .addOnSuccessListener { documents ->
                    savedRecipes = documents.map { document ->
                        Recipe(
                            name = document.getString("name") ?: "",
                            ingredients = document.get("ingredients") as? List<String> ?: emptyList(),
                            description = document.get("description") as? List<String> ?: emptyList(),
                            time = document.getLong("time")?.toInt() ?: 0
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error getting saved recipes", e)
                }
        } else {
            Log.w("Auth", "User is not logged in.")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mine Middager", style = MaterialTheme.typography.headlineMedium)

        // Recipe selection
        LazyColumn {
            items(savedRecipes) { recipe ->
                RecipeItem(recipe) { selectedRecipe ->
                    publishedRecipe = selectedRecipe
                }
            }
        }

        publishedRecipe?.let { recipe ->
            Text("Publisert: ${recipe.name}", style = MaterialTheme.typography.bodyLarge)
        } ?: run {
            Text("Ingen oppskrift funnet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}



// Helper function to display each recipe in a selectable item
@Composable
fun RecipeItem(recipe: Recipe, onRecipeSelected: (Recipe) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRecipeSelected(recipe) }
            .padding(8.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(recipe.name, style = MaterialTheme.typography.bodyMedium)
        Text("${recipe.time} min", style = MaterialTheme.typography.bodyMedium)
    }
}