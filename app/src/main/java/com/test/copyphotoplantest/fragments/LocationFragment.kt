package com.test.copyphotoplantest.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.navigation.Navigation.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.test.copyphotoplantest.Folders
import com.test.copyphotoplantest.Place
import com.test.copyphotoplantest.R

class LocationFragment : Fragment() {

    lateinit var firestore:FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_location, container, false)
        requireActivity().findViewById<Button>(R.id.back_button).visibility = View.GONE
        return root
    }

    override  fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        updateInterface()
    }

    fun updateInterface(){
        val buttonAddFolder = requireView().findViewById<Button>(R.id.button_add_folder)
        buttonAddFolder.setOnClickListener {
            val collectionFolders = firestore.collection("folders")

            collectionFolders.add(mapOf(
                    "nameFolder" to "NewFolder"
            )).addOnCompleteListener { parents ->
                collectionFolders.document(parents.result!!.id).collection("places").add(mapOf(
                        "namePlace" to "NewPlace",
                        "linksPhoto" to ArrayList<String>()
                )).addOnSuccessListener { updateInterface() }
            }
        }
        val layoutButtonOpenFolder = view?.findViewById<LinearLayout>(R.id.layoutButtonOpenFolder)
        layoutButtonOpenFolder?.removeAllViews()
        firestore.collection("folders").get()
                .addOnSuccessListener { documents ->
                    val inflater = LayoutInflater.from(requireContext())
                    for (document in documents) {
                        val obj = document.toObject(Folders::class.java)
                        Log.d("firestore", "${document.id} => ${obj.nameFolder}")

                        val linearButtonOpenFolder = inflater.inflate(R.layout.button_open_folder, null)  as LinearLayoutCompat
                        val buttonOpenFolder = linearButtonOpenFolder.findViewById<Button>(R.id.button_open_folder)
                        buttonOpenFolder.text = obj.nameFolder
                        buttonOpenFolder.setOnClickListener {
                            val bundle = Bundle()
                            bundle.putString("nameFolder",obj.nameFolder)
                            bundle.putString("idDocument", document.id)
                            findNavController(requireActivity(), R.id.nav_host_fragment)
                                    .navigate(R.id.action_locationFragment_to_openesFolderLocationFragment, bundle)
                            requireActivity().findViewById<Button>(R.id.back_button).visibility = View.VISIBLE
                        }
                        //дилог для удаления папки
                        buttonOpenFolder.setOnLongClickListener {
                            val dialog = Dialog(requireContext())

                            val linearChangeName = inflater.inflate(R.layout.alert_dialog_change_name, null)  as LinearLayoutCompat

                            val editTextName = linearChangeName.findViewById<EditText>(R.id.editTextName)
                            editTextName.setText(obj.nameFolder, TextView.BufferType.EDITABLE)

                            val button_save = linearChangeName.findViewById<Button>(R.id.save_new_name)
                            button_save.setOnClickListener {
                                val collectionFolders = firestore.collection("folders")
                                collectionFolders.document(document.id).set(mapOf(
                                        "nameFolder" to editTextName.text.toString()
                                )).addOnSuccessListener {
                                    updateInterface()
                                }
                                dialog.cancel()
                            }

                            val buttonClose = linearChangeName.findViewById<Button>(R.id.close_dialog)
                            buttonClose.setOnClickListener {
                                dialog.cancel()
                            }
                            val buttonDelete = linearChangeName.findViewById<Button>(R.id.delet_folder)
                            buttonDelete.setOnClickListener {
                                val collectionFolders = firestore.collection("folders")
                                firestore.collection("folders").document(document.id).collection("places").get().addOnSuccessListener { documentss ->
                                    val fireStorage = FirebaseStorage.getInstance()
                                    for (document1 in documentss){
                                        val obj1 = document1.toObject(Place::class.java)

                                        obj1.linksPhoto.forEach {
                                            val photoRef: StorageReference = fireStorage.getReferenceFromUrl(it)
                                            photoRef.delete().addOnSuccessListener {
                                                Log.d("delet", "Delet complete")

                                            }
                                        }
                                        collectionFolders.document(document.id).collection("places").document(document1.id).delete().addOnSuccessListener {
                                            collectionFolders.document(document.id).delete().addOnSuccessListener {
                                                updateInterface()
                                            }
                                        }
                                    }

                                }
                                dialog.cancel()
                            }
                            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.setContentView(linearChangeName)
                            dialog.create()
                            dialog.show()
                            true
                        }
                        layoutButtonOpenFolder?.addView(linearButtonOpenFolder)
                    }
                }.addOnFailureListener { exception ->
                    Log.w("firestore", "Error getting documents: ", exception)
                }
    }




}