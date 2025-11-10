package com.example.mycontact

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mycontact.data.AppDatabase
import com.example.mycontact.data.Contact
import com.example.mycontact.databinding.ActivityMainBinding
import com.example.mycontact.databinding.FormContactBinding
import com.example.mycontact.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var contactViewAdapter: ContactViewAdapter

    private val contactDao by lazy { AppDatabase.get(this).contactDao() }

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactViewAdapter = ContactViewAdapter(
            onEdit = { contact -> showEditDialog(contact) },
            onDelete = {contact -> showConfirmDelete(contact)}
        )

        with(binding) {
            rvContact.apply {
                adapter = contactViewAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }

            btnAdd.setOnClickListener {
                showAddDialog()

            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    fun refreshList() {
        lifecycleScope.launch {
            var items = withContext(Dispatchers.IO) { contactDao.getAll() }
            contactViewAdapter.setItems(items)
        }
    }

    private fun showAddDialog() {
        var binding = FormContactBinding.inflate(layoutInflater)
        var builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Contact")
        builder.setView(binding.root)

        builder.setPositiveButton("Save") { dialog, which ->
            var name = binding.edtName.text.toString().trim()
            var phone = binding.edtPhone.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    contactDao.insert(Contact(name = name, phone = phone))
                    withContext(Dispatchers.Main) { refreshList() }
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Nama dan Nomor Telepon harus diisi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.setNeutralButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        builder.show()
    }

    private fun showEditDialog(contact: Contact){
        var binding = FormContactBinding.inflate(layoutInflater)

        // isi form dengan data existing contact
        binding.edtName.setText(contact.name)
        binding.edtPhone.setText(contact.phone)

        var builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Edit Contact")
        builder.setView(binding.root)

        builder.setPositiveButton("Save") { dialog, which ->
            // ambil name dan phone dari form
            var name = binding.edtName.text.toString().trim()
            var phone = binding.edtPhone.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    // update ke dabase
                    contactDao.update(contact.copy(name = name, phone = phone))

                    // minta UI untuk refresh data
                    withContext(Dispatchers.Main) { refreshList() }
                }

                dialog.dismiss()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Nama dan Phone harus diisi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        // dialog di show
        val dialog = builder.create()
        dialog.show()
    }

    private fun showConfirmDelete(contact: Contact) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Delete ${contact.name}?")

        builder.setPositiveButton("Delete") { dialog, which ->
            lifecycleScope.launch(Dispatchers.IO) {
                contactDao.delete(contact)
                withContext(Dispatchers.Main) { refreshList() }
            }
            dialog.dismiss()
        }

        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}