package com.test.copyphotoplantest.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.test.copyphotoplantest.objectClass.Place
import com.test.copyphotoplantest.R
import java.io.ByteArrayOutputStream
import kotlin.collections.ArrayList


class OpenedFolderLocationFragment : Fragment() {

    lateinit var firestore:FirebaseFirestore
    lateinit var fireStorage:FirebaseStorage

    lateinit var buffDocumentIdInner:String
    lateinit var buffDocumentIdOuter:String
    lateinit var buffNamePlace:String
    var buffArrayPhoto:ArrayList<String> = arrayListOf()

    lateinit var buffLinearLayout: LinearLayoutCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        val root = inflater.inflate(R.layout.fragment_openes_folder_location, container, false)

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            //загрузка фотки
            if (requestCode == 1 && data != null) {
                val imageUri = data.getData();
                Log.d("firestore", data.getData().toString())
                val imageView = ImageView(requireContext())
                imageView.setImageURI(data.data)
                val bitMap = (imageView.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitMap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                val byteArrayImage = baos.toByteArray()
                val storageReferense = fireStorage.reference.child(System.currentTimeMillis().toString() + "image")
                val uploadTask = storageReferense.putBytes(byteArrayImage)

                lateinit var uploadLink:String
                uploadTask.continueWithTask {
                    storageReferense.downloadUrl
                }.addOnCompleteListener {
                    uploadLink = it.result.toString()
                    buffArrayPhoto.add(uploadLink)

                    firestore.collection("folders").document(requireArguments().getString("idDocument")!!)
                            .collection("places").document(buffDocumentIdInner).set(mapOf(
                                    "namePlace" to buffNamePlace,
                                    "linksPhoto" to buffArrayPhoto
                            )).addOnSuccessListener {
                                updateInterface()
                            }
                }
            }
    }

