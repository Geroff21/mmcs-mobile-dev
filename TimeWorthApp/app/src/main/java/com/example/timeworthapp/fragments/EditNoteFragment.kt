package com.example.timeworthapp.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.timeworthapp.MainActivity
import com.example.timeworthapp.R
import com.example.timeworthapp.databinding.FragmentEditNoteBinding
import com.example.timeworthapp.fragments.EditNoteFragmentArgs
import com.example.timeworthapp.model.Note
import com.example.timeworthapp.viewmodel.NoteViewModel
import java.io.ByteArrayOutputStream


class EditNoteFragment : Fragment(R.layout.fragment_edit_note), MenuProvider {

    private var editNoteBinding: FragmentEditNoteBinding? = null
    private val binding get() = editNoteBinding!!

    private val REQUEST_IMAGE_CAPTURE = 1

    private lateinit var notesViewModel : NoteViewModel
    private lateinit var currentNote: Note

    private lateinit var photoImageView: ImageView
    private var selectedPhotoUri: Uri? = null

    //Выбрать фото
    private val selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.data
            if (photoUri != null) {
                selectedPhotoUri = photoUri
                binding.photoImageView.setImageURI(photoUri)
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
            binding.photoImageView.setImageURI(tempUri)
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

    private val args: EditNoteFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        editNoteBinding = FragmentEditNoteBinding.inflate(inflater, container, false)
        val v : View = inflater.inflate(R.layout.fragment_edit_note, container, false)

        val spinner: Spinner = binding.editNoteType
        ArrayAdapter.createFromResource(
            v.getContext(),
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
        currentNote = args.note!!

        binding.editNoteTitle.setText(currentNote.noteTitle)
        binding.editNoteDesc.setText(currentNote.noteDesc)
        binding.editNotePrice.setText(currentNote.notePrice.toString())

        val pos = if (currentNote.noteType == "Расход") 1 else 0
        binding.editNoteType.setSelection(pos)

        // Загрузка изображения, если оно сохранено
        if (!currentNote.notePhotoUri.isNullOrEmpty()) {
            binding.photoImageView.setImageURI(Uri.parse(currentNote.notePhotoUri))

            selectedPhotoUri = Uri.parse(currentNote.notePhotoUri)

            binding.clearPhotoButton.setOnClickListener {
                selectedPhotoUri = null
                binding.photoImageView.setImageURI(null)
            }

        }

        binding.selectPhotoButton.setOnClickListener {
            val selectPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectPhotoLauncher.launch(selectPhotoIntent)
        }

        binding.takePhotoButton.setOnClickListener {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.editNoteFab.setOnClickListener {
            val noteTitle = binding.editNoteTitle.text.toString().trim()
            val notePrice = binding.editNotePrice.text.toString()
            val noteType = binding.editNoteType.getSelectedItem().toString()
            val noteDesc = binding.editNoteDesc.text.toString().trim()

            if (noteTitle.isNotEmpty()) {
                if (noteDesc.isNotEmpty()) {
                    if (notePrice != null) {
                        if (noteType.isNotEmpty()) {
                            val note = Note(currentNote.id, noteTitle, notePrice.toFloat(), noteType, noteDesc, selectedPhotoUri?.toString())
                            notesViewModel.updateNote(note)
                            view.findNavController().popBackStack(R.id.homeFragment, false)
                        }
                        else {
                            Toast.makeText(context, "Please, enter type!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else {
                        Toast.makeText(context, "Please, enter price!", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(context, "Please, enter description!", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(context, "Please, enter title!", Toast.LENGTH_SHORT).show()
            }

        }
    }
    private fun deleteNote() {

        val builder = AlertDialog.Builder(activity, R.style.AlertDialogTheme).apply {
            setTitle("Delete Note")
            setMessage("Do you want to delete this note?")
            setPositiveButton("Delete"){_,_ ->
                notesViewModel.deleteNote(currentNote)
                Toast.makeText(context, " Note Deleted ", Toast.LENGTH_SHORT).show()
                view?.findNavController()?.popBackStack(R.id.homeFragment, false)

            }
            setNegativeButton("Cancel", null)

        }.create().show()

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_edit_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.deleteMenu -> {
                deleteNote()
                true
            } else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editNoteBinding = null
    }

    private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

}
