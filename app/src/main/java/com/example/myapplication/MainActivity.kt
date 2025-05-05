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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.model.FavoriteRecipe
import com.example.myapplication.viewModel.FavoritesViewModel
import com.example.myapplication.database.RecipeRepository
import com.example.myapplication.viewModel.FavoritesViewModelFactory
import com.example.myapplication.database.PreferencesRepository
import com.example.myapplication.viewModel.PreferencesViewModel
import com.example.myapplication.viewModel.PreferencesViewModelFactory
import com.example.myapplication.viewModel.SearchViewModel


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
        typography = androidx.compose.material3.Typography(),
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
    preferencesViewModelFactory: PreferencesViewModelFactory) {

    //ViewModel je klasa koja živi izvan composable funkcije
    //preživljava promjene UI-a (npr. rotaciju zaslona)
    //čuva podatke (state) koji su relevantni za neki ekran ili funkcionalnost
    val preferencesViewModel: PreferencesViewModel = viewModel(factory = preferencesViewModelFactory)
    val preferences by preferencesViewModel.preferences.collectAsState()

    // centralizirano upravljanje stanjem (state lifting)
    //remember kaže Compose-u da zapamti vrijednost između recomposition-a (kad se UI ponovo crta).
    var showIngredients by remember { mutableStateOf(true) }
    var showSearchOptions by remember { mutableStateOf(true) }
    var wasSearchPerformed by remember { mutableStateOf(false) }


    // automatsko učitavanje preferenci iz room-a (baza podataka)
    var diet = preferences?.diet ?: ""
    var intolerances = preferences?.intolerances?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    var cuisine = preferences?.cuisine ?: ""


    // deklaracija navigacijske rute za aplikaciju
    NavHost(navController, startDestination = "search") {
        composable("search") {
            SearchScreen(
                navController = navController,

                showIngredients = showIngredients,
                onShowIngredientsChange = { showIngredients = it },

                showSearchOptions = showSearchOptions,
                onShowSearchOptionsChange = { showSearchOptions = it },
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
                preferencesViewModelFactory = preferencesViewModelFactory,
                onSave = { selectedDiet, selectedIntolerances, selectedCuisine ->

                    diet = selectedDiet
                    intolerances = selectedIntolerances
                    cuisine = selectedCuisine
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(navController: NavHostController, showSearch: Boolean) {

    //Kreira navigacijski bar
    TopAppBar(
        //Definicija naslove aplikacije (prikazuje se lijevo)
        title = {
            Text(
                "Meal Planner",
            )
        },
        //Definicija navigacijskih ikona (prikazuju se desno)
        actions = {
            if (showSearch) {
                IconButton(onClick = {
                    navController.navigate("search")
                }) {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    showIngredients: Boolean,
    onShowIngredientsChange: (Boolean) -> Unit,
    showSearchOptions: Boolean,
    onShowSearchOptionsChange: (Boolean) -> Unit,
    wasSearchPerformed: Boolean,
    onSearchPerformed: (Boolean) -> Unit,
    diet: String,
    intolerances: List<String>,
    cuisine: String
) {
    val coroutineScope = rememberCoroutineScope()
    val searchViewModel: SearchViewModel = viewModel()
    var ingredientsList by remember { searchViewModel.ingredientsList }
    val keyboardController = LocalSoftwareKeyboardController.current
    var maxReadyTime by remember { mutableStateOf(30f) }
    val sortOptions = listOf("popularity", "healthiness", "time", "calories")
    var selectedSort by remember { mutableStateOf("popularity") }
    var selectedDirection by remember { mutableStateOf("desc") }

    val recipes by searchViewModel.recipes
    val isLoading = searchViewModel.isLoading.value

    fun fetchRecipes() {
        searchViewModel.fetchRecipes(
            ingredients = ingredientsList,
            maxReadyTime = maxReadyTime.toInt(),
            diet = diet.ifBlank { null },
            intolerances = if (intolerances.isEmpty()) null else intolerances.joinToString(","),
            cuisine = cuisine.ifBlank { null },
            sort = selectedSort,
            sortDirection = selectedDirection
        )
    }

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
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -25) onShowSearchOptionsChange(false)
                            }
                        }
                ) {
                    // lokalna lista prijedloga
                    val allIngredients = listOf(
                        "apple", "apricot", "avocado", "bacon", "balsamic vinegar", "banana", "barley", "basil", "bay leaf",
                        "beef", "beef steak", "beef mince", "beetroot", "bell pepper", "black bean", "blackberry", "blueberry",
                        "bok choy", "bread", "broccoli", "brown sugar", "butter", "buttermilk", "cabbage", "cantaloupe", "carrot",
                        "cauliflower", "celery", "cheddar cheese", "cherries", "chicken", "chicken breast", "chicken thighs", "chicken wings",
                        "chickpeas", "chili", "chocolate", "cinnamon", "cloves", "coconut", "coriander", "curry powder", "dates", "duck",
                        "egg", "eggplant", "fennel", "feta cheese", "flour", "garlic", "ginger", "goat cheese", "green beans", "green onion",
                        "honey", "jalapeno", "kale", "lamb", "lemon", "lettuce", "lime", "mango", "maple syrup", "mayonnaise", "melon",
                        "milk", "mushroom", "mustard", "nutmeg", "oregano", "paprika", "parsley", "pepper", "pork", "pork chops",
                        "pork belly", "prosciutto", "peas", "pineapple", "plum", "potato", "pumpkin", "quinoa", "radish", "raspberry",
                        "rice", "rosemary", "sage", "salt", "saffron", "sesame seeds", "sour cream", "soy sauce", "spinach", "squash",
                        "steak", "striploin", "t-bone", "ribeye", "strawberry", "sugar", "sweet potato", "tahini", "tangerine", "thyme",
                        "tomato", "tofu", "turkey", "turmeric", "tuna", "veal", "venison", "vanilla", "vinegar", "walnut", "watermelon",
                        "yogurt", "zucchini"
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var input by remember { mutableStateOf("") }

                        val suggestions = remember(input) {
                            allIngredients.filter {
                                it.startsWith(input, ignoreCase = true) && input.isNotBlank()
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                placeholder = { Text("Dodaj namirnicu") },
                                shape = MaterialTheme.shapes.large
                            )
                            Button(
                                onClick = {
                                    if (input.isNotBlank()) {
                                        searchViewModel.addIngredient(input)
                                        onShowIngredientsChange(true)
                                        input = ""
                                    }
                                },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Dodaj")
                            }
                        }

                        suggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.addIngredient(suggestion)
                                        onShowIngredientsChange(true)
                                        input = ""
                                    }
                                    .padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Maksimalno vrijeme pripreme: ${maxReadyTime.toInt()} min")
                        Slider(
                            value = maxReadyTime,
                            onValueChange = { newValue ->
                                // Zaokruživanje na najbliži broj koji je djeljiv s 5
                                maxReadyTime = (newValue / 5).toInt() * 5f
                            },
                            valueRange = 5f..120f,
                            steps = 23,  // 23 koraka između 5 i 120
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Button(
                        onClick = {
                            if (ingredientsList.isNotEmpty()) {
                                fetchRecipes()
                            }
                            onShowIngredientsChange(false)
                            keyboardController?.hide()
                            onShowSearchOptionsChange(false)
                            onSearchPerformed(true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Pronađi recepte")
                    }

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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShowSearchOptionsChange(true) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Uredi pretragu")
                    }

                    OutlinedButton(
                        onClick = {
                            onShowIngredientsChange(true)
                            searchViewModel.ingredientsList.value = emptyList()
                            onSearchPerformed(false)
                            onShowSearchOptionsChange(true)
                            searchViewModel.resetSearch()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Očisti pretragu")
                    }
                }
            }

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

            if (ingredientsList.isEmpty() && !wasSearchPerformed) {
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

            if (recipes.isNotEmpty()) {
                Divider()
                Text("Sortiraj po:")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownMenuBox(
                            options = sortOptions,
                            selected = selectedSort,
                            onSelectedChange = {
                                selectedSort = it
                                fetchRecipes()
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            selectedDirection = if (selectedDirection == "asc") "desc" else "asc"
                            fetchRecipes()
                        },
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedDirection == "asc") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Promijeni smjer",
                            tint = Color.Black
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
                                Text(text = recipe.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "⏱ ${recipe.readyInMinutes} min | ❌ ${recipe.missedIngredientCount} sastojaka fali | 🔥 $calories kcal",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                if (wasSearchPerformed && recipes.isNotEmpty()) {
                item {  Button(
                    onClick = {
                        coroutineScope.launch {
                            searchViewModel.loadMoreRecipes(
                                maxReadyTime = maxReadyTime.toInt(),
                                diet = diet,
                                intolerances = intolerances.joinToString(","),
                                cuisine = cuisine,
                                sort = selectedSort,
                                sortDirection = selectedDirection
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Prikaži više")
                    }
                }  }
            }}

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

    suspend fun getRecipeDetails(recipeId: Int): RecipeDetails? {
        return try {
            RetrofitInstance.api.getRecipeDetails(id = recipeId)
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(recipeId) {
        coroutineScope.launch {
            recipeDetails = getRecipeDetails(recipeId)
            isFavorite = viewModel.isFavorite(recipeId.toString())
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
            selectedIntolerances = it.intolerances
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
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
                    val cleanIntolerances = selectedIntolerances
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(",")


                    onSave(cleanDiet, cleanIntolerances.split(","), selectedCuisine)
                    viewModel.save(cleanDiet, cleanIntolerances, selectedCuisine)

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


