package com.example.lojamamager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class MeuPerfilActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var storage: FirebaseStorage

    private lateinit var imageViewProfilePicture: ImageView
    //private lateinit var buttonChangeProfilePicture: Button
    private lateinit var textViewUserName: TextView
    private lateinit var textViewUserEmail: TextView
    private lateinit var textViewUserRole: TextView

    private lateinit var spinnerThemeSelection: Spinner
    private lateinit var buttonApplyTheme: Button

    private var currentUserProfile: UserProfile? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                uploadProfilePicture(imageUri)
            } else {
                Toast.makeText(this, "Não foi possível carregar a imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meu_perfil)

        auth = FirebaseAuth.getInstance()
        userProfileRepository = UserProfileRepository()
        storage = FirebaseStorage.getInstance()

        imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture)
        //buttonChangeProfilePicture = findViewById(R.id.buttonChangeProfilePicture)
        textViewUserName = findViewById(R.id.textViewUserName)
        textViewUserEmail = findViewById(R.id.textViewUserEmail)
        textViewUserRole = findViewById(R.id.textViewUserRole)

        spinnerThemeSelection = findViewById(R.id.spinnerThemeSelection)
        buttonApplyTheme = findViewById(R.id.buttonApplyTheme)

        /*buttonChangeProfilePicture.setOnClickListener {
            openImageChooser()
        }*/

        setupThemeSpinner()
        buttonApplyTheme.setOnClickListener {
            applySelectedTheme()
        }

        loadUserProfile()
    }

    private fun setupThemeSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.theme_names_array,
            R.layout.spinner_item_layout
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerThemeSelection.adapter = adapter
        }

        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString(PREF_THEME_KEY, THEME_DEFAULT)
        val themeNames = resources.getStringArray(R.array.theme_names_array)

        // Configura qual item aparece pré-selecionado ao abrir o perfil
        val selectedIndex = when (currentTheme) {
            THEME_SERIOUS -> themeNames.indexOf("Sério")
            THEME_VIBRANT -> themeNames.indexOf("Vibrante")
            THEME_PASTEL -> themeNames.indexOf("Pastel") // *** ADICIONADO ***
            THEME_DEFAULT -> themeNames.indexOf("Padrão")
            else -> themeNames.indexOf("Padrão")
        }
        if (selectedIndex != -1) {
            spinnerThemeSelection.setSelection(selectedIndex)
        }
    }

    private fun applySelectedTheme() {
        val selectedThemeName = spinnerThemeSelection.selectedItem.toString()
        val themeToSave = when (selectedThemeName) {
            "Sério" -> THEME_SERIOUS
            "Vibrante" -> THEME_VIBRANT
            "Pastel" -> THEME_PASTEL // *** ADICIONADO ***
            "Padrão" -> THEME_DEFAULT
            else -> THEME_DEFAULT
        }

        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit { putString(PREF_THEME_KEY, themeToSave) }

        Toast.makeText(this, "Tema aplicado! Reiniciando aplicativo...", Toast.LENGTH_SHORT).show()

        val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }


    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val profile = userProfileRepository.getUserProfile(currentUser.uid)
                withContext(Dispatchers.Main) {
                    currentUserProfile = profile
                    updateUIWithProfile(profile)
                }
            }
        } else {
            navigateToLogin()
        }
    }

    private fun updateUIWithProfile(profile: UserProfile?) {
        if (profile != null) {
            textViewUserName.text = "Nome: ${profile.nome ?: "Não informado"}"
            textViewUserEmail.text = "E-mail: ${profile.email ?: "Não informado"}"
            textViewUserRole.text = "Cargo: ${profile.role?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Não informado"}"

            profile.profilePictureUrl?.let { url ->
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imageViewProfilePicture)
            } ?: run {
                imageViewProfilePicture.setImageResource(R.drawable.ic_person)
            }
        } else {
            Toast.makeText(this, "Perfil do usuário não encontrado.", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        Toast.makeText(this, "Fazendo upload da foto...", Toast.LENGTH_LONG).show()
        val storageRef = storage.reference.child("profile_pictures/${currentUser.uid}.jpg")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                currentUserProfile?.let { profile ->
                    val updatedProfile = profile.copy(profilePictureUrl = downloadUrl)
                    val success = userProfileRepository.updateUserProfile(updatedProfile)

                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(this@MeuPerfilActivity, "Foto de perfil atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                            updateUIWithProfile(updatedProfile)
                        } else {
                            Toast.makeText(this@MeuPerfilActivity, "Erro ao salvar URL da foto no perfil.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MeuPerfilActivity, "Falha no upload da foto: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}