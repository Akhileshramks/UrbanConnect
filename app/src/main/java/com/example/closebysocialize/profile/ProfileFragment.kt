package com.example.closebysocialize.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.closebysocialize.R
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.closebysocialize.ReportBugDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileFragment : Fragment() {
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var birthYearTextView: TextView
    private lateinit var reportBugs: TextView
    private lateinit var language: TextView
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var aboutMeTextView: TextView
    private var currentProfileImageUrl: String? = null
    private var id: String? = null
    private val shouldShowCurrentUserProfile: Boolean
        get() = id == null

    private val languageOptions = arrayOf("Tamil", "English")

    companion object {
        const val ARG_ID = "id"
        fun newInstance(id: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle().apply {
                putString(ARG_ID, id)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(ARG_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        reportBugs = view.findViewById(R.id.reportBugTextView)
        language = view.findViewById(R.id.languageTextView)
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)
        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.nameTextView)
        aboutMeTextView = view.findViewById(R.id.aboutMeTextView)
        birthYearTextView = view.findViewById(R.id.birthYearTextView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchUserInfo()
        showSelectedInterests()


        reportBugs.setOnClickListener {
            val dialogFragment = ReportBugDialogFragment()
            dialogFragment.show(parentFragmentManager, "ReportBugDialogFragment")
        }
        language.setOnClickListener {
            showLanguagePicker()
        }
        profileImageView.setOnClickListener {

        }

        val currentNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        darkModeSwitch.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }


    private fun fetchUserInfo() {
        val idToUse = id
            ?: if (shouldShowCurrentUserProfile) FirebaseAuth.getInstance().currentUser?.uid else null
        if (idToUse != null) {
            val userRef =
                FirebaseFirestore.getInstance().collection("users").document(idToUse)
            userRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userName = documentSnapshot.getString("name")
                    val profileImageUrl = documentSnapshot.getString("profileImageUrl")
                    val aboutMe = documentSnapshot.getString("aboutMe")
                    val birthYear = documentSnapshot.getLong("birthYear")?.toInt()
                    currentProfileImageUrl = profileImageUrl

                    updateProfileUI(aboutMe, userName, profileImageUrl, birthYear)
                } else {
                    Log.d("ProfileFragment", "User ID is null, cannot fetch user info")
                }
            }.addOnFailureListener { exception ->
                Log.e("ProfileFragment", "Failed to fetch user info", exception)
            }
        } else {
            Log.d("ProfileFragment", "User ID is null")
        }
    }


    private fun showSelectedInterests() {
        val userId = id ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val selectedInterestsRaw = documentSnapshot.get("selectedInterests")
                val selectedInterests = if (selectedInterestsRaw is List<*>) {
                    selectedInterestsRaw.filterIsInstance<String>()
                } else {
                    null
                }
                if (selectedInterests != null) {
                    updateInterestsUI(selectedInterests)
                }
            }
        }.addOnFailureListener {
            Log.e("showSelectedInterests", "Error fetching user data", it)
        }
    }


    private fun updateInterestsUI(interests: List<String>?) {
        val gridLayout = view?.findViewById<GridLayout>(R.id.profileGridLayout)
        gridLayout?.removeAllViews()

        interests?.mapNotNull { interestId ->
            val drawableId = interestId.toIntOrNull()
            drawableId?.let { createInterestImageView(it) }
        }?.forEach { imageView ->
            gridLayout?.addView(imageView)
        }
    }

    private fun createInterestImageView(drawableId: Int): ImageView {
        val imageView = ImageView(context)
        imageView.setImageResource(drawableId)
        val layoutParams = GridLayout.LayoutParams()

        layoutParams.width = 0
        layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT

        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        val marginInPixels = convertDpToPixel(context)
        layoutParams.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels)

        imageView.layoutParams = layoutParams

        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

        return imageView
    }

    private fun convertDpToPixel(context: Context?): Int {
        val dp = 4f
        return if (context != null) {
            val metrics = context.resources.displayMetrics
            (dp * metrics.density).toInt()
        } else {
            0
        }
    }


    private fun updateProfileUI(
        aboutMe: String?,
        userName: String?,
        profileImageUrl: String?,
        birthYear: Int?
    ) {
        nameTextView.text = userName ?: "No name available"
        aboutMeTextView.text = aboutMe ?: "No info available"
        birthYearTextView.text = birthYear?.toString() ?: "No birth year available"

        if (!profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .error(R.drawable.avatar_dark)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ProfileFragment", "Failed to load image", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.avatar_dark)
        }

        profileImageView.setOnClickListener {
            Log.d("ProfileFragment", "Profile image clicked.")
            currentProfileImageUrl?.let { imageUrl ->
                Log.d("ProfileFragment", "Current image URL: $imageUrl")
                val dialogFragment = EnlargeProfilePicFragment.newInstance(imageUrl)
                dialogFragment.show(parentFragmentManager, "imageDialog")
            } ?: run {
                Log.d("ProfileFragment", "No image URL found.")
            }
        }
    }

    private fun showLanguagePicker() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Language")
            .setItems(languageOptions) { dialog, which ->
                val selectedLanguage = languageOptions[which]
                Toast.makeText(
                    requireContext(),
                    "Selected Language: $selectedLanguage",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }
}