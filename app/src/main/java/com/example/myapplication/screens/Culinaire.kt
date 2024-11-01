import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.screens.Recipe
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import com.google.firebase.firestore.FirebaseFirestore

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.MyDinnersScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.sp
import com.example.myapplication.screens.SettingsScreen


class Culinaire : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                // Legg til dette i setContent-blokken i Culinaire-aktiviteten
                NavHost(navController, startDestination = "culinaire") {
                    composable("culinaire") {
                        CulinaireScreen(navController)
                    }
                    composable("my_dinners") {
                        MyDinnersScreen(navController)
                    }
                    composable("settings") {  // Legg til ruten for innstillingsskjermen
                        SettingsScreen()
                    }
                }

            }
        }
    }
}

// Funksjon for å lese oppskrifter fra assets
fun loadRecipes(context: Context): List<Recipe> {
    val inputStream = context.assets.open("recipes.txt")
    val reader = BufferedReader(inputStream.reader())
    val content = reader.use { it.readText() }

    val recipeListType = object : TypeToken<List<Recipe>>() {}.type
    return Gson().fromJson(content, recipeListType)
}

// Funksjon for å finne oppskrift basert på brukerens valg
// Funksjon for å finne oppskrift basert på brukerens valg, med margin på tid
fun findMatchingRecipe(
    recipes: List<Recipe>,
    selectedIngredients: List<String>,
    maxTime: Int,
    timeMargin: Int = 5 // standard margin på 5 minutter
): Recipe? {
    return recipes.firstOrNull { recipe ->
        recipe.ingredients.containsAll(selectedIngredients) &&
                recipe.time in (maxTime - timeMargin)..(maxTime + timeMargin)
    }
}


@Composable
fun CulinaireScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val firestore = FirebaseFirestore.getInstance()
    var selectedProtein by remember { mutableStateOf<String?>(null) }
    var selectedCarbohydrate by remember { mutableStateOf<String?>(null) }
    var selectedVegetable by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf(0f) }

    val proteins = listOf("Kjøttdeig", "Kylling", "Fisk", "Tofu", "Egg")
    val carbohydrates = listOf("Ris", "Pasta", "Poteter", "Brød", "Quinoa")
    val vegetables = listOf("Gulrot", "Brokoli", "Paprika", "Løk", "Spinach", "Tomat")

    val context = LocalContext.current
    var matchingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // State for the active icon
    val activeIcon = remember { mutableStateOf("menu") } // "menu" indicates the menu icon is active

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content in Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Reserve space for the fixed bottom box
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Velg ingredienser og tid",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Slider
            Text(
                text = "Tid: ${selectedTime.toInt()} min",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                valueRange = 0f..30f,
                steps = 29,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Ingredient selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IngredientBox(
                    title = "Protein",
                    selectedIngredient = selectedProtein,
                    onClick = { selectedProtein = it },
                    onRemove = { selectedProtein = null },
                    ingredientList = proteins,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IngredientBox(
                    title = "Karbohydrat",
                    selectedIngredient = selectedCarbohydrate,
                    onClick = { selectedCarbohydrate = it },
                    onRemove = { selectedCarbohydrate = null },
                    ingredientList = carbohydrates,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IngredientBox(
                    title = "Grønnsak",
                    selectedIngredient = selectedVegetable,
                    onClick = { selectedVegetable = it },
                    onRemove = { selectedVegetable = null },
                    ingredientList = vegetables,
                    modifier = Modifier.weight(1f)
                )
            }

            // Display selected ingredients
            matchingRecipe?.let { recipe ->
                Text(
                    "${recipe.name} ${recipe.time} min",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text("Ingredienser:", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    recipe.ingredients.forEachIndexed { index, ingredient ->
                        Text(text = "- $ingredient", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp))
                        if (index < recipe.ingredients.size - 1) {
                            Text(text = ", ", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fremgangsmåte:", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp))
                recipe.description.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } ?: Text(
                "Ingen oppskrift funnet.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        val selectedIngredients = listOfNotNull(selectedProtein, selectedCarbohydrate, selectedVegetable)
                        val recipes = loadRecipes(context)
                        matchingRecipe = findMatchingRecipe(recipes, selectedIngredients, selectedTime.toInt())
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Finn oppskrift", fontSize = 18.sp)
                }

                matchingRecipe?.let { recipe ->
                    Button(
                        onClick = {
                            saveRecipeToFirestore(firestore, recipe)
                            showSnackbar = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(3000)
                                showSnackbar = false
                            }
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("Lagre oppskrift", fontSize = 18.sp)
                    }
                }
            }
        }

        // Fixed bottom box with icon buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.secondary)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Icon with highlight
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = if (activeIcon.value == "menu") Color.White else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            activeIcon.value = "menu" // Set active icon
                            // Add navigation action if needed
                        }
                )
                // Profile Icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (activeIcon.value == "profile") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            activeIcon.value = "profile" // Set active icon
                            // Add navigation action if needed
                        }
                )
                // Settings Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            activeIcon.value = "settings" // Set active icon
                            navController.navigate("settings") // Naviger til SettingsScreen
                        }
                )

            }
        }

        // Drop-down Snackbar at the top
        AnimatedVisibility(
            visible = showSnackbar,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("Oppskrift lagret!", fontSize = 18.sp)
            }
        }
    }
}




// Funksjon for å lagre oppskrift til Firestore
private fun saveRecipeToFirestore(firestore: FirebaseFirestore, recipe: Recipe) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
        val recipeData = hashMapOf(
            "name" to recipe.name,
            "ingredients" to recipe.ingredients,
            "description" to recipe.description,
            "time" to recipe.time
        )

        firestore.collection("users").document(userId)
            .collection("saved_recipes").add(recipeData)
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe saved successfully.")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving recipe", e)
            }
    } else {
        Log.w("Firestore", "User not logged in.")
    }




}




@Composable
fun IngredientBox(
    title: String,
    selectedIngredient: String?,
    onClick: (String) -> Unit,
    onRemove: () -> Unit,
    ingredientList: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(8.dp)
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = selectedIngredient ?: "Velg ingrediens")
            if (selectedIngredient != null) {
                Text(text = "X", color = Color.Red, modifier = Modifier.clickable { onRemove() })
            }
        }

        fun loadRecipes(context: Context): List<Recipe> {
            return try {
                val inputStream = context.assets.open("recipes.txt")
                val reader = BufferedReader(inputStream.reader())
                val content = reader.use { it.readText() }

                val recipeListType = object : TypeToken<List<Recipe>>() {}.type
                Gson().fromJson(content, recipeListType)
            } catch (e: Exception) {
                Log.e("Recipe Loading Error", "Error reading or parsing recipes: ${e.message}")
                emptyList() // Returnerer en tom liste om noe går galt
            }
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ingredientList.forEach { ingredient ->
                DropdownMenuItem(
                    text = { Text(ingredient) },
                    onClick = {
                        onClick(ingredient)
                        expanded = false
                    }
                )
            }
        }
    }
}