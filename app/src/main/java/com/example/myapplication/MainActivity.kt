package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import coil.compose.rememberImagePainter
import com.example.myapplication.network.RetrofitInstance
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.myapplication.model.RecipeDetails
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.model.AppDatabase
import com.example.myapplication.model.FavoriteRecipe
import com.example.myapplication.viewModel.FavoritesViewModel
import com.example.myapplication.model.RecipeRepository
import com.example.myapplication.viewModel.FavoritesViewModelFactory
import com.example.myapplication.model.PreferencesRepository
import com.example.myapplication.viewModel.PreferencesViewModel
import com.example.myapplication.viewModel.PreferencesViewModelFactory
import com.example.myapplication.model.RecipeComplex


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val repository = RecipeRepository(database.recipeDao())

        val preferencesRepository = PreferencesRepository(database.preferencesDao())
        val preferencesViewModel = PreferencesViewModel(preferencesRepository)

        val viewModelFactory = FavoritesViewModelFactory(repository)
        val preferencesFactory = PreferencesViewModelFactory(preferencesRepository)



        setContent {
            val navController = rememberNavController()
            MealPlannerApp(navController, viewModelFactory, preferencesFactory)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerApp(navController: NavHostController, viewModelFactory: FavoritesViewModelFactory, preferencesViewModel: PreferencesViewModelFactory) {
    // Definirajte boje za svijetlu temu
    val colors = lightColorScheme(
        primary = Color(0xFF81C784), // Pastelno zelena
        secondary = Color(0xFFFFB74D), // Pastelno narančasta
        surface = Color(0xFFFFFFFF), // Bijela pozadina
        onSurface = Color(0xFF333333), // Tamno sivi tekst
        background = Color(0xFFF5F5F5), // Svijetlo siva pozadina
        onPrimary = Color(0xFFFFFFFF), // Bijeli tekst na primarnoj boji
        onSecondary = Color(0xFFFFFFFF) // Bijeli tekst na sekundarnoj boji
    )

    // Primijenite MaterialTheme s definiranim bojama
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(), // Koristite Material3 Typography
        content = {
            // Glavni sadržaj aplikacije
            MealPlannerAppContent(navController, viewModelFactory, preferencesViewModel)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerAppContent(
    navController: NavHostController,
    viewModelFactory: FavoritesViewModelFactory,
    preferencesViewModelFactory: PreferencesViewModelFactory

) {
    val preferencesViewModel: PreferencesViewModel = viewModel(factory = preferencesViewModelFactory)
    val preferences by preferencesViewModel.preferences.collectAsState()
    val searchViewModel: SearchViewModel = viewModel()


    var recipes by remember { mutableStateOf<List<RecipeComplex>>(emptyList()) }
    var showIngredients by remember { mutableStateOf(true) }
    var showSearchOptions by remember { mutableStateOf(true) }
    var maxCalories by remember { mutableStateOf(800f) }
    var maxMissingIngredients by remember { mutableStateOf(3f) }
    var wasSearchPerformed by remember { mutableStateOf(false) }



    // automatsko učitavanje preferenci iz room-a
    var diet = preferences?.diet ?: ""
    var intolerances = preferences?.intolerances?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    var cuisine = preferences?.cuisine ?: ""


    NavHost(navController, startDestination = "search") {
        composable("search") {
            SearchScreen(
                navController = navController,
                recipes = recipes,
                showIngredients = showIngredients,
                onShowIngredientsChange = { showIngredients = it },
                onRecipesFetched = { recipes = it },
                showSearchOptions = showSearchOptions,
                onShowSearchOptionsChange = { showSearchOptions = it },
                maxCalories = maxCalories,
                onMaxCaloriesChange = { maxCalories = it },
                maxMissingIngredients = maxMissingIngredients,
                onMaxMissingIngredientsChange = { maxMissingIngredients = it },
                wasSearchPerformed = wasSearchPerformed,
                onSearchPerformed = { wasSearchPerformed = true },
                diet = diet,
                intolerances = intolerances,
                cuisine = cuisine

            )
        }
        composable("favorites") {
            FavoritesScreen(navController, viewModelFactory)
        }
        composable("recipe/{recipeId}?ingredients={ingredients}",
            arguments = listOf(
                navArgument("recipeId") { type = NavType.IntType },
                navArgument("ingredients") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getInt("recipeId") ?: 0
            val ingredientsParam = backStackEntry.arguments?.getString("ingredients") ?: ""
            val ingredientsList = ingredientsParam.split(",").filter { it.isNotBlank() }

            RecipeDetailScreen(
                navController = navController,
                recipeId = recipeId,
                viewModelFactory = viewModelFactory,
                ingredientsList = ingredientsList
            )
        }

        composable("preferences") {
            UserPreferencesScreen(
                navController = navController,
                preferencesViewModelFactory = preferencesViewModelFactory, // 👈 ovo dodaj
                onSave = { selectedDiet, selectedIntolerances, selectedCuisine ->
                    // spremanje preferenci
                    diet = selectedDiet
                    intolerances = selectedIntolerances
                    cuisine = selectedCuisine
                }
            )
        }
    }
}

class SearchViewModel : ViewModel() {
    // Ovdje pohranjujemo popis namirnica
    var ingredientsList = mutableStateOf(listOf<String>())
        private set

    fun addIngredient(ingredient: String) {
        ingredientsList.value = ingredientsList.value + ingredient
    }

    fun removeIngredient(index: Int) {
        ingredientsList.value = ingredientsList.value.toMutableList().apply { removeAt(index) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    recipes: List<RecipeComplex>,
    showIngredients: Boolean,

    onShowIngredientsChange: (Boolean) -> Unit,
    onRecipesFetched: (List<RecipeComplex>) -> Unit,
    showSearchOptions: Boolean,
    onShowSearchOptionsChange: (Boolean) -> Unit,

    maxCalories: Float,
    onMaxCaloriesChange: (Float) -> Unit,
    maxMissingIngredients: Float,
    onMaxMissingIngredientsChange: (Float) -> Unit,

    wasSearchPerformed: Boolean,
    onSearchPerformed: (Boolean) -> Unit,

    diet: String,
    intolerances: List<String>,
    cuisine: String

) {
    var ingredient by remember { mutableStateOf("") }
    val searchViewModel: SearchViewModel = viewModel()
    var ingredientsList by remember { searchViewModel.ingredientsList }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var maxReadyTime by remember { mutableStateOf(30f) }

    val sortOptions = listOf("popularity", "healthiness", "price", "time", "calories")
    var selectedSort by remember { mutableStateOf("popularity") }
    var selectedDirection by remember { mutableStateOf("desc") }

    Scaffold(topBar = { AppBar(navController, showSearch = false) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSearchOptions) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount < -25) { // swipe up
                                    onShowSearchOptionsChange(false)
                                }
                            }
                        }
                ) {
                    // unos + dodavanje
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ingredient,
                            onValueChange = { ingredient = it },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            placeholder = { Text("Dodaj namirnicu") },
                            shape = MaterialTheme.shapes.large
                        )
                        Button(
                            onClick = {
                                onShowIngredientsChange(true)
                                if (ingredient.isNotBlank()) {
                                    searchViewModel.addIngredient(ingredient)
                                    ingredient = ""
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Dodaj")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp)) // ← dodaj ovo
                    // slider za trajanje
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Maksimalno vrijeme pripreme: ${maxReadyTime.toInt()} min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = maxReadyTime,
                            onValueChange = { maxReadyTime = it },
                            valueRange = 5f..120f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                    }

                    // gumb za pretragu
                    Button(
                        onClick = {
                            if (ingredientsList.isNotEmpty()) {
                                coroutineScope.launch {
                                    val response = RetrofitInstance.api.searchRecipesComplex(
                                        ingredients = ingredientsList.joinToString(","),
                                        maxReadyTime = maxReadyTime.toInt(),
                                        diet = diet.ifBlank { null },
                                        intolerances = if (intolerances.isEmpty()) null else intolerances.joinToString(","),
                                        cuisine = cuisine.ifBlank { null } ,
                                                sort = selectedSort,
                                        sortDirection = selectedDirection
                                    )

                                    println("🔍 diet=$diet, intolerances=$intolerances, cuisine=$cuisine")
                                    println("🧾 ingredients=${ingredientsList.joinToString(",")}")
                                    onRecipesFetched(response.results)
                                }
                            }
                            onShowIngredientsChange(false)
                            keyboardController?.hide()
                            onShowSearchOptionsChange(false)
                            onSearchPerformed(true) // ← OVDJE!
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Pronađi recepte")
                    }

                    // gumb sakrij/prikaži
                    if (ingredientsList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onShowIngredientsChange(!showIngredients) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(if (showIngredients) "Sakrij popis namirnica" else "Prikaži popis namirnica")
                        }
                    }
                }
            }else {
                OutlinedButton(
                    onClick = { onShowSearchOptionsChange(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Uredi pretragu")
                }
            }

            // prikaz popisa namirnica
            if (showIngredients && ingredientsList.isNotEmpty()) {
                Divider()
                Text("Popis namirnica:")
                LazyColumn {
                    items(ingredientsList) { item ->
                        Card(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(MaterialTheme.shapes.large),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.replaceFirstChar { it.uppercase() })
                                IconButton(onClick = {
                                    searchViewModel.removeIngredient(ingredientsList.indexOf(item))
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Ukloni", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (ingredientsList.isEmpty()  && !wasSearchPerformed) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.food_man),
                        contentDescription = "Empty illustration",
                        modifier = Modifier
                            .size(300.dp)
                            .alpha(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // rezultati
            if (recipes.isNotEmpty()) {
                Divider()
                Text("Sortiraj po:")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuBox(
                        options = sortOptions,
                        selected = selectedSort,
                        onSelectedChange = { selectedSort = it }
                    )

                    IconButton(
                        onClick = {
                            selectedDirection= if (selectedDirection == "asc") "desc" else "asc"
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedDirection == "asc") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Promijeni smjer",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )

                    }
                }
                Text("Pronađeni recepti:")
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(recipes) { recipe ->

                    val calories = recipe.nutrition?.nutrients
                        ?.firstOrNull { it.name == "Calories" }
                        ?.amount?.toInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                val encodedIngredients = ingredientsList.joinToString(",").replace(" ", "%20")
                                navController.navigate("recipe/${recipe.id}?ingredients=$encodedIngredients")
                            },
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberImagePainter(recipe.image),
                                contentDescription = recipe.title,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipe.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "⏱ ${recipe.readyInMinutes} min | ❌ ${recipe.missedIngredientCount} sastojaka fali | \uD83D\uDD25 $calories kcal\",",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}







@Composable
fun FavoritesScreen(navController: NavHostController, viewModelFactory: FavoritesViewModelFactory) {
    val viewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { AppBar(navController, showSearch = true) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (favorites.isEmpty()) {
                Text(
                    text = "Još nema spremljenih favorita",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                Text(
                    text = "Spremljeni recepti:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites) { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("recipe/${recipe.id}")
                                },
                            elevation = CardDefaults.cardElevation(6.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberImagePainter(recipe.image),
                                    contentDescription = recipe.title,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = recipe.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RecipeDetailScreen(
    navController: NavHostController,
    recipeId: Int,
    viewModelFactory: FavoritesViewModelFactory,
    ingredientsList: List<String>
) {
    var recipeDetails by remember { mutableStateOf<RecipeDetails?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val viewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)

    LaunchedEffect(recipeId) {
        coroutineScope.launch {
            recipeDetails = getRecipeDetails(recipeId)
            isFavorite = viewModel.isFavorite(recipeId.toString())
            println("🧂 ingredientsList u detail screenu: $ingredientsList")
        }
    }

    Scaffold(
        topBar = { AppBar(navController, showSearch = true) },
    ) { paddingValues ->
        recipeDetails?.let { recipe ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Slika
                Image(
                    painter = rememberImagePainter(recipe.image),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                // Naslov
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Osnovne info
                val calories = recipe.nutrition?.nutrients
                    ?.firstOrNull { it.name == "Calories" }
                    ?.amount?.toInt()

                val caloriesText = calories?.let { "🔥 $it kcal" } ?: ""
                val readyTime = "⏱ ${recipe.readyInMinutes} min"


                Text(
                    text = listOf(readyTime, caloriesText)
                        .filter { it?.isNotBlank() == true }
                        .joinToString(" | "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // Upute
                Text(
                    text = "Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            text = HtmlCompat.fromHtml(recipe.instructions, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Sastojci
                if (!recipe.ingredients.isNullOrEmpty()) {
                    Text(
                        text = "Ingredients:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val sortedIngredients = recipe.ingredients.sortedWith(compareByDescending { ingredient ->
                        ingredientsList.any { userIng ->
                            ingredient.name.contains(userIng, ignoreCase = true)
                        }
                    })

                    sortedIngredients.forEach {
                        val isOwned = ingredientsList.any { ing ->
                            it.name.contains(ing, ignoreCase = true)
                        }
                        val backgroundColor = if (isOwned) Color(0xFFD0F0C0) else Color(0xFFFFF8DC)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(backgroundColor)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${it.amount} ${it.unit} • ${it.name.replaceFirstChar { c -> c.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Gumb za favorite
                Button(
                    onClick = {
                        isFavorite = !isFavorite
                        if (isFavorite) {
                            viewModel.addToFavorites(
                                FavoriteRecipe(recipeId.toString(), recipe.title, recipe.image)
                            )
                        } else {
                            viewModel.removeFromFavorites(recipeId.toString())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = if (isFavorite) "Remove from Favorites" else "Add to Favorites")
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}


suspend fun getRecipeDetails(recipeId: Int): RecipeDetails? {
    return try {
        RetrofitInstance.api.getRecipeDetails(id = recipeId)
    } catch (e: Exception) {
        null
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(navController: NavHostController, showSearch: Boolean) {
    TopAppBar(
        title = {
            Text(
                "Meal Planner",
            )
        },
        actions = {
            if (showSearch) {
                IconButton(onClick = { navController.navigate("search") }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Pretraga")
                }
            }
            IconButton(onClick = { navController.navigate("favorites") }) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favoriti")
            }
            IconButton(onClick = { navController.navigate("preferences") }) {
                Icon(Icons.Default.Settings, contentDescription = "Postavke")
            }
        },
        modifier = Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF66BB6A), Color(0xFF42A5F5))
            )
        ),
        colors = TopAppBarDefaults.smallTopAppBarColors(

        )
    )
}

@Composable
fun UserPreferencesScreen(
    navController: NavHostController,
    preferencesViewModelFactory: PreferencesViewModelFactory,
    onSave: (String, List<String>, String) -> Unit // diet, intolerances
) {
    var selectedDiet by remember { mutableStateOf("") }
    var selectedIntolerances by remember { mutableStateOf(setOf<String>()) }
    var selectedCuisine by remember { mutableStateOf("") }
    val viewModel: PreferencesViewModel = viewModel(factory = preferencesViewModelFactory)

    val preferences by viewModel.preferences.collectAsState()

    LaunchedEffect(preferences) {
        preferences?.let {
            selectedDiet = it.diet
            selectedCuisine = it.cuisine
            selectedIntolerances = it.intolerances.split(",").toSet()
        }
    }

    val dietOptions = listOf(
        "None",
        "Gluten Free",
        "Ketogenic",
        "Vegetarian",
        "Lacto-Vegetarian",
        "Ovo-Vegetarian",
        "Vegan",
        "Pescetarian",
        "Paleo",
        "Primal",
        "Low FODMAP",
        "Whole30"
    )
    val intoleranceOptions = listOf(
        "Dairy", "Egg", "Gluten", "Grain", "Peanut", "Seafood",
        "Sesame", "Shellfish", "Soy", "Sulfite", "Tree Nut", "Wheat"
    )
    val cuisineOptions = listOf(
        "African", "American", "British", "Cajun", "Caribbean", "Chinese", "Eastern European",
        "European", "French", "German", "Greek", "Indian", "Irish", "Italian", "Japanese",
        "Jewish", "Korean", "Latin American", "Mediterranean", "Mexican", "Middle Eastern",
        "Nordic", "Southern", "Spanish", "Thai", "Vietnamese"
    )

    Scaffold(
        topBar = { AppBar(navController, showSearch = true) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Odaberi dijetu:")
            DropdownMenuBox(
                options = dietOptions,
                selected = selectedDiet,
                onSelectedChange = { selectedDiet = it }
            )

            Text("Preferirana kuhinja:")
            DropdownMenuBox(
                options = listOf("None") + cuisineOptions,
                selected = selectedCuisine,
                onSelectedChange = { selectedCuisine = it }
            )

            Text("Intolerancije:")
            MultiSelectDropdown(
                title = "Odaberi intolerancije",
                options = intoleranceOptions,
                selectedItems = selectedIntolerances,
                onSelectionChange = { selectedIntolerances = it }
            )


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val cleanDiet = if (selectedDiet == "None") "" else selectedDiet.lowercase()
                    onSave(cleanDiet, selectedIntolerances.toList(), selectedCuisine)
                    viewModel.save(selectedDiet, selectedIntolerances.toList(), selectedCuisine)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spremi postavke")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Odaberi") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Odaberi")
            },
            modifier = Modifier
                .menuAnchor() // potrebno za pravilno pozicioniranje
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize() // automatsko skaliranje
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    title: String,
    options: List<String>,
    selectedItems: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedItems.isEmpty()) "" else selectedItems.joinToString(", "),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedItems.contains(option),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    },
                    onClick = {
                        val newSelection = selectedItems.toMutableSet()
                        if (newSelection.contains(option)) {
                            newSelection.remove(option)
                        } else {
                            newSelection.add(option)
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}


