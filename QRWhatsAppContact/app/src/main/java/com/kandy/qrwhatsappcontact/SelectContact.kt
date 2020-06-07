package com.kandy.qrwhatsappcontact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.FileProvider.getUriForFile
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

class SelectContact: AppCompatActivity() {
    private val SELECT_PHONE_NUMBER = 1
    private lateinit var contactState:TextView
    private lateinit var qrContact:ImageView
    private lateinit var shareContact: Button
    private lateinit var saveQrContact:Button
    private var qrCreated = false
    private var tempImagePath=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_contact)

        contactState = findViewById(R.id.contactState_txt_view)
        qrContact = findViewById(R.id.qr_contact_img_view)
        shareContact = findViewById(R.id.share_qr_btn)
        saveQrContact = findViewById(R.id.save_qr_contact_btn)

        val contactPickerIntent = Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI)
        contactPickerIntent.type = ContactsContract.Contacts.CONTENT_TYPE
        startActivityForResult(contactPickerIntent,SELECT_PHONE_NUMBER)

        shareContact.setOnClickListener {
            if(!qrCreated) {
                Toast.makeText(this,"No QR Contact has been created",Toast.LENGTH_LONG).show()
            } else {
                val imageFile = File(tempImagePath)
                val imageUri = getUriForFile(this,"com.kandy.qrwhatsappcontact.fileprovider",imageFile)
                val sendContactIntent = Intent().apply {
                    this.action = Intent.ACTION_SEND
                    this.putExtra(Intent.EXTRA_STREAM, imageUri)
                    this.type = "image/png"
                }
                val shareIntent = Intent.createChooser(sendContactIntent,null)
                startActivity(shareIntent)
            }
        }

        saveQrContact.setOnClickListener{
            val qrImage = File(tempImagePath)
            val imageBytes = qrImage.readBytes()

            val filePath = "${Environment.getExternalStorageDirectory().path}/Download"
            val externalImageFile = File(filePath ,"temp.png")

            Log.d("External File Path:",externalImageFile.absolutePath)
            FileOutputStream(externalImageFile).use {
                it.write(imageBytes)
            }
            Toast.makeText(this,"Qr Contact has been saved in Downloads",Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        contactState.text = "Reading Contact Details"
        var number = "";
        var name = "";
        var email = "";
        qrCreated = false;
        if(requestCode == SELECT_PHONE_NUMBER && resultCode == Activity.RESULT_OK) {
            val contactUri: Uri = data?.data ?: return
            val c: Cursor? = contentResolver.query(contactUri,null,null,null,null)
            c.let { it ->
                if(it != null) {
                    it.moveToFirst()
                    val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))

                    val phoneDataCursor: Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null)
                    val emailDataCursor: Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null)

                    phoneDataCursor.let {
                        if(it != null) {
                            it.moveToFirst()
                            number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                            it.close()
                        }
                    }

                    phoneDataCursor?.close()

                    emailDataCursor.let {
                        if(it != null) {
                            it.moveToFirst()
                            email = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        }
                    }
                    emailDataCursor?.close()
                }
            }
            c?.close()
        }

        if(number.isEmpty()) {
            contactState.text = "No Contacts Found"
        } else {
            val content = "$name;$number;$email"
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 256, 256)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            qrContact.setImageBitmap(bitmap)
            qrCreated = true

            tempImagePath = filesDir.absolutePath + "/temp.png"
            Log.d("Image Path:",tempImagePath)
            val qrImage = File(tempImagePath)
            val fOut = FileOutputStream(qrImage)
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
            fOut.flush()
            fOut.close()
        }
    }
}