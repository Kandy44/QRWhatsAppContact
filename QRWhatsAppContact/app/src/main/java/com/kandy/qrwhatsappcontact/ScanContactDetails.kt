package com.kandy.qrwhatsappcontact

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.android.synthetic.main.scan_contact_details.*

class ScanContactDetails: AppCompatActivity() {
    lateinit var captureManager: CaptureManager
    var torchState: Boolean = false
    private val IMAGE_PICK_CODE = 2
    private val ADD_CONTACT_CODE = 11
    private var scannedData = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_contact_details)

        val scanButton: Button = findViewById(R.id.btnScan)
        val textResult: TextView = findViewById(R.id.txtResult)
        val barcodeView: DecoratedBarcodeView = findViewById(R.id.barcodeView)
        val torchButton: Button = findViewById(R.id.btnTorch)
        val scanImage:Button = findViewById(R.id.scan_image_btn)

        captureManager = CaptureManager(this,barcodeView)
        captureManager.initializeFromIntent(intent, savedInstanceState)

        scanButton.setOnClickListener {
            textResult.text = "Scanning..."
            barcodeView.decodeSingle(object: BarcodeCallback{
                override fun barcodeResult(result: BarcodeResult?) {
                    result?.let {
                        scannedData = it.text
                        txtResult.text = it.text
                        saveContact()
                    }
                }

                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

                }
            })
        }

        torchButton.setOnClickListener {
            if(torchState) {
                torchState = false
                barcodeView.setTorchOff()
            } else {
                torchState = true
                barcodeView.setTorchOn()
            }
        }

        scanImage.setOnClickListener {
            selectImage()
        }
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                decodeQRCodeImage(it.data)
            }
        } else if(requestCode == ADD_CONTACT_CODE) {
            if(resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this,"Cancelled Added Contact",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeQRCodeImage(imageUri: Uri?){
        val qrCodeReader = QRCodeReader()

        val bitmapImage = MediaStore.Images.Media.getBitmap(this.contentResolver,imageUri)
        val pixels = IntArray(bitmapImage.width*bitmapImage.height)
        bitmapImage.getPixels(pixels, 0, bitmapImage.width, 0, 0, bitmapImage.width,bitmapImage.height)

        val rawResult = qrCodeReader.decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmapImage.width,bitmapImage.height,pixels))))
        scannedData = rawResult.toString()
        Log.d("Decoded Data", rawResult.toString())
        saveContact()
    }

    fun saveContact() {
        val data = scannedData.split(";")
        val name = data[0]
        val number = data[1]
        val email = data[2]

        Log.d("Data in save contact:","$name;$number;$email")

        val saveContactIntent = Intent(ContactsContract.Intents.Insert.ACTION)
        saveContactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
        saveContactIntent.putExtra(ContactsContract.Intents.Insert.NAME,name)
        saveContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE,number)
        saveContactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL,email)
        startActivityForResult(saveContactIntent,ADD_CONTACT_CODE)
    }
}