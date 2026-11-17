package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.FleetRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.MainAppScaffold
import com.example.ui.i18n.LanguagePrefs
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.LocalOnLanguageSelected
import com.example.ui.i18n.stringsFor
import com.example.ui.theme.KrishnaTravelsTheme
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.authStateFlow
import com.example.util.signOut
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var currentLanguage by remember { mutableStateOf(LanguagePrefs.load(context)) }
            val appStrings = remember(currentLanguage) { stringsFor(currentLanguage) }

            CompositionLocalProvider(
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides appStrings,
                LocalOnLanguageSelected provides { lang ->
                    currentLanguage = lang
                    LanguagePrefs.save(context, lang)
                }
            ) {
                KrishnaTravelsTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val scope = rememberCoroutineScope()
                        val credentialManager = remember { CredentialManager.create(context) }
                        val currentUser by remember { Firebase.auth.authStateFlow() }
                            .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                        val user = currentUser
                        if (user == null) {
                            AuthScreen(onAuthSuccess = {})
                        } else {
                            key(user.uid) {
                                val app = context.applicationContext
                                val factory = remember(user.uid) {
                                    viewModelFactory {
                                        initializer {
                                            val db = Firebase.firestore(app.getString(R.string.firestore_database_id))
                                            val repo = FleetRepository(db, Firebase.auth)
                                            FleetViewModel(
                                                repository = repo,
                                                currentUserId = user.uid,
                                                displayName = user.displayName ?: user.email ?: "Krishna Partner",
                                                email = user.email.orEmpty()
                                            )
                                        }
                                    }
                                }
                                val vm: FleetViewModel = viewModel(factory = factory)
                                MainAppScaffold(
                                    viewModel = vm,
                                    onSignOut = {
                                        signOut(credentialManager = credentialManager, scope = scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
