package com.kandy.qrwhatsappcontact
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 23
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scanContact: Button = findViewById(R.id.scan_contact_btn)
        val selectContact: Button = findViewById(R.id.select_contact_btn)
        val requestPermission: Button = findViewById(R.id.request_permissions_btn)

        scanContact.setOnClickListener {
            val scanIntent = Intent(this, ScanContactDetails::class.java)
            startActivity(scanIntent)
        }

        requestPermission.setOnClickListener {
            setupPermissions()
        }

        selectContact.setOnClickListener {
            val selectContactIntent = Intent(this, SelectContact::class.java)
            startActivity(selectContactIntent)
        }
    }

    private fun setupPermissions() {
        val neededPermissions = mutableListOf<String>()
        val permissionsToCheck = listOf(android.Manifest.permission.CAMERA,android.Manifest.permission.READ_CONTACTS,android.Manifest.permission.WRITE_CONTACTS,android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.READ_EXTERNAL_STORAGE)

        permissionsToCheck.forEach {
              if(ContextCompat.checkSelfPermission(this,it) != PackageManager.PERMISSION_GRANTED) {
                  neededPermissions.add(it)
              }
        }

        if(neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this,
            neededPermissions.toTypedArray(),PERMISSIONS_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_REQUEST_CODE) {
            val permissionResults = HashMap<String, Int>()
            var deniedCount = 0

            for(i in grantResults.indices) {
                if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults[permissions[i]] = grantResults[i]
                    deniedCount++
                }
            }

            if(deniedCount==0) {
                return
            }

            else {
                for(entry in permissionResults.entries) {
                    val permName = entry.key
                    val permResult = entry.value

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,permName)) {
                        showPermissionDialog("","This app needs Camera, Contacts and External Storage permissions to work without problems","Yes, Grant Permissions",
                        DialogInterface.OnClickListener {
                            dialog, _ ->
                                dialog.dismiss()
                                setupPermissions()
                        },"No, Exit App",
                        DialogInterface.OnClickListener{
                            dialog, _ ->
                                dialog.dismiss()
                                finish()
                        },false)
                    }

                    else {
                        showPermissionDialog("",
                            "You have denied some permissions. Allow all permissions in Settings",
                            "Go to Settings",
                            DialogInterface.OnClickListener {
                                dialog, _ ->
                                dialog.dismiss()

                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package",packageName,null))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            },
                            "No, Exit App",
                            DialogInterface.OnClickListener {
                                dialog, _ ->
                                dialog.dismiss()
                                finish()
                            },
                            false
                        )
                        break
                    }
                }
            }
        }
    }

    private fun showPermissionDialog(title:String, msg:String, positiveLabel:String, positiveOnClick:DialogInterface.OnClickListener, negativeLabel:String, negativeOnClick:DialogInterface.OnClickListener, isCancelAble:Boolean): AlertDialog{
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setCancelable(isCancelAble)
        builder.setMessage(msg)
        builder.setPositiveButton(positiveLabel, positiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)

        val alert = builder.create()
        alert.show()
        return alert
    }
}