    override  fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val tVNameFolder = requireActivity().findViewById<TextView>(R.id.nameFolder)
        tVNameFolder.text = arguments?.getString("nameFolder")
        buffDocumentIdOuter = arguments?.getString("idDocument")!!
        firestore = FirebaseFirestore.getInstance()
        fireStorage = FirebaseStorage.getInstance()
        updateInterface()
    }

    fun updateInterface() {
        Log.d("updateInterface", "updateInterface")
        //кнопка добавления новой локации
        val buttonAddLocation = view?.findViewById<Button>(R.id.buttonAddLocatin)
        buttonAddLocation?.setOnClickListener {
            firestore.collection("folders").document(requireArguments().getString("idDocument")!!)
                    .collection("places").document().set(mapOf(
                            "namePlace" to "new Place",
                            "linksPhoto" to arrayListOf<String>()
                    )).addOnSuccessListener {
                        updateInterface()
                    }
        }
        //linerlayout для добавления интрефайса локации
        val layoutOpenedFolder = view?.findViewById<LinearLayout>(R.id.scrollLinerLayout)
        layoutOpenedFolder!!.removeAllViews()
        //firestore считывания локаций
        firestore.collection("folders").document(buffDocumentIdOuter).collection("places").get()
                .addOnSuccessListener { documents ->
                    val inflater = LayoutInflater.from(requireContext())
                    for (document in documents) {
                        val obj = document.toObject(Place::class.java)
                        val linearButtonOpenFolder = inflater.inflate(R.layout.menu_place, null) as LinearLayoutCompat
                        val textViewLocation = linearButtonOpenFolder.findViewById<TextView>(R.id.namePlace)
                        textViewLocation.text = obj.namePlace
                        //диалог для изменния названия локации и удаления локации
                        textViewLocation.setOnClickListener {
                            val dialog = Dialog(requireContext())
                            val linearChangeName = inflater.inflate(R.layout.alert_dialog_change_name, null) as LinearLayoutCompat

                            val editTextName = linearChangeName.findViewById<EditText>(R.id.editTextName)
                            editTextName.setText(obj.namePlace, TextView.BufferType.EDITABLE)

                            val button_save = linearChangeName.findViewById<Button>(R.id.save_new_name)
                            button_save.setOnClickListener {
                                firestore.collection("folders").document(requireArguments().getString("idDocument")!!)
                                        .collection("places").document(document.id).set(mapOf(
                                                "namePlace" to editTextName.text.toString(),
                                                "linksPhoto" to obj.linksPhoto
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
                                val collectionFolders = firestore.collection("folders").document(requireArguments().getString("idDocument")!!).collection("places")
                                collectionFolders.document(document.id).delete().addOnSuccessListener {
                                    updateInterface()
                                }
                                obj.linksPhoto.forEach {
                                    val photoRef: StorageReference = fireStorage.getReferenceFromUrl(it)
                                    photoRef.delete().addOnSuccessListener {
                                        Log.d("delet", "Delet complete")
                                    }
                                }
                                dialog.cancel()
                            }
                            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.setContentView(linearChangeName)
                            dialog.create()
                            dialog.show()
                        }
                        //загрузка фотки
                        linearButtonOpenFolder.findViewById<Button>(R.id.buttonAddPhoto).setOnClickListener {
                            buffNamePlace = obj.namePlace
                            buffArrayPhoto = obj.linksPhoto
                            buffDocumentIdInner = document.id
                            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                            startActivityForResult(intent, 1)
                        }
                        val buttonDeletPhoto = linearButtonOpenFolder.findViewById<Button>(R.id.delet_photo)
                        //описания логики для отображения фотографий
                        var countPhoto = 1
                        var isLast = false
                        val arrayCheckBox = arrayListOf<CheckBox>()
                        for (i in 0..obj.linksPhoto.size - 1) {
                            if (countPhoto == 1) {
                                countPhoto++
                                isLast = false

                                buffLinearLayout = inflater.inflate(R.layout.photo_place, null) as LinearLayoutCompat

                                val cardView1 = buffLinearLayout.findViewById<CardView>(R.id.cardView1)
                                cardView1.visibility = View.VISIBLE

                                val image1 = buffLinearLayout.findViewById<ImageView>(R.id.imageView1)
                                Picasso.get().load(obj.linksPhoto[i]).centerCrop().resize(720, 1280).into(image1)
                                image1.setOnVeryLongClickListener {
                                    for (checkBox in arrayCheckBox) {
                                        checkBox.visibility = View.VISIBLE
                                    }
                                    buttonDeletPhoto.visibility = View.VISIBLE
                                }

                                val checkBox1 = buffLinearLayout.findViewById<CheckBox>(R.id.checkBox1)
                                arrayCheckBox.add(checkBox1)
                            } else if (countPhoto == 2) {
                                countPhoto++

                                val cardView2 = buffLinearLayout.findViewById<CardView>(R.id.cardView2)
                                cardView2.visibility = View.VISIBLE

                                val image2 = buffLinearLayout.findViewById<ImageView>(R.id.imageView2)
                                Picasso.get().load(obj.linksPhoto[i]).centerCrop().resize(720, 1280).into(image2)
                                image2.setOnVeryLongClickListener {
                                    for (checkBox in arrayCheckBox) {
                                        checkBox.visibility = View.VISIBLE
                                    }
                                    buttonDeletPhoto.visibility = View.VISIBLE
                                }

                                val checkBox2 = buffLinearLayout.findViewById<CheckBox>(R.id.checkBox2)
                                arrayCheckBox.add(checkBox2)
                            } else if (countPhoto == 3) {
                                isLast = true
                                countPhoto = 1

                                val cardView3 = buffLinearLayout.findViewById<CardView>(R.id.cardView3)
                                cardView3.visibility = View.VISIBLE

                                val image3 = buffLinearLayout.findViewById<ImageView>(R.id.imageView3)
                                Picasso.get().load(obj.linksPhoto[i]).centerCrop().resize(720, 1280).into(image3)
                                image3.setOnVeryLongClickListener {
                                    for (checkBox in arrayCheckBox) {
                                        checkBox.visibility = View.VISIBLE
                                    }
                                    buttonDeletPhoto.visibility = View.VISIBLE
                                }

                                val checkBox3 = buffLinearLayout.findViewById<CheckBox>(R.id.checkBox3)
                                arrayCheckBox.add(checkBox3)
                                linearButtonOpenFolder.findViewById<LinearLayout>(R.id.linerLayoutForPhoto).addView(buffLinearLayout)
                            }
                        }
                        //кнопка для удаления фотографий
                        buttonDeletPhoto.setOnClickListener {
                            val listDeletePhoto = arrayListOf<Int>()
                            for (i in 0 until arrayCheckBox.size) {
                                if (arrayCheckBox[i].isChecked) {
                                    listDeletePhoto.add(i)
                                }
                            }
                            val linksPhotoStorage = arrayListOf<String>()
                            for (i in listDeletePhoto.size - 1 downTo 0 step 1) {
                                for (y in 0..obj.linksPhoto.size - 1) {
                                    if (listDeletePhoto[i] == y) {
                                        linksPhotoStorage.add(obj.linksPhoto[y])
                                        obj.linksPhoto.removeAt(y)
                                    }
                                }
                            }

                            firestore.collection("folders").document(requireArguments().getString("idDocument")!!)
                                    .collection("places").document(document.id).set(mapOf(
                                            "namePlace" to obj.namePlace,
                                            "linksPhoto" to obj.linksPhoto
                                    )).addOnSuccessListener { updateInterface() }

                            linksPhotoStorage.forEach {
                                val photoRef: StorageReference = fireStorage.getReferenceFromUrl(it)
                                photoRef.delete().addOnSuccessListener {
                                    Log.d("delet", "Delet complete")

                                }
                            }
                        }
                        //часть логики для отображения фотографий, сробатывает если меньше 3 фоток или всего 1 фотка
                        if (!isLast && 0 < obj.linksPhoto.size - 1 || obj.linksPhoto.size == 1) {
                            linearButtonOpenFolder.findViewById<LinearLayout>(R.id.linerLayoutForPhoto)
                                    .addView(buffLinearLayout)
                        }
                        layoutOpenedFolder.addView(linearButtonOpenFolder)
                    }
                }
    }

    //продолжительное нажатие длиной 3 сек
    fun View.setOnVeryLongClickListener(listener: () -> Unit) {
        setOnTouchListener(object : View.OnTouchListener {

            private val longClickDuration = 3000L
            private val handler = Handler()

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    handler.postDelayed({ listener.invoke() }, longClickDuration)
                } else if (event?.action == MotionEvent.ACTION_UP) {
                    handler.removeCallbacksAndMessages(null)
                }
                return true
            }
        })
    }

}