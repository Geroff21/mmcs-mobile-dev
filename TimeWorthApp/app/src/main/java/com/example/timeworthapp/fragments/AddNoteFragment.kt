package com.example.timeworthapp.fragments

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.widget.ImageView
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import com.example.timeworthapp.MainActivity
import com.example.timeworthapp.R
import com.example.timeworthapp.adapter.NoteAdapter
import com.example.timeworthapp.model.Note
import com.example.timeworthapp.databinding.FragmentAddNoteBinding
import com.example.timeworthapp.viewmodel.NoteViewModel
import java.io.ByteArrayOutputStream

class AddNoteFragment : Fragment(R.layout.fragment_add_note), MenuProvider {

    private var addNoteBinding: FragmentAddNoteBinding? = null
    private val binding get() = addNoteBinding!!

    private lateinit var notesViewModel: NoteViewModel
    private lateinit var addNoteView: View

    private lateinit var photoImageView: ImageView
    private var selectedPhotoUri: Uri? = null

    //Выбрать фото
    private val selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.data
            if (photoUri != null) {
                selectedPhotoUri = photoUri
                photoImageView.setImageURI(photoUri)
            } else {
                Toast.makeText(requireContext(), "Photo selection failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Photo selection failed", Toast.LENGTH_SHORT).show()
        }
    }

    //Сделать фото
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val tempUri = getImageUri(requireContext(), bitmap)
            selectedPhotoUri = tempUri
            photoImageView.setImageURI(tempUri)
        } else {
            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    //Разрешение на то, чтобы сделать фото
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePhotoLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        addNoteBinding = FragmentAddNoteBinding.inflate(inflater, container, false)

        val spinner: Spinner = binding.addNoteType
        ArrayAdapter.createFromResource(
            inflater.context,
            R.array.planets_array,
            R.layout.spinner
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        notesViewModel = (activity as MainActivity).noteViewModel
        addNoteView = view

        photoImageView = binding.photoImageView

        binding.selectPhotoButton.setOnClickListener {
            val selectPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectPhotoLauncher.launch(selectPhotoIntent)
        }

        binding.takePhotoButton.setOnClickListener {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.clearPhotoButton.setOnClickListener {
            selectedPhotoUri = null
            photoImageView.setImageURI(null)
        }
    }

    private fun saveNote(view: View) {
        val noteTitle = binding.addNoteTitle.text.toString().trim()
        val noteDesc = binding.addNoteDesc.text.toString().trim()
        val notePrice = binding.addNotePrice.text.toString().trim().toFloatOrNull()
        val noteType = binding.addNoteType.selectedItem.toString()

        if (noteTitle.isEmpty()) {
            Toast.makeText(addNoteView.context, "Please, enter title!", Toast.LENGTH_SHORT).show()
            return;
        }

        if (noteDesc.isEmpty()) {
            Toast.makeText(addNoteView.context, "Please, enter description!", Toast.LENGTH_SHORT).show()
            return;
        }

        if (notePrice == null) {
            Toast.makeText(addNoteView.context, "Please, enter price!", Toast.LENGTH_SHORT).show()
            return;
        }

        if (noteType.isEmpty()) {
            Toast.makeText(addNoteView.context, "Please, enter type!", Toast.LENGTH_SHORT).show()
            return;
        }

        val note = Note(
            id = 0,
            noteTitle,
            notePrice,
            noteType,
            noteDesc,
            selectedPhotoUri?.toString()
        )
        notesViewModel.addNote(note)

        Toast.makeText(addNoteView.context, "Item Saved", Toast.LENGTH_SHORT).show()
        view.findNavController().popBackStack(R.id.homeFragment, false)

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_add_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.saveMenu -> {
                saveNote(addNoteView)
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        addNoteBinding = null
    }

    private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }
}