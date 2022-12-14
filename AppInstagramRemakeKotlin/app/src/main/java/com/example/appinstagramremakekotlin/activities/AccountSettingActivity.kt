package com.example.appinstagramremakekotlin.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.appinstagramremakekotlin.R
import com.example.appinstagramremakekotlin.databinding.ActivityAccountSettingBinding
import com.example.appinstagramremakekotlin.databinding.ActivityAddPostsBinding
import com.example.appinstagramremakekotlin.fragments.HomeFragment
import com.example.appinstagramremakekotlin.fragments.ProfileFragment
import com.example.appinstagramremakekotlin.models.User
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingBinding
    private lateinit var firebaseUser: FirebaseUser
    private var checker = ""
    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageProfileRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        storageProfileRef = FirebaseStorage.getInstance().reference.child("Profile Pictures")


        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this@AccountSettingActivity, SignInActivity::class.java )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        binding.saveInfoProfileBtn.setOnClickListener {
            if(checker == "clicked") {
                uploadImageAndUpdateInfo()
            } else {
                updateUserInfoOnly()

            }
        }

        binding.textChangeImage.setOnClickListener {
            checker = "clicked"

            CropImage.activity()
                .setAspectRatio(1,1)
                .start(this@AccountSettingActivity)

        }

        binding.closeProfileBtn.setOnClickListener {
            onBackPressed()
        }

        userInfo()
    }

    private fun uploadImageAndUpdateInfo() {

        when {
            TextUtils.isEmpty(binding.inputUsernameProfileAccount.text.toString()) -> {
                Toast.makeText(this, "Username can't empty", Toast.LENGTH_LONG).show()
            }
            binding.inputFullnameProfileAccount.text.toString() == "" -> {
                Toast.makeText(this, "Fullname can't empty", Toast.LENGTH_LONG).show()
            }
            binding.btnBioProfileAccount.text.toString() == "" -> {
                Toast.makeText(this, "Bio can't empty", Toast.LENGTH_LONG).show()
            }
            imageUri == null -> {
                Toast.makeText(this, "Select Image", Toast.LENGTH_LONG).show()
            }
            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Account Settings")
                progressDialog.setMessage("Please wait")
                progressDialog.show()

                val fileRef = storageProfileRef!!.child(firebaseUser!!.uid + ".jpg")
                var uploadTask: StorageTask<*>

                uploadTask = fileRef.putFile(imageUri!!)
                uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if(!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                            progressDialog.dismiss()
                        }
                    }
                    return@Continuation fileRef.downloadUrl
                }).addOnCompleteListener (OnCompleteListener<Uri> { task ->
                    if(task.isSuccessful) {
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Users")

                        val userMap = HashMap<String, Any>()
                        userMap["username"] = binding.inputUsernameProfileAccount.text.toString().toLowerCase()
                        userMap["fullname"] = binding.inputFullnameProfileAccount.text.toString().toLowerCase()
                        userMap["bio"] = binding.btnBioProfileAccount.text.toString().toLowerCase()
                        userMap["image"] = myUrl

                        ref.child(firebaseUser.uid).updateChildren(userMap)

                        Toast.makeText(this, "Account information has been updated successfully", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@AccountSettingActivity, MainActivity::class.java )
                        startActivity(intent)
                        finish()
                        progressDialog.dismiss()

                    } else {
                        progressDialog.dismiss()
                    }
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK
            && data != null) {

            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            binding.imageProfileAccount.setImageURI(imageUri)
        }
    }

    private fun updateUserInfoOnly() {
        when {
            TextUtils.isEmpty(binding.inputUsernameProfileAccount.text.toString()) -> {
                Toast.makeText(this, "Username can't empty", Toast.LENGTH_LONG).show()
            }
            binding.inputFullnameProfileAccount.text.toString() == "" -> {
                Toast.makeText(this, "Fullname can't empty", Toast.LENGTH_LONG).show()
            }
            binding.btnBioProfileAccount.text.toString() == "" -> {
                Toast.makeText(this, "Bio can't empty", Toast.LENGTH_LONG).show()
            }
            else -> {
                val usersRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

                val userMap = HashMap<String, Any>()
                userMap["username"] = binding.inputUsernameProfileAccount.text.toString().toLowerCase()
                userMap["fullname"] = binding.inputFullnameProfileAccount.text.toString().toLowerCase()
                userMap["bio"] = binding.btnBioProfileAccount.text.toString().toLowerCase()

                usersRef.child(firebaseUser.uid).updateChildren(userMap)

                Toast.makeText(this, "Account information has been updated successfully", Toast.LENGTH_LONG).show()

                val intent = Intent(this@AccountSettingActivity, MainActivity::class.java )
                startActivity(intent)
                finish()

            }
        }

    }

    private fun userInfo() {
        val usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.uid)

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()) {
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(binding.imageProfileAccount)
                    binding.inputUsernameProfileAccount.setText(user!!.getUsername())
                    binding.inputFullnameProfileAccount.setText(user!!.getFullname())
                    binding.btnBioProfileAccount.setText(user!!.getBio())
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}